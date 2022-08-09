package utopia.reflection.test.swing

import java.nio.file.Paths

import utopia.paradigm.color.Color
import utopia.genesis.image.Image
import utopia.paradigm.shape.shape2d.Size
import utopia.reflection.component.swing.label.ImageLabel
import utopia.reflection.container.stack.StackHierarchyManager
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.User
import utopia.reflection.shape.stack.StackLength
import utopia.reflection.test.TestContext._

/**
  * Tests image labels within stack
  * @author Mikko Hilpinen
  * @since 7.7.2019, v1+
  */
object ImageLabelTest extends App
{
	private def run() =
	{
		val originalImage = Image.readFrom(Paths.get("Reflection/test-images/mushrooms.png")).get.withSize(Size(128, 128))
			.downscaled
		val smaller = originalImage.withSize(Size(64, 64))

		val big = new ImageLabel(originalImage, allowUpscaling = true)
		val small1 = new ImageLabel(smaller)
		val small2 = new ImageLabel(smaller, alwaysFillArea = false)
		val small3 = new ImageLabel(smaller, alwaysFillArea = false, allowUpscaling = true)

		Vector(big, small1, small2, small3).foreach { _.background = Color.yellow }

		val smallStack = small1.rowWith(Vector(small2, small3))
		val mainStack = big.columnWith(Vector(smallStack), StackLength.fixed(0))

		val frame = Frame.windowed(mainStack, "Switch Test", User)
		frame.setToExitOnClose()

		StackHierarchyManager.startRevalidationLoop()
		frame.visible = true
	}

	run()
}
