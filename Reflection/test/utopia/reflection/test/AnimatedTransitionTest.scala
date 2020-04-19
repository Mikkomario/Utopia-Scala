package utopia.reflection.test

import utopia.flow.async.ThreadPool
import utopia.flow.util.WaitUtils
import utopia.genesis.color.Color
import utopia.genesis.generic.GenesisDataType
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.shape.Axis.Y
import utopia.reflection.component.drawing.immutable.BorderDrawer
import utopia.reflection.component.swing.AnimatedVisibility
import utopia.reflection.component.swing.label.TextLabel
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
	val originComponent = TextLabel.contextual("I'm Animated :)")
	originComponent.addCustomDrawer(new BorderDrawer(Border.square(2, Color.blue)))
	originComponent.addResizeListener { e => println(s"Component size changed: $e") }
	
	// Creates the transition components
	val transitionWrapper = new AnimatedVisibility(originComponent, actorHandler, Y)
	val content = transitionWrapper.framed(16.downscaling.square, Color.white)
	
	// Starts test
	val frame = Frame.windowed(content, "Animated Transition Test", Program)
	frame.setToCloseOnEsc()
	new SingleFrameSetup(actorHandler, frame).start()
	
	// Displays the component after 0.5 seconds
	WaitUtils.delayed(0.5.seconds) {
		transitionWrapper.isShown = true
		// Hides it again after 5 seconds
		WaitUtils.delayed(5.seconds) {
			// Closes frame once visibility has been changed
			(transitionWrapper.isShown = false).foreach { _ => frame.close() }
		}
	}
}
