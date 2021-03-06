package utopia.reach.test

import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.generic.ValueConversions._
import utopia.flow.event.{AlwaysTrue, ChangingLike, Fixed}
import utopia.flow.util.FileExtensions._
import utopia.genesis.generic.GenesisDataType
import utopia.genesis.image.Image
import utopia.genesis.util.DistanceExtensions._
import utopia.reach.component.factory.ContextualMixed
import utopia.reach.component.input.{ContextualTextFieldFactory, TextField}
import utopia.reach.component.template.{Focusable, ReachComponentLike}
import utopia.reach.component.wrapper.OpenComponent
import utopia.reach.container.Framing
import utopia.reach.window.{InputRowBlueprint, InputWindowFactory}
import utopia.reflection.color.ColorRole
import utopia.reflection.color.ColorRole.Secondary
import utopia.reflection.component.context.{ColorContext, TextContextLike}
import utopia.reflection.component.drawing.immutable.BackgroundDrawer
import utopia.reflection.container.template.window.{ManagedField, RowGroups, WindowButtonBlueprint}
import utopia.reflection.image.SingleColorIcon
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.LengthExtensions._

/**
  * Tests input window creation
  * @author Mikko Hilpinen
  * @since 6.3.2021, v1
  */
object InputWindowTest extends App
{
	GenesisDataType.setup()
	System.setProperty("sun.java2d.noddraw", true.toString)
	
	import utopia.reflection.test.TestContext._
	import TestCursors._
	
	// FIXME: Input checking doesn't work
	// FIXME: Text fields don't properly show field names while focused
	
	object TestWindows extends InputWindowFactory[Model[Constant], Unit]
	{
		// ATTRIBUTES	-------------------------
		
		private val defaultFieldWidth = 5.cm.toScreenPixels.any
		
		override protected lazy val closeIcon =
			new SingleColorIcon(Image.readFrom("Reflection/test-images/close.png").get)
		
		override protected lazy val standardContext = baseContext.inContextWithBackground(colorScheme.primary)
		
		override protected lazy val fieldCreationContext =
			baseContext.inContextWithBackground(colorScheme.primary.light).forTextComponents
		
		
		// IMPLEMENTED	-------------------------
		
		override protected def warningPopupContext =
			baseContext.inContextWithBackground(colorScheme.error).forTextComponents
		
		override protected def inputTemplate =
		{
			val nameErrorPointer = new PointerWithEvents(LocalizedString.empty)
			// TODO: TextContext didn't work (required textContextLike)
			val firstNameField = InputRowBlueprint.using(TextField, "firstName", fieldAlignment = Alignment.Left) { fieldF: ContextualTextFieldFactory[TextContextLike] =>
				val textPointer = new PointerWithEvents[String]("")
				val displayErrorPointer = nameErrorPointer.mergeWith(textPointer) { (error, text) =>
					if (text.isEmpty) error else LocalizedString.empty
				}
				val field = fieldF.forString(defaultFieldWidth, Fixed("First Name"),
					errorMessagePointer = displayErrorPointer)
				// TODO: These conversions don't work without specifying types first
				ManagedField.test[String, TextField[String]](field) { s =>
					if (s.isEmpty)
					{
						nameErrorPointer.value = "Required Field"
						Some("Please insert a value first")
					}
					else
						None
				}
			}
			val lastNameField = InputRowBlueprint.using(TextField, "lastName", fieldAlignment = Alignment.Left) { fieldF: ContextualTextFieldFactory[TextContextLike] =>
				ManagedField.autoConvert[String, TextField[String]](fieldF.forString(defaultFieldWidth, Fixed("Last Name")))
			}
			
			Vector(RowGroups.singleGroup(firstNameField, lastNameField)) -> ()
		}
		
		override protected def buildLayout(factories: ContextualMixed[ColorContext],
										   content: Vector[OpenComponent[ReachComponentLike, ChangingLike[Boolean]]],
										   context: Unit) =
		{
			// Expects a single group only, which is framed
			factories(Framing).withoutContext(content.head, margins.small.any,
				Vector(BackgroundDrawer(fieldCreationContext.containerBackground)))
		}
		
		override protected def specifyButtons(context: Unit, input: => Either[(String, Focusable), Model[Constant]],
											  warn: (String, LocalizedString) => Unit) =
		{
			val okButton = WindowButtonBlueprint[Model[Constant]]("OK", role = Secondary, isDefault = true) { promise =>
				input.toOption.foreach(promise.trySuccess)
			}
			Vector(okButton) -> AlwaysTrue
		}
		
		override protected def defaultCloseButtonText = "Cancel"
		
		override protected def executionContext = exc
		
		override protected def buttonContext(buttonColor: ColorRole, hasIcon: Boolean) =
			standardContext.forTextComponents.withTextAlignment(if (hasIcon) Alignment.Left else Alignment.Center)
				.forButtons(buttonColor)
		
		override protected def defaultResult = Model.empty
		
		override protected def title = "Test"
	}
	
	// Displays a dialog
	val result = TestWindows.displayBlocking(cursors = cursors).get
	println(s"Dialog completed with result: $result")
}
