package utopia.reflection.test.swing

import utopia.firmament.model.enumeration.StackLayout.Leading
import utopia.firmament.model.enumeration.WindowResizePolicy.User
import utopia.firmament.model.stack.LengthExtensions._
import utopia.genesis.handling.event.keyboard.Key.Control
import utopia.genesis.handling.event.keyboard.{KeyStateListener, KeyboardEvents}
import utopia.paradigm.color.ColorRole.{Gray, Secondary}
import utopia.paradigm.color.ColorShade.Light
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.enumeration.Alignment.Center
import utopia.paradigm.generic.ParadigmDataType
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.reflection.component.swing.button.TextButton
import utopia.reflection.component.swing.input.{TabSelection, TextField}
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.layout.multi.Stack.AwtStackable
import utopia.reflection.container.swing.window.{Frame, Popup}
import utopia.reflection.test.TestContext
import utopia.reflection.util.SingleFrameSetup

/**
  * This is a simple test implementation of text fields with content filtering
  * @author Mikko Hilpinen
  * @since 24.4.2019, v1+
  */
object TextFieldTest extends App
{
	ParadigmDataType.setup()
	
	import TestContext._
	
	val standardWidth = 320.any
	
	// Pop-up handling
	def showPopup(origin: AwtStackable, message: String) =
	{
		val popupBG = colorScheme.primary.light
		baseContext.against(popupBG).use { context =>
			// Creates pop-up content
			val okButton = context.forTextComponents.withTextAlignment(Center).withBackground(Secondary)
				.use { implicit btnC => TextButton.contextualWithoutAction("OK") }
			val popUpContent = Stack.buildRowWithContext() { row =>
				row += context.forTextComponents.use { implicit txC => TextLabel.contextual(message) }
				row += okButton
			}(context).framed(margins.medium.downscaling, popupBG)
			
			val popUp = Popup(origin, popUpContent, actorHandler) { (c, p) => Point(c.width + margins.large, (c.height - p.height) / 2) }
			okButton.registerAction { () => popUp.close() }
			popUp.display()
		}
	}
	
	val contentBG = colorScheme.primary
	val content = baseContext.against(contentBG).use { context =>
		Stack.buildColumnWithContext() { mainStack =>
			// Creates the category tab
			val tab = context.forTextComponents.withTextAlignment(Center).use { implicit tabC =>
				TabSelection.contextual(initialChoices = Vector("Goods", "for", "Purchase"))
			}
			tab.selectOne("Goods")
			tab.valuePointer.addContinuousListener { s => println(s"${ s.newValue.getOrElse("No item") } selected") }
			
			mainStack += tab
			// Adds the main interaction area
			mainStack += Stack.buildRowWithContext() { row =>
				// Creates the fields
				val (productField, amountField, priceField) = context.forTextComponents
					.withPromptFont(context.font * 0.8).withBackground(Gray, Light)
					.use { implicit fieldC =>
						println(s"Field context bg = ${ fieldC.background }, field BG = ${ fieldC.background }, text color = ${ fieldC.textColor }")
						val productField = TextField.contextualForStrings(standardWidth, prompt = "Describe product")
						val amountField = TextField.contextualForPositiveInts(standardWidth / 2, prompt = "1-999 Too long a prompt")
						val priceField = TextField.contextualForPositiveDoubles(standardWidth / 2, prompt = "€")
						(productField, amountField, priceField)
					}
				// amountField.textPointer.addContinuousListener { println(_) }
				KeyboardEvents += KeyStateListener.pressed(Control) { _ => println(amountField.value) }
				
				// Pairs the fields with matching labels
				context.forTextComponents.withTextAlignment(Alignment.BottomLeft).withoutTextInsets.use { implicit labelC =>
					val productLabel = TextLabel.contextual("Product")
					val amountLabel = TextLabel.contextual("Amount")
					val priceLabel = TextLabel.contextual("Price")
					
					Vector(productField -> productLabel, amountField -> amountLabel, priceField -> priceLabel)
				}.foreach { case (field, label) =>
					// Adds each pair to the interaction row
					row += Stack.buildColumnWithContext(layout = Leading, isRelated = true) { fieldStack =>
						fieldStack += label
						fieldStack += field
					}(context)
				}
				
				// Adds field interactions
				productField.valuePointer.addListener { e => println(s"Product: ${ e.newValue }") }
				priceField.addEnterListener { p =>
					val product = productField.value
					val amount = amountField.value
					val price = p
					
					if (product.nonEmpty && amount.isDefined && price.isDefined) {
						productField.clear()
						amountField.clear()
						priceField.clear()
						productField.requestFocusInWindow()
						
						showPopup(priceField, s"${ amount.get } x $product = ${ amount.get * price.get } €")
					}
					else
						println("Please select product + amount + price")
				}
				
			}(context)
		}(context)
	}.framed(margins.medium.downscaling, contentBG).framed(margins.medium.any, colorScheme(Gray))
	
	// Displays the frame
	new SingleFrameSetup(actorHandler, Frame.windowed(content, "TextLabel Stack Test", User)).start()
}
