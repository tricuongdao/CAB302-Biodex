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