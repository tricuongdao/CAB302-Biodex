# Biodex — Invasive Species Tracker

Desktop application for tracking invasive species sightings in Brisbane. Built for QUT CAB302.

## Quick Start

```bash
# Requires Java 17+ and Maven
mvn javafx:run
```

## Documentation

**Full technical specifications:** [https://tricuongdao.github.io/CAB302-assignment/](https://tricuongdao.github.io/CAB302-assignment/)

| Module | Spec |
|--------|------|
| Login / Signup | [login-signup-technical-spec.md](docs/login-signup-technical-spec.md) |
| Forgot Password | [forgot-password-technical-spec.md](docs/forgot-password-technical-spec.md) |
| 2FA | [2FA.md](docs/2FA.md) |
| Heat Map | [heat-map-technical-spec.md](docs/heat-map-technical-spec.md) |
| Pest Detail | [pest-detail-technical-spec.md](docs/pest-detail-technical-spec.md) |
| Settings | [settings-page-technical-spec.md](docs/settings-page-technical-spec.md) |

## Tech Stack

- Java 17, JavaFX 21 (FXML + CSS theming)
- SQLite via JDBC (local persistence)
- Atlas of Living Australia API (species data)
- PBKDF2 password hashing (120k iterations)
- Offline-first caching with TTL

## Project Structure

```
src/main/java/com/biodex/
├── api/           # ALA integration + caching
├── controller/    # JavaFX controllers (BaseController pattern)
├── dao/           # Data access (BaseDao + one per table)
├── db/            # Schema + connection
├── model/         # POJOs
├── routing/       # Enum-based SceneRouter
├── session/       # SessionManager (login + 2FA pending)
├── util/          # PasswordHasher, Validator, CodeGenerator, ThemeManager
└── Main.java      # Entry point
```

## Team

| Member | Module |
|--------|--------|
| Ali | Login / Signup |
| Bernard | Forgot Password |
| Tom | 2FA |
| Joshua | Heat Map |
| Vinny | Pest Detail |
| Yash | Settings |

## License

Student project — QUT CAB302 2026.