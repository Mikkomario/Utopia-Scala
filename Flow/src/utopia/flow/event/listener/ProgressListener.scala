package utopia.flow.event.listener

import utopia.flow.event.model.ProgressEvent
import utopia.flow.util.SignificantProgressFilter

import scala.language.implicitConversions

object ProgressListener
{
	// IMPLICIT ------------------------
	
	/**
	 * Converts a function into a progress listener
	 * @param f Function to convert
	 * @tparam A Type of process events accepted
	 * @return A progress listener wrapping the specified function
	 */
	implicit def apply[A](f: ProgressEvent[A] => Unit): ProgressListener[A] = new _ProgressListener[A](f)
	
	
	// OTHER    ------------------------
	
	/**
	 * Creates a progress listener that has a minimum progress trigger interval
	 * @param significantProgressThreshold Smallest progress since the last progress event
	 *                                     that should trigger this listener
	 * @param f Function that will process the progress events
	 * @tparam A Type of accepted process values
	 * @return A new progress listener
	 */
	def usingThreshold[A](significantProgressThreshold: Double)(f: ProgressEvent[A] => Unit): ProgressListener[A] =
		new SignificantProgressFilter[A](significantProgressThreshold, apply(f))
		
	
	// NESTED   ------------------------
	
	private class _ProgressListener[A](f: ProgressEvent[A] => Unit) extends ProgressListener[A]
	{
		override def onProgressEvent(event: ProgressEvent[A]): Unit = f(event)
	}
}

/**
 * Common trait for items that are interested in receiving progress events
 *
 * @tparam A Type of listened process values
 * @author Mikko Hilpinen
 * @since 07.05.2024, v2.4
 */
trait ProgressListener[-A]
{
	/**
	 * This method is called when some progress is made
	 * @param event The progress event that occurred
	 */
	def onProgressEvent(event: ProgressEvent[A]): Unit
}
