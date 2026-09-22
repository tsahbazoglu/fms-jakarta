# Project Rules & Customizations

## Build Profiles & Dependencies
- When building the project with e-signature support, use the profile: `mvn clean install -PesignOyas -Ddependency.scope=provided`
- The `esignOyas` profile activates the e-signature module which governs PrimeFaces dependency resolution.

## UI & Error Handling Rules
- Never print server logs, stack traces, or technical debug dumps to the user in popup dialogs ("Technical Details" segment).
- For errors or exceptions, display a user-friendly message indicating that an error occurred (e.g., "Kaydetme işlemi sırasında bir hata oluştu."), and keep all server logs, stack traces, and diagnostics strictly in the server-side logs.
