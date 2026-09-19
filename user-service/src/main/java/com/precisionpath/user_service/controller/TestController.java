package com.precisionpath.user_service.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patient")
public class TestController {

    @GetMapping("/test")
    public String patientTest(Authentication authentication) {

        return "Hello " +
                authentication.getName() +
                ", you are authenticated as PATIENT";
    }
}
