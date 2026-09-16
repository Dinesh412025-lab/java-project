package com.medgrid.ai.controller;

import com.medgrid.ai.agent.DispatcherAgent;
import com.medgrid.ai.model.PatientRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

@RestController
public class MedGridController {

    @Autowired
    private DispatcherAgent dispatcherAgent;

    @GetMapping("/dashboard")
    public ResponseEntity<Void> dashboard() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/dashboard.html"))
                .build();
    }
    
    @GetMapping("/api/user")
    public Map<String, String> getUser(@AuthenticationPrincipal OAuth2User principal) {
        Map<String, String> map = new HashMap<>();
        if (principal != null && principal.getAttribute("name") != null) {
            map.put("name", principal.getAttribute("name"));
        } else if (principal != null && principal.getAttribute("login") != null) {
            map.put("name", principal.getAttribute("login"));
        } else {
            map.put("name", "Guest");
        }
        return map;
    }

    @PostMapping("/api/request")
    public DispatcherAgent.DispatchResult handlePatientRequest(@RequestBody PatientRequest request) {
        if (request.getId() == null) {
            request.setId(UUID.randomUUID().toString());
        }
        return dispatcherAgent.dispatch(request);
    }
}
