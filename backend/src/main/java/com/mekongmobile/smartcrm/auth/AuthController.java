package com.mekongmobile.smartcrm.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest request) {
        return auth.login(request.username(), request.password());
    }

    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest request) {
        return AuthService.userFields(auth.require(request.getHeader("Authorization")));
    }

    public record LoginRequest(String username, String password) { }
}
