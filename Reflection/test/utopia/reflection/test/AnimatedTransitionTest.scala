package utopia.reflection.test

import utopia.flow.async.ThreadPool
import utopia.flow.util.WaitUtils
import utopia.genesis.color.Color
import utopia.genesis.generic.GenesisDataType
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.shape.Axis.Y
import utopia.genesis.shape.Direction1D.Negative
import utopia.reflection.component.drawing.immutable.BorderDrawer
import utopia.reflection.component.swing.AnimatedTransition
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.container.swing.Stack.AwtStackable
import utopia.reflection.container.swing.SwitchPanel
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.localization.{Localizer, NoLocalization}
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.shape.{Border, StackInsets}
import utopia.reflection.text.Font
import utopia.reflection.text.FontStyle.Plain
import utopia.reflection.util.{ComponentContext, ComponentContextBuilder, SingleFrameSetup}
import utopia.flow.util.TimeExtensions._

import scala.concurrent.ExecutionContext

/**
  * Tests AnimatedTransition class
  * @author Mikko Hilpinen
  * @since 17.4.2020, v1.2.1
  */
object AnimatedTransitionTest extends App
{
	GenesisDataType.setup()
	
	// Sets up localization context
	implicit val defaultLanguageCode: String = "EN"
	implicit val localizer: Localizer = NoLocalization
	
	// Creates component context
	implicit val exc: ExecutionContext = new ThreadPool("Reflection").executionContext
	val actorHandler = ActorHandler()
	val baseCB = ComponentContextBuilder(actorHandler, Font("Arial", 12, Plain, 2), Color.green, Color.yellow, 320,
		insets = StackInsets.symmetric(8.any), stackMargin = 8.downscaling, relatedItemsStackMargin = Some(4.downscaling),
		borderWidth = Some(1))
	
	implicit val baseContext: ComponentContext = baseCB.result
	
	// Creates the component to display
	val originComponent = TextLabel.contextual("This is to be drawn as image")
	originComponent.addCustomDrawer(new BorderDrawer(Border.square(2, Color.blue)))
	originComponent.addResizeListener { e => println(s"Component size changed: $e") }
	
	// Creates the transition components
	val appearance = new AnimatedTransition(originComponent, Y)
	actorHandler += appearance
	
	val disappearance = new AnimatedTransition(originComponent, Y, Negative)
	actorHandler += disappearance
	
	// Wraps the component and transitions to a managed switch panel
	val mainPanel = SwitchPanel[AwtStackable](appearance)
	val content = mainPanel.framed(16.downscaling.square, Color.white)
	
	// Starts test
	val frame = Frame.windowed(content, "Animated Transition Test", Program)
	frame.setToCloseOnEsc()
	new SingleFrameSetup(actorHandler, frame).start()
	
	WaitUtils.delayed(0.5.seconds) {
		appearance.start().foreach { _ =>
			println("Component fully appeared")
			mainPanel.set(originComponent)
			WaitUtils.delayed(3.seconds) {
				println("Component starting to disappear")
				mainPanel.set(disappearance)
				disappearance.start().foreach { _ => frame.close() }
			}
		}
	}
}
