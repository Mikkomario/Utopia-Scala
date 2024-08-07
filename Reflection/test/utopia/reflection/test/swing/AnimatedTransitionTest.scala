package utopia.reflection.test.swing

import utopia.firmament.model.Border
import utopia.firmament.model.stack.LengthExtensions._
import utopia.flow.async.process
import utopia.flow.time.TimeExtensions._
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.enumeration.Axis.Y
import utopia.paradigm.generic.ParadigmDataType
import utopia.firmament.drawing.immutable.BorderDrawer
import utopia.reflection.component.swing.animation.AnimatedVisibility
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.container.swing.window.Frame
import utopia.firmament.model.enumeration.WindowResizePolicy.Program
import utopia.reflection.test.TestContext
import utopia.reflection.util.SingleFrameSetup

/**
  * Tests AnimatedTransition class
  * @author Mikko Hilpinen
  * @since 17.4.2020, v1.2.1
  */
object AnimatedTransitionTest extends App
{
	ParadigmDataType.setup()
	
	// Imports contexts
	
	import TestContext._
	
	val transitionWrapper = baseContext.against(colorScheme.primary.light).forTextComponents
		.withTextAlignment(Alignment.Center)
		.use { implicit txc =>
			// Creates the component to display
			val originComponent = TextLabel.contextual("I'm Animated :)")
			originComponent.addCustomDrawer(BorderDrawer(Border(2, txc.color.secondary)))
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
	process.Delay(0.5.seconds) {
		transitionWrapper.isShown = true
		// Hides it again after 5 seconds
		process.Delay(5.seconds) {
			// Closes frame once visibility has been changed
			(transitionWrapper.isShown = false).foreach { _ => frame.close() }
		}
	}
}
