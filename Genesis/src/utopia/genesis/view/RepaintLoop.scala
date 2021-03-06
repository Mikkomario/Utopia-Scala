package utopia.genesis.view

import utopia.flow.time.TimeExtensions._

import java.awt.Component
import java.time.Instant
import javax.swing.SwingUtilities
import utopia.flow.async.Loop
import utopia.flow.time.{Now, WaitUtils}
import utopia.flow.time.WaitTarget.Until
import utopia.genesis.util.Fps

import scala.ref.WeakReference

/**
  * This loop continuously repaints a single component
  * @param comp The target component
  * @param maxFPS Maximum frames (paints) per second (default = 60)
  */
class RepaintLoop(comp: Component, val maxFPS: Fps = Fps.default) extends Loop
{
	// ATTRIBUTES	-----------------
	
	private val component = WeakReference(comp)
	
	private var lastDrawTime = Instant.now()
	
	
	// INITIAL CODE	-----------------
	
	// Starts listening to component state changes
	// comp.addComponentListener(new ComponentListener())
	
	
	// IMPLEMENTED	-----------------
	
	/**
	  * Paints the target component
	  */
	override def runOnce() =
	{
		// Waits until component is displayable
		while (!isBroken && component.get.exists { c => !c.isDisplayable || !c.isShowing })
		{
			WaitUtils.wait(maxFPS.interval * 3, waitLock)
		}
		
		lastDrawTime = Now
		
		if (component.get.exists { _.isDisplayable })
		{
			// Repaints the component in the swing event thread. Waits until paint is finished.
			SwingUtilities.invokeAndWait(() => component.get.foreach { _.repaint() })
		}
		else if (component.get.isEmpty)
		{
			// Stops once the component is no longer held in memory
			stop()
		}
	}
	
	/**
	  * The time between the end of the current run and the start of the next one
	  */
	override def nextWaitTarget = Until(lastDrawTime + maxFPS.interval)
	
	
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
