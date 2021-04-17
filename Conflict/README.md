# Utopia Conflict

## Parent Modules
- Utopia Flow
- Utopia Inception
- Utopia Genesis

## Main Features
2D collision handling with **Collidable**, **CollisionListener**, **CollidableHandler** 
and **CollisionHandler** traits
- Follows the **Handleable** + **Handler** design logic from **Genesis** and **Inception**
- Both mutable and immutable implementations available
- Supports advanced polygonic shapes from **Genesis**, as well as circles
- Collision events provide access to collision (intersection) points as well as a minimum translation
vector (MTV) which helps the receiver to resolve the collision situation (with translation, for example)
  
## Implementation Hints

### Extensions you should be aware of
- utopia.conflict.collision.**Extensions**
    - Enables collision checking methods in both **Polygonics** and **Circles**

### You should get familiar with these classes
- **DefaultSetup** - This **Setup** implementation replaces **DefaultSetup** in Genesis 
  (contains collision handling).
- **Collidable** - Extend this if you want other objects to collide with your class instance
- **CollisionListener** - Extend this if you want to receive collision events
- **CollisionHandler** - Used for checking collisions between items and for delivering collision events. 
  You should have one in your **Setup**
