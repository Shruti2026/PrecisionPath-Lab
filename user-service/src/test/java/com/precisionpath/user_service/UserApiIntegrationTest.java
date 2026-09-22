package com.precisionpath.user_service;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String login(String email, String password) throws Exception {

        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(body, "$.token");
    }

    @Test
    void patientCanRegisterLoginAndManageProfile() throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Asha Rao","email":"asha.it@example.com",
                                 "password":"password123","phoneNumber":"9876543210",
                                 "gender":"FEMALE","age":29}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("PATIENT"));

        String token = login("asha.it@example.com", "password123");

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Asha Rao"))
                .andExpect(jsonPath("$.phoneNumber").value("9876543210"));

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Asha R","phoneNumber":"9123456780",
                                 "gender":"FEMALE","age":30}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.age").value(30));

        mockMvc.perform(put("/api/users/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"password123","newPassword":"newPassword456"}
                                """))
                .andExpect(status().isOk());

        login("asha.it@example.com", "newPassword456");

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void requestsWithoutTokenAreRejected() throws Exception {

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    @Test
    void wrongPasswordReturnsUnauthorized() throws Exception {

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nobody@example.com","password":"whatever1"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void seededAdminCanCreateReceptionist() throws Exception {

        String adminToken = login("admin@precisionpath.test", "Admin@12345");

        mockMvc.perform(post("/api/admin/staff")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Ravi Kumar","email":"ravi.it@example.com",
                                 "password":"password123","phoneNumber":"9876501234",
                                 "gender":"MALE","age":32,"role":"RECEPTIONIST"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("RECEPTIONIST"));

        mockMvc.perform(get("/api/admin/users")
                        .param("role", "RECEPTIONIST")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("ravi.it@example.com"));
    }
}
