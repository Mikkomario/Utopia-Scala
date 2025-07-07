package utopia.paradigm.test

import utopia.flow.operator.equality.EqualsExtensions._
import utopia.paradigm.transform.Adjustment

/**
  * Tests the Adjustment class
  * @author Mikko Hilpinen
  * @since 06.07.2025, v1.7.3
  */
object AdjustmentTest extends App
{
	private val adj = Adjustment(0.2)
	
	assert(adj(-1) ~== 0.8, adj(-1))
	assert(adj(-2) ~== 0.64, adj(-2))
	assert(adj(1) ~== 1.25, adj(1))
	assert(adj(2) ~== 1.5625, adj(2))
	assert(adj(0) == 1)
	
	println("Success!")
}
