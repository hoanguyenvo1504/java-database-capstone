## MySQL Database Design

List at least 4 tables that your clinic system needs.

Start with: patients, doctors, appointments, admin
Add others if needed: clinic_locations, payments
For each table:

Define columns
Specify data types
Mark primary keys and any foreign key relationships
Consider constraints:

Should some fields be NOT NULL, UNIQUE, or AUTO_INCREMENT?
Should we validate email or phone formats later via code?
Ask yourself:

What happens if a patient is deleted? Should appointments also be deleted?
Should a doctor be allowed to have overlapping appointments?
Example structure:
### Table: appointments
- id: INT, Primary Key, Auto Increment
- doctor_id: INT, Foreign Key → doctors(id)
- patient_id: INT, Foreign Key → patients(id)
- appointment_time: DATETIME, Not Null
- status: INT (0 = Scheduled, 1 = Completed, 2 = Cancelled)

Consider defining the structure of other tables in the format as per the given example.

Think deeper:
Should each doctor have their own available time slots?
Should a patient's past appointment history be retained forever?
Is a prescription tied to a specific appointment or can it exist independently?
Write your design clearly and justify choices as comments in the file if needed.

## MongoDB Collection Design

Some data doesn't fit well into rigid tables. For instance:
Free-form doctor notes
Optional patient feedback
Prescription metadata
File attachments
Log records (when a patient checks in or messages a doctor)
These are great candidates for MongoDB – a flexible NoSQL database.

Instructions:
Think of one collection that complements your MySQL schema.

Use prescriptions, feedback, logs, or messages.
Provide an example document using JSON syntax.

Be creative:

Add fields like tags, metadata, or nested structures.
Use arrays or embedded documents if they make sense.
Example:
### Collection: prescriptions
```json
{
  "_id": "ObjectId('64abc123456')",
  "patientName": "John Smith",
  "appointmentId": 51,
  "medication": "Paracetamol",
  "dosage": "500mg",
  "doctorNotes": "Take 1 tablet every 6 hours.",
  "refillCount": 2,
  "pharmacy": {
    "name": "Walgreens SF",
    "location": "Market Street"
  }
}
