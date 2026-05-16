package com.kfc.kfc_backend.service;

import com.kfc.kfc_backend.entity.UserAccount;
import com.kfc.kfc_backend.repository.UserAccountRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {
    @Autowired
    private UserAccountRepository userAccountRepository;

    @PostConstruct
    public void initDefaultUser() {
        userAccountRepository.findByUsername("admin").orElseGet(() -> {
            UserAccount user = new UserAccount();
            user.setUsername("admin");
            user.setPassword("admin123");
            user.setDisplayName("Store Manager");
            user.setRole("Administrator");
            user.setPhone("13800000000");
            user.setEmail("manager@kfc.local");
            user.setStoreName("KFC Smart Store");
            user.setBio("Responsible for inventory, finance, HR, and operations analysis.");
            return userAccountRepository.save(user);
        });
    }

    public Map<String, Object> login(String username, String password) {
        UserAccount user = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));
        if (!user.getPassword().equals(password)) {
            throw new RuntimeException("Invalid username or password");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("token", "demo-token-" + user.getId() + "-" + Instant.now().toEpochMilli());
        result.put("profile", toProfile(user));
        return result;
    }

    public Map<String, Object> getProfile(String username) {
        UserAccount user = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User does not exist"));
        return toProfile(user);
    }

    public Map<String, Object> updateProfile(String username, Map<String, String> payload) {
        UserAccount user = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User does not exist"));
        user.setDisplayName(payload.getOrDefault("displayName", user.getDisplayName()));
        user.setRole(payload.getOrDefault("role", user.getRole()));
        user.setPhone(payload.getOrDefault("phone", user.getPhone()));
        user.setEmail(payload.getOrDefault("email", user.getEmail()));
        user.setAvatar(payload.getOrDefault("avatar", user.getAvatar()));
        user.setStoreName(payload.getOrDefault("storeName", user.getStoreName()));
        user.setBio(payload.getOrDefault("bio", user.getBio()));
        if (payload.containsKey("password") && payload.get("password") != null && !payload.get("password").isBlank()) {
            user.setPassword(payload.get("password"));
        }
        return toProfile(userAccountRepository.save(user));
    }

    private Map<String, Object> toProfile(UserAccount user) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("displayName", user.getDisplayName());
        profile.put("role", user.getRole());
        profile.put("phone", user.getPhone());
        profile.put("email", user.getEmail());
        profile.put("avatar", user.getAvatar());
        profile.put("storeName", user.getStoreName());
        profile.put("bio", user.getBio());
        return profile;
    }
}
