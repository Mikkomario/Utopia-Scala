package utopia.reflection.test

import utopia.flow.generic.ValueConversions._
import utopia.genesis.generic.GenesisDataType
import utopia.genesis.shape.shape2D.Point
import utopia.reflection.component.swing.button.TextButton
import utopia.reflection.component.swing.{TabSelection, TextField}
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.container.stack.StackLayout.Leading
import utopia.reflection.container.swing.window.{Frame, Popup}
import utopia.reflection.container.swing.window.WindowResizePolicy.User
import utopia.reflection.container.swing.Stack
import utopia.reflection.container.swing.Stack.AwtStackable
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.shape.Alignment.Center
import utopia.reflection.util.SingleFrameSetup

/**
  * This is a simple test implementation of text fields with content filtering
  * @author Mikko Hilpinen
  * @since 24.4.2019, v1+
  */
object TextFieldTest extends App
{
	GenesisDataType.setup()
	
	import TestContext._
	
	val standardWidth = 320.any
	
	// Pop-up handling
	def showPopup(origin: AwtStackable, message: String) =
	{
		val popupBG = colorScheme.primary.light
		baseContext.inContextWithBackground(popupBG).use { context =>
			// Creates pop-up content
			val okButton = context.forTextComponents(Center).forSecondaryColorButtons
				.use { implicit btnC => TextButton.contextualWithoutAction("OK") }
			val popUpContent = Stack.buildRowWithContext() { row =>
				row += context.forTextComponents().use { implicit txC => TextLabel.contextual(message) }
				row += okButton
			} (context).framed(margins.medium.downscaling, popupBG)
			
			val popUp = Popup(origin, popUpContent, actorHandler) { (c, p) => Point(c.width + margins.large, (c.height - p.height) / 2) }
			okButton.registerAction { () => popUp.close() }
			popUp.display()
		}
	}
	
	val contentBG = colorScheme.primary
	val content = baseContext.inContextWithBackground(contentBG).use { context =>
		Stack.buildColumnWithContext() { mainStack =>
			// Creates the category tab
			val tab = context.forTextComponents(Center).use { implicit tabC =>
				TabSelection.contextual(initialChoices = Vector("Goods", "for", "Purchase"))
			}
			tab.selectOne("Goods")
			tab.addValueListener { s => println(s.newValue.getOrElse("No item") + " selected") }
			
			mainStack += tab
			// Adds the main interaction area
			mainStack += Stack.buildRowWithContext() { row =>
				// Creates the fields
				val (productField, amountField, priceField) = context.forTextComponents(Alignment.Left).withPromptFont(context.defaultFont * 0.8).forGrayFields.use { implicit fieldC =>
					println(s"Field context bg = ${fieldC.containerBackground}, field BG = ${fieldC.buttonColor}, text color = ${fieldC.textColor}")
					val productField = TextField.contextual(standardWidth, prompt = Some("Describe product"))
					val amountField = TextField.contextualForPositiveInts(standardWidth / 2, prompt = Some("1-999 Too long a prompt"))
					val priceField = TextField.contextualForPositiveDoubles(standardWidth / 2, prompt = Some("€"))
					(productField, amountField, priceField)
				}
				
				// Pairs the fields with matching labels
				context.forTextComponents(Alignment.BottomLeft).withoutInsets.use { implicit labelC =>
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
				productField.valuePointer.addListener { e => println(s"Product: ${e.newValue}") }
				priceField.addEnterListener { p =>
					val product = productField.value
					val amount = amountField.intValue
					val price = p.double
					
					if (product.isDefined && amount.isDefined && price.isDefined)
					{
						productField.clear()
						amountField.clear()
						priceField.clear()
						productField.requestFocusInWindow()
						
						showPopup(priceField, s"${amount.get} x ${product.get} = ${amount.get * price.get} €")
					}
					else
						println("Please select product + amount + price")
				}
				
			}(context)
		}(context)
	}.framed(margins.medium.downscaling, contentBG).framed(margins.medium.any, colorScheme.gray)
	
	// Displays the frame
	new SingleFrameSetup(actorHandler, Frame.windowed(content, "TextLabel Stack Test", User)).start()
}
