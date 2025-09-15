package utopia.reach.component.interactive.input.selection

import utopia.firmament.component.input.SelectionWithPointers
import utopia.firmament.context.ScrollingContext
import utopia.firmament.context.text.VariableTextContext
import utopia.firmament.image.SingleColorIcon
import utopia.firmament.model.enumeration.SizeCategory
import utopia.firmament.model.stack.{StackLength, StackSize}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.enumeration.End
import utopia.flow.operator.enumeration.End.First
import utopia.flow.operator.equality.EqualsFunction
import utopia.flow.operator.sign.Sign
import utopia.flow.time.Now
import utopia.flow.util.Mutate
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.handling.event.keyboard.Key
import utopia.genesis.handling.event.keyboard.Key.{ArrowKey, Enter, Space, Tab}
import utopia.paradigm.enumeration.Alignment.Bottom
import utopia.paradigm.enumeration.{Alignment, Axis2D}
import utopia.reach.component.factory.contextual.VariableTextContextualFactory
import utopia.reach.component.factory.{ContextualMixed, FromContextComponentFactoryFactory, Mixed}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.interactive.CanDisplayPopupWrapper
import utopia.reach.component.interactive.input._
import utopia.reach.component.template.focus.{Focusable, FocusableWithStateWrapper}
import utopia.reach.component.template.{PartOfComponentHierarchy, ReachComponent, ReachComponentWrapper}
import utopia.reach.component.wrapper.OpenComponent
import utopia.reach.container.multi.{ViewStack, ViewStackSettings}
import utopia.reach.container.wrapper.Swapper
import utopia.reach.container.wrapper.scrolling.ScrollView
import utopia.reach.context.{ReachWindowContext, VariableReachContentWindowContext}
import utopia.reach.focus.FocusListener

import scala.concurrent.ExecutionContext

/**
 * Common trait for field with selection popup factories and settings
 * @tparam Repr Implementing factory/settings type
 * @author Mikko Hilpinen
 * @since 14.09.2025, v1.7
 */
// TODO: Add utility functions, including withoutSelectionMargin
trait FieldWithSelectionPopupSettingsLike[+Repr] extends FieldWithPopupSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	 * Wrapped more generic field-with-popup settings
	 */
	def popupSettings: FieldWithPopupSettings
	/**
	 * Settings that apply to the opened selectable stack
	 */
	def selectionSettings: SelectableStackSettings
	
	/**
	 * A function used for constructing a view to display when no options are selectable
	 */
	def noOptionsViewConstructor: Option[ContextualMixed[VariableReachContentWindowContext] => ReachComponent]
	/**
	 * A function used for constructing an additional selectable view to display
	 */
	def extraOptionConstructor: Option[ContextualMixed[VariableReachContentWindowContext] => ReachComponent]
	/**
	 * The location where the extra option should be placed, if one has been specified
	 */
	def extraOptionPlacement: End
	
	/**
	 * A function used for constructing an additional selectable view to display
	 * @param f New extra option constructor to use.
	 *          A function used for constructing an additional selectable view to display
	 * @return Copy of this factory with the specified extra option constructor
	 */
	def withExtraOptionConstructor(f: Option[ContextualMixed[VariableReachContentWindowContext] => ReachComponent]): Repr
	/**
	 * The location where the extra option should be placed, if one has been specified
	 * @param placement New extra option placement to use.
	 *                  The location where the extra option should be placed, if one has been
	 *                  specified
	 * @return Copy of this factory with the specified extra option placement
	 */
	def withExtraOptionPlacement(placement: End): Repr
	/**
	 * A function used for constructing a view to display when no options are selectable
	 * @param f New no options view constructor to use.
	 *          A function used for constructing a view to display when no options are selectable
	 * @return Copy of this factory with the specified no options view constructor
	 */
	def withNoOptionsViewConstructor(f: Option[ContextualMixed[VariableReachContentWindowContext] => ReachComponent]): Repr
	/**
	 * @param axisPointer A pointer that contains the axis along which the selectable items are listed.
	 *                    May also affect the direction to which the pop-up opens.
	 * @return Copy of this factory with the specified selection axis
	 */
	def withSelectionAxisPointer(axisPointer: Changing[Axis2D]): Repr
	
	/**
	 * Wrapped more generic field-with-popup settings
	 * @param settings New popup settings to use.
	 *                 Wrapped more generic field-with-popup settings
	 * @return Copy of this factory with the specified popup settings
	 */
	def withPopupSettings(settings: FieldWithPopupSettings): Repr
	/**
	 * Settings that apply to the opened selectable stack
	 * @param settings New selection settings to use.
	 *                 Settings that apply to the opened selectable stack
	 * @return Copy of this factory with the specified selection settings
	 */
	def withSelectionSettings(settings: SelectableStackSettings): Repr
	
	
	// COMPUTED	--------------------
	
	/**
	 * @return A pointer that contains the selection list placement axis
	 */
	def selectionAxisPointer = selectionSettings.axisPointer
	/**
	 * focus listeners from the wrapped selectable stack settings
	 */
	def selectionFocusListeners = selectionSettings.focusListeners
	/**
	 * stack settings from the wrapped selectable stack settings
	 */
	def selectionStackSettings = selectionSettings.stackSettings
	/**
	 * selection drawer from the wrapped selectable stack settings
	 */
	def selectionDrawer = selectionSettings.selectionDrawer
	/**
	 * margin pointer from the wrapped selectable stack settings
	 */
	def selectionMarginPointer = selectionSettings.marginPointer
	/**
	 * extra selection keys from the wrapped selectable stack settings
	 */
	def extraSelectionKeys = selectionSettings.extraSelectionKeys
	/**
	 * alternative key selection enabled flag from the wrapped selectable stack settings
	 */
	def alternativeKeySelectionEnabledFlag = selectionSettings.alternativeKeySelectionEnabledFlag
	/**
	 * arrow key selection enabled from the wrapped selectable stack settings
	 */
	def arrowKeySelectionEnabled = selectionSettings.arrowKeySelectionEnabled
	
	
	// IMPLEMENTED	--------------------
	
	override def fieldSettings = popupSettings.fieldSettings
	
	override def activationKeys = popupSettings.activationKeys
	override def appliesFieldBackgroundInPopUp = popupSettings.appliesFieldBackgroundInPopUp
	override def closeKeys = popupSettings.closeKeys
	override def expandAndCollapseIcon = popupSettings.expandAndCollapseIcon
	override def hidesPopUpAfterMouseRelease = popupSettings.hidesPopUpAfterMouseRelease
	override def popupAlignment = popupSettings.popupAlignment
	override def popupMatchesFieldLength = popupSettings.popupMatchesFieldLength
	
	override def withActivationKeys(keys: Set[Key]) =
		withPopupSettings(popupSettings.withActivationKeys(keys))
	override def withCloseKeys(keys: Set[Key]) = withPopupSettings(popupSettings.withCloseKeys(keys))
	override def withExpandAndCollapseIcon(icons: Pair[SingleColorIcon]) =
		withPopupSettings(popupSettings.withExpandAndCollapseIcon(icons))
	override def withFieldBackgroundInPopUp(applyBackground: Boolean) =
		withPopupSettings(popupSettings.withFieldBackgroundInPopUp(applyBackground))
	override def withFieldSettings(settings: FieldSettings) =
		withPopupSettings(popupSettings.withFieldSettings(settings))
	override def withHidePopUpAfterMouseRelease(hide: Boolean) =
		withPopupSettings(popupSettings.withHidePopUpAfterMouseRelease(hide))
	override def withPopupAlignment(alignment: Alignment) =
		withPopupSettings(popupSettings.withPopupAlignment(alignment))
	override def withPopupMatchesFieldLength(matchLength: Boolean) =
		withPopupSettings(popupSettings.withPopupMatchesFieldLength(matchLength))
	
	
	// OTHER	--------------------
	
	def mapPopupSettings(f: Mutate[FieldWithPopupSettings]) = withPopupSettings(f(popupSettings))
	
	def mapSelectionAxisPointer(f: Mutate[Changing[Axis2D]]) = withSelectionAxisPointer(f(selectionAxisPointer))
	def mapAlternativeKeySelectionEnabledFlag(f: Mutate[Flag]) =
		withAlternativeKeySelectionEnabledFlag(f(alternativeKeySelectionEnabledFlag))
	def mapExtraSelectionKeys(f: Mutate[Map[Key, Sign]]) = withExtraSelectionKeys(f(extraSelectionKeys))
	def mapSelectionSettings(f: Mutate[SelectableStackSettings]) = withSelectionSettings(f(selectionSettings))
	def mapSelectionStackSettings(f: Mutate[ViewStackSettings]) =
		withSelectionStackSettings(f(selectionStackSettings))
	
	/**
	 * @param enabledFlag A flag that, when set, makes the keyboard-based selection function even
	 *                    when the component doesn't have focus
	 * @return Copy of this factory with the specified selection alternative key selection enabled flag
	 */
	def withAlternativeKeySelectionEnabledFlag(enabledFlag: Flag) =
		withSelectionSettings(selectionSettings.withAlternativeKeySelectionEnabledFlag(enabledFlag))
	/**
	 * @param enabled Whether arrow key -based selection should be enabled
	 * @return Copy of this factory with the specified selection arrow key selection enabled
	 */
	def withArrowKeySelectionEnabled(enabled: Boolean) =
		withSelectionSettings(selectionSettings.withArrowKeySelectionEnabled(enabled))
	/**
	 * @param keys Keys (other than the arrow keys), which are used for moving the selection around.
	 *             Each key is mapped to the direction to which it moves the selection.
	 * @return Copy of this factory with the specified selection extra selection keys
	 */
	def withExtraSelectionKeys(keys: Map[Key, Sign]) =
		withSelectionSettings(selectionSettings.withExtraSelectionKeys(keys))
	/**
	 * @param listeners Focus listeners to assign to created components
	 * @return Copy of this factory with the specified selection focus listeners
	 */
	def withSelectionFocusListeners(listeners: Seq[FocusListener]) =
		withSelectionSettings(selectionSettings.withFocusListeners(listeners))
	/**
	 * @param p A pointer that contains the margin placed between the components in this stack.
	 *          May be defined as either a general size category -pointer (left), or a specific
	 *          length -pointer (right).
	 * @return Copy of this factory with the specified selection margin pointer
	 */
	def withSelectionMarginPointer(p: Either[Changing[SizeCategory], Changing[StackLength]]) =
		withSelectionSettings(selectionSettings.withMarginPointer(p))
	/**
	 * @param drawer A drawer used for visualizing selection and mouse interaction
	 * @return Copy of this factory with the specified selection drawer
	 */
	def withSelectionDrawer(drawer: Option[SelectionDrawer]) =
		mapSelectionSettings { _.withSelectionDrawer(drawer) }
	/**
	 * @param drawer A drawer used for visualizing selection and mouse interaction
	 * @return Copy of this factory with the specified selection drawer
	 */
	def withSelectionDrawer(drawer: SelectionDrawer): Repr = withSelectionDrawer(Some(drawer))
	/**
	 * @param settings Settings that affect this stack's layout
	 * @return Copy of this factory with the specified selection stack settings
	 */
	def withSelectionStackSettings(settings: ViewStackSettings) =
		withSelectionSettings(selectionSettings.withStackSettings(settings))
}

object FieldWithSelectionPopupSettings
{
	// ATTRIBUTES	--------------------
	
	/**
	 * Default settings used with various pop-up related features
	 */
	lazy val defaultPopupSettings = FieldWithPopupSettings(closeKeys = Set(Enter, Space, Tab), popupAlignment = Bottom,
		popupMatchesFieldLength = true, appliesFieldBackgroundInPopUp = true, hidesPopUpAfterMouseRelease = true)
	val default = apply(defaultPopupSettings)
}
/**
 * Combined settings used when constructing field with selection popups
 * @param popupSettings            Wrapped more generic field-with-popup settings
 * @param selectionSettings        Settings that apply to the opened selectable stack
 * @param noOptionsViewConstructor A function used for constructing a view to display when no
 *                                 options are selectable
 * @param extraOptionConstructor   A function used for constructing an additional selectable
 *                                 view to display
 * @param extraOptionPlacement     The location where the extra option should be placed, if one
 *                                 has been specified
 * @author Mikko Hilpinen
 * @since 14.09.2025, v1.7
 */
case class FieldWithSelectionPopupSettings(popupSettings: FieldWithPopupSettings = FieldWithPopupSettings.default,
                                           selectionSettings: SelectableStackSettings = SelectableStackSettings.default,
                                           noOptionsViewConstructor: Option[ContextualMixed[VariableReachContentWindowContext] => ReachComponent] = None,
                                           extraOptionConstructor: Option[ContextualMixed[VariableReachContentWindowContext] => ReachComponent] = None,
                                           extraOptionPlacement: End = End.Last)
	extends FieldWithSelectionPopupSettingsLike[FieldWithSelectionPopupSettings]
{
	// IMPLEMENTED	--------------------
	
	override def withExtraOptionConstructor(f: Option[ContextualMixed[VariableReachContentWindowContext] => ReachComponent]) =
		copy(extraOptionConstructor = f)
	override def withExtraOptionPlacement(placement: End) = copy(extraOptionPlacement = placement)
	override def withNoOptionsViewConstructor(f: Option[ContextualMixed[VariableReachContentWindowContext] => ReachComponent]) =
		copy(noOptionsViewConstructor = f)
	override def withPopupSettings(settings: FieldWithPopupSettings) = copy(popupSettings = settings)
	override def withSelectionSettings(settings: SelectableStackSettings) = copy(selectionSettings = settings)
	
	// When using a fixed axis, specifies the pop-up opening direction, also
	override def withSelectionAxisPointer(axisPointer: Changing[Axis2D]): FieldWithSelectionPopupSettings =
		axisPointer.fixedValue match {
			case Some(axis) =>
				copy(popupSettings = popupSettings.withPopupAlignment(Alignment.forDirection(axis.forward)),
					selectionSettings = selectionSettings.withAxisPointer(axisPointer))
			case None => mapSelectionSettings { _.withAxisPointer(axisPointer) }
		}
}

/**
 * Common trait for factories that wrap a field with selection popup settings instance
 * @tparam Repr Implementing factory/settings type
 * @author Mikko Hilpinen
 * @since 14.09.2025, v1.7
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
	
	override def extraOptionConstructor = settings.extraOptionConstructor
	override def extraOptionPlacement = settings.extraOptionPlacement
	override def noOptionsViewConstructor = settings.noOptionsViewConstructor
	
	override def popupSettings = settings.popupSettings
	override def selectionSettings = settings.selectionSettings
	
	override def withExtraOptionConstructor(f: Option[ContextualMixed[VariableReachContentWindowContext] => ReachComponent]) =
		mapSettings { _.withExtraOptionConstructor(f) }
	override def withExtraOptionPlacement(placement: End) =
		mapSettings { _.withExtraOptionPlacement(placement) }
	override def withNoOptionsViewConstructor(f: Option[ContextualMixed[VariableReachContentWindowContext] => ReachComponent]) =
		mapSettings { _.withNoOptionsViewConstructor(f) }
	override def withPopupSettings(settings: FieldWithPopupSettings) =
		mapSettings { _.withPopupSettings(settings) }
	override def withSelectionSettings(settings: SelectableStackSettings) =
		mapSettings { _.withSelectionSettings(settings) }
	override def withSelectionAxisPointer(axisPointer: Changing[Axis2D]): Repr =
		mapSettings { _.withSelectionAxisPointer(axisPointer) }
	
	
	// OTHER	--------------------
	
	def mapSettings(f: FieldWithSelectionPopupSettings => FieldWithSelectionPopupSettings) =
		withSettings(f(settings))
}

/**
 * Factory class used for constructing field with selection popups using contextual component
 * creation information
 * @author Mikko Hilpinen
 * @since 14.09.2025, v1.7
 */
case class FieldWithSelectionPopupFactory(hierarchy: ComponentHierarchy, context: VariableTextContext,
                                          settings: FieldWithSelectionPopupSettings = FieldWithSelectionPopupSettings.default)
	extends FieldWithSelectionPopupSettingsWrapper[FieldWithSelectionPopupFactory]
		with VariableTextContextualFactory[FieldWithSelectionPopupFactory]
		with PartOfComponentHierarchy
		with ContextualSelectionFactory[VariableTextContext, FieldWithSelectionPopupFactory]
{
	// COMPUTED ------------------------
	
	/**
	 * Prepares this factory for component-construction using implicitly available information
	 * @param popupContext Pop-up creation context to use (implicit)
	 * @param scrollContext Implicit context for scrolling settings
	 * @param exc Implicit execution context
	 * @return A prepared factory
	 * @see [[withPopupContext]], which does the same with explicit context
	 */
	def contextual(implicit popupContext: ReachWindowContext, scrollContext: ScrollingContext, exc: ExecutionContext) =
		withPopupContext(popupContext)
	
	
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override def withContext(context: VariableTextContext) = copy(context = context)
	override def withSettings(settings: FieldWithSelectionPopupSettings) =
		copy(settings = settings)
		
	
	// OTHER    ------------------------
	
	/**
	 * Prepares this factory for component-construction
	 * @param context Pop-up creation context to use
	 * @param scrollContext Implicit context for scrolling settings
	 * @param exc Implicit execution context
	 * @return A prepared factory
	 */
	def withPopupContext(context: ReachWindowContext)(implicit scrollContext: ScrollingContext, exc: ExecutionContext) =
		new PreparedFieldFactory(context)
		
	
	// NESTED   ------------------------
	
	class PreparedFieldFactory(popupContext: ReachWindowContext)
	                          (implicit scrollContext: ScrollingContext, exc: ExecutionContext)
	{
		/**
		 * Creates a new field that opens a pop-up window for item selection
		 * @param emptyFlag A flag that contains true while the component in the field is empty,
		 *                  displaying no content
		 * @param contentPointer A pointer that contains the list of selectable items
		 * @param valuePointer A mutable pointer that contains the selected value (default = new pointer)
		 * @param makeField A function that constructs the component inside the wrapped field.
		 *                  Receives a [[FieldCreationContext]].
		 * @param makeItemView A function that constructs an individual view for a selectable item.
		 *                     Receives:
		 *                          1. Component factories (with a variable text context)
		 *                          1. Content pointer to display
		 *                          1. A flag that contains true while this item is selected
		 *                          1. Index of this item (0-based)
		 * @param makeRightHintLabel A function that receives an [[ExtraFieldCreationContext]] and yields a
		 *                           label to place on the right side of the hint area, if applicable.
		 *                           Default = no additional component is constructed.
		 * @param equals Implicit equals function for the selected items. Defaults to ==.
		 * @tparam A Type of selected items
		 * @tparam C Type of the component wrapped inside this field
		 * @return A new field
		 */
		def apply[A, C <: ReachComponent with Focusable](emptyFlag: Flag, contentPointer: Changing[Seq[A]],
		                                                 valuePointer: EventfulPointer[Option[A]] = Pointer.eventful.empty)
		                                                (makeField: FieldCreationContext => C)
		                                                (makeItemView: (ContextualMixed[VariableTextContext], Changing[A], Flag, Int) => ReachComponent)
		                                                (makeRightHintLabel: ExtraFieldCreationContext[C] => Option[OpenComponent[ReachComponent, Any]] = { _: ExtraFieldCreationContext[C] => None })
		                                                (implicit equals: EqualsFunction[A] = EqualsFunction.default) =
			new FieldWithSelectionPopup[A, C](hierarchy, context, popupContext, emptyFlag, contentPointer,
				valuePointer, settings, makeField, makeRightHintLabel, makeItemView)
	}
}

/**
 * Used for defining field with selection popup creation settings outside the component building process
 * @author Mikko Hilpinen
 * @since 14.09.2025, v1.7
 */
case class FieldWithSelectionPopupSetup(settings: FieldWithSelectionPopupSettings = FieldWithSelectionPopupSettings.default)
	extends FieldWithSelectionPopupSettingsWrapper[FieldWithSelectionPopupSetup]
		with FromContextComponentFactoryFactory[VariableTextContext, FieldWithSelectionPopupFactory]
{
	// IMPLEMENTED	--------------------
	
	override def withContext(hierarchy: ComponentHierarchy, context: VariableTextContext) =
		FieldWithSelectionPopupFactory(hierarchy, context, settings)
	override def withSettings(settings: FieldWithSelectionPopupSettings) = copy(settings = settings)
}

object FieldWithSelectionPopup extends FieldWithSelectionPopupSetup()
{
	// OTHER	--------------------
	
	def apply(settings: FieldWithSelectionPopupSettings) = withSettings(settings)
}

/**
  * A field wrapper class that displays a selection pop-up when it receives focus
  * @author Mikko Hilpinen
  * @since 22.12.2020, v0.1
  * @tparam A Type of selectable item
  * @tparam C Type of component inside the field
  */
class FieldWithSelectionPopup[A, C <: ReachComponent with Focusable](override val hierarchy: ComponentHierarchy,
                                                                     context: VariableTextContext,
                                                                     popupContext: ReachWindowContext, emptyFlag: Flag,
                                                                     override val contentPointer: Changing[Seq[A]],
                                                                     override val valuePointer: EventfulPointer[Option[A]],
                                                                     settings: FieldWithSelectionPopupSettings,
                                                                     makeField: FieldCreationContext => C,
                                                                     makeRightHintLabel: ExtraFieldCreationContext[C] => Option[OpenComponent[ReachComponent, Any]],
                                                                     makeItemView: (ContextualMixed[VariableTextContext], Changing[A], Flag, Int) => ReachComponent)
                                                                    (implicit eq: EqualsFunction[A],
                                                                     scrollingContext: ScrollingContext,
                                                                     exc: ExecutionContext)
	extends ReachComponentWrapper with FocusableWithStateWrapper
		with SelectionWithPointers[Option[A], EventfulPointer[Option[A]], Seq[A], Changing[Seq[A]]]
		with CanDisplayPopupWrapper
{
	// ATTRIBUTES	------------------------------
	
	lazy val nonEmptyFlag: Flag = contentPointer.lightMap { _.nonEmpty }
	
	private lazy val _field = FieldWithPopup.withContext(hierarchy, context).withSettings(settings.popupSettings)
		// Activates the pop-up with an arrow key, unless the used axis is variable
		.mapActivationKeys { original =>
			settings.selectionAxisPointer.fixedValue match {
				case Some(axis) => original + ArrowKey(axis.forward)
				case None => original
			}
		}
		.withPopupContext(popupContext)
		.withPossibleRightHintLabel[C](emptyFlag)(makeField)(makeRightHintLabel) { factories =>
			// The pop-up content resides in a scroll view with custom background drawing
			// TODO: Once scroll-views support variable axes, apply that here
			factories(ScrollView).withAxis(settings.selectionSettings.axisPointer.value)
				// TODO: Applies fixed cap where it maybe variable
				.withBarMargin(factories.context.margins.small, settings.selectionSettings.capPointer.value.optimal)
				.limitedToContentSize
				.build(Mixed) { factories =>
					def createSelectionStack(factory: SelectableStackFactory[VariableTextContext]) =
						factory.withSettings(settings.selectionSettings).apply(contentPointer, valuePointer)(makeItemView)
						
					def createMainContent(factories: ContextualMixed[VariableReachContentWindowContext]) = {
						// The main content is either:
						//   1. Switchable between options and no-options -view
						//   2. Only the options view
						settings.noOptionsViewConstructor match {
							// Case: No options -view used => Switches between the two views
							case Some(makeNoOptionsView) =>
								factories(Swapper).build(Mixed)
									.apply(nonEmptyFlag) { (factories, nonEmpty: Boolean) =>
										// Case: List constructor
										if (nonEmpty)
											createSelectionStack(factories(SelectableStack))
										// Case: No options -view constructor
										else
											makeNoOptionsView(factories)
									}
							// Case: No no-options -view used => Always displays the selection list
							case None => createSelectionStack(factories(SelectableStack))
						}
					}
					settings.extraOptionConstructor match {
						// Case: Additional view used => Places it above or below the main content
						case Some(makeExtraOptionView) =>
							factories(ViewStack).withoutMargin.build(Mixed) { factories =>
								// The main content may be hidden, if empty
								val mainContentVisibleFlag = {
									if (settings.noOptionsViewConstructor.isDefined) AlwaysTrue else nonEmptyFlag
								}
								// Orders the components based on settings
								val topAndBottomFactories = Pair.fill(factories.next())
								val placement = settings.extraOptionPlacement
								val mainContent = createMainContent(topAndBottomFactories(placement.opposite))
								val additional = makeExtraOptionView(topAndBottomFactories(placement))
								if (placement == First)
									Pair(additional -> AlwaysTrue, mainContent -> mainContentVisibleFlag)
								else
									Pair(mainContent -> mainContentVisibleFlag, additional -> AlwaysTrue)
							}
						// Case: No additional view used => Always displays the main content
						case None => createMainContent(factories)
					}
				}
		}
	
	/**
	 * A pointer that contains a timestamp of the latest pop-up visibility change event
	 */
	// TODO: Move to drop-down
	val lastPopupVisibilityChangedPointer = popupVisibleFlag.strongMap { _ => Now.toInstant }
	
	/**
	 * The axis, along which pop-up and field lengths are attempted to match
	 */
	private val lengthMatchingAxis =
		if (settings.popupMatchesFieldLength) settings.popupAlignment.unaffectedAxes.only else None
	
	
	// COMPUTED	---------------------------------
	
	/**
	 * @return The wrapped field
	 */
	def field = wrapped.field
	
	/**
	 * A pointer that contains true while the pop-up window is NOT displayed, but only while this field has focus
	 */
	def popupHiddenWhileFocusedFlag = wrapped.popupHiddenWhileFocusedFlag
	@deprecated("Renamed to .popupHiddenWhileFocusedFlag", "v1.7")
	def popUpHiddenWhileFocusedFlag = popupHiddenWhileFocusedFlag
	
	/**
	 * @return Timestamp of the last pop-up window visibility change.
	 *         If the pop-up hasn't been displayed yet, yields the creation time of this component.
	 */
	def lastPopupVisibilityChangedTime = lastPopupVisibilityChangedPointer.value
	
	@deprecated("Renamed to .popupVisibleFlag", "v1.7")
	def popUpVisibleFlag: Flag = popupVisibleFlag
	@deprecated("Renamed to .popupHiddenFlag", "v1.7")
	def popUpHiddenFlag: Flag = popupHiddenFlag
	
	@deprecated("Renamed to .lastPopupVisibilityChangedPointer", "v1.7")
	def popUpVisibilityLastChangedPointer = lastPopupVisibilityChangedPointer
	
	
	// IMPLEMENTED	-----------------------------
	
	override protected def wrapped = _field
	override protected def focusable = _field
	
	override def stackSize: StackSize = {
		// May adjust field size to better match that of the pop-up's
		lengthMatchingAxis match {
			case Some(axis) =>
				_field.popup match {
					case Some(popup) => _field.stackSize.mapDimension(axis) { _ && popup.stackSize.along(axis) }
					case None => super.stackSize
				}
			case None => super.stackSize
		}
	}
	
	
	// OTHER	---------------------------------
	
	@deprecated("Renamed to .showPopup()", "v1.7")
	def openPopup(): Unit = showPopup()
}
