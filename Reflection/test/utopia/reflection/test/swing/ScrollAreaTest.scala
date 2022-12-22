package utopia.reflection.test.swing

import utopia.flow.view.mutable.eventful.PointerWithEvents

import java.awt.event.KeyEvent
import java.util.concurrent.TimeUnit
import utopia.paradigm.color.Color
import utopia.paradigm.generic.ParadigmDataType
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.handling.{ActorLoop, KeyStateListener}
import utopia.paradigm.angular.Rotation
import utopia.paradigm.motion.motion1d.LinearAcceleration
import utopia.paradigm.shape.shape2d.Size
import utopia.reflection.component.drawing.immutable.BoxScrollBarDrawer
import utopia.reflection.component.swing.label.ItemLabel
import utopia.reflection.container.stack.StackHierarchyManager
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.layout.wrapper.scrolling.ScrollArea
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.User
import utopia.reflection.localization.DisplayFunction
import utopia.reflection.shape.stack.{StackInsets, StackLengthLimit}
import utopia.reflection.text.Font
import utopia.reflection.text.FontStyle.Plain
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.test.TestContext._

/**
  * This is a simple test implementation of scroll Area
  * @author Mikko Hilpinen
  * @since 18.5.2019, v1+
  */
object ScrollAreaTest extends App
{
	ParadigmDataType.setup()

	// Creates the labels
	val basicFont = Font("Arial", 12, Plain, 2)
	val labels = (1 to 10).toVector.map { row =>
		(1 to 50).toVector.map { i =>
			new ItemLabel(new PointerWithEvents(row * i),
				DisplayFunction.interpolating("Label number %i"), basicFont, initialInsets = StackInsets.symmetric(16.any, 4.fixed))
		}
	}
	val allLabels = labels.flatten
	allLabels.foreach { _.background = Color.yellow }
	allLabels.foreach { _.alignCenter() }

	// Creates the columns
	val columns = labels.map { l => Stack.columnWithItems(l, 8.fixed, 4.fixed) }

	// Creates the main stack
	val stack = Stack.rowWithItems(columns, 16.fixed, 4.fixed)
	stack.background = Color.yellow.minusHue(Rotation.ofDegrees(33)).darkened

	val actorHandler = ActorHandler()

	// Creates the scroll area
	val barDrawer = BoxScrollBarDrawer.roundedBarOnly(Color.black.withAlpha(0.55))
	val scrollArea = new ScrollArea(stack, actorHandler, barDrawer, 16, 64,
		friction = LinearAcceleration(2000)(TimeUnit.SECONDS),
		lengthLimits = StackLengthLimit.sizeLimit(maxOptimal = Some(Size.square(480))),
		scrollBarIsInsideContent = true)

	// Creates the frame and displays it
	val actionLoop = new ActorLoop(actorHandler)

	val frame = Frame.windowed(scrollArea, "Scroll View Test", User)
	frame.setToExitOnClose()

	// Adds additional action on END key
	scrollArea.addKeyStateListener(KeyStateListener.onKeyPressed(KeyEvent.VK_END) { _ => scrollArea.scrollToBottom() })

	actionLoop.runAsync()
	StackHierarchyManager.startRevalidationLoop()
	frame.startEventGenerators(actorHandler)
	frame.visible = true
}
