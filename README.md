# SafeLink

Android safety-support app for detecting risky messenger conversations with OCR, background detection, and on-device rule-based analysis.

SafeLink helps users notice suspicious conversation patterns earlier and move from risk detection to practical response guidance. The project was built for an engineering competition and focuses on real user flows: direct text analysis, screenshot OCR analysis, background detection, result explanation, response guide, and institution support.

```text
Risky conversation
        ↓
On-device rule-based analysis
        ↓
Risk result + evidence
        ↓
Response guide + institution support
```

## Quick Navigation

- [Overview](#overview)
- [Core Features](#core-features)
- [Demo Flows](#demo-flows)
- [Architecture](#architecture)
- [Detection Logic](#detection-logic)
- [Tech Stack](#tech-stack)
- [Repository Structure](#repository-structure)
- [Run Locally](#run-locally)
- [Current Status](#current-status)

---

## Overview

Digital crimes such as phishing, impersonation, romance scams, investment scams, and coercive relationship patterns often begin inside ordinary conversations. Users may notice the danger too late, or may not know what to do after recognizing it.

SafeLink is designed around two goals:

1. Detect risky signals in conversation text before the situation escalates.
2. Connect the user to concrete response guidance instead of stopping at a simple warning.

The app does not use AI as the main decision maker. The primary judgment is performed by an on-device rule-based detection engine. AI-assisted analysis is used only as a secondary support step for additional context when needed.

---

## Core Features

| Feature | Description | Status |
|---|---|---|
| Text Risk Analysis | Analyzes user-entered conversation text and classifies risk level. | Implemented |
| Screenshot OCR Analysis | Extracts Korean text from screenshots and sends it into the same analysis flow. | Implemented |
| Background Detection | Uses accessibility permission to detect risky visible text and notify the user. | Implemented |
| Result Evidence | Shows keyword, sentence-rule, situation-rule, and AI-assisted evidence. | Implemented |
| Response Guide | Provides response actions based on the detected risk. | Implemented |
| Institution Matching | Recommends relevant support institutions by risk type. | Implemented |
| Local Records | Stores analysis and self-diagnosis records locally with Room DB. | Implemented |

---

## Demo Flows

### 1. Direct Text Analysis

```text
Home
  → Text input
  → Risk analysis
  → Result screen
  → Response guide
  → Institution support
```

This is the most stable main demo flow. It shows the full path from user input to risk evidence and response guidance.

### 2. Screenshot OCR Analysis

```text
Home
  → Select screenshot
  → ML Kit OCR
  → Risk analysis
  → Result screen
  → Response guide
```

This flow demonstrates how a real conversation screenshot can be converted into text and analyzed through the same detection engine.

### 3. Background Detection

```text
Other app screen
  → Accessibility-based text detection
  → On-device risk analysis
  → Warning notification
  → Response guide
```

This flow is the key feature expansion. It shows how SafeLink can detect risky expressions while the user is viewing another app, with explicit accessibility permission.

---

## Architecture

```text
User Input
  ├─ Direct text input
  ├─ Screenshot OCR input
  └─ Background accessibility input
        ↓
DetectionViewModel
        ↓
DetectionRepository
        ↓
DetectionEngine
  ├─ Keyword / regex matching
  ├─ Combo rule scoring
  ├─ Sentence rule scoring
  ├─ Situation rule scoring
  └─ Optional AI-assisted analysis
        ↓
DetectionResult
        ↓
Result Screen
        ↓
Response Guide
        ↓
Institution Support
        ↓
Room DB Record Storage
```

### Design Direction

- Core analysis runs on-device.
- OCR uses on-device ML Kit text recognition.
- Background detection requires explicit user permission.
- AI is a supporting layer, not the primary decision maker.
- If network or AI analysis fails, the on-device result remains usable.
- Sensitive values such as phone numbers and URLs are masked before API use.

---

## Detection Logic

SafeLink combines multiple rule types so that the result does not depend only on simple keyword matching.

```text
Input text
  → Keyword / regex matching
  → Base score calculation
  → Combo rule bonus
  → Sentence rule scoring
  → Situation rule scoring
  → Risk level classification
  → Evidence generation
  → Optional AI-assisted explanation
```

### Risk Levels

| Level | Meaning |
|---|---|
| `SAFE` | No meaningful risk signal detected. |
| `CAUTION` | Some suspicious signs exist and should be checked. |
| `WARNING` | Multiple risk signals are detected. |
| `CRITICAL` | Immediate attention is recommended. |

### Rule Examples

- Institution impersonation plus personal information request
- Urgency plus money transfer request
- Link or app installation plus identity verification
- Secrecy request plus financial demand
- Family impersonation plus new phone number and transfer request
- Romance scam trust-building plus urgent money request
- Guaranteed investment return plus deposit request
- Threat of exposure plus money demand
- Gaslighting through memory denial and blame shifting
- Relationship isolation and control

---

## Tech Stack

| Area | Stack |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material3 |
| Navigation | Navigation Compose |
| State Management | ViewModel, Compose State |
| Dependency Injection | Hilt |
| Local Database | Room |
| OCR | Google ML Kit Korean Text Recognition |
| Network | Retrofit, OkHttp, Gson |
| Async | Kotlin Coroutines, Flow |
| Background Detection | Android AccessibilityService |
| Notification | NotificationChannel, NotificationCompat |
| Build | Gradle Kotlin DSL, KSP |
| Backend Demo | FastAPI mock analysis server |

---

## Repository Structure

```text
safelink/
├── android/                         # Android application
│   └── app/src/main/
│       ├── assets/                  # keyword and institution data
│       ├── java/com/safelink/app/
│       │   ├── background/          # AccessibilityService detection
│       │   ├── data/
│       │   │   ├── local/           # Room DB entities and DAO
│       │   │   ├── model/           # Risk and detection result models
│       │   │   ├── ocr/             # ML Kit OCR service
│       │   │   ├── remote/          # AI-assisted API DTO/service
│       │   │   └── repository/      # Detection and record repositories
│       │   ├── notification/        # Risk notification handling
│       │   ├── security/            # App lock manager
│       │   ├── settings/            # Feature toggles and emergency contact storage
│       │   └── ui/                  # Compose screens and components
│       └── res/                     # App resources and launcher assets
├── backend/                         # Mock API server for integration demo
├── data/                            # API/data reference files
├── docs/                            # Requirements, design, and flow documents
└── GitHub_레포_정리_기준.md          # GitHub portfolio cleanup guide
```

---

## Run Locally

### Android App

1. Open the `android/` directory in Android Studio.
2. Sync Gradle.
3. Select the `app` run configuration.
4. Run on an emulator or Android device.

### Backend Mock Server

```bash
cd backend
pip install -r requirements.txt
uvicorn main:app --reload
```

The Android emulator can access the local server through `10.0.2.2`.

---

## Current Status

- Text analysis flow is implemented.
- Screenshot OCR analysis flow is implemented.
- Background detection and notification flow are implemented.
- Result screen displays keyword, sentence-rule, situation-rule, and AI-assisted evidence.
- Response guide and institution support flow are implemented.
- Local record storage is implemented with Room DB.
- AI-assisted analysis architecture is implemented with mock server and fallback behavior.

---

## Competition Highlights

SafeLink is not positioned as a simple keyword scanner. The project emphasizes a hybrid safety-support structure:

- On-device primary judgment
- Sentence and situation rule evidence
- OCR-based screenshot analysis
- Accessibility-based background detection
- Response guide and institution support
- AI-assisted explanation as a secondary layer

This structure makes the app suitable for explaining both technical implementation and practical user value in an engineering competition setting.
