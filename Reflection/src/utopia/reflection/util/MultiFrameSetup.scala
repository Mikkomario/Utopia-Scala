package utopia.reflection.util

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
class MultiFrameSetup(actorHandler: ActorHandler)
{
	// ATTRIBUTES	--------------------
	
	private val actionLoop = new ActorLoop(actorHandler)
	
	private var started = false
	
	
	// INITIAL CODE	--------------------
	
	actionLoop.registerToStopOnceJVMCloses()
	
	
	// OTHER	-------------------------
	
	/**
	 * Starts this setup
	 * @param exc Implicit asynchronous execution context
	 */
	def start()(implicit exc: ExecutionContext) =
	{
		if (!started)
		{
			started = true
			actionLoop.startAsync()
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
