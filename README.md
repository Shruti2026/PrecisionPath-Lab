# 🧪 PrecisionPath Lab
### Smart Diagnostic Appointment & Real-Time Queue Management System

## 🎯 Purpose

PrecisionPath Lab is a **Java-based microservice application** that digitizes diagnostic centre operations by providing online test booking, appointment approval, digital tokens, unified queue management, real-time waiting-time estimation, and report notifications.

## 🛠️ Tech Stack

| Category | Technology |
|---|---|
| Frontend | React, JavaScript, React Router, Axios |
| Backend | Java 21, Spring Boot, Spring MVC |
| Security | Spring Security, JWT |
| Microservices | Spring Cloud Gateway, REST APIs |
| Database | MySQL |
| Cache / Queue State | Redis |
| Real-Time | WebSocket, Spring WebSocket |
| File / PDF | Multipart Upload, OpenPDF |
| API Documentation | Swagger UI |
| Testing | JUnit 5, Mockito, Postman |
| DevOps | Git, GitHub, Docker, Docker Compose |
| CI/CD | GitHub Actions|

## 🏗️ Microservice Architecture

                         ┌──────────────────┐
                         │   React Frontend │
                         └────────┬─────────┘
                                  │
                                  ↓
                       ┌─────────────────────┐
                       │    API Gateway      │
                       │    Spring Cloud     │
                       │    Gateway          │
                       └──────────┬──────────┘
                                  │
             ┌────────────────────┼────────────────────┐
             ↓                    ↓                    ↓
     ┌───────────────┐    ┌───────────────┐    ┌────────────────┐
     │ User Service  │    │  Lab Service  │    │ Queue Service  │
     └───────┬───────┘    └───────┬───────┘    └───────┬────────┘
             │                    │                    │
             ↓                    ↓                    ↓
        MySQL DB              MySQL DB              MySQL DB
                                  │                    │
                                  └─────────┬──────────┘
                                            ↓
                                  ┌────────────────────┐
                                  │ Notification       │
                                  │ Service            │
                                  └─────────┬──────────┘
                                            ↓
                                      MySQL / Email

## ⭐ Special Features

### 🎫 Unified Digital Queue
One patient receives one token for their complete diagnostic visit instead of separate patient-facing queues for every test.

### ⚡ Real-Time Queue Tracking
Patients can track their current token, queue position, and service status without refreshing the page using WebSockets.

### 🧮 Dynamic Waiting-Time Estimation
Estimated waiting time is continuously recalculated based on queue progression, patients ahead, attendants, and service duration.

### 🔔 Smart Notifications
Patients receive notifications for appointment day, approaching turn, and report generation.

### 📋 Prescription / Reason Validation
Patients must upload a prescription or provide a minimum 50-word reason before submitting an appointment request.

### 🧪 Test Prerequisites
Every test displays its purpose, cost, and lab-configured prerequisites before booking and on the digital receipt.

### 📄 Digital Receipt
A digital receipt contains patient, appointment, test, payment, token, waiting-time, and prerequisite information.

### 📑 Digital Reports
Patients can view or download their diagnostic reports after the tests are completed.

### 🔐 Secure Authentication
JWT-based authentication and role-based authorization protect patient and admin/receptionist operations.

### 🎟️ Concurrency-Safe Token Generation
Tokens are generated safely per appointment date to prevent duplicate tokens during concurrent approvals.

## 🏗️ Microservices

| Microservice | Responsibility |
|---|---|
| **User Service** | Registration, login, JWT authentication, patient profiles, and roles |
| **Lab Service** | Tests, packages, cart, appointments, prescriptions, payments, receipts, and reports |
| **Queue Service** | Token generation, unified queue, attendant assignment, queue status, and waiting-time calculation |
| **Notification Service** | Appointment, queue, and report notifications |
| **API Gateway** | Single entry point, request routing, and API access control |