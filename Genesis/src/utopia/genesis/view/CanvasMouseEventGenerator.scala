package utopia.genesis.view

import scala.concurrent.ExecutionContext

/**
 * This class listens to mouse status inside a canvas and generates new mouse events. This 
 * class implements the Actor trait so in order to work, it should be added to a working
 * actorHandler.
 * @author Mikko Hilpinen
 * @since 22.1.2017
 */
@deprecated("Replaced with a new implementation", "v3.3")
class CanvasMouseEventGenerator(canvas: Canvas)(implicit exc: ExecutionContext)
	extends MouseEventGenerator(canvas, canvas.scaling)