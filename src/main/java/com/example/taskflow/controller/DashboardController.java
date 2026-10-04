package com.example.taskflow.controller;

import com.example.taskflow.dto.DashboardResponse;
import com.example.taskflow.security.AuthUser;
import com.example.taskflow.service.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public DashboardResponse get(@AuthenticationPrincipal AuthUser user) {
        return dashboardService.get(user);
    }
}