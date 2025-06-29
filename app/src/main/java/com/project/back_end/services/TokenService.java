package com.project.back_end.services;

import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class TokenService {

    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    private final String jwtSecret;

    public TokenService(
            AdminRepository adminRepository,
            DoctorRepository doctorRepository,
            PatientRepository patientRepository,
            @Value("${jwt.secret}") String jwtSecret
    ) {
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.jwtSecret = jwtSecret;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000); // 7 days

        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractEmail(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (JwtException e) {
            return null;
        }
    }

    public String validateToken(String token, String role) {
        try {
            String email = extractEmail(token);
            if (email == null || email.isEmpty()) {
                return "Invalid token: cannot extract email";
            }

            switch (role.toLowerCase()) {
                case "admin":
                    if (adminRepository.existsByEmail(email)) {
                        return ""; // valid
                    } else {
                        return "Invalid admin token: user not found";
                    }
                case "doctor":
                    if (doctorRepository.findByEmail(email) != null ) {
                        return ""; // valid
                    } else {
                        return "Invalid doctor token: user not found";
                    }
                case "patient":
                    if (patientRepository.findByEmail(email) != null) {
                        return ""; // valid
                    } else {
                        return "Invalid patient token: user not found";
                    }
                default:
                    return "Invalid role: " + role;
            }

        } catch (Exception e) {
            return "Token validation error: " + e.getMessage();
        }
    }
}
