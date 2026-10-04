package com.example.taskflow.controller;

import com.example.taskflow.dto.UserResponse;
import com.example.taskflow.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AuthService authService;

    public AdminController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/users")
    public List<UserResponse> users() {
        return authService.listUsers(); // the URL rule in SecurityConfig restricts this to ADMIN
    }
}