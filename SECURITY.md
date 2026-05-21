# Security Policy

## Supported Versions

Security fixes are handled on the default branch unless a release branch is explicitly maintained.

## Reporting a Vulnerability

Please do not open a public issue for suspected vulnerabilities, exposed credentials, authorization bypasses, or data access problems.

Report privately to the maintainer:

- James Ryan S. Gallego

Include:

- Affected feature or screen
- Steps to reproduce
- Expected and actual behavior
- Any relevant logs or screenshots with private data removed

## Secret Handling

Never commit:

- `google-services.json`
- Appwrite project credentials
- Firebase service account files
- Signing keystores
- Keystore passwords or aliases
- Generated base64 credential files

Rotate any credential that was committed or shared publicly.
