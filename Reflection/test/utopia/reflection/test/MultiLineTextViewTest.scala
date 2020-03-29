package utopia.reflection.test

import utopia.flow.async.ThreadPool
import utopia.reflection.shape.LengthExtensions._
import utopia.genesis.color.Color
import utopia.genesis.generic.GenesisDataType
import utopia.genesis.handling.mutable.ActorHandler
import utopia.reflection.component.swing.{JDropDownWrapper, MultiLineTextView, TextField}
import utopia.reflection.container.swing.Stack
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.localization.{LocalString, Localizer, NoLocalization}
import utopia.reflection.shape.{Alignment, StackInsets}
import utopia.reflection.text.Font
import utopia.reflection.text.FontStyle.Plain
import utopia.reflection.util.{ComponentContext, ComponentContextBuilder, SingleFrameSetup}

import scala.concurrent.ExecutionContext

/**
  * Tests text display with multiple lines
  * @author Mikko Hilpinen
  * @since 17.12.2019, v1+
  */
object MultiLineTextViewTest extends App
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
	
	val textView = MultiLineTextView.contextual("Please type in some text and then press enter",
		baseContext.normalWidth, useLowPriorityForScalingSides = true)(
		baseCB.withInsets(StackInsets.zero).result)
	
	val textInput = TextField.contextual(prompt = Some("Type your own text and press enter"))
	textInput.addEnterListener { _.foreach { s => textView.text = (s: LocalString).localizationSkipped } }
	
	val content = Stack.buildColumnWithContext() { mainStack =>
		mainStack += textView
		
		mainStack += Stack.buildRowWithContext(isRelated = true) { bottomRow =>
			bottomRow += textInput
			
			val alignSelect = JDropDownWrapper.contextual("Select Alignment", initialChoices = Alignment.values)
			alignSelect.selectOne(Alignment.Left)
			alignSelect.addValueListener { _.newValue.foreach { a => textView.alignment = a } }
			
			bottomRow += alignSelect
		}
	}.framed(baseContext.insets, Color.white)
	
	implicit val exc: ExecutionContext = new ThreadPool("Reflection").executionContext
	new SingleFrameSetup(actorHandler, Frame.windowed(content, "Multi Line Text View Test")).start()
	
	println(s"Line splits at ${textView.lineSplitThreshold}. Input width = ${textInput.width}")
}
