# Contributing

Thanks for helping improve this framework. Bug reports, documentation fixes and code changes are
all welcome.

## Ground rules

- Be respectful. This project follows the [Code of Conduct](CODE_OF_CONDUCT.md).
- Report security problems privately as described in [SECURITY.md](SECURITY.md), not in public
  issues.
- **Never commit** credentials, `.env` files, app binaries (`.apk`, `.ipa`, `.app`), signing
  material, or anything taken from a proprietary application, including locators, screenshots and
  test data. Use placeholders such as `com.example.myapp`.

## Development setup

```bash
make doctor      # JDK 17+ (with javac) is the only hard requirement for framework work
make setup
make test        # unit tests, no device needed
```

You only need Appium, an emulator or a simulator to run the device suites (`make test-android`,
`make test-ios`).

## Making a change

1. Open an issue first for larger changes, so the approach can be agreed before you invest time.
2. Create a branch from `main`.
3. Keep framework code (`src/main/java/.../framework`) free of any app-specific knowledge. Examples
   belong in `src/test/java/.../examples`.
4. Add or update unit tests. Logic that can be tested without a device (configuration,
   capabilities, utilities) should be.
5. If you add a configuration setting: add it to `Setting`, document it in
   `docs/CONFIGURATION.md`, and add it to `.env.example`. `ConfigurationDocsTest` enforces this.
6. If you add or change a `make` target, update `make help` (the `## comment`) and
   `docs/MAKEFILE_GUIDE.md`.
7. Run the same checks CI runs:
   ```bash
   make format          # apply google-java-format
   make lint test check-features
   ```
8. Add an entry under **Unreleased** in [CHANGELOG.md](CHANGELOG.md).
9. Open a pull request. Describe what changed, why, and how you tested it. Say whether you ran it
   on a real Android or iOS target, and which one.

## Style

- Java: google-java-format (enforced by Spotless). There are no compiler warnings
  (`make lint` fails on them).
- Use explicit waits (`ElementActions`, `WebDriverWait`), never `Thread.sleep`.
- Error messages should tell the user which setting to change.
- Shell scripts: POSIX/bash 3.2-compatible, `shellcheck`-clean.

## Commit messages

Use short imperative subject lines (for example, "Add iOS scroll helper"), with detail in the body
when it is useful.

## License

By contributing, you agree that your contributions are licensed under the
[Apache License 2.0](LICENSE).
