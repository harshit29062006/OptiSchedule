-- OptiSchedule relational schema. Safe to run repeatedly.
-- Combined sessions are one booking row linked to multiple sections.
CREATE TABLE IF NOT EXISTS branches (
    branch_id VARCHAR(20) PRIMARY KEY,
    branch_name VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS faculty (
    faculty_id VARCHAR(100) PRIMARY KEY,
    faculty_name VARCHAR(100),
    department VARCHAR(50),
    max_hours_per_week INT NOT NULL,
    is_synthetic BOOLEAN NOT NULL DEFAULT FALSE,
    CHECK (max_hours_per_week >= 0)
);

CREATE TABLE IF NOT EXISTS rooms (
    room_id VARCHAR(100) PRIMARY KEY,
    room_name VARCHAR(100) NOT NULL,
    room_type VARCHAR(30) NOT NULL,
    capacity INT NOT NULL,
    is_synthetic BOOLEAN NOT NULL DEFAULT FALSE,
    CHECK (capacity > 0),
    CHECK (room_type IN ('CLASSROOM', 'LAB', 'TUTORIAL_ROOM'))
);

CREATE TABLE IF NOT EXISTS courses (
    course_code VARCHAR(20) NOT NULL,
    course_name VARCHAR(150),
    credits INT,
    period_type VARCHAR(20) NOT NULL,
    PRIMARY KEY (course_code, period_type),
    CHECK (period_type IN ('LECTURE', 'TUTORIAL', 'PRACTICAL'))
);

CREATE TABLE IF NOT EXISTS sections (
    section_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    section_code VARCHAR(30) NOT NULL UNIQUE,
    branch_id VARCHAR(20),
    academic_year SMALLINT NOT NULL DEFAULT 0,
    division VARCHAR(10),
    FOREIGN KEY (branch_id) REFERENCES branches(branch_id)
);

CREATE TABLE IF NOT EXISTS timeslots (
    timeslot_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    weekday VARCHAR(12) NOT NULL,
    period_number SMALLINT NOT NULL,
    start_time TIME,
    end_time TIME,
    UNIQUE (weekday, period_number),
    CHECK (weekday IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
    CHECK (period_number > 0)
);

CREATE TABLE IF NOT EXISTS bookings (
    booking_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_code VARCHAR(20) NOT NULL,
    period_type VARCHAR(20) NOT NULL,
    faculty_id VARCHAR(100) NOT NULL,
    room_id VARCHAR(100) NOT NULL,
    timeslot_id BIGINT NOT NULL,
    FOREIGN KEY (course_code, period_type) REFERENCES courses(course_code, period_type),
    FOREIGN KEY (faculty_id) REFERENCES faculty(faculty_id),
    FOREIGN KEY (room_id) REFERENCES rooms(room_id),
    FOREIGN KEY (timeslot_id) REFERENCES timeslots(timeslot_id),
    -- Each row is a complete session; sections are related below.
    UNIQUE (faculty_id, room_id, timeslot_id),
    UNIQUE (faculty_id, timeslot_id),
    UNIQUE (room_id, timeslot_id)
);

CREATE TABLE IF NOT EXISTS booking_sections (
    booking_id BIGINT NOT NULL,
    section_id BIGINT NOT NULL,
    PRIMARY KEY (booking_id, section_id),
    FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE,
    FOREIGN KEY (section_id) REFERENCES sections(section_id)
);
