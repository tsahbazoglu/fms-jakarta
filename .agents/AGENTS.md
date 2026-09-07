# Project Rules & Customizations

## Build Profiles & Dependencies
- When building the project with e-signature support, use the profile: `mvn clean install -PesignOyas -Ddependency.scope=provided`
- The `esignOyas` profile activates the e-signature module which governs PrimeFaces dependency resolution.
