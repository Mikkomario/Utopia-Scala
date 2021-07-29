# Utopia Ambassador - List of Changes

## v1.1 (in development)
### Breaking Changes
- **GoogleRedirector** is now a class and not an object, since it takes construction parameters.
  - Use `GoogleRedirector.default` instead of **GoogleRedirector** itself.
### New Features
- **GoogleRedirector** now supports two optional parameters: 
  1) Whether user should be prompted to select the applicable account and
  2) Whether user consent should be asked even if provided already
### New Methods
- Added service ids access to user authentication token access point, as well as to 
  **DbUser** via **AuthDbExtensions**