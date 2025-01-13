# Utopia Conflict - List of Changes

## v1.6 (in development)
### Breaking changes
- **CollisionHandler**, **CollisionPartyHandler** and **CollisionTargetHandler** 
  now require access to an implicit **Logger**
### Other changes
- Changed some deprecated **Polygonic** references to **Polygon** references

## v1.5.1 - 04.10.2024
A new build with Scala v2.13.14. No other changes.

## v1.5 - 28.07.2024
This update reflects changes introduced in **Genesis v4.0** update. Mainly, 
the collision handling interface had to be rewritten.

Like with many other **Conflict** updates, this update lacks testing.  
If you intend to use this version, consider contacting and possibly collaborating 
in order to ensure that all possible bugs get fixed.
### Breaking changes
- Removed the **DefaultSetup** class
  - No replacement has been added yet
  - For the time being, please use **DrawableCanvas** in **Reach** instead, and set the collision systems manually
- **CollidableHandler** classes were replaced with **CollisionTargetHandler**
- **Collidable** was replaced with **CanCollideWith**
- **CollisionHandler** class was completely rewritten
  - The new implementation is mutable. Immutable version was removed.
### Other changes
- This module is no longer dependent from the **Inception** module
- **Collision** and **CollisionShape** now use **Seq** instead of **Vector**
- This release has not been properly tested. Exercise discretion.

## v1.4.5 - 22.01.2024
Supports **Flow v2.3**
### Other changes
- Scala version updated to 2.13.12

## v1.4.4 - 27.09.2023
Supports **Paradigm v1.4**

## v1.4.3 - 01.05.2023
Rebuild due to the changes in **Genesis**

## v1.4.2 - 02.02.2023
This update supports the changes introduced in **Flow** v2.0 and **Paradigm** v1.2.

## v1.4.1 - 02.10.2022
New build compatible with **Flow** v1.17.

## v1.4 - 18.08.2022
This update reflects changes in **Flow** v1.16 and **Genesis** v3.0
### Breaking Changes
- **DefaultSetup** now requires an implicit **Logger** parameter

## v1.3.2 - 27.01.2022
Scala version update
### Scala
This module now uses Scala v2.13.7

## v1.3.1 - 04.11.2021
New build / supports changes in **Flow** v1.14

## v1.3 - 17.4.2021
Complies with the changes in Flow and Genesis
- New transformation classes and Vector2D in use
