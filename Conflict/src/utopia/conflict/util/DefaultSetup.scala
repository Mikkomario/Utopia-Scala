package utopia.conflict.util

import utopia.conflict.handling.mutable.{CollidableHandler, CollisionHandler}
import utopia.flow.util.logging.Logger
import utopia.genesis.util.{Fps, Setup}
import utopia.paradigm.shape.shape2d.vector.size.Size

import scala.concurrent.ExecutionContext

/**
  * This class handles basic program setup with default arguments
  * @author Mikko Hilpinen
  * @since 20.4.2019, v1+
  */
class DefaultSetup(initialGameWorldSize: Size, title: String, maxFPS: Fps = Fps.default)
				  (implicit context: ExecutionContext, logger: Logger) extends Setup
{
	// ATTRIBUTES	------------------
	
	/**
	  * A handler for collidables
	  */
	val collidableHandler = CollidableHandler()
	/**
	  * A handler for collision listeners
	  */
	val collisionHandler = CollisionHandler(collidableHandler)
	
	private val genesisSetup = new utopia.genesis.util.DefaultSetup(initialGameWorldSize, title, maxFPS)
	
	
	// INITIAL CODE	------------------
	
	// Registers handlers
	handlers.register(collidableHandler, collisionHandler)
	
	// Sets up listening
	actorHandler += collisionHandler
	
	
	// COMPUTED	----------------------
	
	def actorHandler = genesisSetup.actorHandler
	def drawHandler = genesisSetup.drawHandler
	def mouseButtonHandler = genesisSetup.mouseButtonHandler
	def mouseMoveHandler = genesisSetup.mouseMoveHandler
	def mouseWheelHandler = genesisSetup.mouseWheelHandler
	def keyStateHandler = genesisSetup.keyStateHandler
	def keyTypedHandler = genesisSetup.keyTypedHandler
	
	/**
	  * @return The canvas that draws the objects
	  */
	def canvas = genesisSetup.canvas
	/**
	  * @return A frame where the program is displayed
	  */
	def frame = genesisSetup.frame
	
	
	// IMPLEMENTED	------------------
	
	override def handlers = genesisSetup.handlers
	
	override def start() = genesisSetup.start()
}
