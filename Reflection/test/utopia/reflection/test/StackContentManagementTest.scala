package utopia.reflection.test

import utopia.flow.util.TimeExtensions._
import utopia.flow.util.CollectionExtensions._
import utopia.flow.async.{Loop, ThreadPool}
import utopia.genesis.color.Color
import utopia.genesis.generic.GenesisDataType
import utopia.genesis.handling.mutable.ActorHandler
import utopia.reflection.component.drawing.immutable.BorderDrawer
import utopia.reflection.component.swing.label.{ImageLabel, ItemLabel, TextLabel}
import utopia.reflection.container.swing.Stack
import utopia.reflection.container.swing.Stack.AwtStackable
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.User
import utopia.reflection.controller.data.ContainerContentManager2
import utopia.reflection.localization.{Localizer, NoLocalization}
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.shape.{Border, StackInsets}
import utopia.reflection.text.Font
import utopia.reflection.text.FontStyle.Plain
import utopia.reflection.util.{ComponentContext, ComponentContextBuilder, SingleFrameSetup}

import scala.concurrent.ExecutionContext

/**
  * Tests stack content management
  * @author Mikko Hilpinen
  * @since 17.4.2020, v1.2
  */
object StackContentManagementTest extends App
{
	GenesisDataType.setup()
	
	// Sets up localization context
	implicit val defaultLanguageCode: String = "EN"
	implicit val localizer: Localizer = NoLocalization
	
	// Creates component context
	implicit val exc: ExecutionContext = new ThreadPool("Reflection").executionContext
	val actorHandler = ActorHandler()
	val baseCB = ComponentContextBuilder(actorHandler, Font("Arial", 12, Plain, 2), Color.green, Color.yellow, 320,
		insets = StackInsets.symmetric(8.any), stackMargin = 8.downscaling, relatedItemsStackMargin = Some(4.downscaling),
		borderWidth = Some(1))
	
	implicit val baseContext: ComponentContext = baseCB.result
	
	// Creates the main content
	val stack = Stack.column[ItemLabel[Int]]()
	val manager = new ContainerContentManager2[Int, Stack[ItemLabel[Int]], ItemLabel[Int]](stack)({ i =>
		val label = ItemLabel.contextual(i)
		label.addContentListener { e => println(s"Label content changed: $e") }
		label
	})
	manager.content = Vector(1, 4, 6)
	
	val content = stack.framed(64.any x 16.any, Color.white)
	
	// Starts test
	val frame = Frame.windowed(content, "Component to Image Test", User)
	frame.setToCloseOnEsc()
	new SingleFrameSetup(actorHandler, frame).start()
	
	var nextNumber = 8
	val updateLoop = Loop(1.seconds) {
		val index = (math.random() * manager.content.size).toInt
		manager.content = manager.content.inserted(nextNumber, index)
		nextNumber += 2
	}
	updateLoop.registerToStopOnceJVMCloses()
	updateLoop.startAsync()
}
