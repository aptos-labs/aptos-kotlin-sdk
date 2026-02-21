# Contributing to Aptos Kotlin SDK

Thank you for your interest in contributing! This guide covers the development workflow, code style, and conventions used in this project.

## Prerequisites

- **Java 11+** (JDK 11, 17, or 21 recommended)
- **Kotlin 2.1+** (managed by Gradle)
- **Gradle 8+** (use the included `./gradlew` wrapper)

## Development Workflow

```bash
# Clone the repository
git clone https://github.com/aptos-labs/aptos-kotlin-sdk.git
cd aptos-kotlin-sdk

# Build all modules
./gradlew build

# Run tests
./gradlew test

# Auto-format code
./gradlew spotlessApply

# Check formatting (CI runs this)
./gradlew spotlessCheck

# Run linter
./gradlew detekt

# Generate coverage report
./gradlew koverHtmlReport
# Open build/reports/kover/html/index.html
```

## Code Style

Code formatting is enforced by **ktlint** via the [Spotless](https://github.com/diffplug/spotless) Gradle plugin. The formatter respects the project `.editorconfig`:

- **Indent:** 4 spaces
- **Max line length:** 120 characters
- **Final newline:** required
- **Trailing whitespace:** trimmed

Run `./gradlew spotlessApply` to auto-fix formatting before committing. CI will reject PRs that fail `spotlessCheck`.

Static analysis is handled by **detekt** with the project's `detekt.yml` configuration. Run `./gradlew detekt` locally to check for issues.

## Testing

Tests use **JUnit 5** with **kotest** assertions and **MockK** for mocking.

### Conventions

- Use backtick-quoted descriptive test names:
  ```kotlin
  @Test
  fun `should derive correct address from mnemonic`() { ... }
  ```
- Use kotest matchers for assertions:
  ```kotlin
  result.shouldBe(expected)
  address.toHex().shouldStartWith("0x")
  ```
- Place tests under `src/test/kotlin/` mirroring the main source structure.
- Name test classes with a `Test` or `SpecTest` suffix (e.g., `Ed25519SpecTest`).

### Running Specific Tests

```bash
# All tests in a module
./gradlew :core:test

# A specific test class
./gradlew :core:test --tests "com.aptos.core.crypto.Ed25519SpecTest"
```

## Module Architecture

| Module | Description | Network Access |
|--------|-------------|----------------|
| `:core` | Types, crypto, BCS, accounts, transactions | No |
| `:client` | REST API client, faucet, config | Yes |
| `:sdk` | High-level `Aptos` facade | Yes (via `:client`) |

**Dependency rules:**

- `:core` must **never** depend on `:client` or `:sdk`
- `:client` depends on `:core`
- `:sdk` depends on `:client` (which transitively includes `:core`)

When adding new functionality, place it in the lowest appropriate module.

## Commit Messages

Use short, imperative-mood summaries:

```
Add Ed25519 multi-signature support
Fix BCS deserialization of empty vectors
Update ktor to 3.1.0
```

Prefix with the area when helpful: `core:`, `client:`, `sdk:`, `ci:`, `docs:`.

## Pull Request Checklist

Before submitting a PR, ensure:

- [ ] `./gradlew spotlessCheck` passes (formatting)
- [ ] `./gradlew detekt` passes (lint)
- [ ] `./gradlew test` passes (all tests green)
- [ ] New public APIs have KDoc documentation
- [ ] New features include tests
- [ ] No secrets or credentials committed
- [ ] Commit messages follow the conventions above
