package utopia.reflection.util

import utopia.flow.util.logging.Logger
import utopia.genesis.handling.action.{ActionLoop, ActorHandler}
import utopia.reflection.container.stack.StackHierarchyManager
import utopia.reflection.container.swing.window.Frame

import scala.concurrent.ExecutionContext

/**
  * Used for quickly setting up a program that displays a single frame and closes once that frame closes
  * @author Mikko Hilpinen
  * @since 17.12.2019, v1+
  */
class SingleFrameSetup(actorHandler: ActorHandler, frame: Frame[_])(implicit exc: ExecutionContext, logger: Logger)
{
	// ATTRIBUTES	--------------------
	
	private lazy val actionLoop = new ActionLoop(actorHandler)
	
	private var started = false
	
	
	// OTHER	-------------------------
	
	/**
	  * Starts this setup and displays the frame
	  */
	def start() =
	{
		if (!started)
		{
			started = true
			actionLoop.runAsync()
			StackHierarchyManager.startRevalidationLoop()
			frame.setToExitOnClose()
			frame.startEventGenerators(actorHandler)
			frame.visible = true
		}
	}
}
