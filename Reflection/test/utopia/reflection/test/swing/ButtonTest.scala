package utopia.reflection.test.swing

import utopia.firmament.image.ButtonImageSet
import utopia.firmament.localization.{Localizer, NoLocalization}
import utopia.firmament.model.enumeration.StackLayout.Fit
import utopia.firmament.model.enumeration.WindowResizePolicy.User
import utopia.firmament.model.stack.LengthExtensions._
import utopia.firmament.model.stack.StackInsets
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.genesis.handling.action.{ActionLoop, ActorHandler2}
import utopia.genesis.image.Image
import utopia.genesis.text.Font
import utopia.genesis.text.FontStyle.Plain
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reflection.component.swing.button.{ImageAndTextButton, ImageButton, TextButton}
import utopia.reflection.component.swing.display.ProgressBar
import utopia.reflection.container.stack.StackHierarchyManager
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.test.TestContext._

import java.nio.file.Paths

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
		val images = ButtonImageSet(image).highlighting

		val progressPointer = new EventfulPointer(0.0)

		def action() = progressPointer.value += 0.1

		val color = Color.magenta
		val textInsets = StackInsets.symmetric(8.any, 4.any)
		val borderWidth = 2

		// Creates the buttons
		val imageButton = ImageButton(images) { action() }
		val textButton = TextButton("Text Button", basicFont, color, insets = textInsets, borderWidth = borderWidth) { action() }
		val comboButton = ImageAndTextButton(ButtonImageSet(image).highlighting, "Button", basicFont,
			Color.blue.withLuminosity(0.8), textInsets, borderWidth, Alignment.Left) { action() }

		val row = imageButton.rowWith(Vector(textButton, comboButton), margin = 16.any, layout = Fit)

		// Creates progress bar
		val actorHandler = ActorHandler2()

		val bar = new ProgressBar(actorHandler, 160.any x 12.downscaling, Color.gray(0.7), Color.magenta,
			progressPointer)
		val content = row.columnWith(Vector(bar), margin = 16.downscaling)
		content.background = Color.cyan

		// Creates the frame and displays it
		val actionLoop = new ActionLoop(actorHandler)
		val framing = content.framed(16.any x 8.any)
		framing.background = Color.white
		val frame = Frame.windowed(framing, "Button Test", User)
		frame.setToExitOnClose()

		actionLoop.runAsync()
		StackHierarchyManager.startRevalidationLoop()
		frame.startEventGenerators(actorHandler)
		frame.visible = true
		
		println(s"Content bounds are: ${ content.bounds }")
		content.children.foreach { c =>
			println(s"\t- Child bounds: ${ c.bounds } (${ c.isAttachedToMainHierarchy })")
		}
	}

	run()
}
