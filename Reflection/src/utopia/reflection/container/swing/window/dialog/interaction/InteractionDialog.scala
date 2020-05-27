package utopia.reflection.container.swing.window.dialog.interaction

import java.awt.Window

import utopia.flow.util.CollectionExtensions._
import utopia.reflection.component.context.{ButtonContextLike, ColorContextLike}
import utopia.reflection.component.swing.StackSpace
import utopia.reflection.component.swing.button.{ImageAndTextButton, TextButton}
import utopia.reflection.container.swing.Stack
import utopia.reflection.container.swing.Stack.AwtStackable
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.container.swing.window.dialog.Dialog
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.{Alignment, StackLength}
import utopia.reflection.shape.Alignment.Top
import utopia.reflection.shape.LengthExtensions._

import scala.concurrent.ExecutionContext

/**
  * A common trait for simple dialogs that are used for user-interactions, usually for requesting some sort of input
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1.2
  */
trait InteractionDialog[+A]
{
	// ABSTRACT	-----------------------
	
	/**
	  * @return Context used when creating the dialog. Provided container background specifies the dialog background color.
	  */
	protected def standardContext: ColorContextLike
	
	/**
	  * @param buttonColor The desired button color
	  * @param hasIcon Whether the button uses an icon
	  * @return Context used when creating the button
	  */
	protected def buttonContext(buttonColor: ButtonColor, hasIcon: Boolean): ButtonContextLike
	
	/**
	  * Buttons that are displayed on this dialog. The first button is used as the default.
	  */
	protected def buttonBlueprints: Vector[DialogButtonBlueprint[A]]
	
	/**
	  * Dialog body element(s)
	  */
	protected def dialogContent: AwtStackable
	
	/**
	  * @return Result provided when no result is gained through interacting with the buttons
	  */
	protected def defaultResult: A
	
	/**
	  * @return Title displayed on this dialog
	  */
	protected def title: LocalizedString
	
	
	// OTHER	-----------------------
	
	/**
	  * Displays an interactive dialog to the user
	  * @param parentWindow Window that will "own" the new dialog
	  * @return A future of the closing of the dialog, with a selected result (or default if none was selected)
	  */
	def display(parentWindow: Window)(implicit exc: ExecutionContext) =
	{
		val context = standardContext
		
		// Creates the buttons based on button info
		val actualizedButtons = buttonBlueprints.map { buttonData =>
			implicit val btnC: ButtonContextLike = buttonContext(buttonData.color, buttonData.icon.isDefined)
			val button = buttonData.icon match
			{
				case Some(icon) => ImageAndTextButton.contextualWithoutAction(icon.inButton, buttonData.text)
				case None => TextButton.contextualWithoutAction(buttonData.text)
			}
			buttonData -> button
		}
		// Places content in a stack
		val content = {
			implicit val baseC: ColorContextLike = context
			Stack.buildColumnWithContext() { mainStack =>
				// Some of the buttons may be placed before the dialog content, some after
				val (bottomButtons, topButtons) = actualizedButtons.divideBy { _._1.location.vertical == Top }
				if (topButtons.nonEmpty)
					mainStack += buttonRow(topButtons, context.defaultStackMargin.optimal)
				mainStack += dialogContent
				if (bottomButtons.nonEmpty)
					mainStack += buttonRow(bottomButtons, context.defaultStackMargin.optimal)
			}
		}.framed(context.margins.medium.downscaling, context.containerBackground)
		
		// Creates and sets up the dialog
		val dialog = new Dialog(parentWindow, content, title, Program)
		if (actualizedButtons.nonEmpty)
			dialog.registerButtons(actualizedButtons.head._2, actualizedButtons.drop(1).map { _._2 }: _*)
		dialog.setToCloseOnEsc()
		
		// Adds actions to dialog buttons
		var result: Option[A] = None
		actualizedButtons.foreach { case (data, button) => button.registerAction(() =>
		{
			val (newResult, shouldClose) = data.generateResultOnPress()
			result = newResult
			if (shouldClose)
				dialog.close()
		}) }
		
		// Displays the dialog and returns a promise of final result
		dialog.startEventGenerators(context.actorHandler)
		dialog.display()
		dialog.closeFuture.map { _ => result.getOrElse(defaultResult) }
	}
	
	private def buttonRow(buttons: Seq[(DialogButtonBlueprint[_], AwtStackable)], baseMargin: Double) =
	{
		val nonScalingMargin = baseMargin.downscaling
		
		val buttonsByLocation = buttons.groupMap { _._1.location.horizontal } { _._2 }
		// Case: Items only on one side
		if (buttonsByLocation.size == 1)
		{
			val (alignment, buttons) = buttonsByLocation.head
			Stack.rowWithItems(buttons, nonScalingMargin).aligned(alignment)
		}
		else
		{
			val scalingMargin = baseMargin.upscaling.expanding
			val buttonGroups = Vector(Alignment.Left, Alignment.Center, Alignment.Right).flatMap(buttonsByLocation.get)
			val buttonGroupComponents = buttonGroups.map { buttons =>
				if (buttons.size == 1)
					buttons.head
				else
					Stack.rowWithItems(buttons, nonScalingMargin)
			}
			// Case: Items on left side + right and/or center
			if (buttonsByLocation.contains(Alignment.Left))
				Stack.rowWithItems(buttonGroupComponents, scalingMargin)
			// Case: Items only on center and right
			else
				Stack.rowWithItems(StackSpace.horizontal(StackLength.fixedZero) +: buttonGroupComponents)
		}
	}
}
