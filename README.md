# Sign Up / Login Page — Specification

## Document Control

| Field | Value |
|---|---|
| Module | Login / Signup Screen |
| Page Owner | Ali |
| Related Sub-pages | Forgot Password (Vinny), 2FA (Tom), Profile → Settings → Account (Yash) |
| App | Biodex — Invasive Species Tracker |
| Tech Stack | JavaFX (UI), SQLite via JDBC (persistence) |
| Status | Draft v0.1 — pending full assignment brief |

---

## 1. Overview

The Login/Signup screen is the first thing a user sees when the app opens. No other part of Biodex is reachable until someone has signed in — the home screen, uploads, heat map and profile all assume there is a known user.

Signup creates a new account. Login checks an existing one and starts the session that every other page reads from.

This module also owns the user account records themselves, which means the other pages depend on it: Settings edits the account, Forgot Password resets its password, and 2FA sits inside the login flow as an extra step before the session starts.

---

## 2. Scope

**In scope**

- The login form and its behaviour
- The signup form and its behaviour
- Creating the session on successful sign-in
- Logging out
- The links out to Forgot Password and 2FA, and what those pages are handed

**Out of scope (owned elsewhere)**

- The password reset wizard — Vinny
- The 2FA code entry screen — Tom
- Changing your password while already signed in — Yash (Settings → Account)
- Everything after sign-in

---

## 3. User Stories

- As a **new user**, I want to create an account with my email and a password, so that I can start logging pest sightings.
- As a **returning user**, I want to sign in quickly with either my username or my email, so that I don't have to remember which one I registered with.
- As a **user**, I want to be told what's wrong with my password while I'm typing it, so that I'm not guessing after the form rejects me.
- As a **user who mistyped something**, I want a clear error message that doesn't make me re-enter everything, so that signing in isn't frustrating.
- As a **user**, I don't want the app to reveal whether an account exists when someone guesses wrong, so that my account is harder to target.
- As a **user who forgot my password**, I want an obvious way to recover my account from the login screen, so that I'm not locked out permanently.
- As a **security-conscious user**, I want the option of a second verification step, so that my password alone isn't enough to get into my account.

---

## 4. Functional Requirements

| # | Requirement                                                                                 | Priority |
|---|---------------------------------------------------------------------------------------------|---|
| F1 | The app opens on the login screen when no one is signed in                                  | Must |
| F2 | A user can sign in using either their username or their email, plus a password              | Must |
| F3 | Passwords are checked against a stored scrambled version, never against plain text          | Must |
| F4 | A failed sign-in shows one generic message, whatever the actual cause                       | Must |
| F5 | A user can create an account with a display name, username, email and password              | Must |
| F6 | Signup rejects a username or email that is already registered, and says which one           | Must |
| F7 | Passwords must be at least 8 characters and contain a letter and a number                   | Must |
| F8 | The confirm-password field must match before signup can be submitted                        | Must |
| F9 | A live checklist shows which password rules are met while typing                            | Should |
| F10 | Creating an account also creates that user's default settings                               | Must |
| F11 | A successful sign-in records who is signed in, for other pages to read                      | Must |
| F12 | If the account has 2FA turned on, the user goes to the code screen before being signed in   | Must |
| F13 | The login screen has a "Forgot Password?" link                                              | Must |
| F14 | After 5 failed attempts in a row, the account is locked for 1 min                           | Should |
| F15 | Fields disable and a loading spinner shows while the app is checking, to stop double-clicks | Should |
| F16 | Logging out clears the session and returns to the login screen                              | Must |
| F17 | The app can remember the username for next launch, but never the password                   | Could |
| F18 | Password fields have a show/hide toggle                                                     | Could |
| F19 | Pressing Enter submits the form                                                             | Should |

---

## 5. Screen Description

Both screens are a single centred card on a plain background, so they feel like one screen with two states. The card is a fixed width so the fields don't stretch awkwardly on a large window.

### Login screen

- App logo and a heading
- One field for username or email
- Password field
- "Show password" checkbox
- Error message area (hidden until needed)
- "Sign In" button — the default button, so Enter works
- Loading spinner (hidden until the app is checking)
- "Forgot Password?" link → opens Vinny's reset wizard
- "Don't have an account? Create one" link → opens the signup screen

### Signup screen

- Heading
- Display name field
- Username field
- Email field
- Password field
- Confirm password field
- Live password rule checklist (three lines: length, letter, number — each ticks as it's satisfied)
- Error message area
- "Create Account" button — stays disabled until the form is valid
- "Already have an account? Sign in" link → back to login

### Error display

Two kinds of error, shown differently:

- **Field errors** (bad email format, username taken) put a red outline on the offending field and a short message directly underneath it, so it's obvious which field is the problem.
- **Form errors** (wrong credentials, database unreachable) show once in the card's error area, since they don't belong to any one field.

### UI sketches

*(to be added)*

---

## 6. Validation Rules

| Field | Rule | What the user sees if it fails                     |
|---|---|----------------------------------------------------|
| Display name | Not empty, up to 100 characters | Message under the field                            |
| Username | 3–20 characters, letters, numbers and underscores only | Message under the field                            |
| Username (signup) | Must not already be taken | "That username is already taken."                  |
| Email | Must be a valid email format | Message under the field                            |
| Email (signup) | Must not already be registered | "An account with that email already exists."       |
| Password | At least 8 characters, at least one letter, at least one number | Live checklist; Create Account stays disabled      |
| Confirm password | Must match the password field | "Passwords don't match."                           |
| Sign-in credentials | Must match a registered account | "Incorrect username or password."                  |
| Locked account | 5 failed attempts within the lockout window | "Too many failed attempts. Try again in 1 minute." |
| Database unreachable | — | "Couldn't reach the database. Please try again."   |

The password rule is deliberately the same one used by Forgot Password and by Change Password in Settings, so a user never meets three different definitions of an acceptable password.

On the generic login message (F4): the app says the same thing whether the username doesn't exist or the password was wrong. If it distinguished them, anyone could use the login form to find out which usernames are registered. Signup is the deliberate exception — it has to say "that username is taken" or the form would be impossible to use.

---

## 7. What Happens on Sign-In

1. User enters their details and presses Sign In.
2. The app looks up the account by username or email.
3. If the account is currently locked, it says so and stops.
4. The password is checked against the stored scrambled version.
5. If it doesn't match, the failure is counted and the generic error shows.
6. If it matches, the failure count resets.
7. **If 2FA is off:** the user is recorded as signed in and lands on the home screen.
8. **If 2FA is on:** the user is held in a "pending" state — not signed in — and sent to Tom's code screen. Only once the code is verified does the app record them as signed in and continue to the home screen. Cancelling returns to login and clears the pending state.

**On signup:** the app checks the username and email aren't taken, scrambles the password, creates the account and its default settings row, then returns to the login screen with an "Account created, please sign in" banner.

All of this happens in the background so the window doesn't freeze — the password check is intentionally slow (that's what makes it hard to attack), so it can't run on the same thread as the UI.

---

## 8. What Other Pages Get From This One

Other modules should build against these, and let me know if they need something that isn't here.

### The user account record

Every account stores: an ID, username, email, the scrambled password, display name, whether 2FA is enabled, the failed-attempt count and lock expiry, the last sign-in time, and when the account was created.

The ID is stable and safe for other tables to reference — sightings, photos, settings and reset codes all point at it. Deleting an account removes those records with it.

The scrambled password should never be read directly by another page. If you need to check or change a password, go through the shared helper rather than comparing it yourself.

### The current session

Any page that requires sign-in can ask the app who is currently signed in and get the user record back. This is how Settings knows whose settings to load, and how the upload page knows who to attribute a sighting to.

The session lives in memory only and is cleared on logout. Because this is a desktop app there is only ever one session — there's no "sign out my other devices" concept.

### Shared validation

Email, username and password rules live in one shared place. Any screen that asks for a password should use the shared check rather than writing its own, so the rules stay identical everywhere.

### The 2FA hand-off

The login flow hands Tom's screen a pending user and expects one of two outcomes back: **verified** (sign them in, go home) or **cancelled** (return to login). Being in the pending state is not the same as being signed in, and no page should treat it as such.

---

## 9. Open Questions

1. **Package naming** — the existing specs use `com.pesttracker` but the app is now called Biodex. We should settle on one before anyone writes code.
2. **Are external libraries allowed?** The plan is to use a standard password-scrambling library. If the brief says standard library only, there's a workable fallback, but it changes the implementation.
3. **Is 2FA on by default or opt-in?** This spec assumes opt-in, which means Settings needs a toggle for it — currently listed as out of scope in Yash's spec.
4. **Reset codes and 2FA codes are the same mechanism** — both are a 6-digit emailed code with an expiry and an attempt limit. Vinny and Tom should decide whether to share one implementation rather than building two.
5. **Deleting an account** — permanent delete, or mark as deleted and keep the data? Raised in Yash's spec; affects what happens to that user's sightings.

---

## 10. Not in Version 1

- Sign in with Google or similar
- Verifying the email address before the account can be used
- A password strength meter beyond the pass/fail checklist
- Admin or moderator accounts for reviewing uploads

---

## Changelog

| Version | Date | Change |
|---|---|---|
| v0.1 | — | Initial draft, pending full assignment brief |

---

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

---

**Forgot Password Flow**

Biodex  ·  Invasive Species Tracker  ·  Draft v1.0

## **Document Control**

| Field | Value |
| - | - |
| Module | Login/Signup Screen -\> Forgot Password -\> Reset Password |
| Page Owner | Bernard |
| Related 	Sub-pages | Login, Signup, Settings -\> Account (Change Password) |
| App | Biodex |
| Tech Stack | JavaFX (UI/Controllers), SQL via JDBC (persistence), JavaMail/SMTP 	(email) |
| Status | Draft v1.0 |


**Contents**

Document Control............................................................................... 1

1. Overview............................................................................................. 1

2. Scope Within the Login/Signup Screen.................................................. 1

3. Functional Requirements..................................................................... 1

4. UI Flow................................................................................................ 1

Step 1, Request Code........................................................................... 1

Step 2, Verify Code.............................................................................. 1

Step 3, Reset Password........................................................................ 1

5. Controller Architecture........................................................................ 1

6. Data Model and Schema...................................................................... 1

7. DAO Reference.................................................................................... 1

8. Validation and Error Handling............................................................... 1

9. Security Considerations........................................................................ 1

10. Testing Plan....................................................................................... 1

11. Suggested File Structure..................................................................... 1

# **1. Overview**

The Forgot Password flow allows users who cannot sign in to recover access to Biodex without contacting support. It is opened from the "Forgot Password?" hyperlink beneath the password field on the Login/Signup screen.

Because Biodex is a JavaFX desktop application, an emailed reset link cannot open directly in the app. The system therefore emails a 6-digit verification code for the user to enter in the application. Each code remains valid for 15 minutes.

# **2. Scope Within the Login/Signup Screen**

Login/Signup Screen  
|-- LoginView.fxml  
|     |-- username/password fields  
|     |-- Hyperlink "Forgot Password?" -\> opens ForgotPasswordView  (this spec)  
|     \`-- Hyperlink "Create an account" -\> opens SignupView  
|  
|-- SignupView.fxml (out of scope here)  
|  
\`-- Forgot Password wizard:  
ForgotPasswordView.fxml  (enter email, request code)  
-\> VerifyCodeView.fxml (enter 6-digit code)  
-\> ResetPasswordView.fxml (set new password)  
-\> back to LoginView with a "Password updated" banner

The Login screen starts a one-way wizard that finishes by returning the user to LoginView. Users cannot navigate directly to either of the later steps.

# **3. Functional Requirements**

| \# | Requirement | Priority |
| :-: | - | :-: |
| F0 | A "Forgot Password?" link appears below the password field on the Login screen | Must |
| F1 | A user starts password recovery by entering the email linked to their account | Must |
| F2 | The system sends a 6-digit email code that expires after 15 minutes | Must |
| F3 | The user submits the code, with invalid codes producing an inline error | Must |
| F4 | The user may request another code after a 60-second cooldown | Should |
| F5 | Each verification code can be used only once | Must |
| F6 | After verification, the user enters and confirms a new password | Must |
| F7 | The new password must meet the rules used by the Change Password feature | Must |
| F8 | A successful reset invalidates the user's other active sessions | Should |
| F9 | Step 1 behaves the same for registered and unregistered email addresses | Must |
| F10 | Expired or exhausted codes direct the user to "request a new code" | Must |
| F11 | Input fields are disabled and progress is shown while asynchronous work runs | Should |
| F12 | Password-reset requests are rate-limited by email address and IP address | Should |


# **4. UI Flow**

## **Step 1, Request Code**

This screen contains an email field, a "Send Code" button, and a "Back to Login" link. Any correctly formatted address receives the same confirmation and proceeds to Step 2, regardless of whether the address belongs to an account (F9).

## **Step 2, Verify Code**

The heading displays "We sent a code to \{maskedEmail\}" above the 6-digit code field and "Verify" button. The "Resend code" link remains disabled for 60 seconds after a code is sent. Five incorrect attempts invalidate the code and return the user to Step 1.

## **Step 3, Reset Password**

Provides new-password and confirmation fields, a live checklist of password rules, and a "Reset Password" button.

*\[Wireframe and basic mock-up included at the end of this document\]*

# **5. Controller Architecture**

All three controllers use a shared in-memory PasswordResetSession that stores the email, userId, and codeVerified flag. SessionManager continues to manage signed-in users and must remain separate from the reset session.

public class ForgotPasswordController implements Initializable \{

```
@FXML private TextField emailField;    
@FXML private Label errorLabel;    
  
private final UserDAO userDAO \\= new UserDAO(DatabaseConnection.getConnection());    
private final PasswordResetDAO resetDAO \\= new PasswordResetDAO(DatabaseConnection.getConnection());    
private final EmailService emailService \\= new EmailService();    
  
@FXML    
private void handleSendCode() \{    
    String email \\= emailField.getText().trim();    
    if (\\!Validator.isValidEmail(email)) \{    
        showError("Enter a valid email address.");    
        return;    
    \}    
    Task\\\<Void\\\> task \\= new Task\\\<\\\>() \{    
        protected Void call() \{    
            // Look up the user, but keep the UI path identical either way (F9)    
            User user \\= userDAO.findByEmail(email);    
            if (user \\!= null) \{    
                String code \\= CodeGenerator.sixDigit();    
                resetDAO.createResetCode(user.getUserId(), code, Duration.ofMinutes(15));    
                emailService.sendResetCode(email, code);    
            \}    
            return null;    
        \}    
    \};    
    task.setOnSucceeded(e \\-\\\> \{    
        PasswordResetSession.getInstance().setEmail(email);    
        SceneRouter.getInstance().goToVerifyCode();    
    \});    
    new Thread(task).start();    
\}    
  
private void showError(String msg) \{ errorLabel.setText(msg); errorLabel.setVisible(true); \}    
  
@Override public void initialize(URL location, ResourceBundle resources) \{\}  
```

\}

public class VerifyCodeController implements Initializable \{

```
@FXML private TextField codeField;    
@FXML private Label errorLabel;    
  
private final PasswordResetDAO resetDAO \\= new PasswordResetDAO(DatabaseConnection.getConnection());    
  
@FXML    
private void handleVerifyCode() \{    
    String code \\= codeField.getText().trim();    
    String email \\= PasswordResetSession.getInstance().getEmail();    
  
    Task\\\<Boolean\\\> task \\= new Task\\\<\\\>() \{    
        protected Boolean call() \{ return resetDAO.verifyCode(email, code); \}    
    \};    
    task.setOnSucceeded(e \\-\\\> \{    
        if (Boolean.TRUE.equals(task.getValue())) \{    
            PasswordResetSession.getInstance().setCodeVerified(true);    
            SceneRouter.getInstance().goToResetPassword();    
        \} else \{    
            errorLabel.setText("Incorrect or expired code.");    
            errorLabel.setVisible(true);    
        \}    
    \});    
    new Thread(task).start();    
\}    
  
@Override public void initialize(URL location, ResourceBundle resources) \{\}  
```

\}

public class ResetPasswordController implements Initializable \{

```
@FXML private PasswordField newPasswordField;    
@FXML private PasswordField confirmPasswordField;    
@FXML private Label errorLabel;    
  
private final UserDAO userDAO \\= new UserDAO(DatabaseConnection.getConnection());    
private final PasswordResetDAO resetDAO \\= new PasswordResetDAO(DatabaseConnection.getConnection());    
  
@FXML    
private void handleResetPassword() \{    
    if (\\!PasswordResetSession.getInstance().isCodeVerified()) \{    
        SceneRouter.getInstance().goToLogin();    
        return;    
    \}    
    String pw \\= newPasswordField.getText();    
    String confirm \\= confirmPasswordField.getText();    
  
    if (\\!Validator.isStrongPassword(pw)) \{ showError("Min 8 chars, 1 letter, 1 number."); return; \}    
    if (\\!pw.equals(confirm)) \{ showError("Passwords don't match."); return; \}    
  
    String email \\= PasswordResetSession.getInstance().getEmail();    
    Task\\\<Void\\\> task \\= new Task\\\<\\\>() \{    
        protected Void call() \{    
            User user \\= userDAO.findByEmail(email);    
            userDAO.updatePasswordHash(user.getUserId(), PasswordHasher.hash(pw));    
            resetDAO.invalidateAllCodesForUser(user.getUserId());    
            SessionManager.getInstance().invalidateOtherSessions(user.getUserId()); // F8    
            return null;    
        \}    
    \};    
    task.setOnSucceeded(e \\-\\\> \{    
        PasswordResetSession.getInstance().clear();    
        SceneRouter.getInstance().goToLoginWithBanner("Password updated. Please sign in.");    
    \});    
    new Thread(task).start();    
\}    
  
private void showError(String msg) \{ errorLabel.setText(msg); errorLabel.setVisible(true); \}    
  
@Override public void initialize(URL location, ResourceBundle resources) \{\}  
```

\}

# **6. Data Model and Schema**

public class PasswordResetCode \{  
private int resetId;  
private int userId;  
private String codeHash;   // hashed, not plaintext  
private Instant expiresAt;  
private int attemptCount;  
private boolean used;  
private Instant createdAt;  
private Instant lastSentAt; // resend cooldown  
\}

CREATE TABLE password\_reset\_codes (  
reset\_id   	INTEGER PRIMARY KEY AUTOINCREMENT,  
user\_id    	INTEGER NOT NULL,  
code\_hash  	VARCHAR(255) NOT NULL,  
expires\_at 	TIMESTAMP	NOT NULL,  
attempt\_count  INTEGER      DEFAULT 0,  
used       	BOOLEAN  	DEFAULT 0,  
created\_at 	TIMESTAMP	DEFAULT CURRENT\_TIMESTAMP,  
last\_sent\_at   TIMESTAMP    DEFAULT CURRENT\_TIMESTAMP,  
FOREIGN KEY (user\_id) REFERENCES users(user\_id) ON DELETE CASCADE  
);

CREATE INDEX idx\_reset\_user\_id ON password\_reset\_codes(user\_id);

No changes are required in the users table. Reset codes are stored separately, allowing expired and used records to remain without changing password\_hash until the new hash is saved in Step 3.

# **7. DAO Reference**

public class PasswordResetDAO \{

```
private final Connection connection;    
private static final int MAX\\\_ATTEMPTS \\= 5;    
  
public PasswordResetDAO(Connection connection) \{ this.connection \\= connection; \}    
  
public void createResetCode(int userId, String plainCode, Duration validFor) \{    
    invalidateAllCodesForUser(userId);    
    String sql \\= "INSERT INTO password\\\_reset\\\_codes (user\\\_id, code\\\_hash, expires\\\_at) VALUES (?, ?, ?)";    
    try (PreparedStatement ps \\= connection.prepareStatement(sql)) \{    
        ps.setInt(1, userId);    
        ps.setString(2, PasswordHasher.hash(plainCode));    
        ps.setTimestamp(3, Timestamp.from(Instant.now().plus(validFor)));    
        ps.executeUpdate();    
    \} catch (SQLException e) \{ throw new DataAccessException(e); \}    
\}    
  
public boolean verifyCode(String email, String plainCode) \{    
    String sql \\= "SELECT prc.\\\* FROM password\\\_reset\\\_codes prc "    
                \\+ "JOIN users u ON u.user\\\_id \\= prc.user\\\_id "    
                \\+ "WHERE u.email \\= ? AND prc.used \\= 0 "    
                \\+ "ORDER BY prc.created\\\_at DESC LIMIT 1";    
    try (PreparedStatement ps \\= connection.prepareStatement(sql)) \{    
        ps.setString(1, email);    
        ResultSet rs \\= ps.executeQuery();    
        if (\\!rs.next()) return false;    
        if (rs.getTimestamp("expires\\\_at").toInstant().isBefore(Instant.now())) return false;    
        if (rs.getInt("attempt\\\_count") \\\>= MAX\\\_ATTEMPTS) return false;    
  
        boolean matches \\= PasswordHasher.verify(plainCode, rs.getString("code\\\_hash"));    
        if (matches) markUsed(rs.getInt("reset\\\_id"));    
        else incrementAttempts(rs.getInt("reset\\\_id"));    
        return matches;    
    \} catch (SQLException e) \{ throw new DataAccessException(e); \}    
\}    
  
public void invalidateAllCodesForUser(int userId) \{    
    String sql \\= "UPDATE password\\\_reset\\\_codes SET used \\= 1 WHERE user\\\_id \\= ? AND used \\= 0";    
    try (PreparedStatement ps \\= connection.prepareStatement(sql)) \{    
        ps.setInt(1, userId);    
        ps.executeUpdate();    
    \} catch (SQLException e) \{ throw new DataAccessException(e); \}    
\}    
  
private void markUsed(int resetId) \{ /\\\* SET used \\= 1 WHERE reset\\\_id \\= ? \\\*/ \}    
private void incrementAttempts(int resetId) \{ /\\\* SET attempt\\\_count \\= attempt\\\_count \\+ 1 WHERE reset\\\_id \\= ? \\\*/ \}  
```

\}

All query values are bound using PreparedStatement, and no column names are constructed dynamically.

# **8. Validation and Error Handling**

| Field | Rule | Failure Behaviour |
| - | - | - |
| Email (Step 1) | Non-empty, 			valid format | Inline error; identical response whether or not an account exists |
| Code (Step 2) | Exactly 6 digits | Inline error; PasswordResetDAO records the failed attempt |
| Code expiry | PasswordResetDAO checks expires\_at | "Code expired. Request a new code." |
| Max attempts | The code is invalidated after 5 incorrect attempts | "Too many attempts. Request a new code." |
| New password | At least 8 characters, including 1 letter and 1 number | Inline error; submission remains disabled until the value is valid |
| Confirm	password | Must match the new password exactly | "Passwords do not match." |
| DB/email	failure | Any exception raised inside a Task | Non-blocking alert; the form remains available for another attempt |


# **9. Security Considerations**

•     Step 1 gives registered and unknown addresses the same response, preventing attackers from identifying which emails have accounts (F9).

•     PasswordResetDAO stores only a hash of each code by using the same PasswordHasher used for account passwords; no readable reset code is kept in the database.

•     Codes expire after 15 minutes, and verifyCode marks a matching code as used immediately.

•     verifyCode rejects a code after 5 incorrect guesses, limiting each issued code to 5 brute-force attempts.

•     ResetPasswordController invalidates the user's other active sessions after the new password hash is stored (F8).

•     SMTP credentials are stored in application configuration, and that configuration is excluded from Git.

•     Plaintext reset codes and new passwords must never be written to application logs.

# **10. Testing Plan**

**Unit tests.** Test PasswordResetDAO code creation, verification, expiry, and attempt limits, together with Validator password rules and code hashing.

**UI tests (TestFX).** Run the successful three-screen flow, followed by expired-code handling, lockout after 5 failures, password mismatch, and resend cooldown scenarios.

**Manual QA.** Compare requests for a real account and an unknown address to confirm identical behaviour. Complete an email round trip with a test inbox, then verify that a session already signed in on another machine is invalidated after the reset.

# **11. Suggested File Structure**

com.pesttracker  
|-- app/     	SceneRouter.java, SessionManager.java, PasswordResetSession.java  
|-- controller/  ForgotPasswordController.java, VerifyCodeController.java, ResetPasswordController.java  
|-- model/   	User.java, PasswordResetCode.java  
|-- dao/     	UserDAO.java, PasswordResetDAO.java  
|-- db/      	DatabaseConnection.java, schema.sql  
\`-- util/    	Validator.java, PasswordHasher.java, CodeGenerator.java, EmailService.java  
resources/  
|-- fxml/    	LoginView.fxml, ForgotPasswordView.fxml, VerifyCodeView.fxml, ResetPasswordView.fxml  
\`-- css/     	auth-theme.css

**Wireframe and basic mockup:**![](data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAlUAAAIbCAYAAADCVGuuAAAACXBIWXMAAAsTAAALEwEAmpwYAAAgAElEQVR4nOzde3hU5b33/8+EkAMk5AAmOJGAKBANEdREKYaKBIRKEQsUrVURdYvQq+KvTyt4qJUqCtXdLe4q0ro5SMUHEAzSoBxiRSIbmuADDakByinAQMIhCSEwgZD8/hhmkZADOdzJJJn367q4WJlZs9Z31qyZ+cy673UvW3R0dLkAAADQKD6eLgAAAKAtIFQBAAAYQKgCAAAwwDcuLs7TNQAAALR6viUlJZ6uAQAAoNWj+Q8AAMAAQhUAAIABhCoAAAADCFUAAAAGEKoAAAAMIFQBAAAYQKgCAAAwgFAFAABgAKEKAADAAEIVAACAAYQqAAAAAwhVAAAABhCqAAAADCBUAQAAGECoAgAAMIBQBQAAYAChCgAAwABCFQAAgAGEKgAAAAMIVQAAAAYQqgAAAAwgVAEAABhAqAIAADCAUAUAAGAAoQoAAMAAQhUAAIABhCoAAAADCFUAAAAGEKoAAAAMIFQBAAAYQKgCAAAwgFAFAABgAKEKAADAAEIVAACAAYQqAAAAAwhVAAAABhCqAAAADCBUAQAAGECoAgAAMIBQBQAAYAChCgAAwABCFQAAgAGEKgAAAAMIVQAAAAYQqgAAAAwgVAEAABhAqAIAADCAUAUAAGAAoQoAAMAAQhUAAIABhCoAAAADCFUAAAAGEKoAAAAMIFQBAAAYQKgCAAAwwPb5quRyTxcBAADQ2tnOFBUSqgAAABqJ5j8AAAADCFUAAAAGEKoAAAAMIFQBAAAYQKgCAAAwgFAFAABgAKEKAADAAEIVAACAAYQqAAAAAwhVAAAABhCqAAAADCBUAQAAGECoAgAAMIBQBQAAYAChCgAAwABCFQAAgAGEKgAAAAMIVQAAAAYQqgAAAAwgVAEAABhAqAIAADCAUAUAAGAAoaoF2rdvX633uf+5OZ1OZWVlVTvflbdlZGRUWWZGRob1eKfTWWkdTue5qy43Pz+/0t9Op1MZGRlVanSv2+l06ujRo9Zj3dPu/901VVzu0aNHq6zbfRvahpr2T7eK+4d0ed9x7xf5+fmV9l83p9NpPb7ivK77Lu/f1e1LGRkZVWqquC9XfKzr/eKsVG917xe0bu7P2ys/99zy8/OrvO4Vb3Pvx1fu79V9tle87cr11bTPXammfbW6uq+cdu/r1X0WV1xvdbV4q3YvvvjCq54uApft27dP8+Z9oGHDhlV7/+rVnysv77gcDodiY2OVlrZJy5YtU1FRkU6cOK6ePW+QJK1fv1579+7Vl19+ocTEQcrPz9fq1Z/L6XRqw4b1GjBggCRp3rx5cjqdysnJUffu3VVaWqq0tDRt25ahoqIiXXNNhIKDg6usf/Xqz5WYOEiS9NFHixQfH2/N8/bbbyksLFwbNqy31v3ee39SWFi49u/fp2uuidB77/1Jgwffo3nzPtC+ffsUHx+vt99+S/fcc4+2bXMFsg0b1svpdKpnzxv03nt/0rlzTu3du1eRka6a3LetXv25bDaboqOjm+plQTNYvfpz+fq2l9PpVERERJX7//73v6tPnz7W307nOa1cuVJHjzqUl3dc4eHhOn36tJYvX6Zz51z7ef/+/XTkiEPh4eFasuRj68spPDxcYWFh+uKLL9SnTx+tX79O+/btVWxsX2v5//pXlnbt2n0pvDnUs+cN2rdvr/70pz+pffv21m3u2g8ezNHq1Z/rnnvukSRr/3S/V9E2vP32WyotvaBvv02zPkcrSktL07ffpikvL8/6TE5LS9O+ffvkcDjk6+sru92uZcuWKjAwUMuXL1Ni4iDNmfOOLlwotfaXc+fO6euvv1Za2iadO+e09lk39z7n/pyVpP/8z7c1YMAA+fq2V35+vv7853kKDg7Wtm0Zio3tq/z8fG3YsF7bt2/X8ePHFR0drYsXS+Xr217Lly9Tv379JUnz5n2gAQN+oNde+73at29faR1Llnys/Px8ffHFGgUGBsput1e7/3srX08XgMrs9msrvXGulJg4SD179qxwi03Tp78gSUpL22TdOn78eEnS3r17JbneJO75zp07V2G+nyosLNyaZ9KkZzR+/HitX7++2mA3atT9CgsLk8PhqFCz6001atT9kqTY2FgNGzbMevymTZus29xuu+12SVJ4eLh1W3x8vNLSNsnhcGj8+AclVf6V5X5Os2a9aT0X920VnztaH6fznB57bIIkV9CvLoTcf//9Onr0qK699lotW7bMeu0rvif27t2rsLBwax/etCmt0vvl9ttvr/QD4Npr7ZKkwMBADRt2b6X17dy509oPHY4jklz744wZv7fWNW/ePE2aNKlSDR99tMh6Lu4a0Xa4P3uWLVtW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SJL69eunsWPH6uc//7natWun+Ph4vfLKKx6utnb33XdfvfaXixcvKjk5Wbm5uU1XVCO9++67yszM1IQJEyrdvmrVKq1fv14TJ07U4sWLNWbMmCat47HHHtPcuXM1ZswYde/eXQcPHrTuW7x4sWJjY7V48WLdc889evDBBxUfH9+k9dRXu3bt1KtXL7Vv316ZmZmeLqfRfvzjH0uS/va3v3m4Enia14YqE34Z30H9/ufyaaqdA330r//oLF8fm/6adU6/T2vaX6uobMSIEYqLi6vXY6ZMmdJE1TRMaGionnvuOW3btk07duxQv379VFZWph07dmjHjh1WkAoKCtLatWvVoUMHDRo0yMNVV2/27NnVvh5TpkzR+++/X+X2zMxMtWvXrtr7WoLMzMxa96+zZ8/qvffe05o1a7Ro0SJNnDixSS57VbGO999/X6mpqXryySd14MABde/eXX/5y1+0ceNGSdIf//hHSdIbb7zRYn48PPXUU/L399e3334rPz8/hYWFGbneamTsQPX58dMKv75vzTOVl+t//zRVuVmbG70+t6efflpff/21Nf3nP//Z2LJr0rdvX+3cubPJ14P6I1Q1wsS/FeqxuEB9lHlOknTyXJlu/osrZIUF+Oj/PdFZER18tDzbqec2FHmy1Fp99tlnlY5QXe2X4+TJkyWp3gEGV7dlyxa99dZbSk9PV79+/bRjxw6lpKRo+vTpleY7c+aMhg8frsDAQG3btk233367hyr2Dr/73e90zz331Gne/fv3a9++ffrHP/5h/AjRH//4RyUlJVW6LSkpSZmZmbr//vs1aNAgLV26tMrjrmwi9IRx48apX79+evPNN3X2rOuI/muvvabf/va3RpY/4BfvaPeX8/XN7Mf1wAcZSn6m+m0/+v1/aNWUO4ysc9KkSVq3bp32798vydXRfdKkSVqwYIHOnz9vZB1X8vPz07hx4whVLRShqhG+yy3VxkfCrVBVUb6zTLfOdwWsEH+btk3srJ6h7XT/pwXadKhp3mwNVbEP1dV+jdd0lAHmjBgxQjNmzNCZM2fUr18/zZ49W6+//roWLVqkAQMGKCcnxzoSce7cOb3zzju66aab9P3333u48sp+/ev6XYXg17/+tbZv395E1TTOuHHjNGPGjDrPP2PGDIWEhBiv41e/+pW++eYbjRs3Tnl5eZIqv2cPHTqkadOmaebMmdZjrrvuOr3xxhvGa6mPadOmaf78+fr0008lSddff70eeeQRo03YNh8fde51m2J+/LQkWf936ROvLr1u04k93+nknu90au+ORq/Lz89Pjz/+uBWopk2bprKyMr311ltat26dXnzxRb3xxhtGglVMTIw6deqkf/zjH5KkoUOHWv2eXn31VX3yySfatWtXo9cDM7x6SAUTnKVX75tVWFKu2xec1IpdTq0ZH6p3hgY3Q2VozWJjYyW5xuaZNm2aRo8erZUrV+r555/Xn/70J0VERFjzLl26tMn78DTE2rVr6z1/S+1P5T4SUR9fffWVevXqZbyWCRMmaPHixYqMjNTTrGTTlgAAC2BJREFUTz+tjz76yLqvtLRUDz30kGbPni1/f3/dd999Wrx4sVJSUozXUVc333yzCgoKKnWWnzBhgjIyMoyfPNTlilAV8+On1aXXbdZ9fX70pHZ/Ob/R65k4caKuvfZaa7/4y1/+IpvNptDQUO3fv99q/jXhoYce0nfffWf9feONN1p96A4fPqyf/exnRtZTEWcDNhyhqpFGryjQuJiAOs371JrTsv/3cT3ZL7Be64iIiNCPfvQj6+977723Xo9vKSoGAal1P4+Kr0fXrl2NLv/gwYMaP3689XdZWZlKS0sVFxdn/UtNvXzppPPnz2vcuHH1Xk9beT0kNenrIUlpaWn1fsw333yjH/zgB8Zr2b9/v5555hktXbpUISEheuuttyrdHxcXpz/+8Y/6+c9/rpMnT9a52bIp9O/fX3feeafmzZtn3fbaa69pxowZ+uKLLxQUFKTXXntNzzzzjH71q181en3Zf/uz1eyX/Ex81X+TE5Sb9b+NWsdTTz2ldevWacaMGQoNDdVvfvMbnTp1Sn/4wx/09NNPKzw8XPv379e6dev01FNPNepsuldffVUzZ86sNED1Nddcoy+//FKS9OGHH2rmzJl69dVXG/WcYA6hqpE2H76gBSM7qUdIu6vO+7vEjnL88hrtPF7zCO7VycvL06233qopU6Zow4YNrbYtPS8vT5mZmbLb7a3+eVR8PT7++GOjy1+2bFmVgOPrW3tL/dXur4779Wjt+5WkJn09JFcftvoKCgpSYWGh8VokV7AaPHhwlUDllpubq/nz52vr1q1Nsv662r59uxYsWCBJuuOOO6w+VOXl5brrrrv00EMP6be//a3sdrux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)



---

# Heat Map for Pests

## Document Control

| Field             | Value                                                                                          |
|-------------------|------------------------------------------------------------------------------------------------|
| Module            | Core Features → **Heat Map**                                                                   |
| Page Owner        | Joshua                                                                                         |
| Related Sub-pages | Report Sighting, Species Details                                                               |
| App               | Invasive Species Tracker (pest photo ID, location logging, ethical handling/disposal guidance) |
| Tech Stack        | JavaFX (UI/Controllers), WebView (Leaflet.js), SQL via JDBC, ALA API                           |
| Status            | Draft v1.0                                                                                     |

**Reference Document:** [Brisbane City Council: Invasive Species](https://www.brisbane.qld.gov.au/environment-and-water/wildlife-and-conservation/invasive-species)

## Overview
The main objective of this page is to track and catalogue invasive species and to provide awareness and information about how to ethically handle and/or remove them. This map interface must show an intuitive visual representation of pest density.

## Functional Requirements

* **Geographic display:** A clean and uncluttered map centred on the Brisbane area.
* **Data visualisation:** Areas with reported sightings highlighted using a gradient colour scale.
* **Filtering:** 
    * **By species:** Users can filter by a list of common local invasive species.
    * **By time:** Users can filter sightings by date.
* **Species details:** When a specific coloured hotspot or sighting on the map is clicked, the species details page for the selected species is shown.
* **Search functionality:** A search field allows users to search for a specific area (postcode, suburb, etc.) or specific species.
* **Reporting:** An easily visible primary call to action button labelled "Report Sighting".
* **Visual guide:** A small floating visual guide explaining what the colour zones mean in terms of pest density.

## User Stories

* *As a user*, I want to see a visual representation of pest density in Brisbane, so I know which areas to avoid or monitor closely.
* *As a user*, I want to filter the map by specific invasive species, so I can track the spread of a particular threat like Fire Ants.
* *As a user*, I want to distinguish between official biosecurity data and unverified user reports, so I can trust the accuracy of the map.
* *As a biosecurity officer (admin)*, I want to view clustered reports, so I can identify potential new outbreak zones efficiently.

## Technical Requirements

### Frontend
* Interactive map canvas (utilising a library such as Leaflet.js embedded via JavaFX WebView) locked to the Greater Brisbane region.
* Heat map data overlay processing to calculate visual density and apply colour gradients based on sighting volume.
* Form interface for users to report sightings, incorporating a dynamic search bar for species identification.
* Dynamic geospatial clustering to group close coordinate points into numbered nodes when the map is zoomed out.
* Contextual data pop-ups triggered by map clicks, displaying species identification and actionable handling links.

### Backend
* Java HTTP client integration to securely query `https://api.ala.org.au/species/search/auto` for autocomplete species search and identification.
* Asynchronous execution of all network calls and database queries using JavaFX `Task` or `Service` classes to ensure the main application thread does not freeze.
* Data sanitisation logic to validate incoming GPS coordinates, automatically rejecting data points that fall outside the predefined Greater Brisbane boundaries.
* JSON deserialisation (utilising libraries such as Gson or Jackson) to parse the nested text payload from the ALA API into usable Java objects.

### Database
* Local SQLite database implemented as the primary data store, connected via the `sqlite-jdbc` driver.
* Sighting record table: submission ID, user ID, species ID, date, latitude (REAL), longitude (REAL), and verification status.
* Species reference caching table to store results from the ALA API (scientific name, common name, API reference ID), establishing a local source of truth.
* Prepared statements for all SQL queries to ensure safe data retrieval and prevent SQL injection vulnerabilities.


---

# Pest Detail Screen

## Document Control

| Field | Value |
| --- | --- |
| Module | Species Search → Pest Detail Screen (Species Detail pane + Local Sightings pane) |
| Page Owner | Vinny |
| Related Sub-pages | Species Search, Log Sighting, Add Photo, Full Sightings Map |
| App | Biodex |
| Tech Stack | JavaFX (UI/Controllers), SQL via JDBC (persistence) |
| Status | Draft v1.0 |

---

## 1. Overview

Pest Detail opens when a user taps a species from Species Search. The screen splits into two panes. The left pane, **Species Detail**, shows what the pest is and how dangerous it is. The right pane, **Local Sightings**, shows where it has turned up around Brisbane in the last 30 days.

The two panes load from the same species ID but query different tables, so they run as separate async tasks and render independently. A slow sightings query never blocks the species photo and threat profile from appearing.

---

## 2. Scope Within Species Search

```text
Species Search
 |-- SpeciesSearchView.fxml
 |     |-- search field, filter chips
 |     \-- species list -> opens Pest Detail Screen (this spec)
 |
 |-- Pest Detail Screen (PestDetailView.fxml)
 |     |-- SpeciesDetailView.fxml   (left pane, fx:include)
 |     \-- LocalSightingsView.fxml  (right pane, fx:include)
 |
 |-- Log Sighting wizard   (out of scope here, opened from "Log sighting")
 \-- Full Sightings Map    (out of scope here, opened from "Open full map")
```

Species Search hands Pest Detail a single `speciesId`. Both panes key off that ID and neither pane navigates anywhere on its own except through Log Sighting, Add Photo, or Open Full Map.

---

## 3. Functional Requirements

| # | Requirement | Priority |
| --- | --- | --- |
| F0 | Species list shows a "View details" action per row that opens Pest Detail Screen | Must |
| F1 | Species Detail pane shows the species photo, common name, scientific name, threat badge, and category tags | Must |
| F2 | Species Detail pane shows a Threat Profile section with Aggression, Sting Severity, and Spread Risk bars | Must |
| F3 | Species Detail pane shows Typical Habitat and Size | Must |
| F4 | Species Detail pane shows Disposal Guidance text | Must |
| F5 | "Log sighting" opens the Log Sighting wizard pre-filled with this species | Must |
| F6 | "Add photo" opens the photo capture/upload dialog attached to this species | Should |
| F7 | Local Sightings pane shows report density per suburb for the last 30 days | Must |
| F8 | Density cards sort by report count, highest first | Should |
| F9 | Local Sightings pane lists the 3 most recent reports for the species, each with suburb, relative date, and verification status | Must |
| F10 | "Open full map" opens a Brisbane map with a pin for every report of this species | Should |
| F11 | Both panes show a loading state while their query runs and keep the last successful data on screen if a query fails | Should |
| F12 | A submitted report starts Unverified; only a council-staff account can flip it to Verified | Must |

---

## 4. UI Flow

### Species Detail Pane

Circular photo at the top, common name in bold, scientific name in italics below it, a threat-level badge ("High threat", coloured by severity), and category tags ("Invasive", "Stinging"). Below that: a short description, then the three Threat Profile bars, then Typical Habitat and Size side by side, then Disposal Guidance. "Log sighting" and "Add photo" sit as two buttons at the bottom of the pane.

### Local Sightings Pane

Header reads "Local sightings" with "Brisbane, QLD" aligned right. A grid of suburb cards follows, each showing a suburb name and its report count over the last 30 days, shaded from light to dark red by volume. Below the cards, a "Recent reports near you" list shows up to 3 entries, each with a short description, suburb and relative timestamp, and an Unverified/Verified badge. "Open full map" sits as a full-width button at the bottom of the pane.

---

## 5. Controller Architecture

`PestDetailController` hosts both child panes and loads the species once, then hands the result to each child controller. `SpeciesDetailController` and `LocalSightingsController` never query the database directly for the species record; only `PestDetailController` does that lookup, so the two panes can't drift out of sync on which species they're showing.

```java
public class PestDetailController implements Initializable {

    @FXML private SpeciesDetailController speciesDetailController;   // injected via fx:include
    @FXML private LocalSightingsController localSightingsController; // injected via fx:include

    private final SpeciesDAO speciesDAO = new SpeciesDAO(DatabaseConnection.getConnection());

    public void loadSpecies(int speciesId) {
        Task<Species> task = new Task<>() {
            protected Species call() { return speciesDAO.findById(speciesId); }
        };
        task.setOnSucceeded(e -> {
            Species species = task.getValue();
            speciesDetailController.display(species);
            localSightingsController.load(species.getSpeciesId());
        });
        new Thread(task).start();
    }

    @Override public void initialize(URL location, ResourceBundle resources) {}
}
```

```java
public class SpeciesDetailController implements Initializable {

    @FXML private ImageView photoView;
    @FXML private Label commonNameLabel;
    @FXML private Label scientificNameLabel;
    @FXML private Label threatBadge;
    @FXML private ProgressBar aggressionBar;
    @FXML private ProgressBar stingSeverityBar;
    @FXML private ProgressBar spreadRiskBar;
    @FXML private Label habitatLabel;
    @FXML private Label sizeLabel;
    @FXML private Label disposalLabel;

    private Species species;

    public void display(Species species) {
        this.species = species;
        commonNameLabel.setText(species.getCommonName());
        scientificNameLabel.setText(species.getScientificName());
        threatBadge.setText(species.getThreatLevel().name());
        aggressionBar.setProgress(species.getAggression() / 100.0);
        stingSeverityBar.setProgress(species.getStingSeverity() / 100.0);
        spreadRiskBar.setProgress(species.getSpreadRisk() / 100.0);
        habitatLabel.setText(species.getTypicalHabitat());
        sizeLabel.setText(species.getSizeMinMm() + " to " + species.getSizeMaxMm() + " mm");
        disposalLabel.setText(species.getDisposalGuidance());
        photoView.setImage(new Image(species.getPhotoPath()));
    }

    @FXML
    private void handleLogSighting() {
        SceneRouter.getInstance().goToLogSighting(species.getSpeciesId());
    }

    @FXML
    private void handleAddPhoto() {
        SceneRouter.getInstance().goToAddPhoto(species.getSpeciesId());
    }

    @Override public void initialize(URL location, ResourceBundle resources) {}
}
```

```java
public class LocalSightingsController implements Initializable {

    @FXML private VBox densityCardContainer;
    @FXML private VBox recentReportsContainer;

    private final SightingReportDAO reportDAO = new SightingReportDAO(DatabaseConnection.getConnection());
    private static final int WINDOW_DAYS = 30;
    private static final int RECENT_LIMIT = 3;

    public void load(int speciesId) {
        Task<Map<String, Integer>> densityTask = new Task<>() {
            protected Map<String, Integer> call() { return reportDAO.getDensityBySuburb(speciesId, WINDOW_DAYS); }
        };
        densityTask.setOnSucceeded(e -> renderDensityCards(densityTask.getValue()));
        new Thread(densityTask).start();

        Task<List<SightingReport>> recentTask = new Task<>() {
            protected List<SightingReport> call() { return reportDAO.getRecentReports(speciesId, RECENT_LIMIT); }
        };
        recentTask.setOnSucceeded(e -> renderRecentReports(recentTask.getValue()));
        new Thread(recentTask).start();
    }

    private void renderDensityCards(Map<String, Integer> counts) {
        densityCardContainer.getChildren().clear();
        counts.forEach((suburb, count) -> densityCardContainer.getChildren().add(DensityCardFactory.build(suburb, count)));
    }

    private void renderRecentReports(List<SightingReport> reports) {
        recentReportsContainer.getChildren().clear();
        reports.forEach(r -> recentReportsContainer.getChildren().add(ReportRowFactory.build(r)));
    }

    @FXML
    private void handleOpenFullMap() {
        SceneRouter.getInstance().goToSightingsMap();
    }

    @Override public void initialize(URL location, ResourceBundle resources) {}
}
```

---

## 6. Data Model and Schema

```java
public class Species {
    private int speciesId;
    private String commonName;
    private String scientificName;
    private ThreatLevel threatLevel;   // LOW, MEDIUM, HIGH
    private int aggression;            // 0-100
    private int stingSeverity;         // 0-100
    private int spreadRisk;            // 0-100
    private String typicalHabitat;
    private double sizeMinMm;
    private double sizeMaxMm;
    private String disposalGuidance;
    private String photoPath;
}

public class SightingReport {
    private int reportId;
    private int speciesId;
    private String suburb;
    private String locationLabel;   // e.g. "Mound near footpath, Corsair Ave"
    private double latitude;
    private double longitude;
    private Instant reportedAt;
    private boolean verified;
    private int reporterUserId;
    private String photoPath;       // nullable
}
```

```sql
CREATE TABLE species (
    species_id        INTEGER PRIMARY KEY AUTOINCREMENT,
    common_name       VARCHAR(100) NOT NULL,
    scientific_name   VARCHAR(150) NOT NULL,
    threat_level      VARCHAR(10)  NOT NULL,   -- LOW, MEDIUM, HIGH
    aggression        INTEGER      NOT NULL,
    sting_severity    INTEGER      NOT NULL,
    spread_risk       INTEGER      NOT NULL,
    typical_habitat   VARCHAR(255),
    size_min_mm       DECIMAL(5,2),
    size_max_mm       DECIMAL(5,2),
    disposal_guidance TEXT,
    photo_path        VARCHAR(255)
);

CREATE TABLE species_tags (
    species_id   INTEGER     NOT NULL,
    tag          VARCHAR(30) NOT NULL,   -- Invasive, Stinging, etc.
    PRIMARY KEY (species_id, tag),
    FOREIGN KEY (species_id) REFERENCES species(species_id) ON DELETE CASCADE
);

CREATE TABLE sighting_reports (
    report_id        INTEGER PRIMARY KEY AUTOINCREMENT,
    species_id       INTEGER NOT NULL,
    suburb           VARCHAR(100) NOT NULL,
    location_label   VARCHAR(255) NOT NULL,
    latitude         DECIMAL(9,6),
    longitude        DECIMAL(9,6),
    reported_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    verified         BOOLEAN   DEFAULT 0,
    reporter_user_id INTEGER,
    photo_path       VARCHAR(255),
    FOREIGN KEY (species_id) REFERENCES species(species_id) ON DELETE CASCADE,
    FOREIGN KEY (reporter_user_id) REFERENCES users(user_id) ON DELETE SET NULL
);

CREATE INDEX idx_reports_species_suburb ON sighting_reports(species_id, suburb);
CREATE INDEX idx_reports_species_date   ON sighting_reports(species_id, reported_at);
```

`species_tags` sits in its own table rather than a comma-packed column on `species`, so a future filter-by-tag feature can query it directly instead of parsing strings.

---

## 7. DAO Reference

```java
public class SpeciesDAO {

    private final Connection connection;

    public SpeciesDAO(Connection connection) { this.connection = connection; }

    public Species findById(int speciesId) {
        String sql = "SELECT * FROM species WHERE species_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, speciesId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            return mapRow(rs);
        } catch (SQLException e) { throw new DataAccessException(e); }
    }

    private Species mapRow(ResultSet rs) throws SQLException {
        Species s = new Species();
        s.setSpeciesId(rs.getInt("species_id"));
        s.setCommonName(rs.getString("common_name"));
        s.setScientificName(rs.getString("scientific_name"));
        s.setThreatLevel(ThreatLevel.valueOf(rs.getString("threat_level")));
        s.setAggression(rs.getInt("aggression"));
        s.setStingSeverity(rs.getInt("sting_severity"));
        s.setSpreadRisk(rs.getInt("spread_risk"));
        s.setTypicalHabitat(rs.getString("typical_habitat"));
        s.setSizeMinMm(rs.getDouble("size_min_mm"));
        s.setSizeMaxMm(rs.getDouble("size_max_mm"));
        s.setDisposalGuidance(rs.getString("disposal_guidance"));
        s.setPhotoPath(rs.getString("photo_path"));
        return s;
    }
}
```

```java
public class SightingReportDAO {

    private final Connection connection;

    public SightingReportDAO(Connection connection) { this.connection = connection; }

    public Map<String, Integer> getDensityBySuburb(int speciesId, int windowDays) {
        String sql = "SELECT suburb, COUNT(*) AS report_count FROM sighting_reports "
                   + "WHERE species_id = ? AND reported_at >= ? "
                   + "GROUP BY suburb ORDER BY report_count DESC";
        Map<String, Integer> counts = new LinkedHashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, speciesId);
            ps.setTimestamp(2, Timestamp.from(Instant.now().minus(windowDays, ChronoUnit.DAYS)));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) counts.put(rs.getString("suburb"), rs.getInt("report_count"));
            return counts;
        } catch (SQLException e) { throw new DataAccessException(e); }
    }

    public List<SightingReport> getRecentReports(int speciesId, int limit) {
        String sql = "SELECT * FROM sighting_reports WHERE species_id = ? "
                   + "ORDER BY reported_at DESC LIMIT ?";
        List<SightingReport> reports = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, speciesId);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) reports.add(mapRow(rs));
            return reports;
        } catch (SQLException e) { throw new DataAccessException(e); }
    }

    public void insertReport(SightingReport report) {
        String sql = "INSERT INTO sighting_reports "
                   + "(species_id, suburb, location_label, latitude, longitude, reporter_user_id, photo_path) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, report.getSpeciesId());
            ps.setString(2, report.getSuburb());
            ps.setString(3, report.getLocationLabel());
            ps.setDouble(4, report.getLatitude());
            ps.setDouble(5, report.getLongitude());
            ps.setInt(6, report.getReporterUserId());
            ps.setString(7, report.getPhotoPath());
            ps.executeUpdate();
        } catch (SQLException e) { throw new DataAccessException(e); }
    }

    private SightingReport mapRow(ResultSet rs) throws SQLException {
        SightingReport r = new SightingReport();
        r.setReportId(rs.getInt("report_id"));
        r.setSpeciesId(rs.getInt("species_id"));
        r.setSuburb(rs.getString("suburb"));
        r.setLocationLabel(rs.getString("location_label"));
        r.setLatitude(rs.getDouble("latitude"));
        r.setLongitude(rs.getDouble("longitude"));
        r.setReportedAt(rs.getTimestamp("reported_at").toInstant());
        r.setVerified(rs.getBoolean("verified"));
        r.setReporterUserId(rs.getInt("reporter_user_id"));
        r.setPhotoPath(rs.getString("photo_path"));
        return r;
    }
}
```

Every query binds its parameters through `PreparedStatement`. `getDensityBySuburb` does the grouping in SQL rather than in Java, so the suburb counts stay correct even as `sighting_reports` grows past what the app would want to pull into memory.

---

## 8. Validation and Error Handling

| Field | Rule | Failure Behaviour |
| --- | --- | --- |
| Species lookup | `speciesId` must exist | Pane shows "Species not found"; Pest Detail Screen returns to Species Search |
| Density query | Window fixed at 30 days | Zero rows shows "No reports in the last 30 days" instead of empty cards |
| Recent reports | Limit fixed at 3 | Fewer than 3 reports shows only what exists, no placeholder rows |
| Log sighting | Requires a signed-in session | Redirects to Login if `SessionManager` has no active user |
| Add photo | File must be jpg/png under 10MB | Inline error, upload blocked |
| DB failure | Any exception in a `Task` | Non-blocking alert; pane keeps its last successful data |

---

## 9. Data Integrity and Privacy Considerations

- Density cards count every report for a suburb, verified or not, so the number on screen moves as soon as someone submits a report, before council confirms anything.
- Each entry in Recent Reports keeps its Unverified/Verified badge, so the list never implies confirmation that hasn't happened.
- Only an account flagged `councilStaff` in `SessionManager` can flip a report's `verified` column.
- `location_label` holds whatever text the reporter typed, like a street name. Biodex never resolves that to a street number, and the full map jitters each pin by roughly 50 metres before it renders, so a pin can't pinpoint a reporter's yard.
- `reporter_user_id` ties a report to whoever logged it, but no screen in this spec displays a reporter's name.
- Log Sighting and Add Photo both check `SessionManager` first, so an anonymous user can browse Pest Detail but can't write to it.

---

## 10. Testing Plan

**Unit tests.** Cover `SightingReportDAO.getDensityBySuburb` across suburb boundaries and date windows, and `getRecentReports` for its `ORDER` and `LIMIT` behaviour.

**UI tests (TestFX).** Load a species with reports spread across several suburbs, a species with zero reports, and a species with more than 3 reports, and check the density cards, the recent list, and the empty states.

**Manual QA.** Try Log Sighting while signed out and confirm the redirect to Login. Verify a report as a council-staff account and confirm its badge updates everywhere that report appears.

---

## 11. Suggested File Structure

```text
com.pesttracker
 |-- app/         SceneRouter.java, SessionManager.java
 |-- controller/  PestDetailController.java, SpeciesDetailController.java, LocalSightingsController.java
 |-- model/       Species.java, SightingReport.java
 |-- dao/         SpeciesDAO.java, SightingReportDAO.java
 |-- db/          DatabaseConnection.java, schema.sql
 \-- util/        DensityCardFactory.java, ReportRowFactory.java
resources/
 |-- fxml/        PestDetailView.fxml, SpeciesDetailView.fxml, LocalSightingsView.fxml
 \-- css/         species-theme.css
```

---

## 12. Mockup

<!-- Add the mockup image to docs/images/ and update the path below. -->

![Pest Detail Screen mockup](docs/images/pest-detail-mockup.png)


---

# Settings Page — Technical Specification

## Document Control

| Field | Value |
|---|---|
| Module | Profile Page → **Settings** |
| Page Owner | Yash |
| Related Sub-pages | My Uploads, My Photos |
| App | Invasive Species Tracker (pest photo ID, location logging, ethical handling/disposal guidance) |
| Tech Stack | JavaFX (UI/Controllers), SQL via JDBC (persistence) |
| Status | Draft v1.0 |

---

## 1. Overview

The Settings page is a sub-view of the **Profile Page**, reached via a tab or side-navigation item alongside **My Uploads** and **My Photos**. It lets a signed-in user configure their account, app appearance, photo/upload behaviour, pest-identification behaviour, notifications, and privacy/data controls. All settings are user-scoped, persisted in a relational database, and loaded once per session into memory for fast UI binding.

This spec covers: functional requirements, UI/JavaFX layout, FXML structure, controller design, data model, SQL schema, DAO/query layer, validation, security, and testing.

---

## 2. Scope Within the Profile Page

The Profile Page is expected to use a `TabPane` (or a left-hand `ListView`/`VBox` nav acting as a router) with three destinations:

```
ProfileView (BorderPane)
 ├── left: NavigationPane (My Uploads | My Photos | Settings)
 └── center: <dynamically loaded FXML content>
        ├── MyUploadsView.fxml
        ├── MyPhotosView.fxml
        └── SettingsView.fxml   <-- this spec
```

Only the **Settings** destination is detailed below; the other two are referenced only where they intersect (e.g. "clear my uploads" living under Settings > Data).

---

## 3. Functional Requirements

| # | Requirement | Priority |
|---|---|---|
| F1 | User can view current settings on page load, pre-populated from the database | Must |
| F2 | User can edit account details: display name, email, password | Must |
| F3 | User can toggle Light/Dark/System theme, applied instantly without restart | Must |
| F4 | User can set default suburb (used to pre-fill new sighting location) | Must |
| F5 | User can toggle auto-identification of pests on photo upload | Must |
| F6 | User can adjust the identification confidence threshold (0–100%) | Should |
| F7 | User can control upload behaviour: Wi-Fi only, auto-compress, max resolution | Should |
| F8 | User can toggle notification categories independently | Should |
| F9 | User can control data sharing: public location sharing, anonymised uploads | Must |
| F10 | User can export their data (uploads/sightings) to a file | Could |
| F11 | User can clear local cache / thumbnails without deleting server data | Should |
| F12 | User can delete their account (with confirmation + typed-consent step) | Must |
| F13 | User can log out from Settings | Must |
| F14 | All changes are persisted immediately (auto-save on control change) or via an explicit "Save" action (see §14) | Must |
| F15 | Invalid input (e.g. malformed email) is rejected with inline error messaging | Must |
| F16 | Settings page respects accessibility text-size setting on its own controls | Could |

---

## 4. User Stories

- *As a user*, I want to switch to dark mode so the app is easier to use at night.
- *As a user*, I want to set my default suburb so I don't have to re-enter it every time I log a sighting.
- *As a user*, I want to turn off auto-identification if I only want to log a photo manually.
- *As a user*, I want to control whether my sightings are visible to other users, since some locations are on private property.
- *As a user*, I want to delete my account and be told clearly what will be removed.

---

## 5. UI/UX Specification

### 5.1 Layout Structure

`SettingsView.fxml` root is a `ScrollPane` (settings lists can exceed viewport height) containing a `VBox` of `TitledPane`s grouped inside an `Accordion` (or plain `VBox` if all sections should be visible at once — recommended for discoverability). Each `TitledPane` maps to one functional group:

```
Accordion (or VBox)
 ├── TitledPane "Account"
 │     ├── TextField        displayName
 │     ├── TextField        email
 │     ├── PasswordField    currentPassword (shown only in "change password" flow)
 │     ├── Hyperlink        "Change Password"
 │     └── Button           "Save Account Changes"
 │
 ├── TitledPane "Appearance"
 │     ├── ToggleGroup (RadioButton: Light / Dark / System)
 │     ├── ComboBox<String>  language
 │     └── ComboBox<String>  textSize (Small / Medium / Large)
 │
 ├── TitledPane "Location"
 │     └── ComboBox<Suburb>  defaultSuburb  (searchable / filterable)
 │
 ├── TitledPane "Pest Identification"
 │     ├── CheckBox          autoIdentifyOnUpload
 │     ├── Slider            confidenceThreshold (0–100, step 5)
 │     └── Label             live-bound "%.0f%%" readout of slider
 │
 ├── TitledPane "Photos & Uploads"
 │     ├── CheckBox          wifiOnlyUpload
 │     ├── CheckBox          autoCompressPhotos
 │     └── ComboBox<String>  maxUploadResolution
 │
 ├── TitledPane "Notifications"
 │     ├── CheckBox  notifyNewSightingsNearby
 │     ├── CheckBox  notifyCommunityAlerts
 │     └── CheckBox  notifyAppUpdates
 │
 ├── TitledPane "Privacy & Data"
 │     ├── CheckBox  shareLocationPublicly
 │     ├── CheckBox  anonymizeUploads
 │     ├── Button    "Export My Data"
 │     └── Button    "Clear Local Cache"
 │
 ├── TitledPane "About"
 │     ├── Label   appVersion
 │     ├── Hyperlink "Privacy Policy"
 │     ├── Hyperlink "Terms of Service"
 │     └── Hyperlink "Report a Bug"
 │
 └── HBox (bottom, sticky footer)
       ├── Button "Log Out"           (styled secondary)
       └── Button "Delete Account"    (styled danger, red)
```

### 5.2 Interaction Notes

- Theme changes apply **live** (stylesheet swap on the root `Scene`) so the user can preview before it's persisted.
- The confidence-threshold `Slider` shows a bound `Label` with the live percentage.
- "Delete Account" opens a modal `Dialog<ButtonType>` requiring the user to type `DELETE` into a `TextField` before the confirm button enables — mirrors destructive-action best practice.
- Fields disable and a `ProgressIndicator` shows during any async DB write to prevent double-submits.

---

## 6. FXML Skeleton

```xml
<!-- SettingsView.fxml -->
<ScrollPane fitToWidth="true" xmlns="http://javafx.com/javafx"
            xmlns:fx="http://javafx.com/fxml"
            fx:controller="com.pesttracker.controller.SettingsController">
    <VBox spacing="16" styleClass="settings-root">

        <TitledPane text="Account" collapsible="false">
            <VBox spacing="8">
                <Label text="Display Name"/>
                <TextField fx:id="displayNameField"/>
                <Label text="Email"/>
                <TextField fx:id="emailField"/>
                <Hyperlink fx:id="changePasswordLink" text="Change Password"/>
                <Button fx:id="saveAccountButton" text="Save Account Changes"
                        onAction="#handleSaveAccount"/>
            </VBox>
        </TitledPane>

        <TitledPane text="Appearance" collapsible="false">
            <VBox spacing="8">
                <HBox spacing="12" fx:id="themeToggleGroupBox"/>
                <ComboBox fx:id="languageComboBox"/>
                <ComboBox fx:id="textSizeComboBox"/>
            </VBox>
        </TitledPane>

        <TitledPane text="Pest Identification" collapsible="false">
            <VBox spacing="8">
                <CheckBox fx:id="autoIdentifyCheckBox" text="Auto-identify pests on upload"/>
                <Slider fx:id="confidenceSlider" min="0" max="100" value="70"
                        showTickLabels="true" majorTickUnit="25"/>
                <Label fx:id="confidenceLabel"/>
            </VBox>
        </TitledPane>

        <!-- ...remaining TitledPanes per §5.1... -->

        <HBox spacing="12" alignment="CENTER_RIGHT">
            <Button fx:id="logoutButton" text="Log Out" onAction="#handleLogout"/>
            <Button fx:id="deleteAccountButton" text="Delete Account"
                    styleClass="danger-button" onAction="#handleDeleteAccount"/>
        </HBox>

    </VBox>
</ScrollPane>
```

---

## 7. Controller Architecture

`SettingsController` implements `Initializable`, is instantiated by the FXMLLoader when the Profile Page swaps content into its center region, and receives the logged-in `User` context (via a shared `SessionManager` singleton — avoids threading a `userId` through every FXML load).

```java
public class SettingsController implements Initializable {

    // --- FXML-injected controls ---
    @FXML private TextField displayNameField;
    @FXML private TextField emailField;
    @FXML private CheckBox autoIdentifyCheckBox;
    @FXML private Slider confidenceSlider;
    @FXML private Label confidenceLabel;
    @FXML private ComboBox<Suburb> defaultSuburbComboBox;
    @FXML private ComboBox<String> textSizeComboBox;
    // ...remaining controls per §6

    private final SettingsDAO settingsDAO = new SettingsDAO(DatabaseConnection.getConnection());
    private final UserDAO userDAO = new UserDAO(DatabaseConnection.getConnection());
    private final SuburbDAO suburbDAO = new SuburbDAO(DatabaseConnection.getConnection());
    private UserSettings currentSettings;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        int userId = SessionManager.getInstance().getCurrentUser().getUserId();
        loadSettings(userId);
        bindLiveControls();
        wireAutoSaveListeners();
    }

    private void loadSettings(int userId) {
        currentSettings = settingsDAO.getSettingsForUser(userId);
        populateControlsFrom(currentSettings);
    }

    private void bindLiveControls() {
        confidenceLabel.textProperty().bind(
            Bindings.format("%.0f%%", confidenceSlider.valueProperty()));
    }

    /** Attaches a listener per control so changes persist without a page-wide Save button. */
    private void wireAutoSaveListeners() {
        autoIdentifyCheckBox.selectedProperty().addListener((obs, was, isNow) ->
            persist("auto_identify_on_upload", isNow));

        confidenceSlider.valueProperty().addListener((obs, was, isNow) ->
            persistDebounced("identification_confidence_threshold", isNow.doubleValue() / 100.0));

        // theme, language, text size, suburb, notification checkboxes follow the same pattern
    }

    private void persist(String column, Object value) {
        Task<Void> task = new Task<>() {
            protected Void call() {
                settingsDAO.updateSetting(currentSettings.getUserId(), column, value);
                return null;
            }
        };
        new Thread(task).start(); // off the FX Application Thread
    }

    @FXML
    private void handleSaveAccount() {
        if (!Validator.isValidEmail(emailField.getText())) {
            showFieldError(emailField, "Enter a valid email address.");
            return;
        }
        userDAO.updateProfile(currentSettings.getUserId(),
                displayNameField.getText(), emailField.getText());
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().clear();
        SceneRouter.getInstance().goToLogin();
    }

    @FXML
    private void handleDeleteAccount() {
        boolean confirmed = ConfirmDeleteDialog.show(); // typed "DELETE" confirmation
        if (confirmed) {
            userDAO.deleteUserCascade(currentSettings.getUserId());
            SessionManager.getInstance().clear();
            SceneRouter.getInstance().goToLogin();
        }
    }
}
```

**Key architectural decisions**

- All DB writes run on a background `Task`/`Thread`, never the JavaFX Application Thread, to keep the UI responsive.
- `persistDebounced` (for the slider) should coalesce rapid-fire drag events into a single write ~300ms after the user stops dragging.
- Theme switching calls a shared `ThemeManager.apply(scene, theme)` utility so both Settings and app startup use one code path.

---

## 8. Data Model (Java)

```java
public class UserSettings {
    private int userId;
    private String theme;                 // LIGHT | DARK | SYSTEM
    private String language;
    private String measurementUnit;       // METRIC | IMPERIAL
    private Integer defaultSuburbId;
    private boolean autoIdentifyOnUpload;
    private double identificationConfidenceThreshold; // 0.0–1.0
    private boolean wifiOnlyUpload;
    private boolean autoCompressPhotos;
    private String maxUploadResolution;
    private boolean notifyNewSightingsNearby;
    private boolean notifyCommunityAlerts;
    private boolean notifyAppUpdates;
    private boolean shareLocationPublicly;
    private boolean anonymizeUploads;
    private String textSize;              // SMALL | MEDIUM | LARGE
    // getters / setters / builder
}
```

```java
public class Suburb {
    private int suburbId;
    private String name;
    private String state;
    private String postcode;
    // toString() -> "Name, STATE 0000" for ComboBox display
}
```

---

## 9. Database Schema (SQL)

> Assumes SQLite/MySQL-compatible DDL via JDBC. Adjust `AUTOINCREMENT`/`AUTO_INCREMENT` per target engine.

```sql
CREATE TABLE users (
    user_id        INTEGER PRIMARY KEY AUTOINCREMENT,
    username       VARCHAR(50)  UNIQUE NOT NULL,
    email          VARCHAR(100) UNIQUE NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    display_name   VARCHAR(100),
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE suburbs (
    suburb_id  INTEGER PRIMARY KEY AUTOINCREMENT,
    name       VARCHAR(100) NOT NULL,
    state      VARCHAR(50),
    postcode   VARCHAR(10)
);

CREATE TABLE user_settings (
    setting_id                            INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id                                INTEGER NOT NULL UNIQUE,
    theme                                   VARCHAR(10)  DEFAULT 'LIGHT',
    language                                VARCHAR(10)  DEFAULT 'en-AU',
    measurement_unit                        VARCHAR(10)  DEFAULT 'METRIC',
    default_suburb_id                       INTEGER,
    auto_identify_on_upload                 BOOLEAN      DEFAULT 1,
    identification_confidence_threshold     DECIMAL(3,2) DEFAULT 0.70,
    wifi_only_upload                        BOOLEAN      DEFAULT 0,
    auto_compress_photos                    BOOLEAN      DEFAULT 1,
    max_upload_resolution                   VARCHAR(20)  DEFAULT '1920x1080',
    notify_new_sightings_nearby             BOOLEAN      DEFAULT 1,
    notify_community_alerts                 BOOLEAN      DEFAULT 1,
    notify_app_updates                      BOOLEAN      DEFAULT 0,
    share_location_publicly                 BOOLEAN      DEFAULT 0,
    anonymize_uploads                       BOOLEAN      DEFAULT 0,
    text_size                               VARCHAR(10)  DEFAULT 'MEDIUM',
    updated_at                              TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (default_suburb_id) REFERENCES suburbs(suburb_id)
);

-- Referenced for "Export My Data" / "Delete Account" cascade, defined in full
-- elsewhere (My Uploads / My Photos specs) but shown here for FK context:
CREATE TABLE sightings (
    sighting_id  INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id      INTEGER NOT NULL,
    suburb_id    INTEGER NOT NULL,
    pest_id      INTEGER,
    photo_path   VARCHAR(255),
    logged_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id)   REFERENCES users(user_id)   ON DELETE CASCADE,
    FOREIGN KEY (suburb_id) REFERENCES suburbs(suburb_id)
);
```

Trigger to auto-create a default `user_settings` row on signup:

```sql
CREATE TRIGGER trg_create_default_settings
AFTER INSERT ON users
BEGIN
    INSERT INTO user_settings (user_id) VALUES (NEW.user_id);
END;
```

---

## 10. DAO / SQL Query Reference

```java
public class SettingsDAO {

    private final Connection connection;
    public SettingsDAO(Connection connection) { this.connection = connection; }

    public UserSettings getSettingsForUser(int userId) {
        String sql = "SELECT * FROM user_settings WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? mapRow(rs) : null;
        } catch (SQLException e) { throw new DataAccessException(e); }
    }

    /** Generic single-column update, used by the auto-save listeners in the controller. */
    public void updateSetting(int userId, String column, Object value) {
        // Column name is NEVER taken from user input directly — it is selected from
        // a fixed whitelist enum in the controller, preventing SQL injection via column name.
        String sql = "UPDATE user_settings SET " + column + " = ?, updated_at = CURRENT_TIMESTAMP "
                    + "WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setObject(1, value);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new DataAccessException(e); }
    }

    private UserSettings mapRow(ResultSet rs) throws SQLException {
        UserSettings s = new UserSettings();
        s.setUserId(rs.getInt("user_id"));
        s.setTheme(rs.getString("theme"));
        s.setAutoIdentifyOnUpload(rs.getBoolean("auto_identify_on_upload"));
        s.setIdentificationConfidenceThreshold(rs.getDouble("identification_confidence_threshold"));
        // ...remaining fields
        return s;
    }
}
```

```java
public class UserDAO {
    // ...
    public void deleteUserCascade(int userId) {
        String sql = "DELETE FROM users WHERE user_id = ?"; // ON DELETE CASCADE handles children
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new DataAccessException(e); }
    }
}
```

**Important:** `updateSetting`'s `column` parameter must be validated against a fixed `Set<String> ALLOWED_COLUMNS` (or an enum) before string-concatenation into the query — parameterised placeholders cannot bind column/table names, so a whitelist is the safeguard against injection here.

---

## 11. Settings Catalog

| Setting | Control | Type | Default | DB Column |
|---|---|---|---|---|
| Theme | RadioButtons | enum | LIGHT | `theme` |
| Language | ComboBox | enum | en-AU | `language` |
| Measurement Unit | ComboBox | enum | METRIC | `measurement_unit` |
| Default Suburb | Searchable ComboBox | FK int | null | `default_suburb_id` |
| Auto-identify on Upload | CheckBox | bool | true | `auto_identify_on_upload` |
| ID Confidence Threshold | Slider | decimal | 0.70 | `identification_confidence_threshold` |
| Wi-Fi Only Upload | CheckBox | bool | false | `wifi_only_upload` |
| Auto-compress Photos | CheckBox | bool | true | `auto_compress_photos` |
| Max Upload Resolution | ComboBox | enum | 1920x1080 | `max_upload_resolution` |
| Notify: Nearby Sightings | CheckBox | bool | true | `notify_new_sightings_nearby` |
| Notify: Community Alerts | CheckBox | bool | true | `notify_community_alerts` |
| Notify: App Updates | CheckBox | bool | false | `notify_app_updates` |
| Share Location Publicly | CheckBox | bool | false | `share_location_publicly` |
| Anonymize Uploads | CheckBox | bool | false | `anonymize_uploads` |
| Text Size | ComboBox | enum | MEDIUM | `text_size` |

---

## 12. Validation & Error Handling

| Field | Rule | Failure Behaviour |
|---|---|---|
| Email | RFC-5322-style regex, non-empty | Inline red border + helper `Label` under field |
| Password (on change) | ≥8 chars, 1 number, 1 letter | Inline error, submit button disabled until valid |
| Confidence Threshold | Clamped 0–100 by `Slider` bounds | N/A (control-level) |
| Delete Account | Must type `DELETE` exactly | Confirm button stays disabled |
| DB write failure | Any `SQLException` in a Task | Caught in `setOnFailed`, shows a non-blocking `Notification`/`Alert`, reverts the control to its last known-good value |

All controller-level DB exceptions are wrapped into an unchecked `DataAccessException` at the DAO boundary and caught centrally at the controller/Task layer — the UI never surfaces raw SQL errors to the user.

---

## 13. Security & Privacy Considerations

- Passwords are never stored or compared in plaintext — hash with **BCrypt** (`jBCrypt` library) at signup/change; `password_hash` column stores only the hash.
- All SQL access uses `PreparedStatement` with bound parameters; the one exception (dynamic column name in `updateSetting`) is protected by an application-level whitelist, never raw user input.
- `share_location_publicly` and `anonymize_uploads` gate what the **My Uploads/My Photos/community** views are allowed to query — enforced at the DAO query level (e.g. `WHERE anonymize_uploads = 0 OR requester = owner`), not just hidden in the UI.
- Account deletion is a hard delete cascading via FK constraints (`ON DELETE CASCADE`) — spec should confirm with stakeholders whether a **soft-delete** (e.g. `is_deleted` flag + data retention window) is required instead, given this is user-generated ecological data that may have research value.
- Session state (`SessionManager`) is in-memory only, cleared on logout; no credentials persisted to disk outside the hashed DB column.

---

## 14. Navigation & State Management

- The Settings page is loaded fresh (new `FXMLLoader`) each time the user navigates to it from the Profile nav, always re-querying the DB — avoids stale in-memory state if settings were changed elsewhere (e.g. a "quick theme toggle" in the app header).
- **Auto-save vs explicit Save:** toggles/combo-boxes/sliders auto-save on change (F14). The **Account** section (name/email/password) uses an explicit "Save Account Changes" button, since these fields warrant validation before commit and shouldn't fire a DB write per keystroke.
- Theme changes propagate app-wide via a `ThemeManager` observable so every open view (not just Settings) updates the stylesheet immediately.

---

## 15. Non-Functional Requirements

| Category | Requirement |
|---|---|
| Performance | Settings load (DB fetch → populated UI) completes in <300ms on local SQLite |
| Responsiveness | Layout uses `ScrollPane`/`VBox` with percentage/`Priority.ALWAYS` sizing so it resizes gracefully from 800×600 up |
| Accessibility | All controls have `accessibleText`; tab order follows visual order; text-size setting scales font via a CSS class swap |
| Portability | Runs on JDK 17+ / JavaFX 21+ across Windows, macOS, Linux |
| Offline Support | Settings read/write against local SQLite; if the app later adds cloud sync, writes should queue and reconcile on reconnect |
| Data Integrity | `user_settings.user_id` is `UNIQUE` — one settings row per user, enforced at schema level |

---

## 16. Testing Plan

- **Unit tests (JUnit 5):** `SettingsDAO` CRUD against an in-memory H2/SQLite test DB; `Validator` email/password rules.
- **UI tests (TestFX):** toggle each `CheckBox`/`Slider`, assert the underlying DB row updates; verify theme swap changes the scene's stylesheet list; verify Delete Account button stays disabled until `DELETE` is typed.
- **Manual QA checklist:** dark/light/system theme on OS-level theme change; slow/no network with Wi-Fi-only upload enabled; account deletion cascade removes related sightings/photos rows.

---

## 17. Suggested Package/File Structure

```
com.pesttracker
 ├── app/                Main.java, SceneRouter.java, SessionManager.java
 ├── controller/         ProfileController.java, SettingsController.java, ...
 ├── model/               User.java, UserSettings.java, Suburb.java, Sighting.java
 ├── dao/                 UserDAO.java, SettingsDAO.java, SuburbDAO.java
 ├── db/                   DatabaseConnection.java, schema.sql
 ├── util/                 Validator.java, PasswordHasher.java, ThemeManager.java
resources/
 ├── fxml/                 ProfileView.fxml, SettingsView.fxml
 └── css/                   light-theme.css, dark-theme.css
```

---

## 18. Future Enhancements (Out of Scope for v1)

- Cloud sync of settings across devices.
- Per-suburb notification radius control (e.g. "alert me within 5km").
- Granular data-export formats (CSV vs JSON vs PDF report of sightings).
- Two-factor authentication toggle under Account.

---

## Appendix A: Example Sequence — Toggling Dark Theme

```
User clicks "Dark" RadioButton
   -> SettingsController.onThemeChanged(DARK)
       -> ThemeManager.apply(scene, DARK)      [instant visual update]
       -> background Task: SettingsDAO.updateSetting(userId, "theme", "DARK")
           -> UPDATE user_settings SET theme='DARK', updated_at=NOW WHERE user_id=?
       -> Task.setOnFailed -> revert RadioButton + show Alert (only if write fails)
```

## Appendix B: Core Class Relationships

```
SettingsController --uses--> SettingsDAO --queries--> user_settings (SQL)
SettingsController --uses--> UserDAO      --queries--> users (SQL)
SettingsController --uses--> SuburbDAO    --queries--> suburbs (SQL)
SettingsController --reads--> SessionManager (current UserSettings/User context)
ThemeManager        --applied by--> SettingsController, and any other open Scene
```
