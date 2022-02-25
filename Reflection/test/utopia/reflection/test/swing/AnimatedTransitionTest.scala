package utopia.reflection.test.swing

import utopia.flow.async.Delay
import utopia.flow.time.TimeExtensions._
import utopia.genesis.generic.GenesisDataType
import utopia.genesis.shape.Axis.Y
import utopia.reflection.component.drawing.immutable.BorderDrawer
import utopia.reflection.component.swing.animation.AnimatedVisibility
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.shape.{Alignment, Border}
import utopia.reflection.test.TestContext
import utopia.reflection.util.SingleFrameSetup
import utopia.reflection.shape.LengthExtensions._

/**
  * Tests AnimatedTransition class
  * @author Mikko Hilpinen
  * @since 17.4.2020, v1.2.1
  */
object AnimatedTransitionTest extends App
{
	GenesisDataType.setup()
	
	// Imports contexts
	
	import TestContext._
	
	val transitionWrapper = baseContext.inContextWithBackground(colorScheme.primary.light).forTextComponents
		.withTextAlignment(Alignment.Center)
		.use { implicit txc =>
			// Creates the component to display
			val originComponent = TextLabel.contextual("I'm Animated :)")
			originComponent.addCustomDrawer(BorderDrawer(Border.square(2, txc.secondaryColor)))
			originComponent.addResizeListener { e => println(s"Component size changed: $e") }
			
			// Creates the transition components
			AnimatedVisibility.contextual(originComponent, Some(Y))
		}
	
	val content = transitionWrapper.framed(margins.medium.any, colorScheme.primary.light)
	
	// Starts test
	val frame = Frame.windowed(content, "Animated Transition Test", Program)
	frame.setToCloseOnEsc()
	new SingleFrameSetup(actorHandler, frame).start()
	
	// Displays the component after 0.5 seconds
	Delay(0.5.seconds) {
		transitionWrapper.isShown = true
		// Hides it again after 5 seconds
		Delay(5.seconds) {
			// Closes frame once visibility has been changed
			(transitionWrapper.isShown = false).foreach { _ => frame.close() }
		}
	}
}
