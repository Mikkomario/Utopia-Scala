package utopia.reflection.test.swing

import utopia.firmament.context.base.StaticBaseContext
import utopia.firmament.model.enumeration.WindowResizePolicy.Program
import utopia.firmament.model.stack.LengthExtensions._
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.time.TimeExtensions._
import utopia.genesis.image.Image
import utopia.paradigm.angular.Rotation
import utopia.paradigm.animation.Animation
import utopia.paradigm.generic.ParadigmDataType
import utopia.reflection.component.swing.label.AnimationLabel
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.test.TestContext
import utopia.reflection.util.SingleFrameSetup

/**
  * Tests animated label
  * @author Mikko Hilpinen
  * @since 16.6.2020, v1.2
  */
object AnimatedLabelTest extends App
{
	ParadigmDataType.setup()

	import TestContext._

	implicit val context: StaticBaseContext = baseContext

	val image = Image.readFrom("Reflection/test-images/mushrooms.png").get.withCenterOrigin
	val rotation = Animation(Rotation.clockwise.circles).verySmoothSPathCurved.over(2.seconds)
	val label = AnimationLabel.contextualWithRotatingImage(image, rotation)

	val content = label.framed(margins.medium.any, colorScheme.gray.light)

	// Starts test
	val frame = Frame.windowed(content, "Animated Label Test", Program)
	frame.setToCloseOnEsc()
	new SingleFrameSetup(actorHandler, frame).start()
}
