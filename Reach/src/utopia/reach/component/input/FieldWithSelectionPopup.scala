package utopia.reach.component.input

import utopia.firmament.component.Window
import utopia.firmament.component.display.Refreshable
import utopia.firmament.component.input.SelectionWithPointers
import utopia.firmament.context.{ScrollingContext, TextContext}
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.drawing.view.BackgroundViewDrawer
import utopia.firmament.image.SingleColorIcon
import utopia.firmament.localization.LocalizedString
import utopia.firmament.model.enumeration.{SizeCategory, StackLayout}
import utopia.firmament.model.stack.StackLength
import utopia.flow.async.process.Delay
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeResponse
import utopia.flow.operator.Identity
import utopia.flow.operator.enumeration.End
import utopia.flow.operator.enumeration.End.First
import utopia.flow.operator.equality.EqualsFunction
import utopia.flow.operator.sign.Sign.{Negative, Positive}
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.NotEmpty
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.eventful.{AlwaysFalse, AlwaysTrue, Fixed}
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.caching.ListenableResettableLazy
import utopia.flow.view.mutable.eventful.{EventfulPointer, ResettableFlag}
import utopia.flow.view.template.eventful.{Changing, FlagLike}
import utopia.genesis.handling.event.consume.ConsumeChoice.{Consume, Preserve}
import utopia.genesis.handling.event.keyboard.Key.{Enter, Esc, Shift, Space, Tab}
import utopia.genesis.handling.event.keyboard.{Key, KeyStateEvent, KeyStateListener, KeyboardEvents}
import utopia.genesis.handling.event.mouse.{CommonMouseEvents, MouseButtonStateEvent2, MouseButtonStateListener2}
import utopia.paradigm.color.ColorRole
import utopia.paradigm.enumeration.{Alignment, Axis2D}
import utopia.reach.component.factory.contextual.VariableContextualFactory
import utopia.reach.component.factory.{FromContextComponentFactoryFactory, Mixed}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.input.FieldWithSelectionPopup.ignoreFocusAfterCloseDuration
import utopia.reach.component.input.selection.{SelectionList, SelectionListFactory, SelectionListSettings}
import utopia.reach.component.label.image.ViewImageLabelSettings
import utopia.reach.component.template.focus.{Focusable, FocusableWithPointerWrapper}
import utopia.reach.component.template.{ReachComponentLike, ReachComponentWrapper}
import utopia.reach.component.wrapper.OpenComponent
import utopia.reach.container.multi.{StackSettings, ViewStack}
import utopia.reach.container.wrapper.CachingViewSwapper
import utopia.reach.container.wrapper.scrolling.ScrollView
import utopia.reach.context.ReachContentWindowContext

import scala.concurrent.ExecutionContext

/**
  * Common trait for field with selection popup factories and settings
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 02.06.2023, v1.1
  */
trait FieldWithSelectionPopupSettingsLike[+Repr] extends FieldSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Wrapped more generic field settings
	  */
	def fieldSettings: FieldSettings
	/**
	  * Settings that apply to the opened selection list
	  */
	def listSettings: SelectionListSettings
	
	/**
	  * The expand (first) and the collapse icon (second) that should be displayed at the right side of the
	  * created fields.
	  * Please note that a non-empty right-side icon will override these values.
	  */
	def expandAndCollapseIcon: Pair[SingleColorIcon]
	/**
	  * Additional keys that open the pop-up when they are pressed while this field is in focus.
	  * By default, only the appropriate arrow key opens the pop-up.
	  */
	def activationKeys: Set[Key]
	/**
	  * @return A function used for modifying the pop-up context from that accepted by this factory
	  */
	def popupContextMod: ReachContentWindowContext => ReachContentWindowContext
	/**
	  * A function used for constructing a view to display when no options are selectable
	  */
	def noOptionsViewConstructor: Option[(ComponentHierarchy, Changing[TextContext]) => ReachComponentLike]
	/**
	  * A function used for constructing an additional selectable view to display
	  */
	def extraOptionConstructor: Option[(ComponentHierarchy, Changing[TextContext]) => ReachComponentLike]
	/**
	  * The location where the extra option should be placed, if one has been specified
	  */
	def extraOptionLocation: End
	/**
	  * Size of the margins to place between the selectable items in the pop-up.
	  * None if no margin should be placed.
	  */
	def listMargin: Option[SizeCategory]
	
	/**
	  * Additional key-indices that open the pop-up when they are pressed while this field is in focus.
	  * By default, only the appropriate arrow key opens the pop-up.
	  * @param keys New activation keys to use.
	  *             Additional key-indices that open the pop-up when they are pressed while this field is in focus.
	  *             By default, only the appropriate arrow key opens the pop-up.
	  * @return Copy of this factory with the specified activation keys
	  */
	def withActivationKeys(keys: Set[Key]): Repr
	/**
	  * The expand (first) and the collapse icon (second) that should be displayed at the right side of the
	  * created fields.
	  * Please note that a non-empty right-side icon will override these values.
	  * @param icons New expand and collapse icon to use.
	  *              The expand (first) and the collapse icon (second) that should be displayed at the right side of the
	  *              created fields.
	  *              Please note that a non-empty right-side icon will override these values.
	  * @return Copy of this factory with the specified expand and collapse icon
	  */
	def withExpandAndCollapseIcon(icons: Pair[SingleColorIcon]): Repr
	/**
	  * @param f A function used for modifying the pop-up context from that accepted by this factory
	  * @return Copy of this factory that uses the specified modifying function
	  */
	def withPopupContextMod(f: ReachContentWindowContext => ReachContentWindowContext): Repr
	/**
	  * A function used for constructing an additional selectable view to display
	  * @param f New extra option constructor to use.
	  *          A function used for constructing an additional selectable view to display
	  * @return Copy of this factory with the specified extra option constructor
	  */
	def withExtraOptionConstructor(f: (ComponentHierarchy, Changing[TextContext]) => ReachComponentLike): Repr
	/**
	  * The location where the extra option should be placed, if one has been specified
	  * @param location New extra option location to use.
	  *                 The location where the extra option should be placed, if one has been specified
	  * @return Copy of this factory with the specified extra option location
	  */
	def withExtraOptionLocation(location: End): Repr
	/**
	  * Wrapped more generic field settings
	  * @param settings New field settings to use.
	  *                 Wrapped more generic field settings
	  * @return Copy of this factory with the specified field settings
	  */
	def withFieldSettings(settings: FieldSettings): Repr
	/**
	  * Settings that apply to the opened selection list
	  * @param settings New list settings to use.
	  *                 Settings that apply to the opened selection list
	  * @return Copy of this factory with the specified list settings
	  */
	def withListSettings(settings: SelectionListSettings): Repr
	/**
	  * A function used for constructing a view to display when no options are selectable
	  * @param f New no options view constructor to use.
	  *          A function used for constructing a view to display when no options are selectable
	  * @return Copy of this factory with the specified no options view constructor
	  */
	def withNoOptionsViewConstructor(f: (ComponentHierarchy, Changing[TextContext]) => ReachComponentLike): Repr
	/**
	  * Size of the margins to place between the selectable items in the pop-up.
	  * None if no margin should be placed.
	  * @param margin New list margin to use.
	  *               Size of the margins to place between the selectable items in the pop-up.
	  *               None if no margin should be placed.
	  * @return Copy of this factory with the specified list margin
	  */
	def withListMargin(margin: Option[SizeCategory]): Repr
	
	
	// COMPUTED	--------------------
	
	/**
	  * stack settings from the wrapped selection list settings
	  */
	def listStackSettings = listSettings.stackSettings
	/**
	  * highlight modifier from the wrapped selection list settings
	  */
	def listHighlightModifier = listSettings.highlightModifier
	
	def listAxis = listStackSettings.axis
	def listLayout = listStackSettings.layout
	def listCap = listStackSettings.cap
	def listCustomDrawers = listStackSettings.customDrawers
	
	/**
	  * @return Copy of this factory that doesn't place any margin between the list items
	  */
	def withoutListMargin = withListMargin(None)
	
	
	// IMPLEMENTED	--------------------
	
	override def errorMessagePointer = fieldSettings.errorMessagePointer
	override def fieldNamePointer = fieldSettings.fieldNamePointer
	override def fillBackground = fieldSettings.fillBackground
	override def focusColorRole = fieldSettings.focusColorRole
	override def highlightPointer = fieldSettings.highlightPointer
	override def hintPointer = fieldSettings.hintPointer
	override def hintScaleFactor = fieldSettings.hintScaleFactor
	override def iconPointers = fieldSettings.iconPointers
	override def imageSettings = fieldSettings.imageSettings
	override def promptPointer = fieldSettings.promptPointer
	
	override def withErrorMessagePointer(p: Changing[LocalizedString]) =
		withFieldSettings(fieldSettings.withErrorMessagePointer(p))
	override def withFieldNamePointer(p: Changing[LocalizedString]) =
		withFieldSettings(fieldSettings.withFieldNamePointer(p))
	override def withFillBackground(fill: Boolean) = withFieldSettings(fieldSettings.withFillBackground(fill))
	override def withFocusColorRole(color: ColorRole) = withFieldSettings(fieldSettings.withFocusColorRole(color))
	override def withHighlightPointer(p: Changing[Option[ColorRole]]) =
		withFieldSettings(fieldSettings.withHighlightPointer(p))
	override def withHintPointer(p: Changing[LocalizedString]) =
		withFieldSettings(fieldSettings.withHintPointer(p))
	override def withHintScaleFactor(scaling: Double) =
		withFieldSettings(fieldSettings.withHintScaleFactor(scaling))
	override def withIconPointers(pointers: Pair[Changing[SingleColorIcon]]) =
		withFieldSettings(fieldSettings.withIconPointers(pointers))
	override def withImageSettings(settings: ViewImageLabelSettings) =
		withFieldSettings(fieldSettings.withImageSettings(settings))
	override def withPromptPointer(p: Changing[LocalizedString]) =
		withFieldSettings(fieldSettings.withPromptPointer(p))
	
	
	// OTHER	--------------------
	
	def mapActivationKeys(f: Set[Key] => Set[Key]) = withActivationKeys(f(activationKeys))
	def mapExpandAndCollapseIcon(f: Pair[SingleColorIcon] => Pair[SingleColorIcon]) =
		withExpandAndCollapseIcon(f(expandAndCollapseIcon))
	def mapFieldSettings(f: FieldSettings => FieldSettings) = withFieldSettings(f(fieldSettings))
	def mapListHighlightModifier(f: Double => Double) = withListHighlightModifier(f(listHighlightModifier))
	def mapListSettings(f: SelectionListSettings => SelectionListSettings) = withListSettings(f(listSettings))
	def mapListStackSettings(f: StackSettings => StackSettings) = withListStackSettings(f(listStackSettings))
	
	/**
	  * @param modifier A modifier that is applied to the color highlighting used in this component.
	  *                 1.0 signifies the default color highlighting.
	  * @return Copy of this factory with the specified list highlight modifier
	  */
	def withListHighlightModifier(modifier: Double) =
		withListSettings(listSettings.withHighlightModifier(modifier))
	/**
	  * @param settings Settings that affect the stack layout of this list
	  * @return Copy of this factory with the specified list stack settings
	  */
	def withListStackSettings(settings: StackSettings) =
		withListSettings(listSettings.withStackSettings(settings))
	
	def withListAxis(axis: Axis2D) = mapListStackSettings { _.withAxis(axis) }
	def withListLayout(layout: StackLayout) = mapListStackSettings { _.withLayout(layout) }
	def withListCap(cap: StackLength) = mapListStackSettings { _.withCap(cap) }
	def withListCustomDrawers(drawers: Vector[CustomDrawer]) = mapListStackSettings { _.withCustomDrawers(drawers) }
	
	def withAdditionalActivationKeys(keys: IterableOnce[Key]) = mapActivationKeys { _ ++ keys }
	def activatedWithKey(key: Key) = mapActivationKeys { _ + key }
	
	def withListMargin(margin: SizeCategory): Repr = withListMargin(Some(margin))
}

object FieldWithSelectionPopupSettings
{
	// ATTRIBUTES	--------------------
	
	val default = apply()
}

/**
  * Combined settings used when constructing field with selection popups
  * @param fieldSettings            Wrapped more generic field settings
  * @param listSettings             Settings that apply to the opened selection list
  * @param expandAndCollapseIcon    The expand (first) and the collapse icon (second) that should be displayed at the
  *                                 right side of the created fields.
  *                                 Please note that a non-empty right-side icon will override these values.
  * @param listMargin Size of the margins to place between the selectable items in the pop-up.
  *                   None if no margin should be placed.
  * @param activationKeys           Additional keys that open the pop-up when they are pressed while this field is
  *                                 in focus.
  *                                 By default, only the appropriate arrow key opens the pop-up.
  * @param noOptionsViewConstructor A function used for constructing a view to display when no
  *                                 options are selectable
  * @param extraOptionConstructor   A function used for constructing an additional selectable view to display
  * @param extraOptionLocation      The location where the extra option should be placed, if one has been specified
  * @author Mikko Hilpinen
  * @since 02.06.2023, v1.1
  */
case class FieldWithSelectionPopupSettings(fieldSettings: FieldSettings = FieldSettings.default,
                                           listSettings: SelectionListSettings = SelectionListSettings.default,
                                           expandAndCollapseIcon: Pair[SingleColorIcon] = Pair.twice(SingleColorIcon.empty),
                                           listMargin: Option[SizeCategory] = Some(SizeCategory.Small),
                                           activationKeys: Set[Key] = Set[Key](),
                                           popupContextMod: ReachContentWindowContext => ReachContentWindowContext = Identity,
                                           noOptionsViewConstructor: Option[(ComponentHierarchy, Changing[TextContext]) => ReachComponentLike] = None,
                                           extraOptionConstructor: Option[(ComponentHierarchy, Changing[TextContext]) => ReachComponentLike] = None,
                                           extraOptionLocation: End = End.Last)
	extends FieldWithSelectionPopupSettingsLike[FieldWithSelectionPopupSettings]
{
	// IMPLEMENTED	--------------------
	
	override def withActivationKeys(keys: Set[Key]) = copy(activationKeys = keys)
	override def withExpandAndCollapseIcon(icons: Pair[SingleColorIcon]) = copy(expandAndCollapseIcon = icons)
	override def withExtraOptionLocation(location: End) = copy(extraOptionLocation = location)
	override def withFieldSettings(settings: FieldSettings) = copy(fieldSettings = settings)
	override def withListSettings(settings: SelectionListSettings) = copy(listSettings = settings)
	override def withExtraOptionConstructor(f: (ComponentHierarchy, Changing[TextContext]) => ReachComponentLike): FieldWithSelectionPopupSettings =
		copy(extraOptionConstructor = Some(f))
	override def withNoOptionsViewConstructor(f: (ComponentHierarchy, Changing[TextContext]) => ReachComponentLike): FieldWithSelectionPopupSettings =
		copy(noOptionsViewConstructor = Some(f))
	override def withPopupContextMod(f: ReachContentWindowContext => ReachContentWindowContext): FieldWithSelectionPopupSettings =
		copy(popupContextMod = f)
	override def withListMargin(margin: Option[SizeCategory]) = copy(listMargin = margin)
}

/**
  * Common trait for factories that wrap a field with selection popup settings instance
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 02.06.2023, v1.1
  */
trait FieldWithSelectionPopupSettingsWrapper[+Repr] extends FieldWithSelectionPopupSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Settings wrapped by this instance
	  */
	protected def settings: FieldWithSelectionPopupSettings
	
	/**
	  * @return Copy of this factory with the specified settings
	  */
	def withSettings(settings: FieldWithSelectionPopupSettings): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def activationKeys = settings.activationKeys
	override def expandAndCollapseIcon = settings.expandAndCollapseIcon
	override def extraOptionConstructor = settings.extraOptionConstructor
	override def extraOptionLocation = settings.extraOptionLocation
	override def fieldSettings = settings.fieldSettings
	override def listSettings = settings.listSettings
	override def noOptionsViewConstructor = settings.noOptionsViewConstructor
	override def popupContextMod: ReachContentWindowContext => ReachContentWindowContext = settings.popupContextMod
	override def listMargin: Option[SizeCategory] = settings.listMargin
	
	override def withListMargin(margin: Option[SizeCategory]): Repr = mapSettings { _.withListMargin(margin) }
	override def withActivationKeys(keys: Set[Key]) = mapSettings { _.withActivationKeys(keys) }
	override def withExpandAndCollapseIcon(icons: Pair[SingleColorIcon]) =
		mapSettings { _.withExpandAndCollapseIcon(icons) }
	override def withExtraOptionLocation(location: End) = mapSettings { _.withExtraOptionLocation(location) }
	override def withFieldSettings(settings: FieldSettings) = mapSettings { _.withFieldSettings(settings) }
	override def withListSettings(settings: SelectionListSettings) =
		mapSettings { _.withListSettings(settings) }
	override def withExtraOptionConstructor(f: (ComponentHierarchy, Changing[TextContext]) => ReachComponentLike): Repr =
		mapSettings { _.withExtraOptionConstructor(f) }
	override def withNoOptionsViewConstructor(f: (ComponentHierarchy, Changing[TextContext]) => ReachComponentLike): Repr =
		mapSettings { _.withNoOptionsViewConstructor(f) }
	override def withPopupContextMod(f: ReachContentWindowContext => ReachContentWindowContext): Repr =
		mapSettings { _.withPopupContextMod(f) }
	
	
	// OTHER	--------------------
	
	def mapSettings(f: FieldWithSelectionPopupSettings => FieldWithSelectionPopupSettings) =
		withSettings(f(settings))
}

/**
  * Factory class used for constructing field
  * with selection popups using contextual component creation information
  * @author Mikko Hilpinen
  * @since 02.06.2023, v1.1
  */
case class ContextualFieldWithSelectionPopupFactory(parentHierarchy: ComponentHierarchy,
                                                    contextPointer: Changing[ReachContentWindowContext],
                                                    settings: FieldWithSelectionPopupSettings = FieldWithSelectionPopupSettings.default)
	extends FieldWithSelectionPopupSettingsWrapper[ContextualFieldWithSelectionPopupFactory]
		with VariableContextualFactory[ReachContentWindowContext, ContextualFieldWithSelectionPopupFactory]
{
	// IMPLEMENTED	--------------------
	
	override def withContextPointer(contextPointer: Changing[ReachContentWindowContext]) =
		copy(contextPointer = contextPointer)
	override def withSettings(settings: FieldWithSelectionPopupSettings) =
		copy(settings = settings)
	
	
	// OTHER    ------------------------
	
	/**
	  * Creates a new field that utilizes a selection pop-up
	  * @param isEmptyPointer A pointer that contains true when the wrapped field is empty (of text)
	  * @param contentPointer Pointer to the available options in this field
	  * @param valuePointer Pointer to the currently selected option, if any (default = new empty pointer)
	  * @param sameItemCheck A function for checking whether two options represent the same instance (optional).
	  *                      Should only be specified when equality function (==) shouldn't be used.
	  * @param makeField A function for creating the component inside the main field.
	  *                  Accepts contextual data.
	  * @param makeDisplay A function for constructing new item option fields in the pop-up selection list.
	 *                     Accepts three values:
	 *                     1) A component hierarchy,
	 *                     2) Component creation context (pointer)
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
	                              valuePointer: EventfulPointer[Option[A]] = EventfulPointer.empty(),
	                              sameItemCheck: Option[EqualsFunction[A]] = None)
	                             (makeField: FieldCreationContext => C)
	                             (makeDisplay: (ComponentHierarchy, Changing[TextContext], A) => D)
	                             (makeRightHintLabel: ExtraFieldCreationContext[C] =>
										 Option[OpenComponent[ReachComponentLike, Any]])
	                             (implicit scrollingContext: ScrollingContext, exc: ExecutionContext, log: Logger) =
		new FieldWithSelectionPopup[A, C, D, P](parentHierarchy, contextPointer, isEmptyPointer, contentPointer,
			valuePointer, settings, sameItemCheck)(makeField)(makeDisplay)(makeRightHintLabel)
}

/**
  * Used for defining field with selection popup creation settings outside of the component building process
  * @author Mikko Hilpinen
  * @since 02.06.2023, v1.1
  */
case class FieldWithSelectionPopupSetup(settings: FieldWithSelectionPopupSettings = FieldWithSelectionPopupSettings.default)
	extends FieldWithSelectionPopupSettingsWrapper[FieldWithSelectionPopupSetup]
		with FromContextComponentFactoryFactory[ReachContentWindowContext, ContextualFieldWithSelectionPopupFactory]
{
	// IMPLEMENTED	--------------------
	
	override def withContext(hierarchy: ComponentHierarchy, context: ReachContentWindowContext) =
		ContextualFieldWithSelectionPopupFactory(hierarchy, Fixed(context), settings)
	
	override def withSettings(settings: FieldWithSelectionPopupSettings) = copy(settings = settings)
	
	
	// OTHER	--------------------
	
	/**
	  * @return A new field with selection popup factory that uses the specified (variable) context
	  */
	def withContext(hierarchy: ComponentHierarchy, context: Changing[ReachContentWindowContext]) =
		ContextualFieldWithSelectionPopupFactory(hierarchy, context, settings)
}

object FieldWithSelectionPopup extends FieldWithSelectionPopupSetup()
{
	// ATTRIBUTES   ----------------
	
	private val ignoreFocusAfterCloseDuration = 0.2.seconds
	
	
	// OTHER	--------------------
	
	def apply(settings: FieldWithSelectionPopupSettings) = withSettings(settings)
}

/**
  * A field wrapper class that displays a selection pop-up when it receives focus
  * @author Mikko Hilpinen
  * @since 22.12.2020, v0.1
  * @tparam A Type of selectable item
  * @tparam C Type of component inside the field
  * @tparam D Type of component to display a selectable item
  * @tparam P Type of content pointer used
  */
class FieldWithSelectionPopup[A, C <: ReachComponentLike with Focusable, D <: ReachComponentLike with Refreshable[A],
	+P <: Changing[Vector[A]]]
(parentHierarchy: ComponentHierarchy, contextPointer: Changing[ReachContentWindowContext],
 isEmptyPointer: Changing[Boolean], override val contentPointer: P,
 override val valuePointer: EventfulPointer[Option[A]] = EventfulPointer.empty(),
 settings: FieldWithSelectionPopupSettings = FieldWithSelectionPopupSettings.default,
 sameItemCheck: Option[EqualsFunction[A]] = None)
(makeField: FieldCreationContext => C)
(makeDisplay: (ComponentHierarchy, Changing[TextContext], A) => D)
(makeRightHintLabel: ExtraFieldCreationContext[C] => Option[OpenComponent[ReachComponentLike, Any]])
(implicit scrollingContext: ScrollingContext, exc: ExecutionContext, log: Logger)
	extends ReachComponentWrapper with FocusableWithPointerWrapper
		with SelectionWithPointers[Option[A], EventfulPointer[Option[A]], Vector[A], P]
{
	// ATTRIBUTES	------------------------------
	
	private implicit val equals: EqualsFunction[A] = sameItemCheck.getOrElse(EqualsFunction.default)
	
	// Tracks close time in order to now immediately open the pop-up afterwards
	private var lastPopupCloseTime = Now.toInstant
	
	// Tracks the last selected value in order to return selection when content is updated
	private val lastSelectedValuePointer = Pointer.empty[A]()
	
	private val lazyPopup = ListenableResettableLazy[Window] { createPopup() }
	// Follows the pop-up visibility state with a pointer
	/**
	  * A pointer which shows whether a pop-up is being displayed
	  */
	val popUpVisiblePointer: FlagLike = lazyPopup.stateView.flatMap {
		case Some(window) => window.fullyVisibleFlag
		case None => AlwaysFalse
	}
	/**
	  * A pointer which contains true while a pop-up is hidden
	  */
	val popUpHiddenPointer = !popUpVisiblePointer
	/**
	  * A pointer that contains a timestamp of the latest pop-up visibility change event
	  */
	val popUpVisibilityLastChangedPointer = popUpVisiblePointer.strongMap { _ => Now.toInstant }
	// Merges the expand and the collapse icons, if necessary
	private val rightIconPointer: Changing[SingleColorIcon] = {
		// Case: No expand or collapse icon defined, or an always-present right-side icon is defined
		// => Uses only the right-side icon from settings
		if (settings.rightIconPointer.existsFixed { _.nonEmpty } || settings.expandAndCollapseIcon.forall { _.isEmpty })
			settings.rightIconPointer
		// Case: Expand and/or collapse icon defined => Uses those icons, and possibly right-side icon also
		else
			settings.expandAndCollapseIcon.merge { (expand, collapse) =>
				expand.notEmpty match {
					case Some(expandIcon) =>
						val expandOrCollapsePointer = collapse.notEmpty match {
							// Case: Both icons are specified => merges them
							case Some(collapseIcon) =>
								// Makes sure both icons have the same size
								if (expandIcon.size == collapseIcon.size)
									popUpVisiblePointer.strongMap { visible => if (visible) collapseIcon else expandIcon }
								else {
									val (smaller, larger) = Pair(expandIcon, collapseIcon).minMaxBy { _.size.area }.toTuple
									val targetSize = smaller.size
									val shrankIcon = SingleColorIcon(
										larger.original.fittingWithin(targetSize).paintedToCanvas(targetSize), larger.standardSize)
									
									val (newExpandIcon, newCollapseIcon) =
										if (smaller == expandIcon) smaller -> shrankIcon else shrankIcon -> smaller
									popUpVisiblePointer.strongMap { visible => if (visible) newCollapseIcon else newExpandIcon }
								}
							// Case: Only expand icon is defined => Doesn't use collapse icon
							case None => Fixed(expandIcon)
						}
						// The settings-specified right-side icon still overrides the expand/collapse icon, when present
						settings.rightIconPointer.mergeWith(expandOrCollapsePointer) { _.nonEmptyOrElse(_) }
					// Case: Only collapse icon defined => Uses that when no other icon is present
					case None => settings.rightIconPointer.map { _.nonEmptyOrElse(collapse) }
				}
			}
	}
	
	/**
	  * Field wrapped by this field
	  */
	val field = Field.withContext(parentHierarchy, contextPointer)
		.withSettings(settings.fieldSettings.withRightIconPointer(rightIconPointer))
		.apply(isEmptyPointer)(makeField)(makeRightHintLabel)
	
	/**
	  * A pointer that contains true while the pop-up window is NOT displayed, but only while this field has focus
	  */
	val popUpHiddenWhileFocusedFlag = popUpHiddenPointer && field.focusPointer
	
	private lazy val popUpContextPointer = contextPointer.mergeWith(field.innerBackgroundPointer) { (context, bg) =>
		settings.popupContextMod(context.withBackground(bg))
	}
	
	
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
		}
		// May deselect the current value or select the previously selected value on content changes
		val contentUpdateListener = ChangeListener[Vector[A]] { e =>
			valuePointer.update {
				// Case: No value currently selected => Attempts to select the previously selected value
				case None => lastSelectedValuePointer.value.filter { e.newValue.containsEqual(_) }
				// Case: Value is selected => Deselects it if it no longer appears among the options
				case s: Some[A] => s.filter { e.newValue.containsEqual(_) }
			}
		}
		if (isAttached) {
			valuePointer.addListenerAndSimulateEvent(None)(updateLastValueListener)
			contentPointer.addListenerAndSimulateEvent(Vector())(contentUpdateListener)
			KeyboardEvents += FieldKeyListener
			CommonMouseEvents += PopupHideMouseListener
		}
		else {
			CommonMouseEvents -= PopupHideMouseListener
			KeyboardEvents -= FieldKeyListener
			contentPointer.removeListener(contentUpdateListener)
			valuePointer.removeListener(updateLastValueListener)
			lazyPopup.popCurrent().foreach { _.close() }
		}
	}
	
	// When gains focus, displays the pop-up. Hides the pop-up when focus is lost.
	focusPointer.addContinuousListenerAndSimulateEvent(false) { e =>
		if (e.newValue) {
			if (Now - lastPopupCloseTime > ignoreFocusAfterCloseDuration)
				openPopup()
		}
		else
			cachedPopup.foreach { _.visible = false }
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
		if (lazyPopup.current.forall { _.isNotFullyVisible })
			lazyPopup.value.display()
	}
	
	private def createPopup(): Window = {
		// Creates the pop-up
		implicit val windowContext: ReachContentWindowContext = popUpContextPointer.value
		val popup = field.createOwnedWindow(Alignment.forDirection(settings.listAxis(Positive)), matchEdgeLength = true) { hierarchy =>
			// The pop-up content resides in a scroll view with custom background drawing
			ScrollView(hierarchy).withAxis(settings.listAxis)
				.withScrollBarMargin(windowContext.margins.small, settings.listCap.optimal)
				.limitedToContentSize
				.withCustomDrawer(BackgroundViewDrawer(field.innerBackgroundPointer))
				.build(Mixed) { factories =>
					// The scrollable content consists of either:
					//  1) Main content + additional view, or
					//  2) Main content only
					def makeOptionsList(factory: SelectionListFactory) =
						factory.withContextPointer(popUpContextPointer).withSettings(settings.listSettings)
							.withMargin(settings.listMargin)
							.apply(contentPointer, valuePointer, sameItemCheck) { (hierarchy, item) =>
							makeDisplay(hierarchy, popUpContextPointer, item)
						}
					def makeMainContent(factories: Mixed) = {
						// The main content is either:
						//   1) Switchable between options and no-options -view
						//   2) Only the options view
						settings.noOptionsViewConstructor match {
							// Case: No options -view used => Switches between the two views
							case Some(makeNoOptionsView) =>
								factories(CachingViewSwapper).build(Mixed)
									.generic(contentPointer.map { _.isEmpty }) { (factories, isEmpty: Boolean) =>
										// Case: No options -view constructor
										if (isEmpty)
											makeNoOptionsView(factories.parentHierarchy, popUpContextPointer)
										// Case: List constructor
										else
											makeOptionsList(factories(SelectionList))
									}
							// Case: No no options -view used => Always displays the selection list
							case None => makeOptionsList(factories(SelectionList))
						}
					}
					settings.extraOptionConstructor match {
						// Case: Additional view used => Places it above or below the main content
						case Some(makeAdditionalOption) =>
							factories(ViewStack).withoutMargin.build(Mixed) { factories =>
								// The main content may be hidden, if empty
								val mainContentVisiblePointer = {
									if (settings.noOptionsViewConstructor.isDefined)
										AlwaysTrue
									else
										contentPointer.map { _.nonEmpty }
								}
								// Orders the components based on settings
								val topAndBottomFactories = Pair.fill(factories.next())
								val mainContent = makeMainContent(topAndBottomFactories(
									settings.extraOptionLocation.opposite))
								val additional = makeAdditionalOption(
									topAndBottomFactories(settings.extraOptionLocation).parentHierarchy,
									popUpContextPointer)
								if (settings.extraOptionLocation == First)
									Vector(additional -> AlwaysTrue, mainContent -> mainContentVisiblePointer)
								else
									Vector(mainContent -> mainContentVisiblePointer, additional -> AlwaysTrue)
							}
						// CAse: No additional view used => Always displays the main content
						case None => makeMainContent(factories)
					}
				}
		}
		// Remembers when the pop-up closes
		popup.fullyVisibleFlag.addListener { e =>
			if (!e.newValue)
				lastPopupCloseTime = Now
		}
		// When the mouse is released, hides the pop-up
		// Also hides when not in focus, and on some key-presses
		popup.focusKeyStateHandler += new PopupKeyListener(popup)
		popup.focusedFlag.addListener { e =>
			if (!e.newValue)
				popup.visible = false
			ChangeResponse.continueUnless(popup.hasClosed)
		}
		// Returns the pop-up window
		popup.window
	}
	
	
	// NESTED	---------------------------------
	
	private object PopupHideMouseListener extends MouseButtonStateListener2
	{
		// ATTRIBUTES   ----------------------
		
		override val mouseButtonStateEventFilter = MouseButtonStateEvent2.filter.left
		
		// Only closes the pop-up on mouse release if it was visible on the previous mouse press
		private val closeOnReleaseFlag = ResettableFlag()
		
		
		// IMPLEMENTED  ----------------------
		
		override def handleCondition: FlagLike = AlwaysTrue
		
		override def onMouseButtonStateEvent(event: MouseButtonStateEvent2) = {
			// Case: Mouse press => Saves the pop-up status in order to react correctly to the next mouse release
			if (event.pressed) {
				closeOnReleaseFlag.value = cachedPopup.exists { _.isFullyVisible }
				Preserve
			}
			// Case: Mouse release => Hides the pop-up if it was visible when the mouse was pressed
			else if (closeOnReleaseFlag.reset()) {
				cachedPopup.foreach { _.visible = false }
				Consume("Pop-up closing")
			}
			else
				Preserve
		}
	}
	
	private object FieldKeyListener extends KeyStateListener
	{
		// ATTRIBUTES	-------------------------
		
		// Is interested in key events while the field has focus and pop-up is not open
		override def handleCondition: FlagLike = popUpHiddenWhileFocusedFlag
		
		// Listens to down arrow presses
		// Also supports additional key-strokes (based on the 'additionalActivationKeys' parameter)
		override val keyStateEventFilter = {
			val arrowFilter = KeyStateEvent.filter.arrow(settings.listAxis(Positive))
			val keyFilter = NotEmpty(settings.activationKeys) match {
				case Some(keys) => arrowFilter || KeyStateEvent.filter(keys)
				case None => arrowFilter
			}
			KeyStateEvent.filter.pressed && keyFilter
		}
		
		
		// IMPLEMENTED	-------------------------
		
		override def onKeyState(event: KeyStateEvent) = openPopup()
	}
	
	private class PopupKeyListener(popup: Window) extends KeyStateListener
	{
		// ATTRIBUTES	-------------------------
		
		// Listens to enter and tabulator presses
		override val keyStateEventFilter =
			KeyStateEvent.filter.pressed && KeyStateEvent.filter(Tab, Enter, Esc, Space)
		
		
		// IMPLEMENTED	-------------------------
		
		// Only reacts to events while the pop-up is visible
		override def handleCondition: FlagLike = popup.fullyVisibleFlag
		
		override def onKeyState(event: KeyStateEvent) = {
			// Stores the selected value, if applicable
			// Hides the pop-up
			popup.visible = false
			// On tabulator press, yields focus afterwards
			if (event.index == Tab.index)
				Delay(0.1.seconds) { yieldFocus(if (event.keyboardState(Shift)) Negative else Positive) }
		}
	}
}
