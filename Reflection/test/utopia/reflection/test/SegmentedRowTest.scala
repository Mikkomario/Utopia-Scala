package utopia.reflection.test

import utopia.flow.async.ThreadPool
import utopia.genesis.color.Color
import utopia.genesis.generic.GenesisDataType
import utopia.genesis.handling.ActorLoop
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.shape.Axis._
import utopia.reflection.component.swing.button.TextButton
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.container.stack.segmented.SegmentedGroup
import utopia.reflection.container.stack.StackHierarchyManager
import utopia.reflection.container.swing.{SegmentedRow, Stack}
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.User
import utopia.reflection.localization.{Localizer, NoLocalization}
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.shape.StackInsets
import utopia.reflection.text.Font
import utopia.reflection.text.FontStyle.Plain

import scala.concurrent.ExecutionContext

/**
  * This is a simple test implementation of segmented rows
  * @author Mikko Hilpinen
  * @since 24.4.2019, v1+
  */
object SegmentedRowTest extends App
{
	GenesisDataType.setup()
	
	// Sets up localization context
	implicit val defaultLanguageCode: String = "EN"
	implicit val localizer: Localizer = NoLocalization
	
	// Creates the labels
	val basicFont = Font("Arial", 12, Plain, 2)
	val labels = Vector("Here are some labels", "just for you", "once", "again!").map { s => TextLabel(s, basicFont,
		insets = StackInsets.symmetric(16.any, 0.any)) }
	labels.foreach { _.background = Color.yellow }
	labels.foreach { _.alignCenter() }
	
	// Creates a button too
	val largeFont = basicFont * 1.2
	
	val button1 = TextButton("Yeah!", largeFont, Color.magenta, insets = StackInsets.symmetric(32.any, 8.any),
		borderWidth = 4) { () => labels(1).text += "!" }
	val button2 = TextButton("For Sure!", largeFont, Color.magenta, insets = StackInsets.symmetric(32.any, 8.any),
		borderWidth = 4) { () => labels(3).text += "!" }
	
	// Creates the rows
	val hGroup = new SegmentedGroup(X)
	val row1 = SegmentedRow.partOfGroupWithItems(hGroup, Vector(labels(0), labels(1)), 8.fixed)
	val row2 = SegmentedRow.partOfGroupWithItems(hGroup, Vector(labels(2), labels(3)), 16.fixed, 4.fixed)
	row1.background = Color.cyan
	row2.background = Color.green
	
	// Creates the columns
	val vGroup = new SegmentedGroup(Y)
	val column1 = SegmentedRow.partOfGroupWithItems(vGroup, Vector(row1, row2), 4.any)
	val column2 = SegmentedRow.partOfGroupWithItems(vGroup, Vector(button1, button2), 2.upscaling, 2.upscaling)
	
	// Creates the main stack
	val stack = Stack.rowWithItems(Vector(column1, column2), 16.any)
	
	// val stack = Stack.withItems(Y, Fit, 16.any, 0.fixed, Vector(row1, row2))
	stack.background = Color.black
	
	
	// Creates the frame and displays it
	val actorHandler = ActorHandler()
	val actionLoop = new ActorLoop(actorHandler)
	implicit val context: ExecutionContext = new ThreadPool("Reflection").executionContext
	
	val frame = Frame.windowed(stack, "Segmented Row Test", User)
	frame.setToExitOnClose()
	
	actionLoop.registerToStopOnceJVMCloses()
	actionLoop.startAsync()
	StackHierarchyManager.startRevalidationLoop()
	frame.startEventGenerators(actorHandler)
	frame.isVisible = true
}
