# Utopia Conflict
Easy-to-use polygon-based collision-detection.

## Parent Modules
- Utopia Flow
- Utopia Paradigm
- Utopia Genesis

## Main Features
2D collision handling with **CanCollideWith**, **CollisionListener**, **CollisionTargetHandler** 
and **CollisionHandler** traits
- Follows the **Handleable** + **Handler** design logic from the **Genesis** module
- Supports advanced **Polygonic** shapes from **Genesis**, as well as **Circle**s
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
