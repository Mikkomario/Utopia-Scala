package utopia.reflection.test

import utopia.flow.async.ThreadPool
import utopia.genesis.color.Color
import utopia.genesis.generic.GenesisDataType
import utopia.genesis.handling.ActorLoop
import utopia.genesis.handling.mutable.ActorHandler
import utopia.reflection.component.swing.JDropDownWrapper
import utopia.reflection.container.stack.StackHierarchyManager
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.User
import utopia.reflection.container.swing.Stack
import utopia.reflection.localization.{Localizer, NoLocalization}
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.shape.StackInsets
import utopia.reflection.text.Font
import utopia.reflection.text.FontStyle.Plain

import scala.collection.immutable.HashMap
import scala.concurrent.ExecutionContext

/**
  * This is a simple test implementation of drop downs
  * @author Mikko Hilpinen
  * @since 24.4.2019, v1+
  */
object DropDownTest extends App
{
	GenesisDataType.setup()
	
	// Sets up localization context
	implicit val defaultLanguageCode: String = "EN"
	implicit val localizer: Localizer = NoLocalization
	
	// Creates the labels
	val basicFont = Font("Arial", 14, Plain, 2)
	
	val data = HashMap("Fighter" -> Vector("Aragorn", "Gimli", "Boromir"), "Archer" -> Vector("Legolas"),
		"Wizard" -> Vector("Gandalf", "Radagast"))
	
	val categorySelect = new JDropDownWrapper[String](StackInsets.symmetric(16.any, 4.upscaling), "< Select Class >", basicFont,
		Color.white, Color.magenta, initialContent = data.keySet.toVector.sorted)
	val characterSelect = new JDropDownWrapper[String](StackInsets.symmetric(16.any, 4.upscaling), "< Select Character >", basicFont,
		Color.white, Color.magenta)
	
	categorySelect.component.setFont(basicFont.toAwt)
	
	// Creates the main stack
	val stack = Stack.columnWithItems(Vector(categorySelect, characterSelect), 8.downscaling, 4.fixed)
	// stack.background = Color.yellow.minusHue(33).darkened(1.2)
	stack.background = Color.black
	
	// Adds item listners
	categorySelect.addValueListener { c => characterSelect.content = c.newValue.flatMap(data.get) getOrElse Vector() }
	characterSelect.addValueListener { c => println(c.newValue.map { _ + " is ready for adventure!" } getOrElse "No character selected") }
	
	// Creates the frame and displays it
	val actorHandler = ActorHandler()
	val actionLoop = new ActorLoop(actorHandler)
	implicit val context: ExecutionContext = new ThreadPool("Reflection").executionContext
	
	val frame = Frame.windowed(stack, "Drop Down Test", User)
	frame.setToExitOnClose()
	
	actionLoop.registerToStopOnceJVMCloses()
	actionLoop.startAsync()
	StackHierarchyManager.startRevalidationLoop()
	frame.startEventGenerators(actorHandler)
	frame.isVisible = true
}
