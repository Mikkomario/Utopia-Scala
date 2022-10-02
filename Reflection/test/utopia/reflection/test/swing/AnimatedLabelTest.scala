package utopia.reflection.test.swing

import utopia.flow.time.TimeExtensions._
import utopia.flow.parse.file.FileExtensions._
import utopia.paradigm.animation.Animation
import utopia.paradigm.generic.ParadigmDataType
import utopia.genesis.image.Image
import utopia.paradigm.angular.Rotation
import utopia.reflection.component.context.BaseContext
import utopia.reflection.component.swing.label.AnimationLabel
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.test.TestContext
import utopia.reflection.util.SingleFrameSetup
import utopia.reflection.shape.LengthExtensions._

/**
  * Tests animated label
  * @author Mikko Hilpinen
  * @since 16.6.2020, v1.2
  */
object AnimatedLabelTest extends App
{
	ParadigmDataType.setup()

	import TestContext._

	implicit val context: BaseContext = baseContext

	val image = Image.readFrom("Reflection/test-images/mushrooms.png").get.withCenterOrigin
	val rotation = Animation { Rotation.ofCircles(_) }.verySmoothSPathCurved.over(1.seconds)
	val label = AnimationLabel.contextualWithRotatingImage(image, rotation)

	val content = label.framed(margins.medium.any, colorScheme.gray.light)

	// Starts test
	val frame = Frame.windowed(content, "Animated Transition Test", Program)
	frame.setToCloseOnEsc()
	new SingleFrameSetup(actorHandler, frame).start()
}
