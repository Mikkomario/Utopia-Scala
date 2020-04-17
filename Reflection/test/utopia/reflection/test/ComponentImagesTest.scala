package utopia.reflection.test

import utopia.flow.async.ThreadPool
import utopia.genesis.color.Color
import utopia.genesis.generic.GenesisDataType
import utopia.genesis.handling.mutable.ActorHandler
import utopia.reflection.component.drawing.immutable.BorderDrawer
import utopia.reflection.component.swing.label.{ImageLabel, TextLabel}
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.User
import utopia.reflection.localization.{Localizer, NoLocalization}
import utopia.reflection.shape.{Border, StackInsets}
import utopia.reflection.text.Font
import utopia.reflection.text.FontStyle.Plain
import utopia.reflection.util.{ComponentContext, ComponentContextBuilder, SingleFrameSetup}
import utopia.reflection.shape.LengthExtensions._

import scala.concurrent.ExecutionContext

/**
  * Tests component to image drawing
  * @author Mikko Hilpinen
  * @since 17.4.2020, v1.2.1
  */
object ComponentImagesTest extends App
{
	GenesisDataType.setup()
	
	// Sets up localization context
	implicit val defaultLanguageCode: String = "EN"
	implicit val localizer: Localizer = NoLocalization
	
	// Creates component context
	implicit val exc: ExecutionContext = new ThreadPool("Reflection").executionContext
	val actorHandler = ActorHandler()
	val baseCB = ComponentContextBuilder(actorHandler, Font("Arial", 12, Plain, 2), Color.green, Color.yellow, 320,
		insets = StackInsets.symmetric(8.any), stackMargin = 8.downscaling, relatedItemsStackMargin = Some(4.downscaling),
		borderWidth = Some(1))
	
	implicit val baseContext: ComponentContext = baseCB.result
	
	// Creates the image to draw
	val originComponent = TextLabel.contextual("This is to be drawn as image")
	originComponent.addCustomDrawer(new BorderDrawer(Border.square(2, Color.blue)))
	originComponent.setToOptimalSize()
	val image = originComponent.toImage
	
	println(image.size)
	
	// Wraps the image to an image label and then draws it
	val imageLabel = ImageLabel.contextual(image)
	val content = imageLabel.framed(16.downscaling.square, Color.white)
	
	// Starts test
	val frame = Frame.windowed(content, "Component to Image Test", User)
	frame.setToCloseOnEsc()
	new SingleFrameSetup(actorHandler, frame).start()
}
