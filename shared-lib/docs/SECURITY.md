# Security
- JWT modes: HS256 for local, JWKS for prod (`shared.security.*`)
- Key rotation: update `hs256.secret` or rotate JWKS at IdP
- Secrets: use environment variables or secret manager (never commit)
