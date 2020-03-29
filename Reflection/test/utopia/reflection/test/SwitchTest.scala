package utopia.reflection.test

import utopia.flow.async.ThreadPool
import utopia.genesis.color.Color
import utopia.genesis.generic.GenesisDataType
import utopia.genesis.handling.ActorLoop
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.shape.Axis._
import utopia.reflection.component.swing.Switch
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.container.stack.StackHierarchyManager
import utopia.reflection.container.stack.segmented.SegmentedGroup
import utopia.reflection.container.swing.{SegmentedRow, Stack}
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.User
import utopia.reflection.localization.{Localizer, NoLocalization}
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.shape.StackInsets
import utopia.reflection.text.FontStyle.Plain
import utopia.reflection.text.Font

import scala.concurrent.ExecutionContext

/**
  * This is a simple test implementation of switches
  * @author Mikko Hilpinen
  * @since 24.4.2019, v1+
  */
object SwitchTest extends App
{
	GenesisDataType.setup()
	
	// Sets up localization context
	implicit val defaultLanguageCode: String = "EN"
	implicit val localizer: Localizer = NoLocalization
	
	// Creates the hint labels
	val basicFont = Font("Arial", 12, Plain, 2)
	val labels = Vector("Enabled", "Disabled (OFF)", "Disabled (ON)").map { s => TextLabel(s, basicFont,
		insets = StackInsets.symmetric(8.any, 0.any)) }
	labels.foreach { l => l.alignRight() }
	
	// Creates the switches
	val actorHandler = ActorHandler()
	
	val enabledSwitch = new Switch(32.upTo(64), Color.red, actorHandler)
	val disabledSwitch = new Switch(32.upTo(64), Color.red, actorHandler)
	disabledSwitch.isEnabled = false
	val disabledSwitch2 = new Switch(32.upTo(64), Color.red, actorHandler)
	disabledSwitch2.isOn = true
	disabledSwitch2.isEnabled = false
	
	// Creates the stacks
	val group = new SegmentedGroup(X)
	def combine(label: TextLabel, field: Switch) = SegmentedRow.partOfGroupWithItems(group,
		Vector(label, field), 8.downscaling, 8.downscaling)
	val enabledStack = combine(labels(0), enabledSwitch)
	val disabledStack = combine(labels(1), disabledSwitch)
	val disabledStack2 = combine(labels(2), disabledSwitch2)
	
	val stack = Stack.columnWithItems(Vector(enabledStack, disabledStack, disabledStack2), 8.downscaling)
	
	// Creates the frame and displays it
	val actionLoop = new ActorLoop(actorHandler)
	implicit val context: ExecutionContext = new ThreadPool("Reflection").executionContext
	
	val framing = stack.framed(16.any x 8.any)
	framing.background = Color.white
	val frame = Frame.windowed(framing, "Switch Test", User)
	frame.setToExitOnClose()
	
	actionLoop.registerToStopOnceJVMCloses()
	actionLoop.startAsync()
	StackHierarchyManager.startRevalidationLoop()
	frame.startEventGenerators(actorHandler)
	frame.isVisible = true
}
