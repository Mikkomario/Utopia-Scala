package utopia.conflict.test

import utopia.conflict.handling.CollisionGroup

/**
 * These are the collision groups used in conflict specific tests
 * @author Mikko Hilpinen
 * @since 4.8.2017
 */
object TestCollisionGroups
{
    case object Obstacle extends CollisionGroup
    case object UserInput extends CollisionGroup
}