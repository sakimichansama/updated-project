package com.kfc.kfc_backend.controller;

import com.kfc.kfc_backend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*", "http://64.90.16.27:*"})
public class AuthController {
    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        return authService.login(body.get("username"), body.get("password"));
    }

    @GetMapping("/profile")
    public Map<String, Object> profile(@RequestParam(defaultValue = "admin") String username) {
        return authService.getProfile(username);
    }

    @PutMapping("/profile")
    public Map<String, Object> updateProfile(@RequestParam(defaultValue = "admin") String username,
                                             @RequestBody Map<String, String> body) {
        return authService.updateProfile(username, body);
    }
}

