package utopia.reflection.test

import utopia.reflection.shape.LengthExtensions._
import utopia.flow.util.FileExtensions._
import utopia.flow.async.ThreadPool
import utopia.genesis.color.Color
import utopia.genesis.generic.GenesisDataType
import utopia.genesis.handling.MouseButtonStateListener
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.image.Image
import utopia.genesis.shape.shape2D.Direction2D
import utopia.reflection.component.swing.DropDown
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.container.swing.{CollectionView, Stack}
import utopia.reflection.container.swing.Stack.AwtStackable
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.localization.{Localizer, NoLocalization}
import utopia.reflection.shape.StackInsets
import utopia.reflection.text.Font
import utopia.reflection.text.FontStyle.Plain
import utopia.reflection.util.{ComponentContext, ComponentContextBuilder, SingleFrameSetup}

import scala.collection.immutable.HashMap
import scala.concurrent.ExecutionContext

/**
  * Rewritten version of drop down test for the new drop down implementation
  * @author Mikko Hilpinen
  * @since 15.3.2020, v1
  */
object DropDownTest2 extends App
{
	GenesisDataType.setup()
	
	// Sets up localization context
	implicit val defaultLanguageCode: String = "EN"
	implicit val localizer: Localizer = NoLocalization
	
	// Creates component context
	implicit val exc: ExecutionContext = new ThreadPool("Reflection").executionContext
	val actorHandler = ActorHandler()
	val baseCB = ComponentContextBuilder(actorHandler, Font("Arial", 12, Plain, 2), Color.green, Color.yellow, 320,
		insets = StackInsets.symmetric(8.any), stackMargin = 8.downscaling, relatedItemsStackMargin = Some(4.downscaling), borderWidth = Some(1))
	
	implicit val baseContext: ComponentContext = baseCB.result
	
	// Creates view content
	val data = HashMap("Fighter" -> Vector("Aragorn", "Gimli", "Boromir"), "Archer" -> Vector("Legolas"),
		"Wizard" -> Vector("Gandalf", "Radagast"))
	
	val ddIcon = Image.readOrEmpty("test-images/arrow-back-48dp.png")
	val categorySelect = DropDown.contextualWithTextOnly[String](
		TextLabel.contextual("No content available", isHint = true), ddIcon, "Select Class")
	val characterSelect = DropDown.contextualWithTextOnly[String](
		TextLabel.contextual("No content available", isHint = true), ddIcon, "Select Character")
	
	// Adds item listeners
	categorySelect.addValueListener { c => characterSelect.content = c.newValue.flatMap(data.get) getOrElse Vector() }
	characterSelect.addValueListener { c => println(c.newValue.map { _ + " is ready for adventure!" } getOrElse "No character selected") }
	
	// Adds initial content
	categorySelect.content = data.keySet.toVector.sorted
	
	val stack = Stack.buildColumnWithContext() { stack =>
		stack += categorySelect
		stack += characterSelect
	}
	val content = stack.framed(16.any x 16.any, Color.white)
	
	// Starts test
	val frame = Frame.windowed(content, "Collection View Test", Program)
	frame.setToCloseOnEsc()
	new SingleFrameSetup(actorHandler, frame).start()
}
