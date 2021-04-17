package utopia.reflection.test.swing

import java.nio.file.Paths

import utopia.flow.async.ThreadPool
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.genesis.color.Color
import utopia.genesis.handling.ActorLoop
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.image.Image
import utopia.genesis.shape.shape2D.Size
import utopia.reflection.component.swing.button.{ButtonImageSet, ImageAndTextButton, ImageButton, TextButton}
import utopia.reflection.component.swing.display.ProgressBar
import utopia.reflection.container.stack.StackHierarchyManager
import utopia.reflection.container.stack.StackLayout.Fit
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.User
import utopia.reflection.localization.{Localizer, NoLocalization}
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.stack.StackInsets
import utopia.reflection.text.Font
import utopia.reflection.text.FontStyle.Plain
import utopia.reflection.shape.LengthExtensions._

import scala.concurrent.ExecutionContext

/**
  * Used for visually testing buttons
  * @author Mikko Hilpinen
  * @since 1.8.2019, v1+
  */
object ButtonTest extends App
{
	private def run() =
	{
		implicit val defaultLanguageCode: String = "EN"
		implicit val localizer: Localizer = NoLocalization
		val basicFont = Font("Arial", 12, Plain, 2)

		val image = Image.readFrom(Paths.get("Reflection/test-images/mushrooms.png")).get.withSize(Size(64, 64)).downscaled
		val images = ButtonImageSet.brightening(image)

		val progressPointer = new PointerWithEvents(0.0)

		def action() = progressPointer.value += 0.1

		val color = Color.magenta
		val textInsets = StackInsets.symmetric(8.any, 4.any)
		val borderWidth = 2

		// Creates the buttons
		val imageButton = ImageButton(images) { action() }
		val textButton = TextButton("Text Button", basicFont, color, insets = textInsets, borderWidth = borderWidth) { action() }
		val comboButton = ImageAndTextButton(ButtonImageSet.darkening(image), "Button", basicFont, Color.blue.withLuminosity(0.8), textInsets, borderWidth,
			Alignment.Left) { action() }

		val row = imageButton.rowWith(Vector(textButton, comboButton), margin = 16.any, layout = Fit)

		// Creates progress bar
		val actorHandler = ActorHandler()

		val bar = new ProgressBar(actorHandler, 160.any x 12.downscaling, Color.gray(0.7), Color.magenta,
			progressPointer)
		val content = row.columnWith(Vector(bar), margin = 16.downscaling)

		// Creates the frame and displays it

		val actionLoop = new ActorLoop(actorHandler)
		implicit val context: ExecutionContext = new ThreadPool("Reflection").executionContext

		val framing = content.framed(16.any x 8.any)
		framing.background = Color.white
		val frame = Frame.windowed(framing, "Button Test", User)
		frame.setToExitOnClose()

		actionLoop.registerToStopOnceJVMCloses()
		actionLoop.startAsync()
		StackHierarchyManager.startRevalidationLoop()
		frame.startEventGenerators(actorHandler)
		frame.visible = true
	}

	run()
}
