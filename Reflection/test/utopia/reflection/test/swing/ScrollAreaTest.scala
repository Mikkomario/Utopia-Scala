package utopia.reflection.test.swing

import utopia.firmament.drawing.immutable.BoxScrollBarDrawer
import utopia.firmament.localization.DisplayFunction
import utopia.firmament.model.enumeration.WindowResizePolicy.User
import utopia.firmament.model.stack.LengthExtensions._
import utopia.firmament.model.stack.StackInsets
import utopia.firmament.model.stack.modifier.MaxOptimalLengthModifier
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.genesis.handling.KeyStateListener
import utopia.genesis.handling.action.{ActionLoop, ActorHandler2}
import utopia.genesis.text.Font
import utopia.genesis.text.FontStyle.Plain
import utopia.genesis.view.GlobalKeyboardEventHandler
import utopia.paradigm.angular.Rotation
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.generic.ParadigmDataType
import utopia.paradigm.motion.motion1d.LinearAcceleration
import utopia.reflection.component.swing.label.ItemLabel
import utopia.reflection.container.stack.StackHierarchyManager
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.layout.wrapper.scrolling.ScrollArea
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.test.TestContext._

import java.awt.event.KeyEvent
import java.util.concurrent.TimeUnit

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
			new ItemLabel(new EventfulPointer(row * i),
				DisplayFunction.interpolating("Label number %i"), basicFont, initialInsets = StackInsets.symmetric(16.any, 4.fixed))
		}
	}
	val allLabels = labels.flatten
	allLabels.foreach { _.background = Color.yellow }
	allLabels.foreach { _.alignment = Alignment.Center }

	// Creates the columns
	val columns = labels.map { l => Stack.columnWithItems(l, 8.fixed, 4.fixed) }

	// Creates the main stack
	val stack = Stack.rowWithItems(columns, 16.fixed, 4.fixed)
	stack.background = Color.yellow.minusHue(Rotation.clockwise.degrees(33)).darkened

	val actorHandler = ActorHandler2()

	// Creates the scroll area
	val barDrawer = BoxScrollBarDrawer.roundedBarOnly(Color.black.withAlpha(0.55))
	val scrollArea = new ScrollArea(stack, actorHandler, barDrawer, 16, 64,
		friction = LinearAcceleration(2000)(TimeUnit.SECONDS), scrollBarIsInsideContent = true)
	scrollArea addConstraint MaxOptimalLengthModifier(480).symmetric

	// Creates the frame and displays it
	val actionLoop = new ActionLoop(actorHandler)

	val frame = Frame.windowed(scrollArea, "Scroll View Test", User)
	frame.setToExitOnClose()

	// Adds additional action on END key
	GlobalKeyboardEventHandler += KeyStateListener.onKeyPressed(KeyEvent.VK_END) { _ => scrollArea.scrollToBottom() }

	actionLoop.runAsync()
	StackHierarchyManager.startRevalidationLoop()
	frame.startEventGenerators(actorHandler)
	frame.visible = true
}
