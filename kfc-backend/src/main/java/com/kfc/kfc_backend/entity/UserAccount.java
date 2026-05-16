package com.kfc.kfc_backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "user_account")
@Data
public class UserAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String username;
    private String password;
    private String displayName;
    private String role;
    private String phone;
    private String email;
    private String avatar;
    private String storeName;
    private String bio;
}
