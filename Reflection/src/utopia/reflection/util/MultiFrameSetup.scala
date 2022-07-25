package utopia.reflection.util

import utopia.flow.util.logging.Logger
import utopia.genesis.handling.ActorLoop
import utopia.genesis.handling.mutable.ActorHandler
import utopia.reflection.container.stack.StackHierarchyManager
import utopia.reflection.container.swing.window.Window

import scala.concurrent.ExecutionContext

/**
  * Used for quickly setting up a program that displays a single frame and closes once that frame closes
  * @author Mikko Hilpinen
  * @since 17.12.2019, v1+
  */
class MultiFrameSetup(actorHandler: ActorHandler)(implicit exc: ExecutionContext, logger: Logger)
{
	// ATTRIBUTES	--------------------
	
	private lazy val actionLoop = new ActorLoop(actorHandler)
	
	private var started = false
	
	
	// OTHER	-------------------------
	
	/**
	 * Starts this setup
	 */
	def start() =
	{
		if (!started)
		{
			started = true
			actionLoop.runAsync()
			StackHierarchyManager.startRevalidationLoop()
		}
	}
	
	/**
	  * Starts this setup and displays the frame
	  * @param exc Implicit asynchronous execution context
	  */
	def display(frame: Window[_])(implicit exc: ExecutionContext) =
	{
		start()
		frame.startEventGenerators(actorHandler)
		frame.visible = true
	}
}
