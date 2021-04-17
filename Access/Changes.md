# Utopia Access - List of Changes

## v1.3 - 17.4.2021
### Breaking Changes
- `Status.setup()` must be called before using `Status.values`
### New Features
- `Status.values` can now be expanded by calling `Status.introduce(...)` first
### Other Changes
- **ContentTypeException** class added

## v1.2.1
### Scala
- Module is now based on Scala v2.13.3
### New Features
- **Status** now contains two new fields: **isTemporary**: Boolean and 
**doNotRepeat**: Boolean. These fields act as hints for the client when set to true, 
either prompting the client to change or retry the request.
### Other Changes
- Added alternatives for Headers + -methods which accepted more than 1 argument
### New Methods
- Headers
    - ifModifiedSince and withIfModifiedSince(Instant)
    - bearerAuthorization and withBearerAuthorization(String) for token-based authentication
- Status.values to list all values introduced in *Access*