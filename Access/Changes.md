# Utopia Access - List of Changes

## v1.3.1 (in development)
### New Features
- Added **MovedPermanently** (301) and **Found** (302) status codes to the **Status** enumeration.

## v1.3 - 17.4.2021
This relatively small update changes how the **Status** enumeration works. 
This fixes a run-time issue but requires changes in the implementing code, also.
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