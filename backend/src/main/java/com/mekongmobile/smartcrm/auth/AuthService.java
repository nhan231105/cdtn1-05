package com.mekongmobile.smartcrm.auth;

import com.mekongmobile.smartcrm.reporting.ApiException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AuthService {
    private static final String ISSUER = "smartcrm-l8";
    private static final long TTL_SECONDS = 3600;
    private final UserRepository users;
    private final byte[] secret;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();
    private final String dummyHash = passwords.encode("not-an-account-password");

    public AuthService(UserRepository users, @Value("${app.security.jwt-secret}") String secret) {
        this.users = users;
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public Map<String, Object> login(String username, String password) {
        if (username == null || username.isBlank() || username.length() > 100
                || password == null || password.isBlank() || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw ApiException.unauthenticated();
        }
        var account = users.findEnabled(username);
        String hash = account.isEmpty() ? dummyHash : account.get().passwordHash();
        boolean matches = passwords.matches(password, hash);
        if (account.isEmpty() || !matches) throw ApiException.unauthenticated();
        UserIdentity user = account.get().identity();
        Instant now = Instant.now();
        var claims = new JWTClaimsSet.Builder().issuer(ISSUER).subject(user.username())
                .issueTime(Date.from(now)).expirationTime(Date.from(now.plusSeconds(TTL_SECONDS)))
                .claim("role", user.role()).claim("center_id", user.centerId()).build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        try {
            jwt.sign(new MACSigner(secret));
        } catch (JOSEException exception) {
            throw new IllegalStateException("Cannot sign access token", exception);
        }
        var result = userFields(user);
        result.put("access_token", jwt.serialize());
        result.put("token_type", "Bearer");
        result.put("expires_in", TTL_SECONDS);
        return result;
    }

    public UserIdentity require(String authorization, String... roles) {
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.length() > 4096) {
            throw ApiException.unauthenticated();
        }
        try {
            SignedJWT jwt = SignedJWT.parse(authorization.substring(7));
            if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm()) || !jwt.verify(new MACVerifier(secret))) {
                throw ApiException.unauthenticated();
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            if (!ISSUER.equals(claims.getIssuer()) || claims.getExpirationTime() == null
                    || !claims.getExpirationTime().toInstant().isAfter(Instant.now()) || claims.getSubject() == null) {
                throw ApiException.unauthenticated();
            }
            UserIdentity user = users.findEnabled(claims.getSubject()).orElseThrow(ApiException::unauthenticated).identity();
            if (!user.role().equals(claims.getStringClaim("role"))
                    || !java.util.Objects.equals(user.centerId(), claims.getLongClaim("center_id"))) {
                throw ApiException.unauthenticated();
            }
            if (roles.length > 0 && Arrays.stream(roles).noneMatch(user.role()::equals)) throw ApiException.forbidden();
            return user;
        } catch (ParseException | JOSEException exception) {
            throw ApiException.unauthenticated();
        }
    }

    public static LinkedHashMap<String, Object> userFields(UserIdentity user) {
        var fields = new LinkedHashMap<String, Object>();
        fields.put("username", user.username());
        fields.put("role", user.role());
        fields.put("center_id", user.centerId());
        return fields;
    }

}
