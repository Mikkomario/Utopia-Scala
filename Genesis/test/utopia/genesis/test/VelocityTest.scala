package utopia.genesis.test

import java.util.concurrent.TimeUnit

import utopia.flow.util.TimeExtensions._
import utopia.genesis.shape.Axis.X
import utopia.genesis.shape.{Angle, LinearAcceleration, LinearVelocity, Vector3D, Velocity}

import scala.concurrent.duration.TimeUnit

/**
  * Used for testing velocity-related classes
  * @author Mikko Hilpinen
  * @since 13.9.2019, v2.1+
  */
object VelocityTest extends App
{
	implicit val velocityTimeUnit: TimeUnit = TimeUnit.MILLISECONDS
	
	// Tests linear velocity first
	val v1 = LinearVelocity(1) // Speed of 1 / ms
	val v2 = LinearVelocity(2) // Speed of 2 / ms
	val v3 = LinearVelocity(1, 2.millis) // Speed of 0.5 / ms
	
	assert(v1 < v2)
	assert(v3 < v1)
	assert(v2 > v3)
	
	assert(LinearVelocity(0, 4.millis) == LinearVelocity.zero)
	assert(LinearVelocity(0.0, 1.millis) == LinearVelocity.zero)
	
	assert(v1 * 2 == v2)
	assert(v1 / 2 == v3)
	assert(v3 * 4 == v2)
	assert(v1 + v2 == LinearVelocity(3))
	assert(v1 + v3 == LinearVelocity(3, 2.millis))
	
	assert(v1.abs == v1)
	assert((-v1).abs == v1)
	assert(v1.positive == v1)
	assert((-v1).positive == LinearVelocity.zero)
	assert(v1.average(v2) == LinearVelocity(1.5))
	assert(v1.decreasePreservingDirection(LinearVelocity(1.2)) == LinearVelocity.zero)
	assert(v1.decreasePreservingDirection(LinearVelocity(0.5)) == v3)
	
	// Tests distance calculation
	// x1 = x0 + vt
	assert(v1(1.millis) == 1)
	assert(v1(2.millis) == 2)
	assert(v2(1.millis) == 2)
	assert(v2(2.millis) == 4)
	assert(v3(1.millis) == 0.5)
	assert(v3(2.millis) == 1)
	
	// Next tests velocity
	val v4 = Velocity(X(1))
	val v5 = Velocity(Vector3D(1, 1), 2.millis)
	val v6 = Velocity(Vector3D(2, 1, 0.5))
	
	assert(v4.linear == v1)
	assert(v4 * 2 == Velocity(X(2)))
	assert(v4 + v5 == Velocity(Vector3D(3, 1), 2.millis))
	
	assert(v4(2.millis) == X(2))
	assert(v5(1.millis) == Vector3D(0.5, 0.5))
	assert(v6(2.millis) == Vector3D(4, 2, 1))
	
	assert(v1.withDirection(Angle.right) == v4)
	assert(v4.direction == Angle.right)
	assert(v4.in2D == v4)
	assert(v6.in2D == Velocity(Vector3D(2, 1)))
	
	assert(v4.average(v5) == Velocity(Vector3D(1.5, 0.5), 2.millis))
	assert(v4 + v1 == Velocity(X(2)))
	assert(v4 - v1 * 2 == Velocity(X(-1)))
	assert(v4.decreasePreservingDirection(v1 * 2) == Velocity.zero)
	
	// Next tests acceleration
	val a1 = LinearAcceleration(1)
	val a2 = LinearAcceleration(LinearVelocity(2, 1.millis), 3.millis)
	val a3 = LinearAcceleration(-0.5)
	
	assert(a1(1.millis) == v1)
	assert(a2(6.millis) == LinearVelocity(4))
	assert(a3(0.5.millis) == LinearVelocity(-0.25))
	
	// x1 = 0.5at^2 + v0t + x0
	assert(v1(1.millis, a1) == (1.5, LinearVelocity(2)))
	assert(v1(2.millis, a1) == (4, LinearVelocity(3)))
	assert(v2(1.millis, a3) == (1.75, LinearVelocity(1.5)))
	assert(v2(2.millis, a3) == (3, LinearVelocity(1)))
	
	assert(v1.durationUntilStopWith(a3).contains(2.millis))
	assert(v2.durationUntilStopWith(a3).contains(4.millis))
	assert(v1.durationUntilStopWith(a1).isEmpty)
	
	val expectedResult1 = (4.0, LinearVelocity(0.0, 1.millis))
	assert(v2(4.millis, a3) == expectedResult1)
	assert(v2(8.millis, a3, preserveDirection = true) == expectedResult1)
	
	println("Success!")
}
