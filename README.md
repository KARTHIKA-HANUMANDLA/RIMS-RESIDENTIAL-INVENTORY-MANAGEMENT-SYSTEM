Residential Inventory Management System (RIMS)

Project Overview
The Residential Inventory Management System (RIMS) is a console-based application developed using Java and MySQL.
The system helps manage residential properties, bookings, payments, and residents efficiently through a structured database and role-based access control.
The application allows Administrators, Registered Users, and Visitors to interact with the system with different permissions.

Technologies Used:
Java
MySQL
JDBC (Java Database Connectivity)
Object Oriented Programming (OOP)

Features
Admin (Owner)
Add new properties
View all properties
Change property availability status
Delete property records
Manage booking status
Registered User
Register and login
View available properties
Book a property
Cancel booking
Make payments
View previous bookings
Visitor (Looker)
View available properties without registration
Database Tables

The system uses the following database tables:
admin
user
property
booking
payment
resident
feedback

Project Structure
RIMS-Property-Management-System
│
├── src
│ └── Main.java
│
├── database
│ └── rims.sql
│
├── screenshots
│ ├── main_menu.png
│ ├── booking.png
│ ├── payment.png
│
└── README.md

How to Run the Project
Install Java JDK.
Install MySQL Server.
Import the SQL file:
database/rims.sql
Update database credentials in the Java code:
DB_USER = root
DB_PASS = root

Compile the Java file:
javac Main.java
Run the program:
java Main

Future Improvements:
Web-based interface
Online payment gateway
Mobile application integration
Real-time notifications
