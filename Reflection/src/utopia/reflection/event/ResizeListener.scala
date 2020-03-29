package utopia.reflection.event

import scala.language.implicitConversions

object ResizeListener
{
	implicit def functionToListener(f: ResizeEvent => Unit): ResizeListener = apply(f)
	
	implicit def functionToListener2(f: => Unit): ResizeListener = apply(_ => f)
	
    /**
     * Wraps a function into a resize listener
     */
    def apply(function: ResizeEvent => Unit): ResizeListener = new ResizeListenerWrapper(function)
}

/**
* ResizeListeners are informed about resize events
* @author Mikko Hilpinen
* @since 26.3.2019
**/
trait ResizeListener
{
    /**
     * This method will be called when a resize event occurs in a component this listener is 
     * listening
     */
	def onResizeEvent(event: ResizeEvent): Unit
}

private class ResizeListenerWrapper(val function: ResizeEvent => Unit) extends ResizeListener
{
    def onResizeEvent(event: ResizeEvent) = function(event)
}