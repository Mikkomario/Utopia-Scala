package utopia.reflection.test

import java.awt.event.KeyEvent
import java.util.concurrent.TimeUnit

import utopia.flow.async.ThreadPool
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.genesis.color.Color
import utopia.genesis.generic.GenesisDataType
import utopia.genesis.handling.{ActorLoop, KeyStateListener}
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.shape.LinearAcceleration
import utopia.genesis.shape.shape2D.Size
import utopia.reflection.component.swing.label.ItemLabel
import utopia.reflection.container.stack.{BoxScrollBarDrawer, StackHierarchyManager}
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.User
import utopia.reflection.container.swing.{ScrollArea, Stack}
import utopia.reflection.localization.{DisplayFunction, Localizer, NoLocalization}
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.shape.{StackInsets, StackLengthLimit}
import utopia.reflection.text.Font
import utopia.reflection.text.FontStyle.Plain

import scala.concurrent.ExecutionContext

/**
  * This is a simple test implementation of scroll Area
  * @author Mikko Hilpinen
  * @since 18.5.2019, v1+
  */
object ScrollAreaTest extends App
{
	GenesisDataType.setup()
	
	// Sets up localization context
	implicit val defaultLanguageCode: String = "EN"
	implicit val localizer: Localizer = NoLocalization
	
	// Creates the labels
	val basicFont = Font("Arial", 12, Plain, 2)
	val labels = (1 to 10).toVector.map { row => (1 to 50).toVector.map { i => new ItemLabel(new PointerWithEvents(row * i),
		DisplayFunction.interpolating("Label number %i"), basicFont, initialInsets = StackInsets.symmetric(16.any, 4.fixed)) }}
	val allLabels = labels.flatten
	allLabels.foreach { _.background = Color.yellow }
	allLabels.foreach { _.alignCenter() }
	
	// Creates the columns
	val columns = labels.map { l => Stack.columnWithItems(l, 8.fixed, 4.fixed) }
	
	// Creates the main stack
	val stack = Stack.rowWithItems(columns, 16.fixed, 4.fixed)
	stack.background = Color.yellow.minusHue(33).darkened(1.2)
	
	val actorHandler = ActorHandler()
	
	// Creates the scroll area
	val barDrawer = BoxScrollBarDrawer.roundedBarOnly(Color.black.withAlpha(0.55))
	val scrollArea = new ScrollArea(stack, actorHandler, 64, barDrawer, 16,
		true, friction = LinearAcceleration(2000)(TimeUnit.SECONDS),
		lengthLimits = StackLengthLimit.sizeLimit(maxOptimal =  Some(Size.square(480))))
	
	// Creates the frame and displays it
	val actionLoop = new ActorLoop(actorHandler)
	implicit val context: ExecutionContext = new ThreadPool("Reflection").executionContext
	
	val frame = Frame.windowed(scrollArea, "Scroll View Test", User)
	frame.setToExitOnClose()
	
	// Adds additional action on END key
	scrollArea.addKeyStateListener(KeyStateListener.onKeyPressed(KeyEvent.VK_END) { _ => scrollArea.scrollToBottom() })
	
	actionLoop.registerToStopOnceJVMCloses()
	actionLoop.startAsync()
	StackHierarchyManager.startRevalidationLoop()
	frame.startEventGenerators(actorHandler)
	frame.isVisible = true
}
