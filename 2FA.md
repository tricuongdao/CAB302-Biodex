# 2FA Page

## Overview

The current page provides users with two-factor authentication for the desktop application. After users enter their email and password, they will be provided with a 6-digit code sent to their email, with a 10-minute expiry timer.

The application is desktop-based, so it does not require repeated login or a "Trust this device" option.

## User Stories

- As a user, I want a non-intrusive and non-annoying 2FA system, so I can receive a one-time code via email and keep my account protected.
- As a user, I want the ability to request a new code if the code is not sent or has expired, so that I am not locked out or required to go through the login process again.
- As a user, I want to be notified of multiple login attempts so that it can help deter brute-force attempts.

## Functional Requirements

### Code Generation

- Generate a numerical 6-digit code after a successful email and password match.
- Send the code to the user's email.
- The code must have an expiry limit of 10 minutes.
- The user will be provided with the destination email in a partially masked format, for example: `j****@gmail.com`.
- The code should support copy-and-paste functionality.
- When verification is successful, the verification should follow the session behaviour of the pre-existing application.

### Verification Screen

- Display six input boxes for the verification code.
- Automatically submit the code when the 6th number is entered.
- Provide a verification button as a manual submission option.
- Provide a resend option after 30 seconds.
- Track failed attempts per session.
- Lock the user out after 5 failed attempts, requiring a new email/code.
- Provide robust and situation-specific error messages for:
  - Network errors
  - Expired codes
  - Incorrect codes
  - Symbols or invalid characters
  - Incorrect input formatting

## Technical Requirements

### Frontend

- Six-digit input UI with automatic advancement between input boxes.
- Support copy-and-paste functionality.
- Countdown/expiry timer handling.
- Error and lockout states.

### Backend

- Endpoint to generate a verification code.
- Endpoint to verify the verification code.
- Rate limiting per user to help prevent brute-force attempts.
- Server-side attempt counter and lockout mechanism.

### Email Delivery

- Integration with an email delivery service such as SendGrid or SMTP.

### Storage

- Store a hashed version of the verification code.
- Store the code generation timestamp.
- Store the code expiry timestamp.
- Store the attempt count.

## Non-Functional Requirements

- Code generation and email delivery must occur within 10 seconds.
- The 2FA code must be generated using a modern cryptographically secure number generator.
- The code must be hashed and salted when stored temporarily.
- Failed login attempts after 3 attempts must be logged, with the IP address and timestamp stored.

## Data Model

### VerificationCode

| Field | Description |
|---|---|
| `id` | Unique identifier for the verification code |
| `code_hash` | Hashed version of the verification code |
| `created_time` | Timestamp when the code was generated |
| `expiry_time` | Timestamp when the code expires |
| `attempt_count` | Number of failed verification attempts |