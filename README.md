# SafeLink

SafeLink is an Android safety-support application that detects suspicious signals in messenger or SMS conversations and connects users to practical response guidance.

The project was developed for an engineering competition. It focuses on three user flows:

- Text-based risk analysis
- Screenshot-based conversation analysis using OCR
- Background risk detection with notification and response guidance

SafeLink does not rely on AI as the main decision maker. The core judgment is performed by an on-device rule-based detection engine, and AI-assisted analysis is used only as a secondary support step when additional context is needed.

---

## Key Features

### Conversation Risk Analysis

- Analyzes user-entered conversation text
- Detects risk signals using keyword, regex, sentence-rule, and situation-rule logic
- Classifies the result into `SAFE`, `CAUTION`, `WARNING`, or `CRITICAL`
- Shows matched keywords and analysis evidence on the result screen

### Screenshot OCR Analysis

- Uses Android Photo Picker to select conversation screenshots
- Extracts Korean text with Google ML Kit Text Recognition
- Sends extracted text into the same detection flow as text input
- Handles empty or too-short OCR results with user-facing feedback

### Background Detection

- Uses Android `AccessibilityService`
- Detects visible text from other app screens when the user explicitly enables accessibility permission
- Analyzes detected text with the same on-device detection engine
- Shows neutral warning notifications through `NotificationCompat`
- Connects notification taps to response guidance or emergency flow

### Response Guide and Institution Matching

- Provides risk-level-based response guidance
- Recommends relevant support institutions by risk category
- Connects users to phone, web, or support application guidance

### Local Record Storage

- Stores detection and self-diagnosis records locally with Room DB
- Allows users to revisit past analysis results
- Keeps records on the device instead of sending them to an external server

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

---

## Architecture Overview

```text
User Input
  ├─ Text input
  ├─ Screenshot OCR
  └─ Background accessibility detection
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
  └─ AI-assisted analysis fallback
        ↓
DetectionResult
        ↓
Result Screen → Response Guide → Institution Support
        ↓
Room DB Record Storage
```

---

## Main Modules

```text
safelink/
├── android/                         # Android application
│   └── app/src/main/java/com/safelink/app/
│       ├── background/              # AccessibilityService-based background detection
│       ├── data/
│       │   ├── local/               # Room DB entities and DAO
│       │   ├── model/               # Detection result and risk models
│       │   ├── ocr/                 # ML Kit OCR service
│       │   ├── remote/              # AI-assisted analysis API DTO/service
│       │   └── repository/          # Detection and record repositories
│       ├── notification/            # Risk notification handling
│       ├── security/                # App lock manager
│       ├── settings/                # Feature toggles and emergency contact storage
│       └── ui/                      # Compose screens and reusable components
├── backend/                         # Mock analysis server for API integration demo
├── data/                            # Data/API reference files
└── docs/                            # Requirements, design, flow, and task documents
```

---

## Core Detection Logic

The detection engine calculates risk in the following order:

```text
Input text
→ Keyword / regex matching
→ Base score calculation
→ Combo rule bonus
→ Direct sentence and situation rule scoring
→ Risk level classification
→ Analysis evidence generation
→ Optional AI-assisted analysis
```

Direct sentence and situation rules cover patterns such as:

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

## Privacy and Safety Design

- Core detection runs on-device.
- OCR is performed with on-device ML Kit text recognition.
- Detection records are stored locally with Room DB.
- Accessibility-based background detection requires explicit user permission.
- AI-assisted analysis is optional and secondary.
- Sensitive values such as phone numbers and URLs are masked before API use.
- If the AI API fails, the on-device analysis result remains available.

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
- Result screen displays keyword, sentence-rule, situation-rule, and AI-assisted analysis evidence.
- Local record storage is implemented with Room DB.
- AI-assisted analysis architecture is implemented with a mock server and fallback behavior.

---

## Team Role

SafeLink was developed as a team engineering competition project. The repository includes Android implementation, analysis logic, OCR/background detection features, documentation, and presentation materials.
