package utopia.reach.component.interactive.input

import utopia.firmament.component.Window
import utopia.firmament.context.text.VariableTextContext
import utopia.firmament.image.SingleColorIcon
import utopia.firmament.localization.LocalizedString
import utopia.flow.async.process.Delay
import utopia.flow.collection.immutable.Pair
import utopia.flow.event.model.ChangeResponse
import utopia.flow.operator.sign.Sign.{Negative, Positive}
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.Mutate
import utopia.flow.view.immutable.eventful.{AlwaysFalse, Fixed}
import utopia.flow.view.mutable.Switch
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.handling.event.consume.ConsumeChoice
import utopia.genesis.handling.event.consume.ConsumeChoice.{Consume, Preserve}
import utopia.genesis.handling.event.keyboard.Key._
import utopia.genesis.handling.event.keyboard.{Key, KeyStateEvent, KeyStateListener, KeyboardEvents}
import utopia.genesis.handling.event.mouse.{CommonMouseEvents, MouseButtonStateEvent, MouseButtonStateListener}
import utopia.paradigm.color.ColorRole
import utopia.paradigm.enumeration.Alignment
import utopia.reach.component.factory.ContextualMixed
import utopia.reach.component.factory.ContextualComponentFactories.CCF
import utopia.reach.component.factory.contextual.VariableTextContextualFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.interactive.CanDisplayPopup
import utopia.reach.component.interactive.input.FieldWithPopup.transitionDuration
import utopia.reach.component.label.image.ViewImageLabelSettings
import utopia.reach.component.template.focus.{Focusable, FocusableWithStateWrapper}
import utopia.reach.component.template.{PartOfComponentHierarchy, ReachComponent, ReachComponentWrapper}
import utopia.reach.component.wrapper.Open
import utopia.reach.context.{ReachWindowContext, VariableReachContentWindowContext}

import scala.concurrent.ExecutionContext

/**
 * Common trait for field with popup factories and settings
 * @tparam Repr Implementing factory/settings type
 * @author Mikko Hilpinen
 * @since 14.09.2025, v1.7
 */
trait FieldWithPopupSettingsLike[+Repr] extends FieldSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	 * Wrapped more generic field settings
	 */
	def fieldSettings: FieldSettings
	
	/**
	 * The expand (first) and the collapse icon (second) that should be displayed at the right side
	 * of the created fields.
	 * Please note that a non-empty right-side icon will override these values.
	 */
	def expandAndCollapseIcon: Pair[SingleColorIcon]
	/**
	 * Keyboard keys that open the pop-up window when pressed while this field is in focus.
	 */
	def activationKeys: Set[Key]
	/**
	 * Keyboard keys (in addition to escape), that close a pop-up window when released while the
	 * pop-up is in focus.
	 */
	def closeKeys: Set[Key]
	/**
	 * Alignment used for placing the pop-up window next to this field.
	 */
	def popupAlignment: Alignment
	/**
	 * Whether the pop-up window's length should match that of this field, on the side matching
	 * [[popupAlignment]].
	 */
	def popupMatchesFieldLength: Boolean
	/**
	 * Whether the field's background color should become the pop-up window's background color, also
	 */
	def appliesFieldBackgroundInPopup: Boolean
	/**
	 * Whether the pop-up window should be hidden whenever it loses focus
	 */
	def hidesPopupOnFocusLoss: Boolean
	/**
	 * Whether the opened pop-up window will be automatically hidden after a mouse release -event.
	 */
	def hidesPopupAfterMouseRelease: Boolean
	
	/**
	 * Additional keyboard keys that open the pop-up window when they are pressed, while this field
	 * is in focus.
	 * By default, only the appropriate arrow key opens the pop-up.
	 * @param keys New activation keys to use.
	 *             Keyboard keys that open the pop-up window when pressed while this field is in focus.
	 * @return Copy of this factory with the specified activation keys
	 */
	def withActivationKeys(keys: Set[Key]): Repr
	/**
	 * Keyboard keys (in addition to escape), that close a pop-up window when released while the
	 * pop-up is in focus.
	 * @param keys New close keys to use.
	 *             Keyboard keys (in addition to escape), that close a pop-up window when released
	 *             while the pop-up is in focus.
	 * @return Copy of this factory with the specified close keys
	 */
	def withCloseKeys(keys: Set[Key]): Repr
	/**
	 * The expand (first) and the collapse icon (second) that should be displayed at the right side
	 * of the created fields.
	 * Please note that a non-empty right-side icon will override these values.
	 * @param icons New expand and collapse icon to use.
	 *              The expand (first) and the collapse icon (second) that should be displayed at
	 *              the right side of the created fields.
	 *              Please note that a non-empty right-side icon will override these values.
	 * @return Copy of this factory with the specified expand and collapse icon
	 */
	def withExpandAndCollapseIcon(icons: Pair[SingleColorIcon]): Repr
	/**
	 * Whether the field's background color should become the pop-up window's background color, also
	 * @param applyBackground New applies field background in pop up to use.
	 *                        Whether the field's background color should become the pop-up window's
	 *                        background color, also
	 * @return Copy of this factory with the specified applies field background in pop up
	 */
	def withUsesFieldBackgroundInPopup(applyBackground: Boolean): Repr
	/**
	 * Wrapped more generic field settings
	 * @param settings New field settings to use.
	 *                 Wrapped more generic field settings
	 * @return Copy of this factory with the specified field settings
	 */
	def withFieldSettings(settings: FieldSettings): Repr
	/**
	 * Whether the opened pop-up window will be automatically hidden after a mouse release -event.
	 * @param hide New hides pop up after mouse release to use.
	 *             Whether the opened pop-up window will be automatically hidden after a mouse
	 *             release -event.
	 * @return Copy of this factory with the specified hides pop up after mouse release
	 */
	def withHidePopupAfterMouseRelease(hide: Boolean): Repr
	/**
	 * Whether the pop-up window should be hidden whenever it loses focus
	 * @param hide New hides popup on focus loss to use.
	 *             Whether the pop-up window should be hidden whenever it loses focus
	 * @return Copy of this factory with the specified hides popup on focus loss
	 */
	def withHidesPopupOnFocusLoss(hide: Boolean): Repr
	/**
	 * Alignment used for placing the pop-up window next to this field.
	 * @param alignment New popup alignment to use.
	 *                  Alignment used for placing the pop-up window next to this field.
	 * @return Copy of this factory with the specified popup alignment
	 */
	def withPopupAlignment(alignment: Alignment): Repr
	/**
	 * Whether the pop-up window's length should match that of this field, on the side matching
	 * [[popupAlignment]].
	 * @param matchLength New popup matches field length to use.
	 *                    Whether the pop-up window's length should match that of this field, on the
	 *                    side matching [[popupAlignment]].
	 * @return Copy of this factory with the specified popup matches field length
	 */
	def withPopupMatchesFieldLength(matchLength: Boolean): Repr
	
	
	// COMPUTED ------------------------
	
	/**
	 * @return Copy of this factory where the pop-up window's length matches the field's, if possible
	 */
	def withPopupMatchingFieldLength = withPopupMatchesFieldLength(true)
	/**
	 * @return Copy of this factory where the pop-up window's length is not dependent on the field's
	 */
	def withPopupLengthNotTiedToField = withPopupMatchesFieldLength(false)
	
	/**
	 * @return Copy of this factory that applies the field's background color to the pop-up window
	 */
	def withFieldBackgroundInPopup: Repr = withUsesFieldBackgroundInPopup(true)
	/**
	 * @return Copy of this factory that doesn't apply the field's background color in the pop-up window
	 */
	def withoutFieldBackgroundInPopup = withUsesFieldBackgroundInPopup(false)
	
	/**
	 * @return A copy of this factory that hides the pop-up window whenever it loses focus
	 */
	def hidingPopupOnFocusLoss = withHidesPopupOnFocusLoss(true)
	/**
	 * @return A copy of this factory that won't hide the pop-up window when it loses focus
	 */
	def notHidingPopupOnFocusLoss = withHidesPopupOnFocusLoss(false)
	
	/**
	 * @return Copy of this factory where the created pop-up windows are hid after a mouse release
	 */
	def hidingPopupOnMouseRelease = withHidePopupAfterMouseRelease(true)
	/**
	 * @return A copy of this factory without pop-up window on mouse release enabled
	 */
	def notHidingPopupOnMouseRelease = withHidePopupAfterMouseRelease(false)
	
	
	// IMPLEMENTED	--------------------
	
	override def enabledFlag = fieldSettings.enabledFlag
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
	
	override def withEnabledFlag(flag: Flag) = withFieldSettings(fieldSettings.withEnabledFlag(flag))
	override def withErrorMessagePointer(p: Changing[LocalizedString]) =
		withFieldSettings(fieldSettings.withErrorMessagePointer(p))
	override def withFieldNamePointer(p: Changing[LocalizedString]) =
		withFieldSettings(fieldSettings.withFieldNamePointer(p))
	override def withFillBackground(fill: Boolean) = withFieldSettings(fieldSettings.withFillBackground(fill))
	override def withFocusColorRole(color: ColorRole) =
		withFieldSettings(fieldSettings.withFocusColorRole(color))
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
	
	/**
	 * The expand (first) and the collapse icon (second) that should be displayed one the right side
	 * of the created fields.
	 * Please note that a non-empty right-side icon will override these values.
	 * @param expand New expand icon to use
	 * @param collapse New collapse icon to use
	 * @return Copy of this factory with the specified expand and collapse icon
	 */
	def withExpandAndCollapseIcon(expand: SingleColorIcon, collapse: SingleColorIcon): Repr =
		withExpandAndCollapseIcon(Pair(expand, collapse))
	
	/**
	 * @param key A key that will activate the pop-up window
	 * @param exclusive Whether to set this as the only activation key (default = false = add to existing keys)
	 * @return Copy of this factory with the specified activation key
	 */
	def withActivationKey(key: Key, exclusive: Boolean = false) = {
		if (exclusive)
			withActivationKeys(Set(key))
		else
			mapActivationKeys { _ + key }
	}
	def withActivationKeys(keys: IterableOnce[Key], exclusive: Boolean): Repr = {
		if (exclusive)
			withActivationKeys(Set.from(keys))
		else
			withAdditionalActivationKeys(keys)
	}
	def withAdditionalActivationKeys(keys: IterableOnce[Key]) = mapActivationKeys { _ ++ keys }
	
	/**
	 * @param keys Keys that make the pop-up window close when released
	 * @param exclusive Whether to treat these as the exclusive set of closing keys (default = false)
	 * @return Copy of this factory with the specified closing keys
	 */
	def closingWith(keys: IterableOnce[Key], exclusive: Boolean = false) = {
		if (exclusive)
			withCloseKeys(Set.from(keys))
		else
			mapCloseKeys { _ ++ keys }
	}
	/**
	 * @param key Key that closes the pop-up window when released
	 * @return Copy of this factory with the specified closing key added
	 */
	def closingWith(key: Key): Repr = closingWith(Set(key))
	/**
	 * @param key Key that closes the pop-up window when released
	 * @param exclusive Whether this should be the only key that closes the pop-up window (default = false)
	 * @return Copy of this factory with the specified closing key
	 */
	def closingWith(key: Key, exclusive: Boolean): Repr = closingWith(Set(key), exclusive)
	
	def mapActivationKeys(f: Mutate[Set[Key]]) = withActivationKeys(f(activationKeys))
	def mapCloseKeys(f: Mutate[Set[Key]]) = withCloseKeys(f(closeKeys))
	def mapExpandAndCollapseIcon(f: Mutate[Pair[SingleColorIcon]]) =
		withExpandAndCollapseIcon(f(expandAndCollapseIcon))
	def mapPopupAlignment(f: Mutate[Alignment]) = withPopupAlignment(f(popupAlignment))
	
	def mapFieldSettings(f: Mutate[FieldSettings]) = withFieldSettings(f(fieldSettings))
}

object FieldWithPopupSettings
{
	// ATTRIBUTES	--------------------
	
	val default = apply()
}
/**
 * Combined settings used when constructing field with popups
 *
 * @param fieldSettings                 Wrapped more generic field settings
 * @param expandAndCollapseIcon         The expand (first) and the collapse icon (second) that
 *                                      should be displayed on the right side of the created fields.
 *                                      Please note that a non-empty right-side icon will override these values.
 * @param activationKeys                Keyboard keys that open the pop-up window when pressed
 *                                      while this field is in focus.
 * @param closeKeys                     Keyboard keys (in addition to escape),
 *                                      that close a pop-up window when released while the pop-up is in focus.
 * @param popupAlignment                Alignment used for placing the pop-up window next to this field.
 * @param popupMatchesFieldLength       Whether the pop-up window's length should match that of
 *                                      this field, on the side matching [[popupAlignment]].
 * @param appliesFieldBackgroundInPopup Whether the field's background color should become the
 *                                      pop-up window's background color, also
 * @param hidesPopupOnFocusLoss Whether the pop-up window should be hidden whenever it loses focus
 * @param hidesPopupAfterMouseRelease   Whether the opened pop-up window will be automatically
 *                                      hidden after a mouse release -event.
 * @author Mikko Hilpinen
 * @since 14.09.2025, v1.7
 */
case class FieldWithPopupSettings(fieldSettings: FieldSettings = FieldSettings.default,
                                  expandAndCollapseIcon: Pair[SingleColorIcon] = Pair.twice(SingleColorIcon.empty),
                                  activationKeys: Set[Key] = Set[Key](), closeKeys: Set[Key] = Set[Key](),
                                  popupAlignment: Alignment = Alignment.Right, popupMatchesFieldLength: Boolean = false,
                                  appliesFieldBackgroundInPopup: Boolean = false, hidesPopupOnFocusLoss: Boolean = false,
                                  hidesPopupAfterMouseRelease: Boolean = false)
	extends FieldWithPopupSettingsLike[FieldWithPopupSettings]
{
	// IMPLEMENTED	--------------------
	
	override def withActivationKeys(keys: Set[Key]) = copy(activationKeys = keys)
	override def withCloseKeys(keys: Set[Key]) = copy(closeKeys = keys)
	override def withExpandAndCollapseIcon(icons: Pair[SingleColorIcon]) =
		copy(expandAndCollapseIcon = icons)
	override def withUsesFieldBackgroundInPopup(applyBackground: Boolean) =
		copy(appliesFieldBackgroundInPopup = applyBackground)
	override def withFieldSettings(settings: FieldSettings) = copy(fieldSettings = settings)
	override def withHidePopupAfterMouseRelease(hide: Boolean) =
		copy(hidesPopupAfterMouseRelease = hide)
	override def withPopupAlignment(alignment: Alignment) = copy(popupAlignment = alignment)
	override def withPopupMatchesFieldLength(matchLength: Boolean) =
		copy(popupMatchesFieldLength = matchLength)
	override def withHidesPopupOnFocusLoss(hide: Boolean) = copy(hidesPopupOnFocusLoss = hide)
}

/**
 * Common trait for factories that wrap a field with popup settings instance
 * @tparam Repr Implementing factory/settings type
 * @author Mikko Hilpinen
 * @since 14.09.2025, v1.7
 */
trait FieldWithPopupSettingsWrapper[+Repr] extends FieldWithPopupSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	 * Settings wrapped by this instance
	 */
	protected def settings: FieldWithPopupSettings
	/**
	 * @return Copy of this factory with the specified settings
	 */
	def withSettings(settings: FieldWithPopupSettings): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def activationKeys = settings.activationKeys
	override def closeKeys = settings.closeKeys
	override def appliesFieldBackgroundInPopup = settings.appliesFieldBackgroundInPopup
	override def expandAndCollapseIcon = settings.expandAndCollapseIcon
	override def fieldSettings = settings.fieldSettings
	override def hidesPopupAfterMouseRelease = settings.hidesPopupAfterMouseRelease
	override def popupAlignment = settings.popupAlignment
	override def popupMatchesFieldLength = settings.popupMatchesFieldLength
	override def hidesPopupOnFocusLoss: Boolean = settings.hidesPopupOnFocusLoss
	
	override def withHidesPopupOnFocusLoss(hide: Boolean): Repr = mapSettings { _.withHidesPopupOnFocusLoss(hide) }
	override def withActivationKeys(keys: Set[Key]) = mapSettings { _.withActivationKeys(keys) }
	override def withCloseKeys(keys: Set[Key]) = mapSettings { _.withCloseKeys(keys) }
	override def withExpandAndCollapseIcon(icons: Pair[SingleColorIcon]) =
		mapSettings { _.withExpandAndCollapseIcon(icons) }
	override def withUsesFieldBackgroundInPopup(applyBackground: Boolean) =
		mapSettings { _.withUsesFieldBackgroundInPopup(applyBackground) }
	override def withFieldSettings(settings: FieldSettings) = mapSettings { _.withFieldSettings(settings) }
	override def withHidePopupAfterMouseRelease(hide: Boolean) =
		mapSettings { _.withHidePopupAfterMouseRelease(hide) }
	override def withPopupAlignment(alignment: Alignment) = mapSettings { _.withPopupAlignment(alignment) }
	override def withPopupMatchesFieldLength(matchLength: Boolean) =
		mapSettings { _.withPopupMatchesFieldLength(matchLength) }
	
	
	// OTHER	--------------------
	
	def mapSettings(f: FieldWithPopupSettings => FieldWithPopupSettings) = withSettings(f(settings))
}

/**
 * Factory class used for constructing field with popups using contextual component creation
 * information
 * @author Mikko Hilpinen
 * @since 14.09.2025, v1.7
 */
case class FieldWithPopupFactory(hierarchy: ComponentHierarchy, context: VariableTextContext,
                                 settings: FieldWithPopupSettings = FieldWithPopupSettings.default)
	extends FieldWithPopupSettingsWrapper[FieldWithPopupFactory]
		with VariableTextContextualFactory[FieldWithPopupFactory]
		with PartOfComponentHierarchy
{
	// COMPUTED ------------------------
	
	/**
	 * Initializes this factory using contextual information
	 * @param popupContext Implicit pop-up window-creation context
	 * @param exc Implicit execution context
	 * @return A prepared copy of this factory
	 * @see [[withPopupContext]]
	 */
	def contextual(implicit popupContext: ReachWindowContext, exc: ExecutionContext) =
		withPopupContext(popupContext)
	
	
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override def withContext(context: VariableTextContext): FieldWithPopupFactory = copy(context = context)
	override def withSettings(settings: FieldWithPopupSettings) = copy(settings = settings)
	
	
	// OTHER    ---------------------------
	
	/**
	 * Specifies the pop-up window-creation context, so that the field construction may be finalized
	 * @param context Context used when creating pop-up windows
	 * @param exc Implicit execution context
	 * @return A prepared copy of this factory
	 */
	def withPopupContext(context: ReachWindowContext)(implicit exc: ExecutionContext) =
		new PreparedFieldWithPopupFactory(context)
	
	
	// NESTED   ---------------------------
	
	class PreparedFieldWithPopupFactory(popupContext: ReachWindowContext)(implicit exc: ExecutionContext)
	{
		/**
		 * Creates a new field.
		 * @param emptyFlag A flag that contains true while this field's main content is empty
		 * @param makeField A function that accepts field-creation context information ([[FieldCreationContext]]),
		 *                  and yields the component to wrap inside.
		 * @param makePopupContent A function that accepts an initialized component factory and yields the
		 *                         component to place inside the pop-up window
		 * @tparam C Type of the field component created
		 * @return A new field
		 */
		def apply[C <: ReachComponent with Focusable](emptyFlag: Flag)
		                                             (makeField: FieldCreationContext => C)
		                                             (makePopupContent: ContextualMixed[VariableReachContentWindowContext] => ReachComponent) =
			_apply[C](emptyFlag, makeField, _ => None, makePopupContent)
		/**
		 * Creates a new field. Includes an extra hint label.
		 * @param emptyFlag A flag that contains true while this field's main content is empty
		 * @param makeField A function that accepts field-creation context information ([[FieldCreationContext]]),
		 *                  and yields the component to wrap inside.
		 * @param makeRightHintLabel A function that accepts contextual information ([[ExtraFieldCreationContext]]),
		 *                           and yields the right-side hint label / component in an open format.
		 * @param makePopupContent A function that accepts an initialized component factory and yields the
		 *                         component to place inside the pop-up window
		 * @tparam C Type of the field component created
		 * @return A new field
		 */
		def withRightHintLabel[C <: ReachComponent with Focusable](emptyFlag: Flag)
		                                                          (makeField: FieldCreationContext => C)
		                                                          (makeRightHintLabel: ExtraFieldCreationContext[C] => Open[ReachComponent, Any])
		                                                          (makePopupContent: ContextualMixed[VariableReachContentWindowContext] => ReachComponent) =
			_apply[C](emptyFlag, makeField, c => Some(makeRightHintLabel(c)), makePopupContent)
		/**
		 * Creates a new field. Includes an extra hint label.
		 * @param emptyFlag A flag that contains true while this field's main content is empty
		 * @param makeField A function that accepts field-creation context information ([[FieldCreationContext]]),
		 *                  and yields the component to wrap inside.
		 * @param makeRightHintLabel A function that accepts contextual information ([[ExtraFieldCreationContext]]),
		 *                           and yields the right-side hint label / component in an open format, if applicable.
		 * @param makePopupContent A function that accepts an initialized component factory and yields the
		 *                         component to place inside the pop-up window
		 * @tparam C Type of the field component created
		 * @return A new field
		 */
		def withPossibleRightHintLabel[C <: ReachComponent with Focusable](emptyFlag: Flag)
		                                                                  (makeField: FieldCreationContext => C)
		                                                                  (makeRightHintLabel: ExtraFieldCreationContext[C] => Option[Open[ReachComponent, Any]])
		                                                                  (makePopupContent: ContextualMixed[VariableReachContentWindowContext] => ReachComponent) =
			_apply[C](emptyFlag, makeField, makeRightHintLabel, makePopupContent)
		
		private def _apply[C <: ReachComponent with Focusable](emptyFlag: Flag, makeField: FieldCreationContext => C,
		                                                       makeRightHintLabel: ExtraFieldCreationContext[C] => Option[Open[ReachComponent, Any]],
		                                                       makePopupContent: ContextualMixed[VariableReachContentWindowContext] => ReachComponent) =
			new FieldWithPopup[C](hierarchy, context, popupContext, emptyFlag, settings)(makeField)(makeRightHintLabel)(
				makePopupContent)
	}
}

/**
 * Used for defining field with popup creation settings outside the component building process
 * @author Mikko Hilpinen
 * @since 14.09.2025, v1.7
 */
case class FieldWithPopupSetup(settings: FieldWithPopupSettings = FieldWithPopupSettings.default)
	extends FieldWithPopupSettingsWrapper[FieldWithPopupSetup]
		with CCF[VariableTextContext, FieldWithPopupFactory]
{
	// IMPLEMENTED	--------------------
	
	override def withContext(hierarchy: ComponentHierarchy, context: VariableTextContext) =
		FieldWithPopupFactory(hierarchy, context, settings)
	override def withSettings(settings: FieldWithPopupSettings) = copy(settings = settings)
}

object FieldWithPopup extends FieldWithPopupSetup()
{
	// ATTRIBUTES   ----------------
	
	/**
	 * Minimum duration between possible pop-up close & open events
	 */
	private val transitionDuration = 0.2.seconds
	
	
	// OTHER	--------------------
	
	def apply(settings: FieldWithPopupSettings) = withSettings(settings)
}

/**
  * A field wrapper class that occasionally displays a pop-up window
  * @author Mikko Hilpinen
  * @since 12.9.2025, v1.7
  * @tparam C Type of component inside the field
  */
class FieldWithPopup[C <: ReachComponent with Focusable](override val hierarchy: ComponentHierarchy,
                                                         context: VariableTextContext, popupContext: ReachWindowContext,
                                                         emptyFlag: Flag, settings: FieldWithPopupSettings)
                                                        (makeField: FieldCreationContext => C)
                                                        (makeRightHintLabel: ExtraFieldCreationContext[C] => Option[Open[ReachComponent, Any]])
                                                        (makePopupContent: ContextualMixed[VariableReachContentWindowContext] => ReachComponent)
                                                        (implicit exc: ExecutionContext)
	extends ReachComponentWrapper with FocusableWithStateWrapper with CanDisplayPopup
{
	// ATTRIBUTES	------------------------------
	
	/**
	 * Contains the last time the pop-up window opened.
	 * Used for ensuring that the window is not immediately closed.
	 */
	private var _lastPopupOpenTime = Now.toInstant
	/**
	 * Tracks close time in order to now immediately open the pop-up afterwards
	 */
	private var _lastPopupCloseTime = Now.toInstant
	
	/**
	 * A mutable pointer that contains the pop-up window.
	 * Contains None while no pop-up window has been initialized.
	 */
	private val popupP = Volatile.lockable.empty[Window]
	/**
	 * A pointer that contains the linked pop-up window, whether open or hidden.
	 * Contains None in situations where no pop-up window has been initialized / is available.
	 */
	lazy val popupPointer = popupP.readOnly
	private val hasPopupFlag: Flag = popupP.lightMap { _.isDefined }
	/**
	  * A pointer which shows whether a pop-up is being displayed
	  */
	override lazy val popupVisibleFlag: Flag = popupP.flatMap {
		case Some(window) => window.fullyVisibleFlag
		case None => AlwaysFalse
	}
	/**
	  * A pointer which contains true while a pop-up is hidden
	  */
	override lazy val popupHiddenFlag = !popupVisibleFlag
	
	// Merges the expand and the collapse icons, if necessary
	private lazy val rightIconP: Changing[SingleColorIcon] = {
		// Case: No expand or collapse icon defined, or an always-present right-side icon is defined
		//       => Uses only the right-side icon from settings
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
									popupVisibleFlag.lightMap { visible => if (visible) collapseIcon else expandIcon }
								else {
									val (smaller, larger) = Pair(expandIcon, collapseIcon)
										.minMaxBy { _.size.area }.toTuple
									val targetSize = smaller.size
									val shrunkIcon = SingleColorIcon(
										larger.original.fittingWithin(targetSize).paintedToCanvas(targetSize),
										larger.standardSize)
									
									val (newExpandIcon, newCollapseIcon) =
										if (smaller == expandIcon) smaller -> shrunkIcon else shrunkIcon -> smaller
									popupVisibleFlag
										.lightMap { visible => if (visible) newCollapseIcon else newExpandIcon }
								}
							// Case: Only expand icon is defined => Doesn't use collapse icon
							case None => Fixed(expandIcon)
						}
						// The settings-specified right-side icon still overrides the expand/collapse icon, when present
						if (settings.rightIconPointer.existsFixed { _.isEmpty })
							expandOrCollapsePointer
						else
							settings.rightIconPointer.mergeWith(expandOrCollapsePointer) { _.nonEmptyOrElse(_) }
							
					// Case: Only collapse icon defined => Uses that when no other icon is present
					case None =>
						if (settings.rightIconPointer.existsFixed { _.isEmpty })
							Fixed(collapse)
						else
							settings.rightIconPointer.lightMap { _.nonEmptyOrElse(collapse) }
				}
			}
	}
	
	/**
	  * Field wrapped by this field
	  */
	val field = Field.withContext(hierarchy, context)
		.withSettings(settings.fieldSettings.withRightIconPointer(rightIconP))
		.apply(emptyFlag)(makeField)(makeRightHintLabel)
	
	/**
	  * A pointer that contains true while the pop-up window is NOT displayed, but only while this field has focus
	  */
	lazy val popupHiddenWhileFocusedFlag = popupHiddenFlag && field.focusFlag
	
	private lazy val appliedPopUpContext = {
		val base = VariableReachContentWindowContext(popupContext, context)
		// Applies the field's background to the pop-up (optional feature)
		if (settings.appliesFieldBackgroundInPopup)
			base.withBackgroundPointer(field.innerBackgroundPointer)
		else
			base
	}
	
	
	// INITIAL CODE	-----------------------------
	
	// Initializes keyboard & mouse listening once attached to the component hierarchy
	if (settings.activationKeys.nonEmpty || settings.hidesPopupAfterMouseRelease)
		linkedFlag.onceSet {
			if (settings.activationKeys.nonEmpty)
				KeyboardEvents += new FieldKeyListener
			if (settings.hidesPopupAfterMouseRelease)
				CommonMouseEvents += new PopupHideMouseListener
		}
	// Whenever this field becomes detached from the component hierarchy, disposes the pop-up window
	linkedFlag.addListenerWhile(hasPopupFlag) { e =>
		if (!e.newValue)
			popupP.pop().foreach { _.close() }
	}
	// Once permanently detached, locks the pop-up pointer as well
	linkedFlag.onceChangingStops { popupP.lock() }
	
	// When gains focus, displays the pop-up. Hides the pop-up when focus is lost.
	focusFlag.addContinuousListenerAndSimulateEvent(false) { e =>
		if (e.newValue) {
			if (Now - _lastPopupCloseTime > transitionDuration)
				showPopup()
		}
		else
			popup.foreach { _.visible = false }
	}
	
	
	// COMPUTED	---------------------------------
	
	/**
	 * @return The currently used pop-up window. Not necessarily visible at the moment.
	 *         None if no window has been initialized yet / if none is available at the moment.
	 */
	def popup = popupP.value
	
	/**
	 * The last time the pop-up window was opened.
	 */
	def lastPopupOpenTime = _lastPopupOpenTime
	/**
	 * The last time the pop-up window was closed.
	 */
	def lastPopupCloseTime = _lastPopupCloseTime
	
	
	// IMPLEMENTED	-----------------------------
	
	override protected def wrapped = field
	override protected def focusable = field
	
	override def showPopup(): Option[Window] = {
		// Case: Linked => Creates and/or displays the pop-up window
		if (linkedFlag.value) {
			_lastPopupOpenTime = Now
			Some(popupP.mutate { popup =>
				popup.filter { _.hasNotClosed } match {
					// Case: Pop-up already created => Displays it
					case Some(popup) =>
						if (popup.isNotFullyVisible)
							popup.display()
						popup -> Some(popup)
					
					// Case: No pop-up available yet => Creates and displays a new window
					case None =>
						val popup = createPopup()
						popup.display()
						popup -> Some(popup)
				}
			})
		}
		// Case: Not attached => Won't open the pop-up
		else
			None
	}
	override def hidePopup() = {
		val window = popupP.value.filter { _.hasNotClosed }
		window.foreach { window =>
			_lastPopupCloseTime = Now
			window.visible = false
		}
		window
	}
	
	
	// OTHER	---------------------------------
	
	private def createPopup(): Window = {
		// Creates the pop-up
		val popup = appliedPopUpContext.use { implicit windowContext =>
			field.createOwnedWindow(settings.popupAlignment, matchEdgeLength = settings.popupMatchesFieldLength) {
				(hierarchy, windowP) =>
					makePopupContent(ContextualMixed(hierarchy, windowContext.withWindowPointer(windowP)))
			}
		}
		// Remembers when the pop-up closes
		popup.fullyVisibleFlag.addListener { e =>
			if (e.newValue)
				_lastPopupOpenTime = Now
			else
				_lastPopupCloseTime = Now
		}
		// Hides the pop-up under certain conditions
		popup.focusKeyStateHandler += new PopupKeyListener(popup)
		if (settings.hidesPopupOnFocusLoss)
			popup.focusFlag.addListener { e =>
				if (!e.newValue)
					popup.visible = false
				ChangeResponse.continueUnless(popup.hasClosed)
			}
		// Returns the pop-up window
		popup.window
	}
	
	
	// NESTED	---------------------------------
	
	/**
	  * Listens for mouse events inside the pop-up and closes it if necessary.
	  */
	private class PopupHideMouseListener extends MouseButtonStateListener
	{
		// ATTRIBUTES   ----------------------
		
		override val handleCondition: Flag = linkedFlag && popupVisibleFlag
		override val mouseButtonStateEventFilter = MouseButtonStateEvent.filter.left
		
		// Only closes the pop-up on mouse release if it was visible on the previous mouse press
		private val closeOnReleaseFlag = Switch()
		
		
		// IMPLEMENTED  ----------------------
		
		override def onMouseButtonStateEvent(event: MouseButtonStateEvent): ConsumeChoice = {
			if (Now - _lastPopupOpenTime >= transitionDuration) {
				// Case: Mouse press => Saves the pop-up status in order to react correctly to the next mouse release
				if (event.pressed) {
					closeOnReleaseFlag.set()
					Preserve
				}
				// Case: Mouse release => Hides the pop-up if it was visible when the mouse was pressed
				else if (closeOnReleaseFlag.reset()) {
					popup.foreach { _.visible = false }
					Consume("Pop-up closing")
				}
				else
					Preserve
			}
			else
				Preserve
		}
	}
	
	/**
	  * Listens to keyboard events inside the wrapped field and opens the pop-up when appropriate.
	 * Assumes that activation keys have been specified.
	  */
	private class FieldKeyListener extends KeyStateListener
	{
		// ATTRIBUTES	-------------------------
		
		// Is interested in key events while the field has focus and pop-up is not open
		override val handleCondition: Flag = linkedFlag && popupHiddenWhileFocusedFlag && field.enabledFlag
		
		// Listens to the activation key -presses
		override val keyStateEventFilter = KeyStateEvent.filter.pressed && KeyStateEvent.filter(settings.activationKeys)
		
		
		// IMPLEMENTED	-------------------------
		
		override def onKeyState(event: KeyStateEvent) = showPopup()
	}
	
	private class PopupKeyListener(popup: Window) extends KeyStateListener
	{
		// ATTRIBUTES	-------------------------
		
		// Listens to close key -releases
		override lazy val keyStateEventFilter =
			KeyStateEvent.filter.released && KeyStateEvent.filter(settings.closeKeys + Esc)
		
		
		// IMPLEMENTED	-------------------------
		
		// Only reacts to events while the pop-up is visible
		override def handleCondition: Flag = popup.fullyVisibleFlag
		
		override def onKeyState(event: KeyStateEvent) = {
			if (Now - _lastPopupOpenTime >= transitionDuration) {
				// Hides the pop-up
				popup.visible = false
				// On tabulator press, yields focus afterwards
				if (event.index == Tab.index)
					Delay(0.1.seconds) { yieldFocus(if (event.keyboardState(Shift)) Negative else Positive) }
			}
		}
	}
}
