package utopia.reach.component.input

import utopia.flow.datastructure.mutable.{PointerWithEvents, ResettableLazy}
import utopia.flow.event.{ChangingLike, Fixed}
import utopia.genesis.shape.Axis.Y
import utopia.genesis.shape.shape2D.Size
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{Focusable, FocusableWrapper, ReachComponentLike, ReachComponentWrapper}
import utopia.reach.component.wrapper.{Open, OpenComponent}
import utopia.reach.container.{CachingViewSwapper, ReachCanvas, ScrollView}
import utopia.reflection.color.ColorRole
import utopia.reflection.color.ColorRole.Secondary
import utopia.reflection.component.context.{ScrollingContextLike, TextContextLike}
import utopia.reflection.component.template.display.Refreshable
import utopia.reflection.component.template.input.SelectionWithPointers
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
(parentHierarchy: ComponentHierarchy, context: N, isEmptyPointer: ChangingLike[Boolean], override val contentPointer: P,
 override val valuePointer: PointerWithEvents[Option[A]] = new PointerWithEvents[Option[A]](None),
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
(makeDisplay: (ComponentHierarchy, A) => D)
(makeRightHintLabel: (ExtraFieldCreationContext[C], N) => Option[OpenComponent[ReachComponentLike, Any]])
(implicit scrollingContext: ScrollingContextLike)
	extends ReachComponentWrapper with FocusableWrapper
		with SelectionWithPointers[Option[A], PointerWithEvents[Option[A]], Vector[A], P]
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
		// TODO: Handle automatic pop-up hiding
		field.createOwnedPopup(context.actorHandler, BottomLeft) { hierarchy =>
			implicit val canvas: ReachCanvas = hierarchy.top
			// Creates the pop-up content in open form first
			val openList = Open { hierarchy =>
				SelectionList(hierarchy).apply(context.actorHandler, field.innerBackgroundPointer, contentPointer,
					valuePointer, Y, listLayout, context.defaultStackMargin, listCap, sameItemCheck)(makeDisplay)
			}
			val scrollContent = noOptionsView match
			{
				// Case: "No options view" is used => shows it when there is no options to choose from
				case Some(noOptionsView) =>
					Open { hierarchy =>
						CachingViewSwapper(hierarchy).generic(contentPointer.map { _.isEmpty }) { isEmpty: Boolean =>
							if (isEmpty)
								noOptionsView
							else
								openList
						}
					}
				// Case: Selection list is always displayed, even when empty
				case None => openList
			}
			// Wraps the content in a scroll view with custom background drawing
			ScrollView(hierarchy).apply(scrollContent, scrollBarMargin = Size(context.margins.small, listCap.optimal),
				limitsToContentSize = true)
		}.parent
	}
	
	
	// INITIAL CODE	-----------------------------
	
	// Disposes of the pop-up when this component is removed from the main stack hierarchy
	addHierarchyListener { isAttached =>
		if (!isAttached)
		{
			lazyPopup.current.foreach { popup =>
				popup.close()
				lazyPopup.reset()
			}
		}
	}
	
	
	// IMPLEMENTED	-----------------------------
	
	override protected def wrapped = field
	
	override protected def focusable = field
	
	
	// OTHER	---------------------------------
	
	/**
	  * Displays the selection pop-up
	  */
	def openSelection() = lazyPopup.value.display()
}
