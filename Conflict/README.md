# Utopia Conflict
Easy-to-use polygon-based collision-detection.

## Parent Modules
- [Utopia Flow](https://github.com/Mikkomario/Utopia-Scala/tree/master/Flow)
- [Utopia Paradigm](https://github.com/Mikkomario/Utopia-Scala/tree/master/Paradigm)
- [Utopia Genesis](https://github.com/Mikkomario/Utopia-Scala/tree/master/Genesis)

## Main Features
2D collision handling with **CanCollideWith**, 
[CollisionListener](https://github.com/Mikkomario/Utopia-Scala/blob/master/Conflict/src/utopia/conflict/handling/CollisionListener.scala), 
**CollisionTargetHandler** and 
[CollisionHandler](https://github.com/Mikkomario/Utopia-Scala/blob/master/Conflict/src/utopia/conflict/handling/CollisionHandler.scala) traits
- Follows the **Handleable** + **Handler** design logic from the **Genesis** module
- Supports advanced 
  [Polygonic](https://github.com/Mikkomario/Utopia-Scala/blob/master/Paradigm/src/utopia/paradigm/shape/shape2d/area/polygon/Polygonic.scala) 
  shapes from **Paradigm**, as well as 
  [Circles](https://github.com/Mikkomario/Utopia-Scala/blob/master/Paradigm/src/utopia/paradigm/shape/shape2d/area/Circle.scala)
- Collision events provide access to collision (intersection) points as well as a minimum translation
  vector (MTV) which helps the receiver to resolve the collision situation (using translation, for example)
  
## Implementation Hints

### Extensions you should be aware of
- [utopia.conflict.collision.Extensions](https://github.com/Mikkomario/Utopia-Scala/blob/master/Conflict/src/utopia/conflict/collision/Extensions.scala)
    - Enables collision checking methods in both **Polygonics** and **Circles**

### You should get familiar with these classes
- **CanCollideWith** - Extend this if you want other objects to collide with your **CollisionListener** instance
- **CollisionListener** - Extend this if you want to receive collision events
- **CollisionHandler** - Used for checking collisions between items and for delivering collision events.
