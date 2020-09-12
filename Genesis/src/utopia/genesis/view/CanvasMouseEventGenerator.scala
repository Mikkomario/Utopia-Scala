package utopia.genesis.view

/**
 * This class listens to mouse status inside a canvas and generates new mouse events. This 
 * class implements the Actor trait so in order to work, it should be added to a working
 * actorHandler.
 * @author Mikko Hilpinen
 * @since 22.1.2017
 */
class CanvasMouseEventGenerator(canvas: Canvas) extends MouseEventGenerator(canvas, canvas.scaling)