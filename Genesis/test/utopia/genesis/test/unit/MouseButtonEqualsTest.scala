package utopia.genesis.test.unit

import utopia.genesis.handling.event.mouse.MouseButton

/**
  * Tests mouse button equality check
  * @author Mikko Hilpinen
  * @since 09/02/2024, v4.0
  */
object MouseButtonEqualsTest extends App
{
	assert(MouseButton.Left == MouseButton.Left)
}
