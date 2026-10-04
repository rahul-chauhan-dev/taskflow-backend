package com.example.taskflow.security;

import com.example.taskflow.entity.Role;

// The logged-in user as seen by controllers and services. Built from the token, no database call.
public record AuthUser(Long id, String email, Role role) {
}