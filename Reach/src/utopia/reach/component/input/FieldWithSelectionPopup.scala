package utopia.reach.component.input

import utopia.flow.datastructure.mutable.{PointerWithEvents, ResettableLazy}
import utopia.flow.event.{ChangingLike, Fixed}
import utopia.flow.operator.Sign.{Negative, Positive}
import utopia.paradigm.color.Color
import utopia.genesis.event.KeyStateEvent
import utopia.genesis.handling.KeyStateListener
import utopia.paradigm.enumeration.Axis.Y
import utopia.paradigm.enumeration.Direction2D.Down
import utopia.paradigm.shape.shape2d.Size
import utopia.genesis.view.GlobalKeyboardEventHandler
import utopia.inception.handling.HandlerType
import utopia.reach.component.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.input.selection.SelectionList
import utopia.reach.component.template.focus.{Focusable, FocusableWithPointerWrapper}
import utopia.reach.component.template.{ReachComponentLike, ReachComponentWrapper}
import utopia.reach.component.wrapper.{Open, OpenComponent}
import utopia.reach.container.wrapper.scrolling.ScrollView
import utopia.reach.container.ReachCanvas
import utopia.reach.container.wrapper.CachingViewSwapper
import utopia.reflection.color.ColorRole
import utopia.reflection.color.ColorRole.Secondary
import utopia.reflection.component.context.{ScrollingContextLike, TextContextLike}
import utopia.reflection.component.drawing.view.BackgroundViewDrawer
import utopia.reflection.component.template.display.Refreshable
import utopia.reflection.component.template.input.SelectionWithPointers
import utopia.reflection.container.stack.StackLayout
import utopia.reflection.container.stack.StackLayout.Fit
import utopia.reflection.image.SingleColorIcon
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.Alignment.BottomLeft
import utopia.reflection.shape.stack.StackLength
import utopia.reflection.util.ComponentCreationDefaults

import java.awt.event.{ComponentEvent, ComponentListener, KeyEvent}

object FieldWithSelectionPopup extends ContextInsertableComponentFactoryFactory[TextContextLike,
	FieldWithSelectionPopupFactory, ContextualFieldWithSelectionPopupFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new FieldWithSelectionPopupFactory(hierarchy)
}

class FieldWithSelectionPopupFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[TextContextLike, ContextualFieldWithSelectionPopupFactory]
{
	override def withContext[N <: TextContextLike](context: N) =
		ContextualFieldWithSelectionPopupFactory(parentHierarchy, context)
}

case class ContextualFieldWithSelectionPopupFactory[+N <: TextContextLike](parentHierarchy: ComponentHierarchy,
																		   context: N)
	extends ContextualComponentFactory[N, TextContextLike, ContextualFieldWithSelectionPopupFactory]
{
	override def withContext[N2 <: TextContextLike](newContext: N2) = copy(context = newContext)
	
	/**
	  * Creates a new field that utilizes a selection pop-up
	  * @param isEmptyPointer A pointer that contains true when the wrapped field is empty (of text)
	  * @param contentPointer Pointer to the available options in this field
	  * @param valuePointer Pointer to the currently selected option, if any (default = new empty pointer)
	  * @param rightExpandIcon Icon indicating that this selection may be expanded (optional)
	  * @param rightCollapseIcon Icon indicating that this selection may be collapsed (optional)
	  * @param fieldNamePointer A pointer to the displayed name of this field (default = always empty)
	  * @param promptPointer A pointer to the prompt displayed on this field (default = always empty)
	  * @param hintPointer A pointer to the hint displayed under this field (default = always empty)
	  * @param errorMessagePointer A pointer to the error message displayed on this field (default = always empty)
	  * @param leftIconPointer A pointer to the icon displayed on the left side of this component (default = always None)
	  * @param listLayout Stack layout used in the selection list (default = Fit)
	  * @param listCap Cap placed at each end of the selection list (default = always 0)
	  * @param noOptionsView A view to display when there are no options to choose from (optional, in open form)
	  * @param highlightStylePointer A pointer to an additional highlighting style applied to this field (default = always None)
	  * @param focusColorRole Color role used when this field has focus (default = Secondary)
	  * @param sameItemCheck A function for checking whether two options represent the same instance (optional).
	  *                      Should only be specified when equality function (==) shouldn't be used.
	  * @param fillBackground Whether filled field style should be used (default = global default)
	  * @tparam A Type of selectable item
	  * @tparam C Type of component inside the field
	  * @tparam D Type of component to display a selectable item
	  * @tparam P Type of content pointer used
	  * @return A new field
	  */
	def apply[A, C <: ReachComponentLike with Focusable, D <: ReachComponentLike with Refreshable[A],
		P <: ChangingLike[Vector[A]]](isEmptyPointer: ChangingLike[Boolean], contentPointer: P,
									  valuePointer: PointerWithEvents[Option[A]] = new PointerWithEvents[Option[A]](None),
									  rightExpandIcon: Option[SingleColorIcon] = None,
									  rightCollapseIcon: Option[SingleColorIcon] = None,
									  fieldNamePointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
									  promptPointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
									  hintPointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
									  errorMessagePointer: ChangingLike[LocalizedString] = Fixed(LocalizedString.empty),
									  leftIconPointer: ChangingLike[Option[SingleColorIcon]] = Fixed(None),
									  listLayout: StackLayout = Fit, listCap: StackLength = StackLength.fixedZero,
									  noOptionsView: Option[OpenComponent[ReachComponentLike, Any]] = None,
									  highlightStylePointer: ChangingLike[Option[ColorRole]] = Fixed(None),
									  focusColorRole: ColorRole = Secondary,
									  sameItemCheck: Option[(A, A) => Boolean] = None,
									  fillBackground: Boolean = ComponentCreationDefaults.useFillStyleFields)
									 (makeField: (FieldCreationContext, N) => C)
									 (makeDisplay: (ComponentHierarchy, A) => D)
									 (makeRightHintLabel: (ExtraFieldCreationContext[C], N) =>
										 Option[OpenComponent[ReachComponentLike, Any]])
									 (implicit scrollingContext: ScrollingContextLike) =
		new FieldWithSelectionPopup[A, C, D, P, N](parentHierarchy, context, isEmptyPointer, contentPointer,
			valuePointer, rightExpandIcon, rightCollapseIcon, fieldNamePointer, promptPointer, hintPointer,
			errorMessagePointer, leftIconPointer, listLayout, listCap, noOptionsView, highlightStylePointer,
			focusColorRole, sameItemCheck, fillBackground)(makeField)(makeDisplay)(makeRightHintLabel)
}

/**
  * A field wrapper class that displays a selection pop-up when it receives focus
  * @author Mikko Hilpinen
  * @since 22.12.2020, v0.1
  * @param parentHierarchy Component hierarchy this component will be attached to
  * @param context field creation context
  * @param isEmptyPointer A pointer that contains true when the wrapped field is empty (of text)
  * @param contentPointer Pointer to the available options in this field
  * @param valuePointer Pointer to the currently selected option, if any (default = new empty pointer)
  * @param rightExpandIcon Icon indicating that this selection may be expanded (optional)
  * @param rightCollapseIcon Icon indicating that this selection may be collapsed (optional)
  * @param fieldNamePointer A pointer to the displayed name of this field (default = always empty)
  * @param promptPointer A pointer to the prompt displayed on this field (default = always empty)
  * @param hintPointer A pointer to the hint displayed under this field (default = always empty)
  * @param errorMessagePointer A pointer to the error message displayed on this field (default = always empty)
  * @param leftIconPointer A pointer to the icon displayed on the left side of this component (default = always None)
  * @param listLayout Stack layout used in the selection list (default = Fit)
  * @param listCap Cap placed at each end of the selection list (default = always 0)
  * @param noOptionsView A view to display when there are no options to choose from (optional, in open form)
  * @param highlightStylePointer A pointer to an additional highlighting style applied to this field (default = always None)
  * @param focusColorRole Color role used when this field has focus (default = Secondary)
  * @param sameItemCheck A function for checking whether two options represent the same instance (optional).
  *                      Should only be specified when equality function (==) shouldn't be used.
  * @param fillBackground Whether filled field style should be used (default = global default)
  * @tparam A Type of selectable item
  * @tparam C Type of component inside the field
  * @tparam D Type of component to display a selectable item
  * @tparam P Type of content pointer used
  */
class FieldWithSelectionPopup[A, C <: ReachComponentLike with Focusable, D <: ReachComponentLike with Refreshable[A],
	+P <: ChangingLike[Vector[A]], +N <: TextContextLike]
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
	extends ReachComponentWrapper with FocusableWithPointerWrapper
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
	/**
	  * Field wrapped by this field
	  */
	val field = Field(parentHierarchy).withContext(context).apply(isEmptyPointer, fieldNamePointer,
		promptPointer, hintPointer, errorMessagePointer, leftIconPointer, rightIconPointer,
		highlightStylePointer = highlightStylePointer, focusColorRole = focusColorRole,
		fillBackground = fillBackground)(makeField)(makeRightHintLabel)
	
	// Creates the pop-up when necessary
	private val lazyPopup = ResettableLazy {
		println("Creating pop-up")
		// Automatically hides the pop-up when it loses focus
		val popup = field.createOwnedPopup(context.actorHandler, BottomLeft) { hierarchy =>
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
			ScrollView(hierarchy).apply(scrollContent,
				scrollBarMargin = Size(context.margins.small, listCap.optimal), limitsToContentSize = true,
				customDrawers = Vector(BackgroundViewDrawer(field.innerBackgroundPointer.lazyMap { c => c: Color })))
				.withResult(openList.component)
		}.parent
		println("Setting up pop-up")
		popup.component.addComponentListener(PopupVisibilityTracker)
		popup.setToHideWhenNotInFocus()
		popup.addKeyStateListener(PopupKeyListener)
		println("Pop-up set up")
		popup
	}
	
	
	// INITIAL CODE	-----------------------------
	
	// Disposes of the pop-up when this component is removed from the main stack hierarchy
	addHierarchyListener { isAttached =>
		if (isAttached)
			GlobalKeyboardEventHandler += FieldKeyListener
		else
		{
			GlobalKeyboardEventHandler -= FieldKeyListener
			cachedPopup.foreach { popup =>
				println("Disposing of pop-up")
				popup.close()
				lazyPopup.reset()
				popUpDisplayPointer.value = false
				popup.component.removeComponentListener(PopupVisibilityTracker)
			}
		}
	}
	
	// Updates pop-up location when field bounds change
	// TODO: Should be based on the absolute bounds
	field.boundsPointer.mergeWith(popUpDisplayPointer) { (fieldBounds, popupIsVisible) =>
		if (popupIsVisible)
			cachedPopup.foreach { popup =>
				println("Repositions the pop-up")
				// Positions the pop-up
				popup.position = field.absolutePosition.plusY(fieldBounds.height)
				// Matches field width, if possible
				val stackWidth = popup.stackSize.width
				if (fieldBounds.width < stackWidth.min)
					popup.width = stackWidth.min
				else
					stackWidth.max.filter { _ < fieldBounds.width } match
					{
						case Some(maxWidth) => popup.width = maxWidth
						case None => popup.width = fieldBounds.width
					}
			}
	}
	
	// When gains focus, displays the pop-up. Hides the pop-up when focus is lost.
	focusPointer.addListenerAndSimulateEvent(false) { event =>
		if (event.newValue) openPopup() else hidePopup()
	}
	
	
	// COMPUTED	---------------------------------
	
	private def cachedPopup = lazyPopup.current
	
	
	// IMPLEMENTED	-----------------------------
	
	override protected def wrapped = field
	
	override protected def focusable = field
	
	
	// OTHER	---------------------------------
	
	/**
	  * Displays the selection pop-up
	  */
	def openPopup() = {
		println("Displaying pop-up")
		lazyPopup.value.display()
	}
	
	private def hidePopup() = cachedPopup.foreach { _.visible = false }
	
	
	// NESTED	---------------------------------
	
	private object FieldKeyListener extends KeyStateListener
	{
		// ATTRIBUTES	-------------------------
		
		// Listens to down arrow presses
		override val keyStateEventFilter = KeyStateEvent.wasPressedFilter && KeyStateEvent.arrowKeyFilter(Down)
		
		
		// IMPLEMENTED	-------------------------
		
		override def onKeyState(event: KeyStateEvent) = openPopup()
		
		// Is interested in key events while the field has focus and pop-up is not open
		override def allowsHandlingFrom(handlerType: HandlerType) = !popUpDisplayPointer.value && field.hasFocus
	}
	
	private object PopupKeyListener extends KeyStateListener
	{
		// ATTRIBUTES	-------------------------
		
		// Listens to enter and tabulator presses
		override val keyStateEventFilter = KeyStateEvent.wasPressedFilter &&
			KeyStateEvent.keysFilter(KeyEvent.VK_TAB, KeyEvent.VK_ENTER)
		
		
		// IMPLEMENTED	-------------------------
		
		override def onKeyState(event: KeyStateEvent) =
		{
			println("Hides the pop-up")
			// Hides the pop-up
			hidePopup()
			// On tabulator press, yields focus afterwards
			if (event.index == KeyEvent.VK_TAB)
				yieldFocus(if (event.keyStatus.shift) Negative else Positive)
		}
		
		// Only reacts to events while the pop-up is visible
		override def allowsHandlingFrom(handlerType: HandlerType) = popUpDisplayPointer.value
	}
	
	private object PopupVisibilityTracker extends ComponentListener
	{
		// IMPLEMENTED	-------------------------
		
		override def componentResized(e: ComponentEvent) = ()
		
		override def componentMoved(e: ComponentEvent) = ()
		
		override def componentShown(e: ComponentEvent) = popUpDisplayPointer.value = true
		
		override def componentHidden(e: ComponentEvent) = popUpDisplayPointer.value = false
	}
}
