package utopia.reflection.test

import java.awt.event.KeyEvent

import utopia.flow.async.ThreadPool
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.genesis.color.Color
import utopia.genesis.generic.GenesisDataType
import utopia.genesis.handling.KeyStateListener
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.image.Image
import utopia.reflection.component.swing.SearchFrom
import utopia.reflection.component.swing.button.TextButton
import utopia.reflection.container.stack.StackHierarchyManager
import utopia.reflection.container.swing.Stack
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.localization.{Localizer, NoLocalization}
import utopia.reflection.text.Font
import utopia.reflection.text.FontStyle.Plain
import utopia.reflection.util.{ComponentContext, ComponentContextBuilder, SingleFrameSetup}
import utopia.reflection.shape.LengthExtensions._
import utopia.flow.util.FileExtensions._
import utopia.reflection.shape.StackInsets

import scala.concurrent.ExecutionContext

/**
  * Tests SearchFromField
  * @author Mikko Hilpinen
  * @since 29.2.2020, v1
  */
object SearchFromFieldTest extends App
{
	GenesisDataType.setup()
	
	// Sets up localization context
	implicit val defaultLanguageCode: String = "EN"
	implicit val localizer: Localizer = NoLocalization
	
	// Creates component context
	val actorHandler = ActorHandler()
	val baseCB = ComponentContextBuilder(actorHandler, Font("Arial", 12, Plain, 2), Color.green, Color.yellow, 320,
		insets = StackInsets.symmetric(8.any), stackMargin = 8.downscaling, relatedItemsStackMargin = Some(4.downscaling))
	
	implicit val baseContext: ComponentContext = baseCB.result
	implicit val exc: ExecutionContext = new ThreadPool("Reflection").executionContext
	
	val searchImage = Image.readFrom("test-images/arrow-back-48dp.png")
	val searchPointer = new PointerWithEvents[Option[String]](None)
	val field = SearchFrom.contextualWithTextOnly[String](
		SearchFrom.noResultsLabel("No results for '%s'", searchPointer), "Search for string",
		searchIcon = searchImage.toOption, searchFieldPointer = searchPointer)
	val button = TextButton.contextual("OK", () => println(field.value))
	val content = Stack.buildColumnWithContext() { s =>
		s += field
		s += button
	}.framed(16.any x 16.any, Color.black)
	
	field.content = Vector("The first string", "Another piece of text", "More text", "Lorem ipsum", "Tramboliini",
		"Keijupuisto", "Ääkkösiä", "Pulppura", "Potentiaalinen koneisto")
	
	val frame = Frame.windowed(content, "Search Field Test", Program)
	frame.setToCloseOnEsc()
	
	frame.addKeyStateListener(KeyStateListener.onKeyPressed(KeyEvent.VK_P) { _ => println(StackHierarchyManager.description) })
	
	new SingleFrameSetup(actorHandler, frame).start()
}
