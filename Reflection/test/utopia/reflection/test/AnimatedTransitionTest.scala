package utopia.reflection.test

import utopia.flow.util.WaitUtils
import utopia.genesis.generic.GenesisDataType
import utopia.genesis.shape.Axis.Y
import utopia.reflection.component.drawing.immutable.BorderDrawer
import utopia.reflection.component.swing.AnimatedVisibility
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.shape.Border
import utopia.reflection.util.SingleFrameSetup
import utopia.flow.util.TimeExtensions._
import utopia.reflection.shape.Alignment.Center
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
	
	val transitionWrapper = baseContext.inContextWithBackground(colorScheme.primary.light).forTextComponents(Center).use { implicit txc =>
		// Creates the component to display
		val originComponent = TextLabel.contextual("I'm Animated :)")
		originComponent.addCustomDrawer(new BorderDrawer(Border.square(2, txc.secondaryColor)))
		originComponent.addResizeListener { e => println(s"Component size changed: $e") }
		
		// Creates the transition components
		AnimatedVisibility.contextual(originComponent, Y)
	}
	
	val content = transitionWrapper.framed(margins.medium.any, colorScheme.primary.light)
	
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
