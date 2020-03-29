package utopia.reflection.test

import utopia.flow.generic.ValueConversions._
import utopia.flow.async.ThreadPool
import utopia.genesis.color.Color
import utopia.genesis.generic.GenesisDataType
import utopia.genesis.handling.ActorLoop
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.shape.shape2D.Point
import utopia.reflection.component.swing.button.TextButton
import utopia.reflection.component.swing.{TabSelection, TextField}
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.container.stack.StackHierarchyManager
import utopia.reflection.container.swing.window.{Frame, Popup}
import utopia.reflection.container.swing.window.WindowResizePolicy.User
import utopia.reflection.container.swing.Stack
import utopia.reflection.localization.{Localizer, NoLocalization}
import utopia.reflection.shape.{Alignment, StackInsets}
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.text.Font
import utopia.reflection.text.FontStyle.Plain
import utopia.reflection.shape.Alignment.BottomLeft
import utopia.reflection.util.ComponentContextBuilder

import scala.concurrent.ExecutionContext

/**
  * This is a simple test implementation of text fields with content filtering
  * @author Mikko Hilpinen
  * @since 24.4.2019, v1+
  */
object TextFieldTest extends App
{
	GenesisDataType.setup()
	
	// Sets up localization context
	implicit val defaultLanguageCode: String = "EN"
	implicit val localizer: Localizer = NoLocalization
	
	// Creates component context
	val actorHandler = ActorHandler()
	val base = ComponentContextBuilder(actorHandler, Font("Arial", 12, Plain, 2), Color.green, Color.yellow, 320,
		insets = StackInsets.symmetric(8.any), stackMargin = 8.downscaling, relatedItemsStackMargin = Some(4.downscaling))
	
	val content = base.use { implicit context =>
		
		Stack.buildColumn(0.fixed) { mainStack =>
		
			val contextWithBackground = base.withBackground(Color.magenta).result
			val tab = TabSelection.contextual(initialChoices = Vector("Goods", "for", "Purchase"))(
				contextWithBackground)
			tab.selectOne("Goods")
			tab.addValueListener { s => println(s.newValue.getOrElse("No item") + " selected") }
			
			mainStack += tab
			mainStack += Stack.buildRowWithContext() { textRow =>
			 
				val labelContext = base.withAlignment(BottomLeft).withTextColor(Color.white).result
				val textFieldContext = base.withAlignment(Alignment.Left).result
				
				val productField = TextField.contextual(prompt = Some("Describe product"))(textFieldContext)
				productField.valuePointer.addListener { e => println(s"Product: ${e.newValue}") }
				// productField.addMouseButtonListener(MouseButtonStateListener.onLeftPressedInside(productField.bounds, e => { println(e); None }))
				
				textRow += Stack.buildColumnWithContext(isRelated = true) { productColumn =>
					
					productColumn += TextLabel.contextual("Product")(labelContext)
					productColumn += productField
				}
				
				val amountField = TextField.contextualForPositiveInts(prompt = Some("1-999"))(textFieldContext)
				
				textRow += Stack.buildColumnWithContext(isRelated = true) { amountColumn =>
					amountColumn += TextLabel.contextual("Amount")(labelContext)
					amountColumn += amountField
				}
				textRow += Stack.buildColumnWithContext(isRelated = true) { priceColumn =>
					
					priceColumn += TextLabel.contextual("Price")(labelContext)
					val field = TextField.contextualForPositiveDoubles(prompt = Some("€"))(textFieldContext)
					priceColumn += field
					
					// Pop-up handling
					def showPopup(message: String) =
					{
						val okButton = TextButton.contextual("OK")
						val popupContent = Stack.buildRowWithContext() { row =>
							row += TextLabel.contextual(message)
							row += okButton
						}.framed(8.any x 8.any, Color.white)
						
						val popup = Popup(field, popupContent, actorHandler) { (c, _) => Point(c.width + 16, 0) }
						okButton.registerAction(() => popup.close())
						popup.isVisible = true
					}
					
					// Adds listening to field(s)
					field.addEnterListener
					{
						p =>
							val product = productField.value
							val amount = amountField.intValue
							val price = p.double
							
							if (product.isDefined && amount.isDefined && price.isDefined)
							{
								productField.clear()
								amountField.clear()
								field.clear()
								productField.requestFocusInWindow()
								
								showPopup(s"${amount.get} x ${product.get} = ${amount.get * price.get} €")
							}
							else
								println("Please select product + amount + price")
					}
				}
				
			}.framed(8.downscaling x 8.downscaling, contextWithBackground.background.get)
		
		}.framed(16.any x 8.any, Color.white)
	}
	
	// Creates the frame and displays it
	val actionLoop = new ActorLoop(actorHandler)
	implicit val context: ExecutionContext = new ThreadPool("Reflection").executionContext
	
	val frame = Frame.windowed(content, "TextLabel Stack Test", User)
	frame.setToExitOnClose()
	
	actionLoop.registerToStopOnceJVMCloses()
	actionLoop.startAsync()
	StackHierarchyManager.startRevalidationLoop()
	frame.startEventGenerators(actorHandler)
	frame.isVisible = true
}
