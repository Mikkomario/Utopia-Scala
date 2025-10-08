package utopia.firmament.awt

import utopia.firmament.component.Window.JWindow

import java.awt.event.{ComponentEvent, ComponentListener, WindowAdapter, WindowEvent}
import scala.concurrent.{Future, Promise}

/**
 * Provides some utility functions for dealing with AWT components.
 * @see [[AwtComponentExtensions]]; Most of the utility features are implemented here as implicit functions
 * @author Mikko Hilpinen
 * @since 08.10.2025, v1.6
 */
object AwtUtils
{
	// OTHER    ----------------------------
	
	/**
	 * Creates a future that resolves once the specified window has been closed
	 * @param window A window to track
	 * @param resolveIfHidden Whether to resolve the future even in case the window is only hidden (default = false)
	 * @param closeIfHidden Whether to close the window automatically in the event it becomes hidden (default = false)
	 * @return A future that resolves once the window closes
	 */
	def windowCloseFuture(window: JWindow, resolveIfHidden: Boolean = false, closeIfHidden: Boolean = false) = {
		// Case: Window is visible or has not been shown yet => Starts tracking window state
		if (window.isDisplayable || window.isVisible) {
			val releasePromise = Promise[Unit]()
			
			// Listens to window hidden & window closed events, fulfilling the promise when appropriate
			val listener = new ClosedOrHiddenTracker({ closed =>
				if (closed || resolveIfHidden)
					releasePromise.trySuccess(())
				if (!closed && closeIfHidden)
					window.dispose()
			})
			window.addWindowListener(listener)
			if (resolveIfHidden || closeIfHidden)
				window.addComponentListener(listener)
			
			releasePromise.future
		}
		// Case: Window was already closed => Resolves immediately
		else
			Future.successful(())
	}
	
	
	// NESTED   ----------------------------
	
	private class ClosedOrHiddenTracker(f: Boolean => Unit) extends WindowAdapter with ComponentListener
	{
		override def windowClosed(e: WindowEvent): Unit = f(true)
		
		override def componentShown(e: ComponentEvent): Unit = ()
		override def componentHidden(e: ComponentEvent): Unit = f(false)
		
		override def componentResized(e: ComponentEvent): Unit = ()
		override def componentMoved(e: ComponentEvent): Unit = ()
	}
}
