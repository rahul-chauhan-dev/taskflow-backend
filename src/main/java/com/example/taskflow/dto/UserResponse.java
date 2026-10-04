package com.example.taskflow.dto;

import com.example.taskflow.entity.Role;
import com.example.taskflow.entity.User;

public record UserResponse(Long id, String name, String email, Role role) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole());
    }
}