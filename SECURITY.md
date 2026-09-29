# Security Policy

## Sensitive local files

Never commit real credentials or machine-specific configuration. The following files are intentionally ignored:

- `app/google-services.json`
- `local.properties`
- `secrets.properties`
- `*.env`
- signing keys and keystores

Use `app/google-services.json.example` and `local.properties.example` as templates. Obtain the real Firebase configuration from the Firebase console and keep it only in your local working copy or an approved CI secret store.

## Before publishing

Review staged changes and scan them for secrets before every push. If a credential is committed accidentally, remove it from history and rotate or revoke it immediately; deleting it only from the latest commit is not sufficient.

## Reporting a vulnerability

Please report security issues privately to the maintainers instead of opening a public issue containing sensitive details.
