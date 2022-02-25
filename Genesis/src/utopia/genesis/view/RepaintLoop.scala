package utopia.genesis.view

import utopia.flow.time.TimeExtensions._

import java.awt.Component
import java.time.Instant
import javax.swing.SwingUtilities
import utopia.flow.async.LoopingProcess
import utopia.flow.time.Now
import utopia.flow.time.WaitTarget.{Until, WaitDuration}
import utopia.genesis.util.Fps

import scala.concurrent.ExecutionContext
import scala.ref.WeakReference

/**
  * This loop continuously repaints a single component
  * @param comp The target component
  * @param maxFPS Maximum frames (paints) per second (default = 60)
  */
class RepaintLoop(comp: Component, val maxFPS: Fps = Fps.default)(implicit exc: ExecutionContext)
	extends LoopingProcess
{
	// ATTRIBUTES	-----------------
	
	private val component = WeakReference(comp)
	
	private var hasDrawn = false // Set to true once the component has been drawn at least once
	private var lastDrawTime = Instant.now()
	
	
	// INITIAL CODE	-----------------
	
	// Starts listening to component state changes
	// comp.addComponentListener(new ComponentListener())
	
	
	// IMPLEMENTED	-----------------
	
	// This process may be restarted as long as the component is available
	override protected def isRestartable = component.get.isDefined
	
	/**
	  * Paints the target component
	  */
	override def iteration() =
	{
		component.get match {
			case Some(component) =>
				// Case: Component can't be displayed
				if (!component.isDisplayable) {
					// Case: Component was removed => stops drawing
					if (hasDrawn)
						None
					// Case: Component hasn't been attached yet => waits with increased delay
					else
						Some(WaitDuration(maxFPS.interval * 10))
				}
				// Case: Component is (temporarily) hidden => waits with increased delay
				else if (!component.isShowing)
					Some(WaitDuration(maxFPS.interval * 5))
				// Case: Component may be drawn
				else {
					hasDrawn = true
					lastDrawTime = Now
					// Repaints the component in the swing event thread. Waits until paint is finished.
					SwingUtilities.invokeAndWait { () => component.repaint() }
					Some(Until(lastDrawTime + maxFPS.interval))
				}
			// Case: The component is no longer held in memory => stops
			case None => None
		}
	}
	
	
	// NESTED CLASSES	-----------------
	
	/* For some reason, component listening didn't work at all
	private class ComponentListener extends ComponentAdapter with HierarchyListener
	{
		override def hierarchyChanged(e: HierarchyEvent) =
		{
			println("Notifies loop")
			WaitUtils.notify(waitLock)
		}
		
		override def componentShown(e: ComponentEvent) =
		{
			// When component is shown, reawakens the loop
			println("Notifies loop")
			WaitUtils.notify(waitLock)
		}
	}*/
}
