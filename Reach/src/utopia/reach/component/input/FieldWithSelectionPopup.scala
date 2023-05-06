package utopia.reach.component.input

import utopia.firmament.component.Window
import utopia.firmament.component.display.Refreshable
import utopia.firmament.component.input.SelectionWithPointers
import utopia.firmament.context.{ComponentCreationDefaults, ScrollingContext, TextContext}
import utopia.firmament.drawing.view.BackgroundViewDrawer
import utopia.firmament.image.SingleColorIcon
import utopia.firmament.localization.LocalizedString
import utopia.firmament.model.enumeration.StackLayout
import utopia.firmament.model.enumeration.StackLayout.Fit
import utopia.firmament.model.stack.StackLength
import utopia.flow.async.process.Delay
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.DetachmentChoice
import utopia.flow.operator.EqualsFunction
import utopia.flow.operator.Sign.{Negative, Positive}
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.NotEmpty
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.eventful.{AlwaysFalse, AlwaysTrue, Fixed}
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.caching.ListenableResettableLazy
import utopia.flow.view.mutable.eventful.{PointerWithEvents, ResettableFlag}
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.event.{ConsumeEvent, KeyStateEvent, MouseButtonStateEvent}
import utopia.genesis.handling.{KeyStateListener, MouseButtonStateListener}
import utopia.genesis.view.{GlobalKeyboardEventHandler, GlobalMouseEventHandler}
import utopia.inception.handling.HandlerType
import utopia.paradigm.color.ColorRole.Secondary
import utopia.paradigm.color.{Color, ColorRole}
import utopia.paradigm.enumeration.Alignment.Bottom
import utopia.paradigm.enumeration.Axis.Y
import utopia.paradigm.enumeration.Direction2D.Down
import utopia.paradigm.shape.shape2d.Size
import utopia.reach.component.factory.FromGenericContextComponentFactoryFactory.Gccff
import utopia.reach.component.factory.{GenericContextualFactory, Mixed}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.input.selection.{SelectionList, SelectionListFactory}
import utopia.reach.component.template.focus.{Focusable, FocusableWithPointerWrapper}
import utopia.reach.component.template.{ReachComponentLike, ReachComponentWrapper}
import utopia.reach.component.wrapper.OpenComponent
import utopia.reach.container.multi.ViewStack
import utopia.reach.container.wrapper.CachingViewSwapper
import utopia.reach.container.wrapper.scrolling.ScrollView
import utopia.reach.context.{ReachContentWindowContext, ReachWindowContext}

import java.awt.event.KeyEvent
import scala.concurrent.ExecutionContext

object FieldWithSelectionPopup extends Gccff[ReachContentWindowContext, ContextualFieldWithSelectionPopupFactory]
{
	override def withContext[N <: ReachContentWindowContext](parentHierarchy: ComponentHierarchy, context: N) =
		ContextualFieldWithSelectionPopupFactory[N](parentHierarchy, context)
}

case class ContextualFieldWithSelectionPopupFactory[+N <: ReachContentWindowContext](parentHierarchy: ComponentHierarchy,
                                                                                     context: N)
	extends GenericContextualFactory[N, ReachContentWindowContext, ContextualFieldWithSelectionPopupFactory]
{
	private implicit val c: ReachContentWindowContext = context
	
	override def withContext[N2 <: ReachContentWindowContext](newContext: N2) =
		copy(context = newContext)
	
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
	  * @param makeNoOptionsView   An optional function used for constructing the view to display,
	 *                             when no options are available.
	 *                             Accepts 3 parameters:
	 *                             1) Component hierarchy,
	 *                             2) Component creation context, and
	 *                             3) Background color pointer
	 * @param makeAdditionalOption An optional function used for constructing an additional view that is presented
	 *                             under the main selection list. May be used, for example, for providing an "add" option.
	 *                             Accepts 3 parameters:
	 *                             1) Component hierarchy,
	 *                             2) Component creation context, and
	 *                             3) Background color pointer
	  * @param highlightStylePointer A pointer to an additional highlighting style applied to this field (default = always None)
	  * @param focusColorRole Color role used when this field has focus (default = Secondary)
	  * @param additionalActivationKeys Additional key-indices that should activate the pop-up.
	  *                                 By default, only the down arrow activates the pop-up.
	  *                                 Default = empty.
	  * @param sameItemCheck A function for checking whether two options represent the same instance (optional).
	  *                      Should only be specified when equality function (==) shouldn't be used.
	  * @param fillBackground Whether filled field style should be used (default = global default)
	  * @param makeField A function for creating the component inside teh main field.
	  *                  Accepts contextual data (specific context and context of this factory).
	  * @param makeDisplay A function for constructing new item option fields in the pop-up selection list.
	 *                     Accepts four values:
	 *                     1) A component hierarchy,
	 *                     2) Component creation context,
	 *                     3) Background color pointer
	 *                     4) Item to display initially
	 *                     Returns a properly initialized display
	  * @param makeRightHintLabel – A function for producing an additional right edge hint field.
	  *                           Accepts created main field and component creation context.
	  *                           Returns an open component or None if no label should be placed.
	  * @param scrollingContext Context used for the created scroll view
	  * @param exc Context used for parallel operations
	  * @param log Logger for various errors
	  * @tparam A Type of selectable item
	  * @tparam C Type of component inside the field
	  * @tparam D Type of component to display a selectable item
	  * @tparam P Type of content pointer used
	  * @return A new field
	  */
	def apply[A, C <: ReachComponentLike with Focusable, D <: ReachComponentLike with Refreshable[A],
		P <: Changing[Vector[A]]](isEmptyPointer: Changing[Boolean], contentPointer: P,
	                              valuePointer: PointerWithEvents[Option[A]] = new PointerWithEvents[Option[A]](None),
	                              rightExpandIcon: SingleColorIcon = SingleColorIcon.empty,
	                              rightCollapseIcon: SingleColorIcon = SingleColorIcon.empty,
	                              fieldNamePointer: Changing[LocalizedString] = LocalizedString.alwaysEmpty,
	                              promptPointer: Changing[LocalizedString] = LocalizedString.alwaysEmpty,
	                              hintPointer: Changing[LocalizedString] = LocalizedString.alwaysEmpty,
	                              errorMessagePointer: Changing[LocalizedString] = LocalizedString.alwaysEmpty,
	                              leftIconPointer: Changing[SingleColorIcon] = SingleColorIcon.alwaysEmpty,
	                              listLayout: StackLayout = Fit, listCap: StackLength = StackLength.fixedZero,
	                              makeNoOptionsView: Option[(ComponentHierarchy, TextContext, Changing[Color]) => ReachComponentLike] = None,
	                              makeAdditionalOption: Option[(ComponentHierarchy, TextContext, Changing[Color]) => ReachComponentLike] = None,
	                              highlightStylePointer: Changing[Option[ColorRole]] = Fixed(None),
	                              focusColorRole: ColorRole = Secondary,
	                              additionalActivationKeys: Set[Int] = Set(),
	                              sameItemCheck: Option[EqualsFunction[A]] = None,
	                              fillBackground: Boolean = ComponentCreationDefaults.useFillStyleFields)
	                             (makeField: (FieldCreationContext, N) => C)
	                             (makeDisplay: (ComponentHierarchy, TextContext, Changing[Color], A) => D)
	                             (makeRightHintLabel: (ExtraFieldCreationContext[C], N) =>
										 Option[OpenComponent[ReachComponentLike, Any]])
	                             (implicit scrollingContext: ScrollingContext, exc: ExecutionContext, log: Logger) =
		new FieldWithSelectionPopup[A, C, D, P, N](parentHierarchy, context, isEmptyPointer, contentPointer,
			valuePointer, rightExpandIcon, rightCollapseIcon, fieldNamePointer, promptPointer, hintPointer,
			errorMessagePointer, leftIconPointer, listLayout, listCap, makeNoOptionsView, makeAdditionalOption,
			highlightStylePointer, focusColorRole, additionalActivationKeys, sameItemCheck,
			fillBackground)(makeField)(makeDisplay)(makeRightHintLabel)
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
  * @param makeNoOptionsView An optional function used for constructing the view to display,
 *                          when no options are available.
 *                          Accepts 3 parameters:
 *                              1) Component hierarchy,
 *                              2) Component creation context, and
 *                              3) Background color pointer
  * @param makeAdditionalOption An optional function used for constructing an additional view that is presented
 *                             under the main selection list. May be used, for example, for providing an "add" option.
 *                              Accepts 3 parameters:
 *                                  1) Component hierarchy,
 *                                  2) Component creation context, and
 *                                  3) Background color pointer
 * @param highlightStylePointer A pointer to an additional highlighting style applied to this field (default = always None)
  * @param focusColorRole Color role used when this field has focus (default = Secondary)
  * @param additionalActivationKeys Additional key-indices that should activate the pop-up.
  *                                 By default, only the down arrow activates the pop-up.
  *                                 Default = empty.
  * @param sameItemCheck A function for checking whether two options represent the same instance (optional).
  *                      Should only be specified when equality function (==) shouldn't be used.
  * @param fillBackground Whether filled field style should be used (default = global default)
  * @param makeField          A function for creating the component inside teh main field.
  *                           Accepts contextual data (specific context and context of this factory).
  * @param makeDisplay       A function for constructing new item option fields in the pop-up selection list.
 *                           Accepts four values:
 *                              1) A component hierarchy,
 *                              2) Component creation context,
 *                              3) Background color pointer
 *                              4) Item to display initially
 *                            Returns a properly initialized display
  * @param makeRightHintLabel – A function for producing an additional right edge hint field.
  *                           Accepts created main field and component creation context.
  *                           Returns an open component or None if no label should be placed.
  * @param popupContext       Context that is used for the created pop-up windows.
  * @param scrollingContext   Context used for the created scroll view
  * @param exc                Context used for parallel operations
  * @param log                Logger for various errors
  * @tparam A Type of selectable item
  * @tparam C Type of component inside the field
  * @tparam D Type of component to display a selectable item
  * @tparam P Type of content pointer used
  */
class FieldWithSelectionPopup[A, C <: ReachComponentLike with Focusable, D <: ReachComponentLike with Refreshable[A],
	+P <: Changing[Vector[A]], +N <: TextContext]
(parentHierarchy: ComponentHierarchy, context: N, isEmptyPointer: Changing[Boolean], override val contentPointer: P,
 override val valuePointer: PointerWithEvents[Option[A]] = new PointerWithEvents[Option[A]](None),
 rightExpandIcon: SingleColorIcon = SingleColorIcon.empty, rightCollapseIcon: SingleColorIcon = SingleColorIcon.empty,
 fieldNamePointer: Changing[LocalizedString] = LocalizedString.alwaysEmpty,
 promptPointer: Changing[LocalizedString] = LocalizedString.alwaysEmpty,
 hintPointer: Changing[LocalizedString] = LocalizedString.alwaysEmpty,
 errorMessagePointer: Changing[LocalizedString] = LocalizedString.alwaysEmpty,
 leftIconPointer: Changing[SingleColorIcon] = SingleColorIcon.alwaysEmpty, listLayout: StackLayout = Fit,
 listCap: StackLength = StackLength.fixedZero,
 makeNoOptionsView: Option[(ComponentHierarchy, TextContext, Changing[Color]) => ReachComponentLike] = None,
 makeAdditionalOption: Option[(ComponentHierarchy, TextContext, Changing[Color]) => ReachComponentLike] = None,
 highlightStylePointer: Changing[Option[ColorRole]] = Fixed(None), focusColorRole: ColorRole = Secondary,
 additionalActivationKeys: Set[Int] = Set(), sameItemCheck: Option[EqualsFunction[A]] = None,
 fillBackground: Boolean = ComponentCreationDefaults.useFillStyleFields)
(makeField: (FieldCreationContext, N) => C)
(makeDisplay: (ComponentHierarchy, TextContext, Changing[Color], A) => D)
(makeRightHintLabel: (ExtraFieldCreationContext[C], N) => Option[OpenComponent[ReachComponentLike, Any]])
(implicit popupContext: ReachWindowContext, scrollingContext: ScrollingContext, exc: ExecutionContext, log: Logger)
	extends ReachComponentWrapper with FocusableWithPointerWrapper
		with SelectionWithPointers[Option[A], PointerWithEvents[Option[A]], Vector[A], P]
{
	// ATTRIBUTES	------------------------------
	
	private implicit val equals: EqualsFunction[A] = sameItemCheck.getOrElse(EqualsFunction.default)
	
	// Tracks the last selected value in order to return selection when content is updated
	private val lastSelectedValuePointer = Pointer.empty[A]()
	
	private val lazyPopup = ListenableResettableLazy[Window] { createPopup() }
	// Follows the pop-up visibility state with a pointer
	/**
	  * A pointer which shows whether a pop-up is being displayed
	  */
	private val popUpVisiblePointer = lazyPopup.stateView.flatMap {
		case Some(window) => window.fullyVisibleFlag
		case None => AlwaysFalse
	}
	// Merges the expand and the collapse icons, if necessary
	private val rightIconPointer: Changing[SingleColorIcon] = rightExpandIcon.notEmpty match {
		case Some(expandIcon) =>
			rightCollapseIcon.notEmpty match {
				case Some(collapseIcon) =>
					// Makes sure both icons have the same size
					if (expandIcon.size == collapseIcon.size)
						popUpVisiblePointer.map { visible => if (visible) collapseIcon else expandIcon }
					else {
						val (smaller, larger) = Pair(expandIcon, collapseIcon).minMaxBy { _.size.area }.toTuple
						val targetSize = smaller.size
						val shrankIcon = SingleColorIcon(
							larger.original.fittingWithin(targetSize).paintedToCanvas(targetSize), larger.standardSize)
						
						val (newExpandIcon, newCollapseIcon) =
							if (smaller == expandIcon) smaller -> shrankIcon else shrankIcon -> smaller
						popUpVisiblePointer.map { visible => if (visible) newCollapseIcon else newExpandIcon }
					}
				case None => Fixed(expandIcon)
			}
		case None => Fixed(rightCollapseIcon)
	}
	
	/**
	  * Field wrapped by this field
	  */
	val field = Field(parentHierarchy).withContext(context)
		.apply(isEmptyPointer, fieldNamePointer, promptPointer, hintPointer, errorMessagePointer, leftIconPointer,
			rightIconPointer, highlightStylePointer = highlightStylePointer, focusColorRole = focusColorRole,
			fillBackground = fillBackground)(makeField)(makeRightHintLabel)
	
	
	// INITIAL CODE	-----------------------------
	
	// Disposes of the pop-up when this component is removed from the main stack hierarchy
	// Also manages content and selection while attached
	addHierarchyListener { isAttached =>
		// Tracks last selected value in order to return in on content changes
		val updateLastValueListener = ChangeListener[Option[A]] { e =>
			if (e.newValue.isEmpty)
				lastSelectedValuePointer.value = e.oldValue
			else
				lastSelectedValuePointer.clear()
			DetachmentChoice.continue
		}
		// May deselect the current value or select the previously selected value on content changes
		val contentUpdateListener = ChangeListener[Vector[A]] { e =>
			valuePointer.update {
				// Case: No value currently selected => Attempts to select the previously selected value
				case None => lastSelectedValuePointer.value.filter { e.newValue.containsEqual(_) }
				// Case: Value is selected => Deselects it if it no longer appears among the options
				case s: Some[A] => s.filter { e.newValue.containsEqual(_) }
			}
			DetachmentChoice.continue
		}
		if (isAttached) {
			valuePointer.addListenerAndSimulateEvent(None)(updateLastValueListener)
			contentPointer.addListenerAndSimulateEvent(Vector())(contentUpdateListener)
			GlobalKeyboardEventHandler += FieldKeyListener
			GlobalMouseEventHandler += PopupHideMouseListener
		}
		else {
			GlobalMouseEventHandler -= PopupHideMouseListener
			GlobalKeyboardEventHandler -= FieldKeyListener
			contentPointer.removeListener(contentUpdateListener)
			valuePointer.removeListener(updateLastValueListener)
			lazyPopup.popCurrent().foreach { _.close() }
		}
	}
	
	// When gains focus, displays the pop-up. Hides the pop-up when focus is lost.
	focusPointer.addContinuousListenerAndSimulateEvent(false) { e =>
		if (e.newValue) openPopup() else cachedPopup.foreach { _.visible = false }
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
	def openPopup() = lazyPopup.value.display()
	
	private def createPopup(): Window = {
		// Creates the pop-up
		val popup = field.createOwnedWindow(Bottom, matchEdgeLength = true) { hierarchy =>
			// The pop-up content resides in a scroll view with custom background drawing
			ScrollView(hierarchy).build(Mixed).apply(scrollBarMargin = Size(context.margins.small, listCap.optimal),
				limitsToContentSize = true,
				customDrawers = Vector(BackgroundViewDrawer(field.innerBackgroundPointer))) { factories =>
				// The scrollable content consists of either:
				//  1) Main content + additional view, or
				//  2) Main content only
				def makeOptionsList(factory: SelectionListFactory) =
					factory.apply(context.actorHandler, field.innerBackgroundPointer, contentPointer, valuePointer, Y,
						listLayout, context.stackMargin, listCap, 1.0, sameItemCheck) { (hierarchy, item) =>
						makeDisplay(hierarchy, context.against(field.innerBackgroundPointer.value),
							field.innerBackgroundPointer, item)
					}
				def makeMainContent(factories: Mixed) = {
					// The main content is either:
					//   1) Switchable between options and no-options -view
					//   2) Only the options view
					makeNoOptionsView match {
						// Case: No options -view used => Switches between the two views
						case Some(makeNoOptionsView) =>
							factories(CachingViewSwapper).build(Mixed)
								.generic(contentPointer.map { _.isEmpty }) { (factories, isEmpty: Boolean) =>
									// Case: No options -view constructor
									if (isEmpty)
										makeNoOptionsView(factories.parentHierarchy,
											context.against(field.innerBackgroundPointer.value),
											field.innerBackgroundPointer)
									// Case: List constructor
									else
										makeOptionsList(factories(SelectionList))
								}
						// Case: No no options -view used => Always displays the selection list
						case None => makeOptionsList(factories(SelectionList))
					}
				}
				makeAdditionalOption match {
					// Case: Additional view used => Places it below the main content
					case Some(makeAdditionalOption) =>
						factories(ViewStack).withoutMargin.build(Mixed) { factories =>
							val mainContent = makeMainContent(factories.next())
							val additional = makeAdditionalOption(factories.next().parentHierarchy,
								context.against(field.innerBackgroundPointer.value), field.innerBackgroundPointer)
							// The main content may be hidden, if empty
							val mainContentVisiblePointer = {
								if (makeNoOptionsView.isDefined) AlwaysTrue else contentPointer.map { _.nonEmpty }
							}
							Vector(mainContent -> mainContentVisiblePointer, additional -> AlwaysTrue)
						}
					// CAse: No additional view used => Always displays the main content
					case None => makeMainContent(factories)
				}
			}
		}
		// When the mouse is released, hides the pop-up
		// Also hides when not in focus, and on some key-presses
		popup.focusKeyStateHandler += new PopupKeyListener(popup)
		popup.focusedFlag.addListener { e =>
			if (!e.newValue)
				popup.visible = false
			DetachmentChoice.continueUntil(popup.hasClosed)
		}
		// Returns the pop-up window
		popup.window
	}
	
	
	// NESTED	---------------------------------
	
	private object PopupHideMouseListener extends MouseButtonStateListener
	{
		// ATTRIBUTES   ----------------------
		
		override lazy val mouseButtonStateEventFilter = MouseButtonStateEvent.leftButtonFilter
		
		// Only closes the pop-up on mouse release if it was visible on the previous mouse press
		private val closeOnReleaseFlag = ResettableFlag()
		
		
		// IMPLEMENTED  ----------------------
		
		override def onMouseButtonState(event: MouseButtonStateEvent): Option[ConsumeEvent] = {
			// Case: Mouse press => Saves the pop-up status in order to react correctly to the next mouse release
			if (event.isDown) {
				closeOnReleaseFlag.value = cachedPopup.exists { _.isFullyVisible }
				None
			}
			// Case: Mouse release => Hides the pop-up if it was visible when the mouse was pressed
			else if (closeOnReleaseFlag.reset()) {
				cachedPopup.foreach { _.visible = false }
				Some(ConsumeEvent("Pop-up closing"))
			}
			else
				None
		}
		
		override def allowsHandlingFrom(handlerType: HandlerType): Boolean = true
	}
	
	private object FieldKeyListener extends KeyStateListener
	{
		// ATTRIBUTES	-------------------------
		
		// Listens to down arrow presses
		// Also supports additional key-strokes (based on the 'additionalActivationKeys' parameter)
		override val keyStateEventFilter = {
			val arrowFilter = KeyStateEvent.arrowKeyFilter(Down)
			val keyFilter = NotEmpty(additionalActivationKeys) match {
				case Some(keys) => arrowFilter || { e: KeyStateEvent => keys.exists(e.keyStatus.apply) }
				case None => arrowFilter
			}
			KeyStateEvent.wasPressedFilter && keyFilter
		}
		
		
		// IMPLEMENTED	-------------------------
		
		override def onKeyState(event: KeyStateEvent) = openPopup()
		
		// Is interested in key events while the field has focus and pop-up is not open
		override def allowsHandlingFrom(handlerType: HandlerType) = !popUpVisiblePointer.value && field.hasFocus
	}
	
	private class PopupKeyListener(popup: Window) extends KeyStateListener
	{
		// ATTRIBUTES	-------------------------
		
		// Listens to enter and tabulator presses
		override val keyStateEventFilter = KeyStateEvent.wasPressedFilter &&
			KeyStateEvent.keysFilter(KeyEvent.VK_TAB, KeyEvent.VK_ENTER, KeyEvent.VK_ESCAPE, KeyEvent.VK_SPACE)
		
		
		// IMPLEMENTED	-------------------------
		
		override def onKeyState(event: KeyStateEvent) = {
			// Hides the pop-up
			popup.visible = false
			// On tabulator press, yields focus afterwards
			if (event.index == KeyEvent.VK_TAB)
				Delay(0.1.seconds) { yieldFocus(if (event.keyStatus.shift) Negative else Positive) }
		}
		
		// Only reacts to events while the pop-up is visible
		override def allowsHandlingFrom(handlerType: HandlerType) = popup.isFullyVisible
	}
}
