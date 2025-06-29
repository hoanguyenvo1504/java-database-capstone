/**
 * header.js
 * Renders the dynamic header based on the user's role and login status.
 * Includes helper functions for logout and attaching event listeners.
 */

function renderHeader() {
    const headerDiv = document.getElementById("header");

    // Check if we are on the homepage
    if (window.location.pathname.endsWith("/")) {
        localStorage.removeItem("userRole");
        localStorage.removeItem("token");
        headerDiv.innerHTML = `
      <header class="header">
        <div class="logo-section">
          <img src="../assets/images/logo/logo.png" alt="Hospital CRM Logo" class="logo-img">
          <span class="logo-title">Hospital CMS</span>
        </div>
      </header>`;
        return;
    }

    // Get user role and token
    const role = localStorage.getItem("userRole");
    const token = localStorage.getItem("token");

    // Handle invalid session (missing token for authenticated roles)
    if ((role === "loggedPatient" || role === "admin" || role === "doctor") && !token) {
        localStorage.removeItem("userRole");
        alert("Session expired or invalid login. Please log in again.");
        window.location.href = "/";
        return;
    }

    // Start building the header HTML
    let headerContent = `
    <header class="header">
      <div class="logo-section">
        <img src="../assets/images/logo/logo.png" alt="Hospital CRM Logo" class="logo-img">
        <span class="logo-title">Hospital CMS</span>
      </div>
      <nav>`;

    // Role-based header controls
    if (role === "admin") {
        headerContent += `
      <button id="addDocBtn" class="adminBtn" onclick="openModal('addDoctor')">Add Doctor</button>
      <a href="#" onclick="logout()">Logout</a>`;
    } else if (role === "doctor") {
        headerContent += `
      <button class="adminBtn" onclick="window.location.href='/pages/doctorDashboard.html'">Home</button>
      <a href="#" onclick="logout()">Logout</a>`;
    } else if (role === "patient") {
        headerContent += `
      <button id="patientLogin" class="adminBtn">Login</button>
      <button id="patientSignup" class="adminBtn">Sign Up</button>`;
    } else if (role === "loggedPatient") {
        headerContent += `
      <button class="adminBtn" onclick="window.location.href='/pages/loggedPatientDashboard.html'">Home</button>
      <button class="adminBtn" onclick="window.location.href='/pages/patientAppointments.html'">Appointments</button>
      <a href="#" onclick="logoutPatient()">Logout</a>`;
    }

    headerContent += `
      </nav>
    </header>`;

    // Inject the generated header
    headerDiv.innerHTML = headerContent;

    // Attach event listeners if needed (e.g., login/signup buttons)
    attachHeaderButtonListeners();
}

function attachHeaderButtonListeners() {
    const loginBtn = document.getElementById("patientLogin");
    if (loginBtn) {
        loginBtn.addEventListener("click", () => openModal('patientLogin'));
    }

    const signupBtn = document.getElementById("patientSignup");
    if (signupBtn) {
        signupBtn.addEventListener("click", () => openModal('patientSignup'));
    }
}

function logout() {
    localStorage.removeItem("userRole");
    localStorage.removeItem("token");
    window.location.href = "/";
}

function logoutPatient() {
    localStorage.removeItem("token");
    localStorage.setItem("userRole", "patient");
    window.location.href = "/pages/patientDashboard.html";
}

// Call renderHeader on page load
document.addEventListener("DOMContentLoaded", renderHeader);
