package utopia.reflection.test.swing

import utopia.firmament.drawing.immutable.BoxScrollBarDrawer
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.localization.DisplayFunction
import utopia.firmament.model.enumeration.StackLayout
import utopia.firmament.model.enumeration.WindowResizePolicy.User
import utopia.firmament.model.stack.LengthExtensions._
import utopia.firmament.model.stack.StackInsets
import utopia.firmament.model.stack.modifier.MaxOptimalLengthModifier
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.genesis.graphics.{DrawLevel, DrawSettings, StrokeSettings}
import utopia.genesis.handling.action.{ActionLoop, ActorHandler}
import utopia.genesis.handling.event.keyboard.Key.RightArrow
import utopia.genesis.handling.event.keyboard.{KeyStateListener, KeyboardEvents}
import utopia.genesis.handling.event.mouse.MouseButtonStateListener
import utopia.genesis.text.Font
import utopia.genesis.text.FontStyle.Plain
import utopia.paradigm.angular.Rotation
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Alignment.Center
import utopia.paradigm.enumeration.Axis._
import utopia.paradigm.generic.ParadigmDataType
import utopia.reflection.component.swing.label.ItemLabel
import utopia.reflection.container.stack.StackHierarchyManager
import utopia.reflection.container.swing.layout.multi.{AnimatedStack, Stack}
import utopia.reflection.container.swing.layout.wrapper.scrolling.ScrollView
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.controller.data.ContainerSelectionManager
import utopia.reflection.test.TestContext._

/**
  * Tests scrolling in a static scroll view
  * @author Mikko Hilpinen
  * @since 30.8.2024, v2.2.1
  */
object ScrollingTest extends App
{
	ParadigmDataType.setup()
	
	KeyboardEvents.specifyExecutionContext(exc)
	private implicit val selectionDs: DrawSettings = DrawSettings(Color.black.withAlpha(0.33))(
		StrokeSettings(Color.black.withAlpha(0.8), 2))
	
	// Label creation function
	val basicFont = Font("Arial", 12, Plain, 2)
	val displayFunction = DisplayFunction.interpolating("Label number %i")
	def makeLabel(number: Int) = {
		val label = new ItemLabel(EventfulPointer(number), displayFunction, basicFont,
			initialInsets = StackInsets.symmetric(16.any, 4.fixed))
		label.background = Color.yellow
		label.alignment = Center
		
		label
	}
	
	val actorHandler = ActorHandler()
	
	// Creates the main stack
	val stack = Stack.column[ItemLabel[Int]](margin = 8.fixed, layout = StackLayout.Leading, cap = 4.fixed)
	stack.background = Color.yellow.minusHue(Rotation.clockwise.degrees(33)).darkenedBy(1.2)
	
	// Adds content management
	val selectionDrawer = CustomDrawer(DrawLevel.Foreground) { (d, b) => d.draw(b) }
	
	val contentManager = new ContainerSelectionManager[Int, ItemLabel[Int]](stack, selectionDrawer)(makeLabel)
	// contentManager.valuePointer.addContinuousListener { i => println(s"Selected ${ i.newValue }") }
	contentManager.enableKeyHandling(actorHandler)
	contentManager.enableMouseHandling(false)
	contentManager.content = 1 to 50
	
	KeyboardEvents += KeyStateListener.pressed(RightArrow) { _ => contentManager.updateSingle(2) }
	
	// Creates the scroll view
	val barDrawer = BoxScrollBarDrawer(Color.black.withAlpha(0.55), Color.red)
	val scrollView = new ScrollView(stack, Y, actorHandler, barDrawer)
	scrollView.addHeightConstraint(l => l.copy(newMin = 128))
	scrollView.addHeightConstraint(MaxOptimalLengthModifier(480))
	
	contentManager.selectedDisplayPointer.addContinuousListener { _.newValue.headOption.foreach { row =>
		val rowBounds = row.bounds.enlarged(Y(row.height * 4))
		scrollView.ensureAreaIsVisible(rowBounds)
	} }
	
	// Creates the frame and displays it
	val actionLoop = new ActionLoop(actorHandler)
	
	val frame = Frame.windowed(scrollView, "Scroll View Test", User)
	frame.setToExitOnClose()
	
	actionLoop.runAsync()
	StackHierarchyManager.startRevalidationLoop()
	frame.startEventGenerators(actorHandler)
	frame.visible = true
}


