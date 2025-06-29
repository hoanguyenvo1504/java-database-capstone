/**
 * doctorCard.js
 *
 * This module exports a function that creates and returns a DOM element
 * representing a single doctor card with appropriate role-based actions.
 */

// Import helpers
import { showBookingOverlay } from "../services/loggedPatient.js";
import { deleteDoctor } from "../services/doctorServices.js";
import { getPatientData } from "../services/patientServices.js";

/**
 * Creates a DOM element for a doctor card
 * @param {Object} doctor - The doctor data object with fields like name, specialty, email, availableTimes
 * @returns {HTMLElement} - The DOM element representing the doctor card
 */
export function createDoctorCard(doctor) {
    // Create main card container
    const card = document.createElement("div");
    card.classList.add("doctor-card");

    // Get current user role
    const role = localStorage.getItem("userRole");

    // Doctor Info Section
    const infoDiv = document.createElement("div");
    infoDiv.classList.add("doctor-info");

    const name = document.createElement("h3");
    name.textContent = doctor.name;

    const specialization = document.createElement("p");
    specialization.textContent = `Specialty: ${doctor.specialty}`;

    const email = document.createElement("p");
    email.textContent = `Email: ${doctor.email}`;

    const availability = document.createElement("p");
    availability.textContent = `Available Times: ${doctor.availableTimes.join(", ")}`;

    // Append info elements
    infoDiv.appendChild(name);
    infoDiv.appendChild(specialization);
    infoDiv.appendChild(email);
    infoDiv.appendChild(availability);

    // Actions Section
    const actionsDiv = document.createElement("div");
    actionsDiv.classList.add("card-actions");

    // === Admin role ===
    if (role === "admin") {
        const removeBtn = document.createElement("button");
        removeBtn.textContent = "Delete";
        removeBtn.classList.add("delete-btn");

        removeBtn.addEventListener("click", async () => {
            const confirmDelete = confirm(`Are you sure you want to delete Dr. ${doctor.name}?`);
            if (!confirmDelete) return;

            try {
                const adminToken = localStorage.getItem("token");
                const result = await deleteDoctor(doctor.id, adminToken);
                alert(result.message || "Doctor deleted successfully.");
                card.remove();
            } catch (error) {
                console.error("Delete error:", error);
                alert("Failed to delete doctor. Please try again.");
            }
        });

        actionsDiv.appendChild(removeBtn);
    }

    // === Patient (not logged in) role ===
    else if (role === "patient") {
        const bookNow = document.createElement("button");
        bookNow.textContent = "Book Now";
        bookNow.classList.add("book-btn");

        bookNow.addEventListener("click", () => {
            alert("You need to log in before booking an appointment.");
        });

        actionsDiv.appendChild(bookNow);
    }

    // === Logged-in Patient role ===
    else if (role === "loggedPatient") {
        const bookNow = document.createElement("button");
        bookNow.textContent = "Book Now";
        bookNow.classList.add("book-btn");

        bookNow.addEventListener("click", async () => {
            const token = localStorage.getItem("token");
            if (!token) {
                alert("Session expired. Please log in again.");
                window.location.href = "/";
                return;
            }

            try {
                const patientData = await getPatientData(token);
                showBookingOverlay(doctor, patientData);
            } catch (error) {
                console.error("Booking error:", error);
                alert("Error fetching your data. Please try again.");
            }
        });

        actionsDiv.appendChild(bookNow);
    }

    // Final Assembly
    card.appendChild(infoDiv);
    card.appendChild(actionsDiv);

    return card;
}
