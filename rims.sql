*DBMS FINAL FINAL CODE* 

CREATE DATABASE IF NOT EXISTS rims;
USE rims;

CREATE TABLE admin (
    admin_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE user (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(15) NOT NULL
);

CREATE TABLE property (
    property_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    location VARCHAR(100) NOT NULL,
    price_per_month DECIMAL(10,2) NOT NULL,
    availability_status VARCHAR(20) DEFAULT 'Available',
    sharing INT DEFAULT NULL
);

CREATE TABLE booking (
    booking_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    property_id INT,
    start_date DATE,
    end_date DATE,
    status VARCHAR(20) DEFAULT 'Active',
    FOREIGN KEY (user_id) REFERENCES user(user_id),
    FOREIGN KEY (property_id) REFERENCES property(property_id)
);

CREATE TABLE payment (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,
    booking_id INT,
    amount DECIMAL(10,2),
    method VARCHAR(50),
    status VARCHAR(20),
    date DATE,
    FOREIGN KEY (booking_id) REFERENCES booking(booking_id)
);

CREATE TABLE resident (
    resident_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    property_id INT,
    FOREIGN KEY (user_id) REFERENCES user(user_id),
    FOREIGN KEY (property_id) REFERENCES property(property_id)
);

CREATE TABLE feedback (
    feedback_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    property_id INT,
    rating INT,
    comments VARCHAR(255),
    date DATE,
    FOREIGN KEY (user_id) REFERENCES user(user_id),
    FOREIGN KEY (property_id) REFERENCES property(property_id)
);

INSERT INTO admin (username, password) VALUES ('owner', 'RIMS@2025');
INSERT INTO user (name, email, password, phone) VALUES ('Default User', 'user@rims.com', 'user@2025', '9876543210');

INSERT INTO admin (username, password)
SELECT 
  CONCAT('admin', LPAD(FLOOR(RAND()*1000), 3, '0')),
  CONCAT('pass', LPAD(FLOOR(RAND()*10000), 4, '0'))
FROM information_schema.tables
LIMIT 9;

-- 4.2 Add 9 random users
INSERT INTO user (name, email, password, phone)
SELECT 
  CONCAT('User', LPAD(FLOOR(RAND()*10000), 4, '0')),
  CONCAT('user', LPAD(FLOOR(RAND()*10000), 4, '0'), '@mail.com'),
  CONCAT('pw', LPAD(FLOOR(RAND()*10000), 4, '0')),
  CONCAT('9', LPAD(FLOOR(RAND()*1000000000), 9, '0'))
FROM information_schema.tables
LIMIT 9;

-- 4.3 Add 10 random properties
INSERT INTO property (name, type, location, price_per_month, availability_status, sharing)
SELECT
  CONCAT('Property_', LPAD(FLOOR(RAND()*1000), 3, '0')),
  ELT(FLOOR(1 + RAND()*3), 'Apartment', 'House', 'PG'),
  ELT(FLOOR(1 + RAND()*5), 'Hyderabad', 'Bangalore', 'Pune', 'Delhi', 'Chennai'),
  ROUND(5000 + (RAND()*25000), 2),
  ELT(FLOOR(1 + RAND()*3), 'Available', 'Booked', 'Not Available'),
  CASE WHEN RAND() > 0.5 THEN FLOOR(2 + RAND()*4) ELSE NULL END
FROM information_schema.tables
LIMIT 10;

-- 4.4 Add 10 valid random bookings
INSERT INTO booking (user_id, property_id, start_date, end_date, status)
SELECT 
    u.user_id,
    p.property_id,
    DATE_ADD(CURDATE(), INTERVAL -FLOOR(RAND()*90) DAY),
    DATE_ADD(CURDATE(), INTERVAL FLOOR(RAND()*60) DAY),
    ELT(FLOOR(1 + RAND()*3), 'Active', 'Cancelled', 'Completed')
FROM user u
JOIN (
    SELECT property_id FROM property ORDER BY RAND() LIMIT 10
) AS p
ORDER BY RAND()
LIMIT 10;


-- 4.5 Add 10 random payments linked to valid bookings
INSERT INTO payment (booking_id, amount, method, status, date)
SELECT 
    b.booking_id,
    ROUND(5000 + (RAND()*25000), 2),
    ELT(FLOOR(1 + RAND()*3), 'Cash', 'Card', 'UPI'),
    ELT(FLOOR(1 + RAND()*3), 'Paid', 'Pending', 'Refunded'),
    DATE_ADD(CURDATE(), INTERVAL -FLOOR(RAND()*60) DAY)
FROM booking b
ORDER BY RAND()
LIMIT 10;

-- 4.6 Add 10 random residents linked to existing users and properties
INSERT INTO resident (user_id, property_id)
SELECT 
    u.user_id,
    p.property_id
FROM user u
JOIN (
    SELECT property_id FROM property ORDER BY RAND() LIMIT 10
) AS p
ORDER BY RAND()
LIMIT 10;