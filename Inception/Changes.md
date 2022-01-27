# Utopia Inception - List of Changes

## v2.3.1 (in development)
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