# 🎓 Student Wellness & Productivity Platform — Java Desktop Application

> A comprehensive JavaFX desktop application designed to support student well-being through smart tools for health tracking, mental wellness, community engagement, and academic organization. Part of an integrated dual-platform project developed at **Esprit School of Engineering**.

---

## 📌 Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Project Architecture](#project-architecture)
- [Modules](#modules)
  - [Module 1 — User Management](#module-1--user-management)
  - [Module 2 — Planning & Events](#module-2--planning--events)
  - [Module 3 — Physical & Nutritional Activities](#module-3--physical--nutritional-activities)
  - [Module 4 — Mental Health](#module-4--mental-health)
  - [Module 5 — Training & Resources](#module-5--training--resources)
  - [Module 6 — Forum & Community](#module-6--forum--community)
- [Database](#database)
- [Getting Started](#getting-started)
- [Team](#team)

---

## Overview

The **Student Wellness Platform** is a dual-platform system composed of:

| Platform | Technology | Purpose |
|---|---|---|
| 🖥️ Desktop Application | Java / JavaFX | Offline-ready rich client with advanced UI |
| 🌐 Web Application | Symfony 6.4 | Browser-accessible companion platform |

Both platforms share a **single unified database**, ensuring real-time data consistency across every touchpoint of the student's journey. The project addresses a critical problem: student cognitive overload caused by the fragmentation of wellness tools. Instead of juggling multiple disconnected apps, students get everything in one place.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17+ |
| UI Framework | JavaFX + FXML |
| Database | MySQL (shared with Symfony web app) |
| ORM / DB Access | JDBC / Hibernate |
| AI / ML | Face recognition (histogram comparison), Content moderation AI, Emotional analysis API |
| PDF Generation | iText / JasperReports |
| Email | JavaMail API |
| Fonts | SF Pro Text, Feather Icons |
| Build Tool | Maven / IntelliJ IDEA |

---

## Project Architecture

```
student-wellness-java/
├── src/
│   └── main/
│       ├── java/
│       │   ├── controllers/          # JavaFX controllers (FXML-bound)
│       │   │   ├── auth/             # Login, registration, Face ID
│       │   │   ├── admin/            # Admin dashboard, user moderation
│       │   │   ├── activities/       # Physical & nutrition tracking
│       │   │   ├── mental/           # Journal, emotional avatar
│       │   │   ├── forum/            # Posts, likes, moderation
│       │   │   └── planning/         # Events & scheduling
│       │   ├── models/               # Entity classes (User, Activity, Post...)
│       │   ├── services/             # Business logic layer
│       │   ├── utils/                # DB connection, AI helpers, validators
│       │   └── Main.java             # Application entry point
│       └── resources/
│           ├── fxml/                 # All FXML view files
│           ├── css/                  # Stylesheets
│           ├── fonts/                # Custom fonts (Feather, SF Pro)
│           └── images/               # Icons and assets
├── database/
│   └── schema.sql                    # Shared DB schema
├── pom.xml
└── README.md
```

---

## Modules

---

### Module 1 — User Management

Complete and secure user lifecycle management with AI-powered safety features.

#### 🔐 Secure Authentication
- Email + encrypted password login
- Strict credential validation on every attempt
- Persistent session with automatic token management

#### 🪪 Face ID Biometric Verification
- Facial capture during registration
- Automatic face verification on each login
- Enable / disable Face ID from the login screen
- Intelligent comparison using adaptive tolerance threshold (histogram-based similarity score)

#### 🤖 CAPTCHA Anti-bot Protection
- Random sequence generation on each login attempt
- Protection against automated login attacks
- Auto-regeneration on every submission

#### 🔑 Email-based Password Recovery
- Secure one-time reset link sent by email
- Time-limited link with automatic expiration
- Step-by-step guided reset flow
- Email confirmation after successful change

#### 📝 Multi-step Registration
| Step | Content |
|---|---|
| Step 1 | Personal information with strict validation |
| Step 2 | Physical & academic data (weight, height, level) |
| Step 3 | Optional facial capture to activate Face ID |

Data is preserved between steps for seamless navigation.

#### 🤖 AI Content Moderation
- Automatic analysis of uploaded profile photos
- Intelligent detection of gore and sensitive content
- Immediate blocking and real-time flagging
- Community protection from account creation

#### ✏️ User Profile Management
- Real-time personal info editing
- Instant profile photo upload and preview
- Password change with dynamic strength indicator
- Physical and academic data update at any time

#### 🛡️ Admin — Suspicious Account Detection
- Automatic suspicion score calculated out of 100
- Multi-criteria analysis: suspicious keywords, repetitions, disposable email domain, etc.
- Filter & sort accounts by risk level: `Normal` / `Moderate` / `Suspicious` / `Very Suspicious`
- One-click account suspension and reactivation

#### 📊 Admin Dashboard
- Real-time stats: total users, active, suspended
- Interactive charts: gender distribution, academic level, physical activity
- Registration trend over the last 12 months
- Full PDF export of statistical report

---

### Module 2 — Planning & Events

> _Full feature details coming soon._

Smart scheduling and event management to help students organize their academic and personal life.

---

### Module 3 — Physical & Nutritional Activities

Complete wellness tracking — exercise, nutrition, sleep — in one unified module.

#### 🏋️ Physical Activity Management
- Add, edit, delete and display workout sessions
- Track calories burned, sets, reps, and weights
- Exercise catalog with name, type, and associated video

#### 🥗 Nutrition Tracking
- Log food consumption by meal type (breakfast, lunch, dinner)
- Track water intake and food quantity in grams
- Full nutritional database: calories, proteins, carbohydrates, fats per 100g

#### 😴 Sleep Monitoring
- Record sleep sessions with bedtime and wake-up time
- Quality assessment and disturbance factor tracking (stress, caffeine, noise)

#### 🔍 Smart Search & Navigation
- Filter activities, meals, and sleep records by user and date
- Chronological sorting (most recent first)
- Smooth navigation from main menu (`AccueilController`)

#### 🤖 AI Features
- Comparative histogram analysis for identity verification (`histScore`)

#### 🎨 UI/UX Highlights
- JavaFX interface with dynamic FXML view loading
- **Innovative 3D display** for the exercise journal (`chargerExercices3D`)
- Custom font loading (Feather, SF-Pro-Text-Bold, SF-Pro-Text-Light)
- Formatted sleep schedule display (`dd/MM/yyyy à HH:mm`)
- Modal dialogs for add/edit operations

---

### Module 4 — Mental Health

A private, AI-enhanced mental wellness space for students.

#### 📚 Author & Advice Management
- Admin manages authors and wellness content
- Curated tips and resources to reduce student stress

#### 📓 Smart Private Journal
- Personal diary with full confidentiality
- Supports manual text entry, voice input, and free emotional expression

#### 🧠 AI Emotional Analysis (PDF Report)
After each journal entry, the system automatically generates a report including:
- General emotional assessment
- Detection of stress, sadness, or anxiety
- Anonymous global statistics
- Downloadable PDF format

#### 😊 Emotional Avatar
Visual avatar adapting to the student's detected emotional state:
`Happy` · `Stressed` · `Sad` · `Tired` · `Anxious`

---

### Module 5 — Training & Resources

> _Full feature details coming soon._

Access to curated learning resources, tutorials, and academic support materials.

---

### Module 6 — Forum & Community

A safe, AI-moderated community space for students to connect and share.

#### ❤️ User Interactions
- Interactive like system with instant feedback
- Simple and responsive desktop interface

#### 🌍 Automatic Translation
- Instant content translation
- Multi-language support for international students

#### ✍️ Intelligent Writing Assistance
- Real-time spell checking
- Automatic quality improvement suggestions

#### 🛡️ Automatic Moderation
- Profanity and offensive content detection
- Intelligent post filtering
- Maintains a respectful community environment

#### 🖼️ AI Image Generation
- Text-to-image generation for post illustrations
- Modern AI-powered visual personalization

#### 🔎 Smart Search
- Advanced keyword-based search
- Fast and relevant results

#### 📊 Statistics Dashboard
- Forum activity overview
- User engagement tracking and interaction analytics

---

## Database

Both the Java desktop application and the Symfony 6.4 web application share a **single MySQL database**.

```
┌─────────────────────┐         ┌─────────────────────┐
│   Java Desktop App  │         │  Symfony 6.4 Web App │
│     (JavaFX)        │         │     (PHP/Twig)       │
└────────┬────────────┘         └──────────┬──────────┘
         │                                 │
         └──────────────┬──────────────────┘
                        │
               ┌────────▼────────┐
               │   MySQL Database │
               │  (Shared Schema) │
               └─────────────────┘
```

To initialize the database:
```bash
mysql -u root -p < database/schema.sql
```

---

## Getting Started

### Prerequisites

- Java 17 or higher
- JavaFX SDK 17+
- MySQL 8.0+
- Maven 3.8+
- IntelliJ IDEA (recommended)

### Installation

```bash
# 1. Clone the repository
git clone https://github.com/your-org/student-wellness-java.git
cd student-wellness-java

# 2. Set up the database
mysql -u root -p < database/schema.sql

# 3. Configure database connection
# Edit src/main/java/utils/DatabaseConnection.java
DB_URL=jdbc:mysql://localhost:3306/student_wellness
DB_USER=your_username
DB_PASSWORD=your_password

# 4. Build the project
mvn clean install

# 5. Run the application
mvn javafx:run
```

### Configuration

| Parameter | Location | Description |
|---|---|---|
| DB Connection | `utils/DatabaseConnection.java` | MySQL host, port, credentials |
| Email (SMTP) | `utils/EmailService.java` | Mail server config for password reset |
| Face ID Threshold | `utils/FaceRecognitionUtils.java` | Similarity tolerance (default: 0.75) |
| AI API Key | `utils/AIService.java` | Key for emotional analysis API |

---

## Team

Developed by students at **Esprit School of Engineering** as part of the Integrated Project evaluation.

| Member | Module |
|---|---|
| [Name 1] | Module 1 — User Management |
| [Name 2] | Module 2 — Planning & Events |
| [Name 3] | Module 3 — Physical & Nutritional Activities |
| [Name 4] | Module 4 — Mental Health |
| [Name 5] | Module 5 — Training & Resources |
| [Name 6] | Module 6 — Forum & Community |

> 📌 **Academic Supervisor:** [Tutor Name] — Esprit School of Engineering

---

## License

This project was developed for academic purposes at Esprit School of Engineering. All rights reserved © 2024–2025.
