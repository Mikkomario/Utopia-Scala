# Utopia Inception - List of Changes

## v2.3.4 - 01.05.2023
Rebuild due to the changes in other modules.

## v2.3.3 - 02.02.2023
This update supports the changes introduced in **Flow** v2.0.

## v2.3.2 - 06.06.2022
This very minor update changes **Handler** type variance.
### Other Changes
- The **Handler** base trait type parameter is now **covariant** instead of invariant.

## v2.3.1 - 27.01.2022
Scala version update
### Scala
This module now uses Scala v2.13.7

## v2.3 - 17.4.2021
This small update fixes a design problem in the **Killable** trait. 
It is generally not a good idea to include attributes in traits, 
but I didn't understand this when I initially created the **Killable** trait.
### Breaking Changes
- The **Killable** trait no longer contains any attributes in itself, only the definition of method `.kill()`
### New Methods
- Added `.debugString` to **Handler** and **HandlerRelay**

## v2.2
### Scala
- Module is now based on Scala v2.13.3
### Breaking Changes
- Removed *parent* property from **Handleable** trait
