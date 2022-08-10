package utopia.reflection.test.swing

import utopia.paradigm.generic.ParadigmDataType
import utopia.reflection.component.drawing.immutable.BorderDrawer
import utopia.reflection.component.swing.label.{ImageLabel, TextLabel}
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.User
import utopia.paradigm.enumeration.Alignment.Center
import utopia.reflection.shape.Border
import utopia.reflection.test.TestContext
import utopia.reflection.util.SingleFrameSetup
import utopia.reflection.shape.LengthExtensions._

/**
  * Tests component to image drawing
  * @author Mikko Hilpinen
  * @since 17.4.2020, v1.2.1
  */
object ComponentImagesTest extends App
{
	ParadigmDataType.setup()

	// Creates component context

	import TestContext._

	val backgroundColor = colorScheme.primary.light
	val imageLabel = baseContext.inContextWithBackground(backgroundColor).forTextComponents.withTextAlignment(Center)
		.use { implicit txc =>
			// Creates the image to draw
			val originComponent = TextLabel.contextual("This is to be drawn as an image")
			originComponent.addCustomDrawer(BorderDrawer(Border.square(2, colorScheme.secondary)))
			originComponent.setToOptimalSize()
			val image = originComponent.toImage

			println(image.size)

			// Wraps the image to an image label and then draws it
			ImageLabel.contextual(image)(txc)
		}

	val content = imageLabel.framed(margins.medium.any, backgroundColor)

	// Starts test
	val frame = Frame.windowed(content, "Component to Image Test", User)
	frame.setToCloseOnEsc()
	new SingleFrameSetup(actorHandler, frame).start()
}
