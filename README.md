# Leave Management System (LMS) - Spring Boot

A comprehensive Leave Management System built with Spring Boot, featuring role-based authentication, leave request management, and team reports.

## Features

- **Authentication System**: Login with different roles (Employee, Manager, Admin)
- **Role-Based Access Control**: Different views and permissions for each role
- **Leave Management**: Submit, approve, and reject leave requests
- **Leave Balance**: Track annual, sick, and family leave balances
- **Team Reports**: Managers can view team leave reports
- **Email Notifications**: Simulated email notifications for leave requests
- **H2 Database**: In-memory database for easy testing
- **RESTful API**: Well-structured API endpoints
- **Modern UI**: Black and yellow themed interface

## Technology Stack

- **Backend**: Spring Boot 3.2.3, Spring MVC, Spring Data JPA
- **Database**: H2 (In-memory)
- **Build Tool**: Maven
- **Frontend**: HTML, CSS, JavaScript
- **Security**: Session-based authentication

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- IntelliJ IDEA (recommended) or any Java IDE

## Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/YOUR_USERNAME/lms-spring-boot.git
   cd lms-spring-boot

Demo Credentials
Role	Username	Password
Employee	john	employee123
Employee	jane	employee123
Manager	mike	manager123
Manager	sarah	manager123
Admin	admin	admin123