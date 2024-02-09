package utopia.genesis.test

import utopia.genesis.event.MouseButton

/**
  * Tests mouse button equality check
  * @author Mikko Hilpinen
  * @since 09/02/2024, v4.0
  */
object MouseButtonEqualsTest extends App
{
	assert(MouseButton.Left == MouseButton.Left)
}
