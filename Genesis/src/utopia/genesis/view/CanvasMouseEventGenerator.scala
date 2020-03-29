package utopia.genesis.view

import utopia.genesis.handling.{MouseButtonStateListener, MouseMoveListener, MouseWheelListener}

/**
 * This class listens to mouse status inside a canvas and generates new mouse events. This 
 * class implements the Actor trait so in order to work, it should be added to a working
 * actorHandler.
 * @author Mikko Hilpinen
 * @since 22.1.2017
 */
class CanvasMouseEventGenerator(val canvas: Canvas, moveHandler: MouseMoveListener,
                                buttonHandler: MouseButtonStateListener, wheelHandler: MouseWheelListener)
    extends MouseEventGenerator(canvas, moveHandler, buttonHandler, wheelHandler, () => canvas.scaling)