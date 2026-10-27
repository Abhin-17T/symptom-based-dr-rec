CREATE TABLE IF NOT EXISTS users (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 full_name VARCHAR(120) NOT NULL,
 email VARCHAR(190) NOT NULL UNIQUE,
 phone VARCHAR(30),
 password_hash VARCHAR(100) NOT NULL,
 role VARCHAR(20) NOT NULL DEFAULT 'PATIENT',
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS doctors (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 full_name VARCHAR(120) NOT NULL,
 specialty VARCHAR(100) NOT NULL,
 experience_years INT NOT NULL DEFAULT 1,
 rating DECIMAL(2,1) NOT NULL DEFAULT 4.8,
 bio VARCHAR(500),
 available_days VARCHAR(150) NOT NULL DEFAULT 'Mon,Tue,Wed,Thu,Fri',
 available BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE IF NOT EXISTS appointments (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 user_id BIGINT NOT NULL,
 doctor_id BIGINT NOT NULL,
 appointment_date DATE NOT NULL,
 appointment_time TIME NOT NULL,
 reason VARCHAR(500),
 status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
 active_slot_date DATE GENERATED ALWAYS AS (CASE WHEN status IN ('PENDING','CONFIRMED') THEN appointment_date ELSE NULL END) STORED,
 active_slot_time TIME GENERATED ALWAYS AS (CASE WHEN status IN ('PENDING','CONFIRMED') THEN appointment_time ELSE NULL END) STORED,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_appointment_user FOREIGN KEY (user_id) REFERENCES users(id),
 CONSTRAINT fk_appointment_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id),
 CONSTRAINT uq_doctor_slot UNIQUE (doctor_id, active_slot_date, active_slot_time)
);
INSERT INTO doctors (full_name,specialty,experience_years,rating,bio,available_days,available) SELECT 'Dr. Ananya Menon','General Physician',9,4.9,'Focused on whole-person primary care, fever, fatigue and everyday health concerns.','Mon,Tue,Wed,Thu,Fri,Sat',TRUE WHERE NOT EXISTS (SELECT 1 FROM doctors WHERE full_name='Dr. Ananya Menon');
INSERT INTO doctors (full_name,specialty,experience_years,rating,bio,available_days,available) SELECT 'Dr. Rahul Nair','Cardiologist',12,4.9,'Heart health, blood pressure and preventive cardiology.','Mon,Tue,Thu,Fri',TRUE WHERE NOT EXISTS (SELECT 1 FROM doctors WHERE full_name='Dr. Rahul Nair');
INSERT INTO doctors (full_name,specialty,experience_years,rating,bio,available_days,available) SELECT 'Dr. Meera Krishnan','Dermatologist',8,4.8,'Care for skin, hair and nail conditions.','Tue,Wed,Thu,Sat',TRUE WHERE NOT EXISTS (SELECT 1 FROM doctors WHERE full_name='Dr. Meera Krishnan');
INSERT INTO doctors (full_name,specialty,experience_years,rating,bio,available_days,available) SELECT 'Dr. Arjun Varma','Neurologist',11,4.9,'Evaluation of headaches, dizziness and neurological concerns.','Mon,Wed,Fri,Sat',TRUE WHERE NOT EXISTS (SELECT 1 FROM doctors WHERE full_name='Dr. Arjun Varma');
INSERT INTO doctors (full_name,specialty,experience_years,rating,bio,available_days,available) SELECT 'Dr. Lakshmi Pillai','Pediatrician',10,5.0,'Compassionate care for infants, children and adolescents.','Mon,Tue,Wed,Thu,Fri',TRUE WHERE NOT EXISTS (SELECT 1 FROM doctors WHERE full_name='Dr. Lakshmi Pillai');
INSERT INTO doctors (full_name,specialty,experience_years,rating,bio,available_days,available) SELECT 'Dr. Nikhil Thomas','Orthopedist',14,4.8,'Bone, joint and muscle health, including sports injuries.','Mon,Tue,Thu,Fri,Sat',TRUE WHERE NOT EXISTS (SELECT 1 FROM doctors WHERE full_name='Dr. Nikhil Thomas');
