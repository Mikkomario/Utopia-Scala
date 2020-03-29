package utopia.reflection.util

import utopia.genesis.handling.ActorLoop
import utopia.genesis.handling.mutable.ActorHandler
import utopia.reflection.container.stack.StackHierarchyManager
import utopia.reflection.container.swing.window.{Frame, Window}

import scala.concurrent.ExecutionContext

/**
  * Used for quickly setting up a program that displays a single frame and closes once that frame closes
  * @author Mikko Hilpinen
  * @since 17.12.2019, v1+
  */
class SingleFrameSetup(actorHandler: ActorHandler, private val frame: Frame[_])
{
	// ATTRIBUTES	--------------------
	
	private val actionLoop = new ActorLoop(actorHandler)
	
	private var started = false
	
	
	// INITIAL CODE	--------------------
	
	frame.setToExitOnClose()
	actionLoop.registerToStopOnceJVMCloses()
	
	
	// OTHER	-------------------------
	
	/**
	  * Starts this setup and displays the frame
	  * @param exc Implicit asynchronous execution context
	  */
	def start()(implicit exc: ExecutionContext) =
	{
		if (!started)
		{
			started = true
			actionLoop.startAsync()
			StackHierarchyManager.startRevalidationLoop()
			frame.startEventGenerators(actorHandler)
			frame.isVisible = true
		}
	}
}
