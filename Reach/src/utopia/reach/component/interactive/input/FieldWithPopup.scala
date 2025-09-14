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
import utopia.reach.component.factory.FromContextComponentFactoryFactory.Ccff
import utopia.reach.component.factory.contextual.VariableTextContextualFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.interactive.input.FieldWithPopup.ignoreFocusAfterCloseDuration
import utopia.reach.component.label.image.ViewImageLabelSettings
import utopia.reach.component.template.focus.{Focusable, FocusableWithStateWrapper}
import utopia.reach.component.template.{PartOfComponentHierarchy, ReachComponent, ReachComponentWrapper}
import utopia.reach.component.wrapper.OpenComponent
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
	def appliesFieldBackgroundInPopUp: Boolean
	/**
	 * Whether the opened pop-up window will be automatically hidden after a mouse release -event.
	 */
	def hidesPopUpAfterMouseRelease: Boolean
	
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
	def withFieldBackgroundInPopUp(applyBackground: Boolean): Repr
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
	def withHidePopUpAfterMouseRelease(hide: Boolean): Repr
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
 * @param appliesFieldBackgroundInPopUp Whether the field's background color should become the
 *                                      pop-up window's background color, also
 * @param hidesPopUpAfterMouseRelease   Whether the opened pop-up window will be automatically
 *                                      hidden after a mouse release -event.
 * @author Mikko Hilpinen
 * @since 14.09.2025, v1.7
 */
case class FieldWithPopupSettings(fieldSettings: FieldSettings = FieldSettings.default,
                                  expandAndCollapseIcon: Pair[SingleColorIcon] = Pair.twice(SingleColorIcon.empty),
                                  activationKeys: Set[Key] = Set[Key](), closeKeys: Set[Key] = Set[Key](),
                                  popupAlignment: Alignment = Alignment.Right, popupMatchesFieldLength: Boolean = false,
                                  appliesFieldBackgroundInPopUp: Boolean = false,
                                  hidesPopUpAfterMouseRelease: Boolean = false)
	extends FieldWithPopupSettingsLike[FieldWithPopupSettings]
{
	// IMPLEMENTED	--------------------
	
	override def withActivationKeys(keys: Set[Key]) = copy(activationKeys = keys)
	override def withCloseKeys(keys: Set[Key]) = copy(closeKeys = keys)
	override def withExpandAndCollapseIcon(icons: Pair[SingleColorIcon]) =
		copy(expandAndCollapseIcon = icons)
	override def withFieldBackgroundInPopUp(applyBackground: Boolean) =
		copy(appliesFieldBackgroundInPopUp = applyBackground)
	override def withFieldSettings(settings: FieldSettings) = copy(fieldSettings = settings)
	override def withHidePopUpAfterMouseRelease(hide: Boolean) =
		copy(hidesPopUpAfterMouseRelease = hide)
	override def withPopupAlignment(alignment: Alignment) = copy(popupAlignment = alignment)
	override def withPopupMatchesFieldLength(matchLength: Boolean) =
		copy(popupMatchesFieldLength = matchLength)
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
	override def appliesFieldBackgroundInPopUp = settings.appliesFieldBackgroundInPopUp
	override def expandAndCollapseIcon = settings.expandAndCollapseIcon
	override def fieldSettings = settings.fieldSettings
	override def hidesPopUpAfterMouseRelease = settings.hidesPopUpAfterMouseRelease
	override def popupAlignment = settings.popupAlignment
	override def popupMatchesFieldLength = settings.popupMatchesFieldLength
	
	override def withActivationKeys(keys: Set[Key]) = mapSettings { _.withActivationKeys(keys) }
	override def withCloseKeys(keys: Set[Key]) = mapSettings { _.withCloseKeys(keys) }
	override def withExpandAndCollapseIcon(icons: Pair[SingleColorIcon]) =
		mapSettings { _.withExpandAndCollapseIcon(icons) }
	override def withFieldBackgroundInPopUp(applyBackground: Boolean) =
		mapSettings { _.withFieldBackgroundInPopUp(applyBackground) }
	override def withFieldSettings(settings: FieldSettings) = mapSettings { _.withFieldSettings(settings) }
	override def withHidePopUpAfterMouseRelease(hide: Boolean) =
		mapSettings { _.withHidePopUpAfterMouseRelease(hide) }
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
		                                                          (makeRightHintLabel: ExtraFieldCreationContext[C] => OpenComponent[ReachComponent, Any])
		                                                          (makePopupContent: ContextualMixed[VariableReachContentWindowContext] => ReachComponent) =
			_apply[C](emptyFlag, makeField, c => Some(makeRightHintLabel(c)), makePopupContent)
		
		private def _apply[C <: ReachComponent with Focusable](emptyFlag: Flag, makeField: FieldCreationContext => C,
		                                                       makeRightHintLabel: ExtraFieldCreationContext[C] => Option[OpenComponent[ReachComponent, Any]],
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
		with Ccff[VariableTextContext, FieldWithPopupFactory]
{
	// IMPLEMENTED	--------------------
	
	override def withContext(hierarchy: ComponentHierarchy, context: VariableTextContext) =
		FieldWithPopupFactory(hierarchy, context, settings)
	override def withSettings(settings: FieldWithPopupSettings) = copy(settings = settings)
}

object FieldWithPopup extends FieldWithPopupSetup()
{
	// ATTRIBUTES   ----------------
	
	private val ignoreFocusAfterCloseDuration = 0.2.seconds
	
	
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
                                                        (makeRightHintLabel: ExtraFieldCreationContext[C] => Option[OpenComponent[ReachComponent, Any]])
                                                        (makePopupContent: ContextualMixed[VariableReachContentWindowContext] => ReachComponent)
                                                        (implicit exc: ExecutionContext)
	extends ReachComponentWrapper with FocusableWithStateWrapper
{
	// ATTRIBUTES	------------------------------
	
	// Tracks close time in order to now immediately open the pop-up afterwards
	private var lastPopupCloseTime = Now.toInstant
	
	/**
	 * A mutable pointer that contains the pop-up window.
	 * Contains None while no pop-up window has been initialized.
	 */
	private val popupP = Volatile.lockable.empty[Window]
	private val hasPopupFlag: Flag = popupP.lightMap { _.isDefined }
	/**
	  * A pointer which shows whether a pop-up is being displayed
	  */
	val popUpVisibleFlag: Flag = popupP.flatMap {
		case Some(window) => window.fullyVisibleFlag
		case None => AlwaysFalse
	}
	/**
	  * A pointer which contains true while a pop-up is hidden
	  */
	lazy val popUpHiddenFlag = !popUpVisibleFlag
	/**
	  * A pointer that contains a timestamp of the latest pop-up visibility change event
	  */
	val lastPopupVisibilityChangedPointer = popUpVisibleFlag.strongMap { _ => Now.toInstant }
	// Merges the expand and the collapse icons, if necessary
	private val rightIconP: Changing[SingleColorIcon] = {
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
									popUpVisibleFlag.lightMap { visible => if (visible) collapseIcon else expandIcon }
								else {
									val (smaller, larger) = Pair(expandIcon, collapseIcon)
										.minMaxBy { _.size.area }.toTuple
									val targetSize = smaller.size
									val shrunkIcon = SingleColorIcon(
										larger.original.fittingWithin(targetSize).paintedToCanvas(targetSize),
										larger.standardSize)
									
									val (newExpandIcon, newCollapseIcon) =
										if (smaller == expandIcon) smaller -> shrunkIcon else shrunkIcon -> smaller
									popUpVisibleFlag
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
	lazy val popUpHiddenWhileFocusedFlag = popUpHiddenFlag && field.focusFlag
	
	private lazy val appliedPopUpContext = {
		val base = VariableReachContentWindowContext(popupContext, context)
		// Applies the field's background to the pop-up (optional feature)
		if (settings.appliesFieldBackgroundInPopUp)
			base.withBackgroundPointer(field.innerBackgroundPointer)
		else
			base
	}
	
	
	// INITIAL CODE	-----------------------------
	
	// Initializes keyboard & mouse listening once attached to the component hierarchy
	if (settings.activationKeys.nonEmpty || settings.hidesPopUpAfterMouseRelease)
		linkedFlag.onceSet {
			if (settings.activationKeys.nonEmpty)
				KeyboardEvents += new FieldKeyListener
			if (settings.hidesPopUpAfterMouseRelease)
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
			if (Now - lastPopupCloseTime > ignoreFocusAfterCloseDuration)
				openPopup()
		}
		else
			cachedPopup.foreach { _.visible = false }
	}
	
	
	// COMPUTED	---------------------------------
	
	/**
	 * @return Whether a pop-up window is currently being displayed
	 */
	def showingPopup = popUpVisibleFlag.value
	def showingPopup_=(show: Boolean) = {
		if (show) openPopup() else hidePopup()
	}
	/**
	 * @return Whether the pop-up window is currently hidden / not displayed
	 */
	def popupHidden = !showingPopup
	def popupHidden_=(hidden: Boolean) = showingPopup = !hidden
	
	/**
	 * @return Timestamp of the last pop-up window visibility change.
	 *         If the pop-up hasn't been displayed yet, yields the creation time of this component.
	 */
	def lastPopupVisibilityChangedTime = lastPopupVisibilityChangedPointer.value
	
	private def cachedPopup = popupP.value
	
	
	// IMPLEMENTED	-----------------------------
	
	override protected def wrapped = field
	override protected def focusable = field
	
	
	// OTHER	---------------------------------
	
	/**
	  * Displays this field's pop-up window, but only if this component is attached to the main component hierarchy
	 * @return The displayed pop-up window. None if this field was not attached to the main component hierarchy.
	  */
	def openPopup(): Option[Window] = {
		// Case: Linked => Creates and/or displays the pop-up window
		if (linkedFlag.value)
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
		// Case: Not attached => Won't open the pop-up
		else
			None
	}
	/**
	 * Hides the currently displayed pop-up window
	 * @return The pop-up window. None if no window is open at the moment.
	 */
	def hidePopup() = {
		val window = popupP.value
		if (popUpVisibleFlag.value)
			window.foreach { _.visible = false }
		window.filter { _.hasNotClosed }
	}
	
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
	
	/**
	  * Listens for mouse events inside the pop-up and closes it if necessary.
	  */
	private class PopupHideMouseListener extends MouseButtonStateListener
	{
		// ATTRIBUTES   ----------------------
		
		override val handleCondition: Flag = linkedFlag && popUpVisibleFlag
		override val mouseButtonStateEventFilter = MouseButtonStateEvent.filter.left
		
		// Only closes the pop-up on mouse release if it was visible on the previous mouse press
		private val closeOnReleaseFlag = Switch()
		
		
		// IMPLEMENTED  ----------------------
		
		override def onMouseButtonStateEvent(event: MouseButtonStateEvent): ConsumeChoice = {
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
	
	/**
	  * Listens to keyboard events inside the wrapped field and opens the pop-up when appropriate.
	 * Assumes that activation keys have been specified.
	  */
	private class FieldKeyListener extends KeyStateListener
	{
		// ATTRIBUTES	-------------------------
		
		// Is interested in key events while the field has focus and pop-up is not open
		override val handleCondition: Flag = linkedFlag && popUpHiddenWhileFocusedFlag && field.enabledFlag
		
		// Listens to the activation key -presses
		override val keyStateEventFilter = KeyStateEvent.filter.pressed && KeyStateEvent.filter(settings.activationKeys)
		
		
		// IMPLEMENTED	-------------------------
		
		override def onKeyState(event: KeyStateEvent) = openPopup()
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
			// Hides the pop-up
			popup.visible = false
			// On tabulator press, yields focus afterwards
			if (event.index == Tab.index)
				Delay(0.1.seconds) { yieldFocus(if (event.keyboardState(Shift)) Negative else Positive) }
		}
	}
}
