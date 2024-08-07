package utopia.reflection.test.swing

import utopia.firmament.model.enumeration.WindowResizePolicy.User
import utopia.firmament.model.stack.LengthExtensions._
import utopia.firmament.model.stack.StackInsets
import utopia.genesis.handling.action.{ActionLoop, ActorHandler}
import utopia.genesis.text.Font
import utopia.genesis.text.FontStyle.Plain
import utopia.paradigm.color.Color
import utopia.paradigm.generic.ParadigmDataType
import utopia.reflection.component.swing.input.JDropDownWrapper
import utopia.reflection.container.stack.StackHierarchyManager
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.test.TestContext._

import scala.collection.immutable.HashMap

/**
  * This is a simple test implementation of drop downs
  * @author Mikko Hilpinen
  * @since 24.4.2019, v1+
  */
object DropDownTest extends App
{
	ParadigmDataType.setup()

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
	categorySelect.valuePointer.addContinuousListener { c =>
		characterSelect.content = c.newValue.flatMap(data.get) getOrElse Vector()
	}
	characterSelect.valuePointer.addContinuousListener { c =>
		println(c.newValue.map { _ + " is ready for adventure!" } getOrElse "No character selected")
	}

	// Creates the frame and displays it
	val actorHandler = ActorHandler()
	val actionLoop = new ActionLoop(actorHandler)

	val frame = Frame.windowed(stack, "Drop Down Test", User)
	frame.setToExitOnClose()

	actionLoop.runAsync()
	StackHierarchyManager.startRevalidationLoop()
	frame.startEventGenerators(actorHandler)
	frame.visible = true
}
