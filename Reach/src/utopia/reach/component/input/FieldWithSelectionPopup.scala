package utopia.reach.component.input

import utopia.flow.datastructure.mutable.{PointerWithEvents, ResettableLazy}
import utopia.flow.event.{ChangingLike, Fixed}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{Focusable, ReachComponentLike}
import utopia.reach.component.wrapper.OpenComponent
import utopia.reach.container.ScrollView
import utopia.reflection.color.ColorRole
import utopia.reflection.color.ColorRole.Secondary
import utopia.reflection.component.context.TextContextLike
import utopia.reflection.component.template.display.Refreshable
import utopia.reflection.container.stack.StackLayout
import utopia.reflection.container.stack.StackLayout.Fit
import utopia.reflection.image.SingleColorIcon
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.Alignment.BottomLeft
import utopia.reflection.shape.stack.StackLength
import utopia.reflection.util.ComponentCreationDefaults

/**
  * A field wrapper class that displays a selection pop-up when it receives focus
  * @author Mikko Hilpinen
  * @since 22.12.2020, v1
  */
class FieldWithSelectionPopup[A, C <: ReachComponentLike with Focusable, D <: ReachComponentLike with Refreshable[A],
	P <: ChangingLike[Vector[A]], N <: TextContextLike]
(parentHierarchy: ComponentHierarchy, context: N, isEmptyPointer: ChangingLike[Boolean], optionsPointer: P,
 valuePointer: PointerWithEvents[Option[A]] = new PointerWithEvents[Option[A]](None),
 rightExpandIcon: Option[SingleColorIcon] = None, rightCollapseIcon: Option[SingleColorIcon] = None,
 fieldNamePointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
 promptPointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
 hintPointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
 errorMessagePointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
 leftIconPointer: ChangingLike[Option[SingleColorIcon]] = Fixed(None), listLayout: StackLayout = Fit,
 listCap: StackLength = StackLength.fixedZero, noOptionsView: Option[OpenComponent[ReachComponentLike, Any]] = None,
 highlightStylePointer: ChangingLike[Option[ColorRole]] = Fixed(None), focusColorRole: ColorRole = Secondary,
 sameItemCheck: Option[(A, A) => Boolean] = None,
 fillBackground: Boolean = ComponentCreationDefaults.useFillStyleFields)
(makeField: (FieldCreationContext, N) => C)
(makeDisplay: (ComponentHierarchy, A) => C)
(makeRightHintLabel: (ExtraFieldCreationContext[C], N) => Option[OpenComponent[ReachComponentLike, Any]])
{
	// ATTRIBUTES	------------------------------
	
	// Follows the pop-up visibility state with a pointer
	private val popUpDisplayPointer = new PointerWithEvents(false)
	private val rightIconPointer = rightExpandIcon match {
		case Some(expandIcon) =>
			rightCollapseIcon match
			{
				case Some(collapseIcon) =>
					// Makes sure both icons have the same size
					if (expandIcon.size == collapseIcon.size)
						popUpDisplayPointer.map { visible => Some(if (visible) collapseIcon else expandIcon) }
					else
					{
						val smallerAndLarger = Vector(expandIcon, collapseIcon).sortBy { _.size.area }
						val smaller = smallerAndLarger.head
						val larger = smallerAndLarger(1)
						val targetSize = smaller.size
						val shrankIcon = new SingleColorIcon(larger.original.fitting(targetSize).paintedToCanvas(targetSize))
						
						val (newExpandIcon, newCollapseIcon) =
							if (smaller == expandIcon) smaller -> shrankIcon else shrankIcon -> smaller
						popUpDisplayPointer.map { visible => Some(if (visible) newCollapseIcon else newExpandIcon) }
					}
				case None => Fixed(Some(expandIcon))
			}
		case None => Fixed(rightCollapseIcon)
	}
	
	// Creates the wrapped field first
	private val field = Field(parentHierarchy).withContext(context).apply(isEmptyPointer, fieldNamePointer,
		promptPointer, hintPointer, errorMessagePointer, leftIconPointer, rightIconPointer,
		highlightStylePointer = highlightStylePointer, focusColorRole = focusColorRole,
		fillBackground = fillBackground)(makeField)(makeRightHintLabel)
	
	// Creates the pop-up when necessary
	private val lazyPopup = ResettableLazy {
		field.createPopup(context.actorHandler, BottomLeft) { hierarchy =>
			// ScrollView(hierarchy).fill
			???
		}
	}
}
