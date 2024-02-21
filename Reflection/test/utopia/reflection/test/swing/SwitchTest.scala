package utopia.reflection.test.swing

import utopia.firmament.model.enumeration.StackLayout.{Leading, Trailing}
import utopia.firmament.model.enumeration.WindowResizePolicy.User
import utopia.firmament.model.stack.LengthExtensions._
import utopia.firmament.model.stack.StackInsets
import utopia.genesis.handling.action.{ActionLoop, ActorHandler}
import utopia.genesis.text.Font
import utopia.genesis.text.FontStyle.Plain
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Axis.X
import utopia.paradigm.enumeration.Direction2D
import utopia.paradigm.generic.ParadigmDataType
import utopia.reflection.component.swing.input.Switch
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.container.stack.StackHierarchyManager
import utopia.reflection.container.swing.layout.SegmentGroup
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.test.TestContext._

/**
  * This is a simple test implementation of switches
  * @author Mikko Hilpinen
  * @since 24.4.2019, v1+
  */
object SwitchTest extends App
{
	ParadigmDataType.setup()

	// Creates the hint labels
	val basicFont = Font("Arial", 12, Plain, 2)
	val labels = Vector("Enabled", "Disabled (OFF)", "Disabled (ON)").map { s =>
		TextLabel(s, basicFont,
			insets = StackInsets.symmetric(8.any, 0.any))
	}
	labels.foreach { l => l.alignTo(Direction2D.Right) }

	// Creates the switches
	val actorHandler = ActorHandler()

	val enabledSwitch = new Switch(actorHandler, 32.upTo(64), Color.red, initialState = true)
	val disabledSwitch = new Switch(actorHandler, 32.upTo(64), Color.red)
	disabledSwitch.enabled = false
	val disabledSwitch2 = new Switch(actorHandler, 32.upTo(64), Color.red, initialState = true)
	disabledSwitch2.enabled = false

	// Creates the stacks
	val group = new SegmentGroup(X, Vector(Trailing, Leading))

	def combine(label: TextLabel, field: Switch) = Stack.rowWithItems(
		group.wrap(Vector(label, field)), 8.downscaling, 8.downscaling)

	val enabledStack = combine(labels(0), enabledSwitch)
	val disabledStack = combine(labels(1), disabledSwitch)
	val disabledStack2 = combine(labels(2), disabledSwitch2)

	val stack = Stack.columnWithItems(Vector(enabledStack, disabledStack, disabledStack2), 8.downscaling)

	// Creates the frame and displays it
	val actionLoop = new ActionLoop(actorHandler)

	val framing = stack.inRoundedFraming(16.any, Color.white)
	framing.background = Color.gray(0.66)
	val frame = Frame.windowed(framing, "Switch Test", User)
	frame.setToExitOnClose()

	actionLoop.runAsync()
	StackHierarchyManager.startRevalidationLoop()
	frame.startEventGenerators(actorHandler)
	frame.visible = true
}
