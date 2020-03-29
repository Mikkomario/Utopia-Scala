package utopia.reflection.test

import java.nio.file.Paths

import utopia.flow.async.ThreadPool
import utopia.genesis.color.Color
import utopia.genesis.image.Image
import utopia.genesis.shape.shape2D.Size
import utopia.reflection.component.swing.label.ImageLabel
import utopia.reflection.container.stack.StackHierarchyManager
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.User
import utopia.reflection.localization.{Localizer, NoLocalization}
import utopia.reflection.shape.StackLength

import scala.concurrent.ExecutionContext

/**
  * Tests image labels within stack
  * @author Mikko Hilpinen
  * @since 7.7.2019, v1+
  */
object ImageLabelTest extends App
{
	private def run() =
	{
		val originalImage = Image.readFrom(Paths.get("test-images/mushrooms.png")).get.withSize(Size(128, 128))
			.downscaled
		val smaller = originalImage.withSize(Size(64, 64))
		
		val big = new ImageLabel(originalImage, allowUpscaling = true)
		val small1 = new ImageLabel(smaller)
		val small2 = new ImageLabel(smaller, alwaysFillArea = false)
		val small3 = new ImageLabel(smaller, alwaysFillArea = false, allowUpscaling = true)
		
		Vector(big, small1, small2, small3).foreach { _.background = Color.yellow }
		
		val smallStack = small1.rowWith(Vector(small2, small3))
		val mainStack = big.columnWith(Vector(smallStack), StackLength.fixed(0))
		
		implicit val language: String = "en"
		implicit val localizer: Localizer = NoLocalization
		val frame = Frame.windowed(mainStack, "Switch Test", User)
		frame.setToExitOnClose()
		
		implicit val context: ExecutionContext = new ThreadPool("ImageLabelTest").executionContext
		StackHierarchyManager.startRevalidationLoop()
		frame.isVisible = true
	}
	
	run()
}
