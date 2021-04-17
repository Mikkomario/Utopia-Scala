package utopia.reflection.container.swing.window.interaction

import utopia.flow.async.AsyncExtensions.RichFuture
import utopia.flow.collection.VolatileList
import utopia.flow.util.CollectionExtensions._
import utopia.reflection.component.context.{ButtonContextLike, ColorContextLike}
import utopia.reflection.component.swing.StackSpace
import utopia.reflection.component.swing.button.{ImageAndTextButton, TextButton}
import utopia.reflection.container.swing.layout.multi.Stack.AwtStackable
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.window.{Dialog, Frame, Window}
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.Alignment.Top
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.shape.stack.StackLength

import scala.concurrent.ExecutionContext

/**
  * A common trait for simple dialogs that are used for user-interactions, usually for requesting some sort of input
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1.2
  */
trait InteractionWindow[+A]
{
	// ATTRIBUTES   -------------------
	
	// Keeps track of the currently displayed dialog.
	private val _visibleDialogs = VolatileList[Window[_]]()
	
	
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
	
	
	// COMPUTED -----------------------
	
	/**
	 * @return Currently displayed dialogs from this instance. Empty if no dialogs are being displayed at this time.
	 */
	def visibleDialogs = _visibleDialogs.value
	
	/**
	 * @return Whether a dialog from this instance is currently being displayed (may be hidden, see .isVisible)
	 */
	def isDisplaying = _visibleDialogs.nonEmpty
	
	/**
	 * @return Whether a dialog from this instance is currently visible
	 */
	def isVisible = _visibleDialogs.exists { _.visible }
	
	
	// OTHER	-----------------------
	
	/**
	  * Displays an interactive dialog to the user. Blocks while the dialog is visible
	  * @param parentWindow Window over which this window is displayed
	  * @param exc Implicit execution context
	  */
	def displayBlockingOver(parentWindow: java.awt.Window)(implicit exc: ExecutionContext) =
		displayBlocking(Some(parentWindow))
	
	/**
	  * Displays an interactive dialog to the user. Blocks while the dialog is visible
	  * @param parentWindow Window over which this window is displayed (optional)
	  * @param exc Implicit execution context
	  */
	def displayBlocking(parentWindow: Option[java.awt.Window] = None)(implicit exc: ExecutionContext): Unit =
		display(parentWindow).waitFor()
	
	/**
	 * Displays an interactive dialog to the user
	 * @param parentWindow Window that will "own" the new window. None if the new window should be independent (default)
	 * @return A future of the closing of the dialog, with a selected result (or default if none was selected)
	 */
	def displayOver(parentWindow: java.awt.Window)(implicit exc: ExecutionContext) = display(Some(parentWindow))
	
	/**
	  * Displays an interactive window to the user
	  * @param parentWindow Window that will "own" the new window. None if the new window should be independent (default)
	  * @return A future of the closing of the dialog, with a selected result (or default if none was selected)
	  */
	def display(parentWindow: Option[java.awt.Window] = None)(implicit exc: ExecutionContext) =
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
		val window = parentWindow match
		{
			case Some(parent) => new Dialog(parent, content, title, Program)
			case None => Frame.windowed(content, title, Program)
		}
		if (actualizedButtons.nonEmpty)
			window.registerButtons(actualizedButtons.head._2, actualizedButtons.drop(1).map { _._2 }: _*)
		window.setToCloseOnEsc()
		
		// Adds actions to dialog buttons
		var result: Option[A] = None
		actualizedButtons.foreach { case (data, button) => button.registerAction(() =>
		{
			val (newResult, shouldClose) = data.generateResultOnPress()
			result = newResult
			if (shouldClose)
				window.close()
		}) }
		
		// Remembers the dialog
		_visibleDialogs :+= window
		
		// Displays the dialog and returns a promise of final result
		window.startEventGenerators(context.actorHandler)
		window.display()
		window.closeFuture.map { _ =>
			_visibleDialogs -= window
			result.getOrElse(defaultResult)
		}
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
