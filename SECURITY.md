# Security policy

## Supported versions

Security fixes are made on the latest release and on `main`.

## Reporting a vulnerability

Please **do not** open a public issue for security problems.

Report privately through GitHub's
[private vulnerability reporting](https://docs.github.com/en/code-security/security-advisories/guidance-on-reporting-and-writing-information-about-vulnerabilities/privately-reporting-a-security-vulnerability)
("Report a vulnerability" on the repository's **Security** tab). Include:

- what is affected (file, class, `make` target or dependency),
- steps to reproduce or a proof of concept,
- the impact you expect.

You should receive an acknowledgement within 7 days. Please allow reasonable time for a fix before
you disclose the problem publicly.

## Handling secrets when using this framework

This framework runs against **your** applications, devices and possibly cloud device farms. Keep
their credentials safe:

- Put local settings in `.env` (git-ignored) and never in `.env.example`, feature files or source
  code.
- Inject secrets (test-account passwords, cloud access keys, signing material) from your CI
  system's secret store as environment variables, or as a generated `CAPABILITIES_FILE`.
- The framework masks `user:password@` in URLs when it logs configuration, and logs only the
  *names* of capabilities. However, when a session cannot be created, Selenium/Appium exception
  messages may include the requested capabilities. Treat test logs from runs that use cloud
  credentials in capabilities as sensitive. Avoid printing secrets in your own steps.
- Failure screenshots and screen recordings can capture sensitive on-screen data. Treat
  `target/` and CI artifacts accordingly.
- If a secret is ever committed, **rotate it**. Removing it from the latest commit does not
  invalidate a credential that has been exposed.
