package utopia.reflection.container.swing.window.interaction

import utopia.firmament.context.text.StaticTextContext
import utopia.firmament.image.SingleColorIcon
import utopia.firmament.localization.LocalizedString
import utopia.firmament.model.RowGroups
import utopia.firmament.model.enumeration.StackLayout.Center
import utopia.firmament.model.stack.LengthExtensions._
import utopia.flow.async.process
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.paradigm.enumeration.{Alignment, Direction2D}
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.reflection.component.swing.button.ImageButton
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.component.swing.template.AwtComponentRelated
import utopia.reflection.component.template.{Focusable, ReflectionComponentLike}
import utopia.reflection.container.swing.layout.SegmentGroup
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.reflection.container.swing.layout.multi.Stack.AwtStackable
import utopia.reflection.container.swing.window.Popup
import utopia.reflection.container.swing.window.Popup.PopupAutoCloseLogic.WhenAnyKeyPressed

import scala.concurrent.ExecutionContext

/**
  * Used for requesting user input via multiple input fields
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1.2
  */
trait InputWindow[+A] extends InteractionWindow[A]
{
	// ABSTRACT	------------------------------------
	
	/**
	  * @return Text displayed on the OK/Accept/Next button that processes user input
	  */
	protected def okButtonText: LocalizedString
	
	/**
	  * @return Text displayed on the Cancel/Close button that closes the dialog without processing user input
	  */
	protected def cancelButtonText: LocalizedString
	
	/**
	  * @return Icon displayed on the OK/Accept/Next button. None if no icon should be displayed.
	  */
	protected def okButtonIcon: Option[SingleColorIcon]
	
	/**
	  * @return Icon displayed on the Cancel/Close button, as well as in the pop-up's close button
	  */
	protected def closeIcon: SingleColorIcon
	
	/**
	  * @return Component creation context for the field name labels.
	  */
	protected def fieldLabelContext: StaticTextContext
	
	/**
	  * @return Component creation context for the missing or invalid value pop-up,
	  *         includes the pop-up background color and text layout.
	  */
	protected def popupContext: StaticTextContext
	
	/**
	  * @return Execution context for asynchronous tasks
	  */
	protected def executionContext: ExecutionContext
	
	/**
	  * @return Blueprints for the fields that are used in producing input in this dialog. The blueprints should be
	  *         grouped in separate vectors.
	  */
	protected def fields: Vector[RowGroups[InputRowBlueprint]]
	
	/**
	 * @return Buttons used in addition to ok and cancel buttons
	 */
	protected def additionalButtons: Vector[DialogButtonBlueprint[A]]
	
	/**
	  * Combines the specified rows into a single component (Eg. stack). The rows are segmented to all share same
	  * width for label and input component.
	  * @param rowGroups Row groups that form the dialog content.
	  * @return A container that will be displayed in the dialog
	  */
	protected def buildLayout(rowGroups: Vector[RowGroups[AwtStackable]]): AwtStackable
	
	/**
	  * Produces a result based on dialog input when the user selects "OK"
	  * @return Either the produced input (right) or a field to return the focus to, along with a message
	  *         to display to the user (left)
	  */
	protected def produceResult: Either[(Focusable with ReflectionComponentLike with AwtComponentRelated, LocalizedString), A]
	
	
	// IMPLEMENTED	----------------------------------
	
	override protected def buttonBlueprints =
	{
		implicit val logger: Logger = SysErrLogger
		implicit val exc: ExecutionContext = executionContext
		
		val okButton = new DialogButtonBlueprint[A](okButtonText, okButtonIcon, ButtonColor.secondary)(() =>
		{
			// Checks the results. If failed, returns focus to an item and displays a message
			produceResult match
			{
				case Right(result) => Some(result) -> true
				case Left((component, message)) =>
					// Moves the focus
					component.requestFocusInWindow()
					
					// Creates the notification pop-up
					val popup =
					{
						implicit val context: StaticTextContext = popupContext
						val dismissButton = ImageButton.contextualWithoutAction(closeIcon.asButton.contextual)
						val popupContent = Stack.buildRowWithContext(layout = Center) { row =>
							row += dismissButton
							row += TextLabel.contextual(message)
						}.framed(popupContext.margins.medium.any, popupContext.background)
						val popup = Popup(component, popupContent, popupContext.actorHandler,
							WhenAnyKeyPressed, Alignment.Left) { (cSize, pSize) =>
							Point(cSize.width + popupContext.margins.medium, -(pSize.height - cSize.height) / 2) }
						dismissButton.registerAction { () => popup.close() }
						
						popup
					}
					
					// Closes the pop-up if any key is pressed or after a delay
					process.Delay(5.seconds) { popup.close() }
					popup.display(false)
					None -> false
			}
		})
		val cancelButton = DialogButtonBlueprint.closeButton(cancelButtonText, closeIcon)
		(okButton +: additionalButtons) :+ cancelButton
	}
	
	override protected def dialogContent =
	{
		implicit val context: StaticTextContext = fieldLabelContext
		
		// Uses one segmented group for each row group group
		val rows = fields.map { groups =>
			val segmentGroup = new SegmentGroup()
			groups.mapRows { row =>
				val fieldInRow =
				{
					if (row.spansWholeRow)
						row.field
					else
						row.field.alignedToSide(Direction2D.Left)
				}
				val rowComponent = Stack.buildRowWithContext() { s =>
					segmentGroup.wrap(Vector(TextLabel.contextual(row.fieldName), fieldInRow)).foreach { s += _ }
				}
				// Some rows have dependent visibility state
				row.rowVisibilityPointer.foreach { pointer =>
					rowComponent.visible = pointer.value
					pointer.addContinuousListener { e => rowComponent.visible = e.newValue }
				}
				rowComponent
			}
		}
		buildLayout(rows)
	}
}
