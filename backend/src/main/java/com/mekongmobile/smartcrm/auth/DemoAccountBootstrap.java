package com.mekongmobile.smartcrm.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoAccountBootstrap implements ApplicationRunner {
    private final UserRepository users;
    private final boolean enabled;
    private final String password;

    public DemoAccountBootstrap(UserRepository users,
            @Value("${app.security.bootstrap-enabled:false}") boolean enabled,
            @Value("${app.security.bootstrap-password:}") String password) {
        this.users = users;
        this.enabled = enabled;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) return;
        if (password.length() < 12 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("BOOTSTRAP_PASSWORD must contain 12–72 bytes");
        }
        String hash = new BCryptPasswordEncoder().encode(password);
        users.createIfAbsent("marketing", "MARKETING", null, hash);
        users.createIfAbsent("director", "BAN_GIAM_DOC", null, hash);
        for (Long centerId : users.centerIds()) {
            users.createIfAbsent("manager" + centerId, "QL_TRUNG_TAM", centerId, hash);
        }
    }

}
