# CLAUDE.md

Project conventions for Claude Code.

## Build Commands

```bash
./gradlew build            # compile all modules
./gradlew test             # run all tests (JUnit 5)
./gradlew detekt           # static analysis (detekt)
./gradlew spotlessApply    # auto-format code (ktlint)
./gradlew spotlessCheck    # verify formatting
./gradlew koverHtmlReport  # generate coverage report
```

Run a single test class:
```bash
./gradlew :core:test --tests "com.aptos.core.crypto.Ed25519SpecTest"
```

## Module Layout

```
:core     -> com.aptos.core.*     (types, crypto, BCS, accounts, transactions — no network)
:client   -> com.aptos.client.*   (REST client, faucet, config — depends on :core)
:sdk      -> com.aptos.sdk.*      (Aptos facade — depends on :client)
```

`:core` must never depend on `:client` or `:sdk`. Place new code in the lowest appropriate module.

## Testing Patterns

- JUnit 5 + kotest assertions + MockK
- Backtick test names: `` fun `should serialize address correctly`() ``
- Kotest matchers: `shouldBe`, `shouldThrow`, `shouldStartWith`
- Test classes end in `Test` or `SpecTest`

## Code Style

- Kotlin, formatted by ktlint via Spotless
- 4-space indent, 120-char max line length (see `.editorconfig`)
- Static analysis: detekt with `detekt.yml` config
- Run `./gradlew spotlessApply` before committing
