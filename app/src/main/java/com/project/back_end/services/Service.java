package com.project.back_end.services;


import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.models.Admin;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@org.springframework.stereotype.Service
public class Service {
// 1. **@Service Annotation**
// The @Service annotation marks this class as a service component in Spring. This allows Spring to automatically detect it through component scanning
// and manage its lifecycle, enabling it to be injected into controllers or other services using @Autowired or constructor injection.

// 2. **Constructor Injection for Dependencies**
// The constructor injects all required dependencies (TokenService, Repositories, and other Services). This approach promotes loose coupling, improves testability,
// and ensures that all required dependencies are provided at object creation time.
    private final TokenService tokenService;
    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final DoctorService doctorService;
    private final PatientService patientService;

    public Service(TokenService tokenService,
                        AdminRepository adminRepository,
                        DoctorRepository doctorRepository,
                        PatientRepository patientRepository,
                        DoctorService doctorService,
                        PatientService patientService) {
        this.tokenService = tokenService;
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.doctorService = doctorService;
        this.patientService = patientService;
    }

// 3. **validateToken Method**
// This method checks if the provided JWT token is valid for a specific user. It uses the TokenService to perform the validation.
// If the token is invalid or expired, it returns a 401 Unauthorized response with an appropriate error message. This ensures security by preventing
// unauthorized access to protected resources.
    public ResponseEntity<Map<String, Object>> validateToken(String token, String role) {
        Map<String, Object> response = new HashMap<>();
        String error = tokenService.validateToken(token, role);

        if (error == null || error.isBlank()) {
            response.put("message", "Valid token.");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            response.put("message", error);
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
    }

// 4. **validateAdmin Method**
// This method validates the login credentials for an admin user.
// - It first searches the admin repository using the provided username.
// - If an admin is found, it checks if the password matches.
// - If the password is correct, it generates and returns a JWT token (using the admin’s username) with a 200 OK status.
// - If the password is incorrect, it returns a 401 Unauthorized status with an error message.
// - If no admin is found, it also returns a 401 Unauthorized.
// - If any unexpected error occurs during the process, a 500 Internal Server Error response is returned.
// This method ensures that only valid admin users can access secured parts of the system.
    public ResponseEntity<Map<String, Object>> validateAdmin(String username, String password) {
        Map<String, Object> response = new HashMap<>();
        try {
            Admin admin = adminRepository.findByUsername(username);
            if (admin == null) {
                response.put("message", "Invalid username.");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }
            if (!admin.getPassword().equals(password)) {
                response.put("message", "Invalid password.");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }
            String token = tokenService.generateToken(admin.getUsername());
            response.put("token", token);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("message", "Error validating admin: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

// 5. **filterDoctor Method**
// This method provides filtering functionality for doctors based on name, specialty, and available time slots.
// - It supports various combinations of the three filters.
// - If none of the filters are provided, it returns all available doctors.
// This flexible filtering mechanism allows the frontend or consumers of the API to search and narrow down doctors based on user criteria.
    public ResponseEntity<List<Doctor>> filterDoctor(String name, String specialty, String time) {
        try {
            List<Doctor> doctors;
            if (name != null && specialty != null && time != null) {
                doctors = doctorService.filterDoctorsByNameSpecilityandTime(name, specialty, time);
            } else if (name != null && specialty != null) {
                doctors = doctorService.filterDoctorByNameAndSpecility(name, specialty);
            } else if (name != null && time != null) {
                doctors = doctorService.filterDoctorByNameAndTime(name, time);
            } else if (specialty != null && time != null) {
                doctors = doctorService.filterDoctorByTimeAndSpecility(specialty, time);
            } else if (name != null) {
                doctors = doctorService.findDoctorByName(name);
            } else if (specialty != null) {
                doctors = doctorService.filterDoctorBySpecility(specialty);
            } else if (time != null) {
                doctors = doctorService.filterDoctorsByTime(time);
            } else {
                doctors = doctorService.getDoctors();
            }
            return new ResponseEntity<>(doctors, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(List.of(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

// 6. **validateAppointment Method**
// This method validates if the requested appointment time for a doctor is available.
// - It first checks if the doctor exists in the repository.
// - Then, it retrieves the list of available time slots for the doctor on the specified date.
// - It compares the requested appointment time with the start times of these slots.
// - If a match is found, it returns 1 (valid appointment time).
// - If no matching time slot is found, it returns 0 (invalid).
// - If the doctor doesn’t exist, it returns -1.
// This logic prevents overlapping or invalid appointment bookings.
    public int validateAppointment(Long doctorId, LocalDate date, String timeSlot) {
        try {
            Optional<Doctor> doctorOpt = doctorRepository.findById(doctorId);
            if (doctorOpt.isEmpty()) {
                return -1;
            }
            Doctor doctor = doctorOpt.get();
            List<String> availableTimes = doctor.getAvailableTimes();
            if (availableTimes == null) {
                return 0;
            }
            return availableTimes.contains(timeSlot) ? 1 : 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

// 7. **validatePatient Method**
// This method checks whether a patient with the same email or phone number already exists in the system.
// - If a match is found, it returns false (indicating the patient is not valid for new registration).
// - If no match is found, it returns true.
// This helps enforce uniqueness constraints on patient records and prevent duplicate entries.
    public boolean validatePatient(String email, String phone) {
        try {
            Patient existing = patientRepository.findByEmailOrPhone(email, phone);
            return existing == null;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

// 8. **validatePatientLogin Method**
// This method handles login validation for patient users.
// - It looks up the patient by email.
// - If found, it checks whether the provided password matches the stored one.
// - On successful validation, it generates a JWT token and returns it with a 200 OK status.
// - If the password is incorrect or the patient doesn't exist, it returns a 401 Unauthorized with a relevant error.
// - If an exception occurs, it returns a 500 Internal Server Error.
// This method ensures only legitimate patients can log in and access their data securely.
    public ResponseEntity<Map<String, Object>> validatePatientLogin(String email, String password) {
        Map<String, Object> response = new HashMap<>();
        try {
            Patient patient = patientRepository.findByEmail(email);
            if (patient == null) {
                response.put("message", "Invalid email.");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

            if (!patient.getPassword().equals(password)) {
                response.put("message", "Invalid password.");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

            String token = tokenService.generateToken(patient.getEmail());
            response.put("token", token);
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("message", "Error during patient login: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

// 9. **filterPatient Method**
// This method filters a patient's appointment history based on condition and doctor name.
// - It extracts the email from the JWT token to identify the patient.
// - Depending on which filters (condition, doctor name) are provided, it delegates the filtering logic to PatientService.
// - If no filters are provided, it retrieves all appointments for the patient.
// This flexible method supports patient-specific querying and enhances user experience on the client side.
    public ResponseEntity<List<AppointmentDTO>> filterPatient(String token, String condition, String doctor) {
        try {
            String email = tokenService.extractEmail(token);
            if (email == null || email.isBlank()) {
                return new ResponseEntity<>(List.of(), HttpStatus.UNAUTHORIZED);
            }
            Patient patient = patientRepository.findByEmail(email);
            if (patient == null) {
                return new ResponseEntity<>(List.of(), HttpStatus.UNAUTHORIZED);
            }
            Long patientId = patient.getId();
            List<AppointmentDTO> results;
            if (doctor != null && condition != null) {
                results = patientService.filterByDoctorAndCondition(patientId, doctor, condition);
            } else if (doctor != null) {
                results = patientService.filterByDoctor(patientId, doctor);
            } else if (condition != null) {
                results = patientService.filterByCondition(patientId, condition);
            } else {
                results = patientService.getPatientAppointment(patientId);
            }
            return new ResponseEntity<>(results, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(List.of(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Long getDoctorIdFromToken(String token) {
        String email = tokenService.extractEmail(token);
        if (email == null || email.isBlank()) {
            return null;
        }
        Doctor doctor = doctorRepository.findByEmail(email);
        return doctor != null ? doctor.getId() : null;
    }

    public Long getPatientIdFromToken(String token) {
        String email = tokenService.extractEmail(token);
        if (email == null || email.isBlank()) {
            return null;
        }
        Patient patient = patientRepository.findByEmail(email);
        return patient != null ? patient.getId() : null;
    }
}
