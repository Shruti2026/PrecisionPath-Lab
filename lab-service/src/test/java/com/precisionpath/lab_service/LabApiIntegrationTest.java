package com.precisionpath.lab_service;

import com.jayway.jsonpath.JsonPath;
import com.precisionpath.lab_service.client.UserProfile;
import com.precisionpath.lab_service.client.UserServiceClient;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Walks through the whole lab flow: catalogue -> cart -> booking -> approval
 * -> payment -> receipt -> queue info -> report -> completion.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LabApiIntegrationTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-32-bytes-long!!";

    @TempDir
    static Path storage;

    @DynamicPropertySource
    static void storageLocation(DynamicPropertyRegistry registry) {
        registry.add("app.storage.location", () -> storage.toString());
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserServiceClient userServiceClient;

    private final UUID patientId = UUID.randomUUID();
    private String admin;
    private String receptionist;
    private String patient;

    @BeforeEach
    void setUp() {
        admin = token(UUID.randomUUID(), "admin@lab.test", "ADMIN");
        receptionist = token(UUID.randomUUID(), "desk@lab.test", "RECEPTIONIST");
        patient = token(patientId, "asha@lab.test", "PATIENT");

        when(userServiceClient.getCurrentUser(anyString())).thenReturn(new UserProfile(
                patientId, "Asha Rao", "asha@lab.test", "9876543210", "FEMALE", 29
        ));
    }

    private static String token(UUID userId, String email, String role) {
        return "Bearer " + Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private String perform(AbstractMockHttpServletRequestBuilder<?> request, String auth, int expectedStatus) throws Exception {
        if (auth != null) {
            request.header("Authorization", auth);
        }
        return mockMvc.perform(request)
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
    }

    private String json(AbstractMockHttpServletRequestBuilder<?> request, String body) {
        request.contentType(MediaType.APPLICATION_JSON).content(body);
        return body;
    }

    private String createTest(String code, String name, int price, String prerequisites) throws Exception {
        var request = post("/api/admin/tests");
        json(request, """
                {"code":"%s","name":"%s","purpose":"Checks %s levels","price":%d,
                 "durationMinutes":10,"sampleType":"Blood","reportTurnaroundHours":24,
                 "prerequisites":%s}
                """.formatted(code, name, name, price, prerequisites));
        return JsonPath.read(perform(request, admin, 201), "$.id");
    }

    private static String words(int count) {
        return String.join(" ", Collections.nCopies(count, "symptom"));
    }

    @Test
    void completeLabFlow() throws Exception {

        // --- Catalogue (admin) ---
        String cbc = createTest("cbc", "Complete Blood Count", 300, "[]");
        String lft = createTest("LFT", "Liver Function Test", 500, "[\"Fast for 10-12 hours\"]");
        String tsh = createTest("TSH", "Thyroid Stimulating Hormone", 450, "[\"Take sample before thyroid medication\"]");

        var duplicate = post("/api/admin/tests");
        json(duplicate, """
                {"code":"CBC","name":"Dup","purpose":"x","price":1,"durationMinutes":1}
                """);
        perform(duplicate, admin, 409);

        var createPackage = post("/api/admin/packages");
        json(createPackage, """
                {"name":"Basic Health Checkup","description":"CBC + LFT","price":700,
                 "testIds":["%s","%s"]}
                """.formatted(cbc, lft));
        String packageBody = perform(createPackage, admin, 201);
        String packageId = JsonPath.read(packageBody, "$.id");
        assertThat(JsonPath.<Double>read(packageBody, "$.individualTotal")).isEqualTo(800.0);

        // Patients cannot manage the catalogue
        var patientCreate = post("/api/admin/tests");
        json(patientCreate, "{}");
        perform(patientCreate, patient, 403);

        // Public catalogue needs no login
        mockMvc.perform(get("/api/tests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", hasItems("CBC", "LFT", "TSH")));
        mockMvc.perform(get("/api/tests").param("search", "liver"))
                .andExpect(jsonPath("$[0].prerequisites[0]").value("Fast for 10-12 hours"));

        // --- Cart ---
        var addPackage = post("/api/patient/cart/items");
        json(addPackage, "{\"itemType\":\"PACKAGE\",\"itemId\":\"%s\"}".formatted(packageId));
        perform(addPackage, patient, 201);

        var addCbc = post("/api/patient/cart/items");
        json(addCbc, "{\"itemType\":\"TEST\",\"itemId\":\"%s\"}".formatted(cbc));
        perform(addCbc, patient, 409);

        var addTsh = post("/api/patient/cart/items");
        json(addTsh, "{\"itemType\":\"TEST\",\"itemId\":\"%s\"}".formatted(tsh));
        String cart = perform(addTsh, patient, 201);
        assertThat(JsonPath.<Double>read(cart, "$.totalAmount")).isEqualTo(1150.0);

        // --- Booking ---
        String date = LocalDate.now().plusDays(2).toString();

        perform(multipart("/api/patient/appointments")
                .param("appointmentDate", date)
                .param("reason", "Feeling tired"), patient, 400);

        MockMultipartFile prescription = new MockMultipartFile(
                "prescription", "rx.pdf", "application/pdf", "%PDF-1.4 prescription".getBytes());

        String booked = perform(multipart("/api/patient/appointments")
                .file(prescription)
                .param("appointmentDate", date), patient, 201);
        String appointmentId = JsonPath.read(booked, "$.id");
        assertThat(JsonPath.<String>read(booked, "$.status")).isEqualTo("PENDING");
        assertThat(JsonPath.<Double>read(booked, "$.totalAmount")).isEqualTo(1150.0);
        assertThat(JsonPath.<Boolean>read(booked, "$.prescriptionUploaded")).isTrue();

        String emptyCart = perform(get("/api/patient/cart"), patient, 200);
        assertThat(JsonPath.<Integer>read(emptyCart, "$.itemCount")).isZero();

        // Another patient cannot see it
        String otherPatient = token(UUID.randomUUID(), "other@lab.test", "PATIENT");
        perform(get("/api/patient/appointments/" + appointmentId), otherPatient, 404);

        // Receipt and payment are not available before approval
        perform(get("/api/patient/appointments/" + appointmentId + "/receipt"), patient, 409);
        var earlyPay = post("/api/patient/appointments/" + appointmentId + "/payments");
        json(earlyPay, "{\"method\":\"UPI\"}");
        perform(earlyPay, patient, 409);

        // --- Staff review ---
        String pending = perform(get("/api/staff/appointments")
                .param("status", "PENDING").param("date", date), receptionist, 200);
        assertThat(JsonPath.<List<String>>read(pending, "$[*].id")).contains(appointmentId);

        byte[] rx = mockMvc.perform(get("/api/staff/appointments/" + appointmentId + "/prescription")
                        .header("Authorization", receptionist))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(rx)).isEqualTo("%PDF-1.4 prescription");

        perform(patch("/api/staff/appointments/" + appointmentId + "/approve"), patient, 403);
        String approved = perform(patch("/api/staff/appointments/" + appointmentId + "/approve"), receptionist, 200);
        assertThat(JsonPath.<String>read(approved, "$.status")).isEqualTo("APPROVED");
        perform(patch("/api/staff/appointments/" + appointmentId + "/approve"), receptionist, 409);

        // --- Payment ---
        var cashPay = post("/api/patient/appointments/" + appointmentId + "/payments");
        json(cashPay, "{\"method\":\"CASH\"}");
        perform(cashPay, patient, 400);

        var upiPay = post("/api/patient/appointments/" + appointmentId + "/payments");
        json(upiPay, "{\"method\":\"UPI\"}");
        String payment = perform(upiPay, patient, 201);
        assertThat(JsonPath.<String>read(payment, "$.status")).isEqualTo("PAID");
        assertThat(JsonPath.<Double>read(payment, "$.amount")).isEqualTo(1150.0);

        var payAgain = post("/api/staff/appointments/" + appointmentId + "/payments");
        json(payAgain, "{\"method\":\"CASH\"}");
        perform(payAgain, receptionist, 409);

        // --- Queue-service callback ---
        var queueNoKey = put("/api/internal/appointments/" + appointmentId + "/queue-info");
        json(queueNoKey, "{\"tokenNumber\":7,\"estimatedWaitMinutes\":35}");
        perform(queueNoKey, null, 401);

        String forQueue = mockMvc.perform(get("/api/internal/appointments/" + appointmentId)
                        .header("X-Internal-Api-Key", "test-internal-key"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<Integer>read(forQueue, "$.totalDurationMinutes")).isEqualTo(30);

        mockMvc.perform(put("/api/internal/appointments/" + appointmentId + "/queue-info")
                        .header("X-Internal-Api-Key", "test-internal-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tokenNumber\":7,\"estimatedWaitMinutes\":35}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenNumber").value(7));

        // --- Receipt ---
        String receipt = perform(get("/api/patient/appointments/" + appointmentId + "/receipt"), patient, 200);
        assertThat(JsonPath.<String>read(receipt, "$.patient.name")).isEqualTo("Asha Rao");
        assertThat(JsonPath.<List<String>>read(receipt, "$.tests[*].code")).containsExactlyInAnyOrder("CBC", "LFT", "TSH");
        assertThat(JsonPath.<String>read(receipt, "$.payment.status")).isEqualTo("PAID");
        assertThat(JsonPath.<Integer>read(receipt, "$.queue.tokenNumber")).isEqualTo(7);
        assertThat(JsonPath.<List<String>>read(receipt, "$.tests[?(@.code=='LFT')].prerequisites[0]"))
                .containsExactly("Fast for 10-12 hours");

        byte[] pdf = mockMvc.perform(get("/api/patient/appointments/" + appointmentId + "/receipt/pdf")
                        .header("Authorization", patient))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");

        // --- Reports ---
        MockMultipartFile reportFile = new MockMultipartFile(
                "file", "tsh-report.pdf", "application/pdf", "%PDF-1.4 TSH result".getBytes());

        String report = perform(multipart("/api/staff/appointments/" + appointmentId + "/reports")
                .file(reportFile)
                .param("labTestId", tsh)
                .param("remarks", "Within normal range"), receptionist, 201);
        String reportId = JsonPath.read(report, "$.reportId");

        MockMultipartFile badType = new MockMultipartFile(
                "file", "report.txt", "text/plain", "hello".getBytes());
        perform(multipart("/api/staff/appointments/" + appointmentId + "/reports")
                .file(badType), receptionist, 400);

        String reports = perform(get("/api/patient/appointments/" + appointmentId + "/reports"), patient, 200);
        assertThat(JsonPath.<String>read(reports, "$[0].labTestName")).isEqualTo("Thyroid Stimulating Hormone");

        byte[] downloaded = mockMvc.perform(get("/api/patient/reports/" + reportId + "/download")
                        .header("Authorization", patient))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(downloaded)).isEqualTo("%PDF-1.4 TSH result");

        perform(get("/api/patient/reports/" + reportId + "/download"), otherPatient, 404);

        // --- Complete ---
        String completed = perform(patch("/api/staff/appointments/" + appointmentId + "/complete"), receptionist, 200);
        assertThat(JsonPath.<String>read(completed, "$.status")).isEqualTo("COMPLETED");
    }

    @Test
    void cancellingPaidAppointmentRefundsPayment() throws Exception {

        String test = createTest("HBA1C", "HbA1c", 600, "[]");

        var add = post("/api/patient/cart/items");
        json(add, "{\"itemType\":\"TEST\",\"itemId\":\"%s\"}".formatted(test));
        perform(add, patient, 201);

        String booked = perform(multipart("/api/patient/appointments")
                .param("appointmentDate", LocalDate.now().plusDays(5).toString())
                .param("reason", words(50)), patient, 201);
        String appointmentId = JsonPath.read(booked, "$.id");

        perform(patch("/api/staff/appointments/" + appointmentId + "/approve"), admin, 200);

        var pay = post("/api/staff/appointments/" + appointmentId + "/payments");
        json(pay, "{\"method\":\"CASH\"}");
        perform(pay, receptionist, 201);

        String cancelled = perform(patch("/api/patient/appointments/" + appointmentId + "/cancel"), patient, 200);
        assertThat(JsonPath.<String>read(cancelled, "$.status")).isEqualTo("CANCELLED");
        assertThat(JsonPath.<String>read(cancelled, "$.paymentStatus")).isEqualTo("REFUNDED");

        perform(patch("/api/patient/appointments/" + appointmentId + "/cancel"), patient, 409);
    }

    @Test
    void rejectedAppointmentKeepsReason() throws Exception {

        String test = createTest("VITD", "Vitamin D", 900, "[]");

        var add = post("/api/patient/cart/items");
        json(add, "{\"itemType\":\"TEST\",\"itemId\":\"%s\"}".formatted(test));
        perform(add, patient, 201);

        String booked = perform(multipart("/api/patient/appointments")
                .param("appointmentDate", LocalDate.now().plusDays(10).toString())
                .param("reason", words(55)), patient, 201);
        String appointmentId = JsonPath.read(booked, "$.id");

        var reject = patch("/api/staff/appointments/" + appointmentId + "/reject");
        json(reject, "{\"reason\":\"Please upload a doctor's prescription\"}");
        String rejected = perform(reject, receptionist, 200);

        assertThat(JsonPath.<String>read(rejected, "$.status")).isEqualTo("REJECTED");
        assertThat(JsonPath.<String>read(rejected, "$.rejectionReason"))
                .isEqualTo("Please upload a doctor's prescription");
    }

    @Test
    void requestsWithoutTokenAreRejected() throws Exception {
        mockMvc.perform(get("/api/patient/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }
}
