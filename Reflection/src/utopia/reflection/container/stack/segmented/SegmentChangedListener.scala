package utopia.reflection.container.stack.segmented

import scala.language.implicitConversions

object SegmentChangedListener
{
	implicit def functionToListener(f: Segmented => Unit): SegmentChangedListener = apply(f)
	
	implicit def functionToListener2(f: => Unit): SegmentChangedListener = apply(f)
	
	/**
	  * Creates a new simple listener
	  * @param f A function that will be called when a segment is updated
	  * @return A listner that wraps the function
	  */
	def apply(f: Segmented => Unit): SegmentChangedListener = new FunctionalSegmentChangedListener(f)
	
	/**
	  * Creates a new simple listener
	  * @param f A function that will be called when a segment is updated
	  * @return A listener that wraps the function
	  */
	def apply(f: => Unit): SegmentChangedListener = new FunctionalSegmentChangedListener(_ => f)
}

/**
  * These listeners will be informed when the stack length of a segment is updated
  * @author Mikko Hilpinen
  * @since 28.4.2019, v1+
  */
trait SegmentChangedListener
{
	/**
	  * This method is called when a segment length is updated within a segmented item
	  * @param source The segmented item who's segment was updated
	  */
	def onSegmentUpdated(source: Segmented): Unit
}

private class FunctionalSegmentChangedListener(val f: Segmented => Unit) extends SegmentChangedListener
{
	override def onSegmentUpdated(source: Segmented) = f(source)
}