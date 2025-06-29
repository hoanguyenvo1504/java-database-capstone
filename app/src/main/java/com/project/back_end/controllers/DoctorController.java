package com.project.back_end.controllers;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Doctor;
import com.project.back_end.services.DoctorService;
import com.project.back_end.services.ServiceLayer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 1. Set Up the Controller Class:
//    - Annotate the class with `@RestController` to define it as a REST controller that serves JSON responses.
//    - Use `@RequestMapping("${api.path}doctor")` to prefix all endpoints with a configurable API path followed by "doctor".
//    - This class manages doctor-related functionalities such as registration, login, updates, and availability.
@RestController
@RequestMapping("${api.path}doctor")
public class DoctorController {

// 2. Autowire Dependencies:
//    - Inject `DoctorService` for handling the core logic related to doctors (e.g., CRUD operations, authentication).
//    - Inject the shared `Service` class for general-purpose features like token validation and filtering.

    private final DoctorService doctorService;
    private final ServiceLayer serviceLayer;

    // 2️⃣ Constructor injection
    public DoctorController(DoctorService doctorService, ServiceLayer serviceLayer) {
        this.doctorService = doctorService;
        this.serviceLayer = serviceLayer;
    }


// 3. Define the `getDoctorAvailability` Method:
//    - Handles HTTP GET requests to check a specific doctor’s availability on a given date.
//    - Requires `user` type, `doctorId`, `date`, and `token` as path variables.
//    - First validates the token against the user type.
//    - If the token is invalid, returns an error response; otherwise, returns the availability status for the doctor.
    @GetMapping("/availability/{user}/{doctorId}/{date}/{token}")
    public ResponseEntity<?> getDoctorAvailability(
            @PathVariable String user,
            @PathVariable Long doctorId,
            @PathVariable String date,
            @PathVariable String token
    ) {
        // Validate token by role
        ResponseEntity<Map<String, Object>> validation = serviceLayer.validateToken(token, user);
        if (validation.getStatusCode() != HttpStatus.OK) {
            return validation;
        }
        try {
            LocalDate parsedDate = LocalDate.parse(date);
            List<String> availability = doctorService.getDoctorAvailability(doctorId, parsedDate, getAllSlots());
            Map<String, Object> response = new HashMap<>();
            response.put("doctorId", doctorId);
            response.put("date", parsedDate);
            response.put("availableTimes", availability);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Invalid date format. Use yyyy-MM-dd.", HttpStatus.BAD_REQUEST);
        }
    }

// 4. Define the `getDoctor` Method:
//    - Handles HTTP GET requests to retrieve a list of all doctors.
//    - Returns the list within a response map under the key `"doctors"` with HTTP 200 OK status.

    @GetMapping
    public ResponseEntity<?> getDoctor() {
        List<Doctor> doctors = doctorService.getDoctors();
        Map<String, Object> response = new HashMap<>();
        response.put("doctors", doctors);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


// 5. Define the `saveDoctor` Method:
//    - Handles HTTP POST requests to register a new doctor.
//    - Accepts a validated `Doctor` object in the request body and a token for authorization.
//    - Validates the token for the `"admin"` role before proceeding.
//    - If the doctor already exists, returns a conflict response; otherwise, adds the doctor and returns a success message.
    @PostMapping("/{token}")
    public ResponseEntity<?> saveDoctor(
            @RequestBody Doctor doctor,
            @PathVariable String token
    ) {
        // Validate token for admin
        ResponseEntity<Map<String, Object>> validation = serviceLayer.validateToken(token, "admin");
        if (validation.getStatusCode() != HttpStatus.OK) {
            return validation;
        }
        int result = doctorService.saveDoctor(doctor);
        if (result == -1) {
            return new ResponseEntity<>("Doctor already exists with this email.", HttpStatus.CONFLICT);
        } else if (result == 1) {
            return new ResponseEntity<>("Doctor registered successfully.", HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>("Failed to register doctor.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


// 6. Define the `doctorLogin` Method:
//    - Handles HTTP POST requests for doctor login.
//    - Accepts a validated `Login` DTO containing credentials.
//    - Delegates authentication to the `DoctorService` and returns login status and token information.

    @PostMapping("/login")
    public ResponseEntity<?> doctorLogin(@RequestBody Login login) {
        return doctorService.validateDoctor(login.getEmail(), login.getPassword());
    }


// 7. Define the `updateDoctor` Method:
//    - Handles HTTP PUT requests to update an existing doctor's information.
//    - Accepts a validated `Doctor` object and a token for authorization.
//    - Token must belong to an `"admin"`.
//    - If the doctor exists, updates the record and returns success; otherwise, returns not found or error messages.

    @PutMapping("/{token}")
    public ResponseEntity<?> updateDoctor(
            @RequestBody Doctor doctor,
            @PathVariable String token
    ) {
        // Validate token for admin
        ResponseEntity<Map<String, Object>> validation = serviceLayer.validateToken(token, "admin");
        if (validation.getStatusCode() != HttpStatus.OK) {
            return validation;
        }
        int result = doctorService.updateDoctor(doctor.getId(), doctor);
        if (result == -1) {
            return new ResponseEntity<>("Doctor not found.", HttpStatus.NOT_FOUND);
        } else if (result == 1) {
            return new ResponseEntity<>("Doctor updated successfully.", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Failed to update doctor.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


// 8. Define the `deleteDoctor` Method:
//    - Handles HTTP DELETE requests to remove a doctor by ID.
//    - Requires both doctor ID and an admin token as path variables.
//    - If the doctor exists, deletes the record and returns a success message;
//    otherwise, responds with a not found or error message.
    @DeleteMapping("/{doctorId}/{token}")
    public ResponseEntity<?> deleteDoctor(
            @PathVariable Long doctorId,
            @PathVariable String token
    ) {
        // Validate token for admin
        ResponseEntity<Map<String, Object>> validation = serviceLayer.validateToken(token, "admin");
        if (validation.getStatusCode() != HttpStatus.OK) {
            return validation;
        }

        int result = doctorService.deleteDoctor(doctorId);
        if (result == -1) {
            return new ResponseEntity<>("Doctor not found.", HttpStatus.NOT_FOUND);
        } else if (result == 1) {
            return new ResponseEntity<>("Doctor deleted successfully.", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Failed to delete doctor.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

// 9. Define the `filter` Method:
//    - Handles HTTP GET requests to filter doctors based on name, time, and specialty.
//    - Accepts `name`, `time`, and `speciality` as path variables.
//    - Calls the shared `Service` to perform filtering logic and returns matching doctors in the response.
    @GetMapping("/filter/{name}/{time}/{specialty}")
    public ResponseEntity<?> filter(
            @PathVariable String name,
            @PathVariable String time,
            @PathVariable String specialty
    ) {
        return serviceLayer.filterDoctor(name, specialty, time);
    }

    private List<LocalTime> getAllSlots() {
        return List.of(
                LocalTime.of(8, 0),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                LocalTime.of(13, 0),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0),
                LocalTime.of(16, 0)
        );
    }

}
