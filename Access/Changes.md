# Utopia Access - List of Changes

## v1.5.3 (in development)
This small update makes calling `Status.setup()` optional. The "failures" to do so will now be handled automatically, 
instead of throwing exceptions.
### New methods
- **Status**
  - Added `.apply(Int)` that finds or constructs a **Status** instance
### Other changes
- `Status.values` & `.introduce(...)` no longer throw if `.setup()` has not been called, 
  but instead call it themselves, if necessary

## v1.5.2 - 04.10.2024
A new build with Scala v2.13.14. No other changes.

## v1.5.1 - 28.07.2024
Mostly smaller character-set and content-type -related updates.
### Bugfixes
- Fixed the parsing of the `charset` property in **Headers**
### New methods
- **ContentType**
  - Added `.charset` and `.withCharset(Charset)`
- **Headers**
  - Added `.contains(String)`
  - Added `.mapAcceptedTypes(...)`
### Other changes
- Added **StatusGroup.Custom** in order to cover status codes outside the normal (100-599) code range

## v1.5 - 22.01.2024
This update, while quite small in scale, contains a very important bugfix to the **Headers** class. 
Some functions and classes have also been updated to match the more recent (Flow) style.
### Breaking changes
- **Headers**`.apply(String)` and other header-retrieving functions now return a **String** instead of an **Option**
- `.isTemporary` and `.doNotRepeat` in **Status** are now of type **UncertainBoolean** instead of **Boolean**
### Bugfixes
- **Header**-retrieving functions didn't work at all because of a casing issue - fixed
### Other changes
- Scala version updated to 2.13.12

## v1.4.7 - 27.09.2023
Supports latest **Flow** (v2.2) changes

## v1.4.6 - 02.02.2023
The main purpose of this update is to support the changes introduced in **Flow** v2.0.
### New Methods
- **Status**
  - Added `.isSuccess` and `.isFailure`

## v1.4.5 - 02.10.2022
Supports changes in **Flow** v1.17

## v1.4.4 - 18.08.2022
New Build / Supports changes in **Flow** v1.16

## v1.4.3 - 27.01.2022
Minor update to Headers
### Scala
This module now uses Scala v2.13.7
### New Methods
- **Headers**
  - Added `.withAcceptedType(ContentType)`

## v1.4.2 - 04.11.2021
New build / supports changes in **Flow** v1.14

## v1.4.1 - 3.10.2021
This minor update reflects some additions made to the **Flow** module. Namely, adding **Pair** support to **Headers**.
### New Methods
- **Headers**
  - Added a variant of + that accepts a **Pair**

## v1.4 - 13.7.2021
This update makes **Headers** instance creation more intuitive and also adds support for redirection status codes. 
### Breaking Changes
- `new Headers(...)` is now private. Please use `Headers.apply(...)` instead
### New Features
- Added **MovedPermanently** (301) and **Found** (302) status codes to the **Status** enumeration.
### Other Changes
- The **Headers** object is now implicitly convertible to an empty **Headers** instance

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
