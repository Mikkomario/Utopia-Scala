package utopia.reflection.test.swing

import java.awt.event.KeyEvent
import utopia.flow.async.{Loop, ThreadPool}
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.time.TimeExtensions._
import utopia.flow.time.WaitTarget
import utopia.genesis.color.Color
import utopia.genesis.generic.GenesisDataType
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.handling.{ActorLoop, KeyStateListener, MouseButtonStateListener}
import utopia.genesis.shape.Axis._
import utopia.genesis.shape.shape1D.Rotation
import utopia.genesis.view.GlobalMouseEventHandler
import utopia.reflection.component.drawing.immutable.BoxScrollBarDrawer
import utopia.reflection.component.drawing.template.{CustomDrawer, DrawLevel}
import utopia.reflection.component.swing.label.ItemLabel
import utopia.reflection.component.template.display.Refreshable
import utopia.reflection.container.stack.StackHierarchyManager
import utopia.reflection.container.swing.layout.multi.AnimatedStack
import utopia.reflection.container.swing.layout.wrapper.scrolling.ScrollView
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.User
import utopia.reflection.controller.data.ContainerSelectionManager
import utopia.reflection.localization.{DisplayFunction, Localizer, NoLocalization}
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.shape.stack.{StackInsets, StackLengthLimit}
import utopia.reflection.text.Font
import utopia.reflection.text.FontStyle.Plain

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

/**
  * This is a simple test implementation of scroll view
  * @author Mikko Hilpinen
  * @since 24.4.2019, v1+
  */
object ScrollViewTest extends App
{
	GenesisDataType.setup()
	implicit val context: ExecutionContext = new ThreadPool("Reflection").executionContext
	
	// Sets up localization context
	implicit val defaultLanguageCode: String = "EN"
	implicit val localizer: Localizer = NoLocalization
	
	// Label creation function
	val basicFont = Font("Arial", 12, Plain, 2)
	val displayFunction = DisplayFunction.interpolating("Label number %i")
	def makeLabel(number: Int) =
	{
		val label = new ItemLabel(new PointerWithEvents(number), displayFunction, basicFont,
			initialInsets = StackInsets.symmetric(16.any, 4.fixed))
		label.background = Color.yellow
		label.alignCenter()
		
		label
	}
	
	val actorHandler = ActorHandler()
	
	// Creates the main stack
	val stack = new AnimatedStack[ItemLabel[Int]](actorHandler, Y, 8.fixed, 4.fixed) // Stack.column[ItemLabel[Int]](8.fixed, 4.fixed)
	stack.background = Color.yellow.minusHue(Rotation.ofDegrees(33)).darkened(1.2)
	
	// Adds content management
	val selectionDrawer = CustomDrawer(DrawLevel.Foreground) { (d, b) =>
		d.withColor(Color.black.withAlpha(0.33), Color.black.withAlpha(0.8)).withStroke(2).draw(b)
	}
	
	val contentManager = new ContainerSelectionManager[Int, ItemLabel[Int]](stack, selectionDrawer)(makeLabel)
	contentManager.addValueListener(i => println("Selected " + i.newValue))
	contentManager.enableKeyHandling(actorHandler)
	contentManager.enableMouseHandling(false)
	private val contentUpdateLoop = new ContentUpdateLoop(contentManager)
	
	stack.addKeyStateListener(KeyStateListener.onKeyPressed(KeyEvent.VK_RIGHT) { _ => contentManager.updateSingle(2) })
	
	// Creates the scroll view
	val barDrawer = BoxScrollBarDrawer(Color.black.withAlpha(0.55), Color.red)
	val scrollView = new ScrollView(stack, Y, actorHandler, barDrawer,
		lengthLimit = StackLengthLimit(min = 128, maxOptimal =  Some(480)))
	
	// Creates the frame and displays it
	val actionLoop = new ActorLoop(actorHandler)
	
	val frame = Frame.windowed(scrollView, "Scroll View Test", User)
	frame.setToExitOnClose()
	frame.addMouseButtonListener(MouseButtonStateListener() { event => println(event); None })
	
	actionLoop.registerToStopOnceJVMCloses()
	actionLoop.startAsync()
	StackHierarchyManager.startRevalidationLoop()
	contentUpdateLoop.registerToStopOnceJVMCloses()
	contentUpdateLoop.startAsync()
	frame.startEventGenerators(actorHandler)
	frame.visible = true
	
	println("Global mouse handling:")
	println(GlobalMouseEventHandler.debugString)
	
	println("Frame mouse handling:")
	println(frame.mouseButtonHandler.debugString)
}

private class ContentUpdateLoop(val target: Refreshable[Vector[Int]]) extends Loop
{
	// ATTRIBUTES	---------------------
	
	private val maxLength = 10
	private var nextWait: Duration = 3.seconds
	private var increasing = true
	
	
	// IMPLEMENTED	--------------------
	
	override def runOnce() =
	{
		val turnAround =
		{
			if (increasing)
			{
				if (target.content.size < maxLength)
				{
					target.content :+= target.content.size
					false
				}
				else
					true
			}
			else
			{
				if (target.content.nonEmpty)
				{
					target.content = target.content.dropRight(1)
					false
				}
				else
					true
			}
		}
		
		if (turnAround)
		{
			increasing = !increasing
			nextWait = 3.seconds
		}
		else
			nextWait = nextWait * 0.9
	}
	
	override def nextWaitTarget = WaitTarget.WaitDuration(nextWait)
}
