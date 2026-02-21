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

Integration tests (not in CI, requires testnet):
```bash
./gradlew :sdk:integrationTest
```

JMH benchmarks:
```bash
./gradlew :benchmarks:jmh
```

## Module Layout

```
:core        -> com.aptos.core.*      (types, crypto, BCS, accounts, transactions — no network)
:client      -> com.aptos.client.*    (REST client, faucet, keyless clients, config — depends on :core)
:sdk         -> com.aptos.sdk.*       (Aptos facade — depends on :client)
:indexer     -> com.aptos.indexer.*   (GraphQL indexer client — depends on :core, opt-in)
:benchmarks  -> com.aptos.benchmarks  (JMH benchmarks — depends on :core, not in kover)
```

`:core` must never depend on `:client` or `:sdk`. Place new code in the lowest appropriate module.

`:indexer` is opt-in — users add it separately. Not bundled in `:sdk`.

## Testing Patterns

- JUnit 5 + kotest assertions + MockK
- Backtick test names: `` fun `should serialize address correctly`() ``
- Kotest matchers: `shouldBe`, `shouldThrow`, `shouldStartWith`
- Test classes end in `Test` or `SpecTest`
- Integration tests in `sdk/src/integrationTest/` — tagged `@Tag("integration")`

## Code Style

- Kotlin, formatted by ktlint via Spotless
- 4-space indent, 120-char max line length (see `.editorconfig`)
- Static analysis: detekt with `detekt.yml` config
- Run `./gradlew spotlessApply` before committing
