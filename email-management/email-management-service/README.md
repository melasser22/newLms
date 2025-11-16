# email-management-service

Gateway facade that exposes a consolidated API surface for tenants while routing work to the
specialized child services (template, sending, webhook, and usage). The service discovers the child
base URLs via `child-services.*` properties and forwards requests using Spring's `RestClient`.
