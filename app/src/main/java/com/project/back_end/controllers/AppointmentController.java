package com.project.back_end.controllers;

import com.project.back_end.models.Appointment;
import com.project.back_end.services.AppointmentService;
import com.project.back_end.services.Service;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// 1. Set Up the Controller Class:
//    - Annotate the class with `@RestController` to define it as a REST API controller.
//    - Use `@RequestMapping("/appointments")` to set a base path for all appointment-related endpoints.
//    - This centralizes all routes that deal with booking, updating, retrieving, and canceling appointments.
@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    // 2. Autowire Dependencies:
//    - Inject `AppointmentService` for handling the business logic specific to appointments.
//    - Inject the general `Service` class, which provides shared functionality like token validation and appointment checks.

    private final AppointmentService appointmentService;
    private final Service service;
    public AppointmentController(AppointmentService appointmentService, Service service) {
        this.appointmentService = appointmentService;
        this.service = service;
    }

// 3. Define the `getAppointments` Method:
//    - Handles HTTP GET requests to fetch appointments based on date and patient name.
//    - Takes the appointment date, patient name, and token as path variables.
//    - First validates the token for role `"doctor"` using the `Service`.
//    - If the token is valid, returns appointments for the given patient on the specified date.
//    - If the token is invalid or expired, responds with the appropriate message and status code.
    @GetMapping("/{date}/{patientName}/{token}")
    public ResponseEntity<?> getAppointments(
            @PathVariable String date,
            @PathVariable String patientName,
            @PathVariable String token
    ) {
        // Validate token for doctor role
        ResponseEntity<Map<String, Object>> validation = service.validateToken(token, "doctor");
        if (validation.getStatusCode() != HttpStatus.OK) {
            return validation;
        }

        try {
            // Parse LocalDate
            LocalDate parsedDate = LocalDate.parse(date);
            // Build startOfDay and endOfDay
            LocalDateTime startOfDay = parsedDate.atStartOfDay();
            LocalDateTime endOfDay = parsedDate.atTime(23, 59, 59, 999_999_999);

            // Extract doctorId from token
            Long doctorId = service.getDoctorIdFromToken(token);
            if (doctorId == null) {
                return new ResponseEntity<>("Invalid doctor token: doctor not found.", HttpStatus.UNAUTHORIZED);
            }
            // Call service
            List<Appointment> appointments = appointmentService.getAppointmentsForDoctor(
                    doctorId, startOfDay, endOfDay, patientName
            );
            return new ResponseEntity<>(appointments, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Invalid date format. Use yyyy-MM-dd.", HttpStatus.BAD_REQUEST);
        }
    }


// 4. Define the `bookAppointment` Method:
//    - Handles HTTP POST requests to create a new appointment.
//    - Accepts a validated `Appointment` object in the request body and a token as a path variable.
//    - Validates the token for the `"patient"` role.
//    - Uses service logic to validate the appointment data (e.g., check for doctor availability and time conflicts).
//    - Returns success if booked, or appropriate error messages if the doctor ID is invalid or the slot is already taken.
    @PostMapping("/{token}")
    public ResponseEntity<?> bookAppointment(
            @RequestBody Appointment appointment,
            @PathVariable String token
    ) {
        // Validate token for patient role
        ResponseEntity<Map<String, Object>> validation = service.validateToken(token, "patient");
        if (validation.getStatusCode() != HttpStatus.OK) {
            return validation;
        }

        // Validate appointment time
        int checkResult = service.validateAppointment(
                appointment.getDoctor().getId(),
                appointment.getAppointmentTime().toLocalDate(),
                appointment.getAppointmentTime().toLocalTime().toString());
        if (checkResult == -1) {
            return new ResponseEntity<>("Invalid doctor ID.", HttpStatus.BAD_REQUEST);
        } else if (checkResult == 0) {
            return new ResponseEntity<>("Requested time slot is not available.", HttpStatus.CONFLICT);
        }

        // Save appointment
        int result = appointmentService.bookAppointment(appointment);
        if (result == 1) {
            return new ResponseEntity<>("Appointment booked successfully.", HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>("Failed to book appointment.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


// 5. Define the `updateAppointment` Method:
//    - Handles HTTP PUT requests to modify an existing appointment.
//    - Accepts a validated `Appointment` object and a token as input.
//    - Validates the token for `"patient"` role.
//    - Delegates the update logic to the `AppointmentService`.
//    - Returns an appropriate success or failure response based on the update result.
    @PutMapping("/{token}")
    public ResponseEntity<?> updateAppointment(
            @RequestBody Appointment updatedData,
            @PathVariable String token
    ) {
        ResponseEntity<Map<String, Object>> validation = service.validateToken(token, "patient");
        if (validation.getStatusCode() != HttpStatus.OK) {
            return validation;
        }
        Long patientId = service.getPatientIdFromToken(token);
        if (patientId == null) {
            return new ResponseEntity<>("Invalid patient token: patient not found.", HttpStatus.UNAUTHORIZED);
        }
        if (updatedData.getId() == null) {
            return new ResponseEntity<>("Appointment ID is required.", HttpStatus.BAD_REQUEST);
        }
        String result = appointmentService.updateAppointment(updatedData.getId(), patientId, updatedData);
        if ("Appointment updated successfully.".equalsIgnoreCase(result)) {
            return new ResponseEntity<>(result, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
        }
    }


// 6. Define the `cancelAppointment` Method:
//    - Handles HTTP DELETE requests to cancel a specific appointment.
//    - Accepts the appointment ID and a token as path variables.
//    - Validates the token for `"patient"` role to ensure the user is authorized to cancel the appointment.
//    - Calls `AppointmentService` to handle the cancellation process and returns the result.

    // 6️⃣ cancelAppointment Method
    @DeleteMapping("/{appointmentId}/{token}")
    public ResponseEntity<?> cancelAppointment(
            @PathVariable Long appointmentId,
            @PathVariable String token
    ) {
        // 1️⃣ Validate token for patient role
        ResponseEntity<Map<String, Object>> validation = service.validateToken(token, "patient");
        if (validation.getStatusCode() != HttpStatus.OK) {
            return validation;
        }

        // 2️⃣ Get patientId from token
        Long patientId = service.getPatientIdFromToken(token);
        if (patientId == null) {
            return new ResponseEntity<>("Invalid patient token: patient not found.", HttpStatus.UNAUTHORIZED);
        }

        // 3️⃣ Call service
        String result = appointmentService.cancelAppointment(appointmentId, patientId);

        // 4️⃣ Build response
        if ("Appointment cancelled successfully.".equalsIgnoreCase(result)) {
            return new ResponseEntity<>(result, HttpStatus.OK);
        } else if ("Unauthorized: Patient ID mismatch.".equalsIgnoreCase(result)) {
            return new ResponseEntity<>(result, HttpStatus.UNAUTHORIZED);
        } else if ("Appointment not found.".equalsIgnoreCase(result)) {
            return new ResponseEntity<>(result, HttpStatus.NOT_FOUND);
        } else {
            return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
