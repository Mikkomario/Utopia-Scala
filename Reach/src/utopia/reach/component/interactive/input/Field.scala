package utopia.reach.component.interactive.input

import utopia.firmament.context.ComponentCreationDefaults
import utopia.firmament.context.color.VariableColorContext
import utopia.firmament.context.text.VariableTextContext
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.drawing.view.{BackgroundViewDrawer, BorderViewDrawer, TextViewDrawer}
import utopia.firmament.image.SingleColorIcon
import utopia.firmament.localization.LocalizedString
import utopia.firmament.model.stack.LengthExtensions._
import utopia.firmament.model.stack.StackInsets
import utopia.firmament.model.{Border, TextDrawContext}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.caching.cache.Cache
import utopia.flow.collection.immutable.{Pair, Single}
import utopia.flow.event.listener.ChangeListener
import utopia.flow.operator.enumeration.End
import utopia.flow.operator.enumeration.End.{First, Last}
import utopia.flow.util.Mutate
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.immutable.eventful.{AlwaysFalse, AlwaysTrue, Fixed}
import utopia.flow.view.mutable.eventful.ResettableFlag
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.graphics.MeasuredText
import utopia.genesis.graphics.Priority.High
import utopia.genesis.handling.event.mouse.MouseMoveListener
import utopia.paradigm.color.{Color, ColorRole}
import utopia.paradigm.enumeration.LinearAlignment.Far
import utopia.paradigm.enumeration.{Alignment, Direction2D}
import utopia.paradigm.shape.shape2d.insets.Insets
import utopia.reach.component.factory.contextual.VariableTextContextualFactory
import utopia.reach.component.factory.{FromContextComponentFactoryFactory, Mixed}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.image.{ViewImageLabel, ViewImageLabelSettings}
import utopia.reach.component.label.text.ViewTextLabel
import utopia.reach.component.template.focus.{Focusable, FocusableWithState, FocusableWrapper}
import utopia.reach.component.template.{ConcreteReachComponent, PartOfComponentHierarchy, ReachComponent, ReachComponentWrapper}
import utopia.reach.component.wrapper.{Open, OpenComponent}
import utopia.reach.container.multi.ViewStack
import utopia.reach.container.wrapper.{Framing, FramingFactory}
import utopia.reach.focus.{FocusChangeListener, FocusStateTracker}

/**
  * A set of context variables provided when creating field contents
  * @param hierarchy   Component hierarchy to use
  * @param context    Variable field creation text context
  * @param focusListener     Focus listener to assign to the created component
  * @param promptDrawers     Custom drawers to assign for prompt drawing
  */
case class FieldCreationContext(hierarchy: ComponentHierarchy, context: VariableTextContext,
                                focusListener: FocusChangeListener, promptDrawers: Seq[CustomDrawer])
	extends PartOfComponentHierarchy

/**
  * A set of context variables provided when creating an additional right side label
  * @param content           Field content
  * @param lazyContext    The variable context prevalent in this area (lazy)
  * @tparam C Type of field contents
  */
case class ExtraFieldCreationContext[C](content: C, lazyContext: Lazy[VariableTextContext])
{
	/**
	  * @return Context prevalent in this area
	  */
	def context = lazyContext.value
}

/**
  * Common trait for field factories and settings
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 01.06.2023, v1.1
  */
trait FieldSettingsLike[+Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Pointer that determines the name of this field displayed on this field
	  */
	def fieldNamePointer: Changing[LocalizedString]
	/**
	  * Pointer that determines what text is displayed while there is no input
	  */
	def promptPointer: Changing[LocalizedString]
	/**
	  * Pointer that determines the hint to show underneath this field
	  */
	def hintPointer: Changing[LocalizedString]
	/**
	  * Pointer that determines an error message to display underneath this field
	  */
	def errorMessagePointer: Changing[LocalizedString]
	/**
	  * Pointer that determines external highlight state/style to apply
	  */
	def highlightPointer: Changing[Option[ColorRole]]
	/**
	  * @return A pointer that contains true while this field is interactive
	  */
	def enabledFlag: Flag
	/**
	  * Color (role) used to highlight the focused-state
	  */
	def focusColorRole: ColorRole
	/**
	  * Scaling factor applied to displayed hint, error and field name text, relative to the input text size
	  */
	def hintScaleFactor: Double
	/**
	  * Pointers that determines the icons to display on the left (first) and right (second) side of this field
	  */
	def iconPointers: Pair[Changing[SingleColorIcon]]
	/**
	  * Settings that defined how icons are displayed (if they are displayed)
	  */
	def imageSettings: ViewImageLabelSettings
	/**
	  * Whether field background should be filled with color.
	  * If false, outlined style will be used instead.
	  */
	def fillBackground: Boolean
	
	/**
	  * @param flag A flag that determines when this field is enabled
	  * @return Copy of this factory that applies the specified enabled flag
	  */
	def withEnabledFlag(flag: Flag): Repr
	/**
	  * Pointer that determines an error message to display underneath this field
	  * @param p New error message pointer to use.
	  *          Pointer that determines an error message to display underneath this field
	  * @return Copy of this factory with the specified error message pointer
	  */
	def withErrorMessagePointer(p: Changing[LocalizedString]): Repr
	/**
	  * Pointer that determines the name of this field displayed on this field
	  * @param p New field name pointer to use.
	  *          Pointer that determines the name of this field displayed on this field
	  * @return Copy of this factory with the specified field name pointer
	  */
	def withFieldNamePointer(p: Changing[LocalizedString]): Repr
	/**
	  * Whether field background should be filled with color.
	  * If false, outlined style will be used instead.
	  * @param fill New fill background to use.
	  *             Whether field background should be filled with color.
	  *             If false, outlined style will be used instead.
	  * @return Copy of this factory with the specified fill background
	  */
	def withFillBackground(fill: Boolean): Repr
	/**
	  * Color (role) used to highlight the focused-state
	  * @param color New focus color role to use.
	  *              Color (role) used to highlight the focused-state
	  * @return Copy of this factory with the specified focus color role
	  */
	def withFocusColorRole(color: ColorRole): Repr
	/**
	  * Pointer that determines external highlight state/style to apply
	  * @param p New highlight pointer to use.
	  *          Pointer that determines external highlight state/style to apply
	  * @return Copy of this factory with the specified highlight pointer
	  */
	def withHighlightPointer(p: Changing[Option[ColorRole]]): Repr
	/**
	  * Pointer that determines the hint to show underneath this field
	  * @param p New hint pointer to use.
	  *          Pointer that determines the hint to show underneath this field
	  * @return Copy of this factory with the specified hint pointer
	  */
	def withHintPointer(p: Changing[LocalizedString]): Repr
	/**
	  * Scaling factor applied to displayed hint, error and field name text, relative to the input text size
	  * @param scaling New hint scale factor to use.
	  *                Scaling factor applied to displayed hint, error and field name text, relative to the input text size
	  * @return Copy of this factory with the specified hint scale factor
	  */
	def withHintScaleFactor(scaling: Double): Repr
	/**
	  *
	  * Pointers that determines the icons to display on the left (first) and right (second) side of this field
	  * @param pointers New icon pointers to use.
	  *
	  *                                  Pointers that determines the icons to display on the left (first) and right (second) side of this field
	  * @return Copy of this factory with the specified icon pointers
	  */
	def withIconPointers(pointers: Pair[Changing[SingleColorIcon]]): Repr
	/**
	  * Settings that defined how icons are displayed (if they are displayed)
	  * @param settings New image settings to use.
	  *                 Settings that defined how icons are displayed (if they are displayed)
	  * @return Copy of this factory with the specified image settings
	  */
	def withImageSettings(settings: ViewImageLabelSettings): Repr
	/**
	  * Pointer that determines what text is displayed while there is no input
	  * @param p New prompt pointer to use.
	  *          Pointer that determines what text is displayed while there is no input
	  * @return Copy of this factory with the specified prompt pointer
	  */
	def withPromptPointer(p: Changing[LocalizedString]): Repr
	
	
	// COMPUTED	--------------------
	
	/**
	  * @return Pointer to the icon displayed on the left side of this field
	  */
	def leftIconPointer = iconPointers.first
	/**
	  * @return Pointer to the icon displayed on the right side of this field
	  */
	def rightIconPointer = iconPointers.second
	
	/**
	  * custom drawers from the wrapped view image label settings
	  */
	def imageCustomDrawers = imageSettings.customDrawers
	/**
	  * insets pointer from the wrapped view image label settings
	  */
	def imageInsetsPointer = imageSettings.insetsPointer
	/**
	  * alignment pointer from the wrapped view image label settings
	  */
	def imageAlignmentPointer = imageSettings.alignmentPointer
	/**
	  * color overlay pointer from the wrapped view image label settings
	  */
	def imageColorOverlayPointer = imageSettings.colorOverlayPointer
	/**
	  * image scaling pointer from the wrapped view image label settings
	  */
	def imageScalingPointer = imageSettings.imageScalingPointer
	/**
	  * uses low priority size from the wrapped view image label settings
	  */
	def imageUsesLowPrioritySize = imageSettings.usesLowPrioritySize
	
	/**
	  * @return Copy of this factory that uses the outlined style
	  */
	def outlined = withFillBackground(fill = false)
	/**
	  * @return Copy of this factory that uses the filled style
	  */
	def filled = withFillBackground(fill = true)
	
	
	// OTHER	--------------------
	
	def mapEnabledFlag(f: Mutate[Flag]) = withEnabledFlag(f(enabledFlag))
	def mapErrorMessagePointer(f: Changing[LocalizedString] => Changing[LocalizedString]) =
		withErrorMessagePointer(f(errorMessagePointer))
	def mapFieldNamePointer(f: Changing[LocalizedString] => Changing[LocalizedString]) =
		withFieldNamePointer(f(fieldNamePointer))
	def mapHighlightPointer(f: Changing[Option[ColorRole]] => Changing[Option[ColorRole]]) =
		withHighlightPointer(f(highlightPointer))
	def mapHintPointer(f: Changing[LocalizedString] => Changing[LocalizedString]) =
		withHintPointer(f(hintPointer))
	def mapIconPointers(f: Pair[Changing[SingleColorIcon]] => Pair[Changing[SingleColorIcon]]) =
		withIconPointers(f(iconPointers))
	def mapImageAlignmentPointer(f: Changing[Alignment] => Changing[Alignment]) =
		withImageAlignmentPointer(f(imageAlignmentPointer))
	def mapImageColorOverlayPointer(f: Option[Changing[Color]] => Option[Changing[Color]]) =
		withImageColorOverlayPointer(f(imageColorOverlayPointer))
	def mapImageScalingPointer(f: Changing[Double] => Changing[Double]) =
		withImageScalingPointer(f(imageScalingPointer))
	def mapImageInsetsPointer(f: Changing[StackInsets] => Changing[StackInsets]) =
		withImageInsetsPointer(f(imageInsetsPointer))
	def mapImageSettings(f: ViewImageLabelSettings => ViewImageLabelSettings) =
		withImageSettings(f(imageSettings))
	def mapPromptPointer(f: Changing[LocalizedString] => Changing[LocalizedString]) =
		withPromptPointer(f(promptPointer))
	
	def withIconPointer(pointer: Changing[SingleColorIcon], side: End) = mapIconPointers { _.withSide(pointer, side) }
	def withLeftIconPointer(pointer: Changing[SingleColorIcon]) = withIconPointer(pointer, First)
	def withRightIconPointer(pointer: Changing[SingleColorIcon]) = withIconPointer(pointer, Last)
	def withIcon(icon: SingleColorIcon, side: End) = withIconPointer(Fixed(icon), side)
	def withLeftIcon(icon: SingleColorIcon) = withIcon(icon, First)
	def withRightIcon(icon: SingleColorIcon) = withIcon(icon, Last)
	
	def mapIconPointer(side: End)(f: Changing[SingleColorIcon] => Changing[SingleColorIcon]) =
		mapIconPointers { _.mapSide(side)(f) }
	def mapIcon(side: End)(f: SingleColorIcon => SingleColorIcon) = mapIconPointer(side) { _.map(f) }
	def mapLeftIconPointer(f: Changing[SingleColorIcon] => Changing[SingleColorIcon]) = mapIconPointer(First)(f)
	def mapLeftIcon(f: SingleColorIcon => SingleColorIcon) = mapIcon(First)(f)
	def mapRightIconPointer(f: Changing[SingleColorIcon] => Changing[SingleColorIcon]) = mapIconPointer(Last)(f)
	def mapRightIcon(f: SingleColorIcon => SingleColorIcon) = mapIcon(Last)(f)
	
	/**
	  * @param p Pointer that determines the image drawing location within this component
	  * @return Copy of this factory with the specified image alignment pointer
	  */
	def withImageAlignmentPointer(p: Changing[Alignment]) =
		withImageSettings(imageSettings.withAlignmentPointer(p))
	/**
	  * @param p Pointer that, when defined, places a color overlay over the drawn image
	  * @return Copy of this factory with the specified image color overlay pointer
	  */
	def withImageColorOverlayPointer(p: Option[Changing[Color]]) =
		withImageSettings(imageSettings.withColorOverlayPointer(p))
	/**
	  * @param drawers Custom drawers to assign to created components
	  * @return Copy of this factory with the specified image custom drawers
	  */
	def withImageCustomDrawers(drawers: Seq[CustomDrawer]) =
		withImageSettings(imageSettings.withCustomDrawers(drawers))
	/**
	  * @param p Pointer that determines image scaling, in addition to the original image scaling
	  * @return Copy of this factory with the specified image image scaling pointer
	  */
	def withImageScalingPointer(p: Changing[Double]) =
		withImageSettings(imageSettings.withImageScalingPointer(p))
	/**
	  * @param p Pointer that determines the insets placed around the image
	  * @return Copy of this factory with the specified image insets pointer
	  */
	def withImageInsetsPointer(p: Changing[StackInsets]) = withImageSettings(imageSettings
		.withInsetsPointer(p))
	/**
	  * @param lowPriority Whether this label should use low priority size constraints
	  * @return Copy of this factory with the specified image uses low priority size
	  */
	def withImageUsesLowPrioritySize(lowPriority: Boolean) =
		withImageSettings(imageSettings.withUseLowPrioritySize(lowPriority))
	
	/**
	  * @param name Name displayed within this field
	  * @return Copy of this factory with the specified field name
	  */
	def withFieldName(name: LocalizedString) = withFieldNamePointer(Fixed(name))
	/**
	  * @param hint Hint to display under the created fields
	  * @return Copy of this factory with the specified hint
	  */
	def withHint(hint: LocalizedString) = withHintPointer(Fixed(hint))
	/**
	  * @param prompt Prompt to display at the input area
	  * @return Copy of this factory with the specified prompt
	  */
	def withPrompt(prompt: LocalizedString) = withPromptPointer(Fixed(prompt))
}

object FieldSettings
{
	// ATTRIBUTES	--------------------
	
	lazy val default = apply()
}
/**
  * Combined settings used when constructing fields
  * @param fieldNamePointer    Pointer that determines the name of this field displayed on this field
  * @param promptPointer       Pointer that determines what text is displayed while there is no input
  * @param hintPointer         Pointer that determines the hint to show underneath this field
  * @param errorMessagePointer Pointer that determines an error message to display underneath this field
  * @param highlightPointer    Pointer that determines external highlight state/style to apply
  * @param enabledFlag         A flag that contains true while this field is interactive
  * @param focusColorRole      Color (role) used to highlight the focused-state
  * @param hintScaleFactor     Scaling factor applied to displayed hint, error and field name text,
  *                            relative to the input text size
  * @param iconPointers        Pointers that determines the icons to display on the left (first) and right (second)
  *                            side of this field
  * @param imageSettings       Settings that defined how icons are displayed (if they are displayed)
  * @param fillBackground      Whether field background should be filled with color.
  *                            If false, outlined style will be used instead.
  * @author Mikko Hilpinen
  * @since 01.06.2023, v1.1
  */
case class FieldSettings(fieldNamePointer: Changing[LocalizedString] = LocalizedString.alwaysEmpty,
                         promptPointer: Changing[LocalizedString] = LocalizedString.alwaysEmpty,
                         hintPointer: Changing[LocalizedString] = LocalizedString.alwaysEmpty,
                         errorMessagePointer: Changing[LocalizedString] = LocalizedString.alwaysEmpty,
                         highlightPointer: Changing[Option[ColorRole]] = Fixed(None), enabledFlag: Flag = AlwaysTrue,
                         focusColorRole: ColorRole = ColorRole.Secondary, hintScaleFactor: Double = 0.7,
                         iconPointers: Pair[Changing[SingleColorIcon]] = Pair.twice(SingleColorIcon.alwaysEmpty),
                         imageSettings: ViewImageLabelSettings = ViewImageLabelSettings.default,
                         fillBackground: Boolean = ComponentCreationDefaults.useFillStyleFields)
	extends FieldSettingsLike[FieldSettings]
{
	// IMPLEMENTED	--------------------
	
	override def withEnabledFlag(flag: Flag): FieldSettings = copy(enabledFlag = flag)
	override def withErrorMessagePointer(p: Changing[LocalizedString]) = copy(errorMessagePointer = p)
	override def withFieldNamePointer(p: Changing[LocalizedString]) = copy(fieldNamePointer = p)
	override def withFillBackground(fill: Boolean) = copy(fillBackground = fill)
	override def withFocusColorRole(color: ColorRole) = copy(focusColorRole = color)
	override def withHighlightPointer(p: Changing[Option[ColorRole]]) = copy(highlightPointer = p)
	override def withHintPointer(p: Changing[LocalizedString]) = copy(hintPointer = p)
	override def withHintScaleFactor(scaling: Double) = copy(hintScaleFactor = scaling)
	override def withIconPointers(pointers: Pair[Changing[SingleColorIcon]]) = copy(iconPointers = pointers)
	override def withImageSettings(settings: ViewImageLabelSettings) = copy(imageSettings = settings)
	override def withPromptPointer(p: Changing[LocalizedString]) = copy(promptPointer = p)
}

/**
  * Common trait for factories that wrap a field settings instance
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 01.06.2023, v1.1
  */
trait FieldSettingsWrapper[+Repr] extends FieldSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Settings wrapped by this instance
	  */
	protected def settings: FieldSettings
	
	/**
	  * @return Copy of this factory with the specified settings
	  */
	def withSettings(settings: FieldSettings): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def enabledFlag: Flag = settings.enabledFlag
	override def errorMessagePointer = settings.errorMessagePointer
	override def fieldNamePointer = settings.fieldNamePointer
	override def fillBackground = settings.fillBackground
	override def focusColorRole = settings.focusColorRole
	override def highlightPointer = settings.highlightPointer
	override def hintPointer = settings.hintPointer
	override def hintScaleFactor = settings.hintScaleFactor
	override def iconPointers = settings.iconPointers
	override def imageSettings = settings.imageSettings
	override def promptPointer = settings.promptPointer
	
	override def withEnabledFlag(flag: Flag): Repr = mapSettings { _.withEnabledFlag(flag) }
	override def withErrorMessagePointer(p: Changing[LocalizedString]) =
		mapSettings { _.withErrorMessagePointer(p) }
	override def withFieldNamePointer(p: Changing[LocalizedString]) =
		mapSettings { _.withFieldNamePointer(p) }
	override def withFillBackground(fill: Boolean) = mapSettings { _.withFillBackground(fill) }
	override def withFocusColorRole(color: ColorRole) = mapSettings { _.withFocusColorRole(color) }
	override def withHighlightPointer(p: Changing[Option[ColorRole]]) =
		mapSettings { _.withHighlightPointer(p) }
	override def withHintPointer(p: Changing[LocalizedString]) = mapSettings { _.withHintPointer(p) }
	override def withIconPointers(pointers: Pair[Changing[SingleColorIcon]]) =
		mapSettings { _.withIconPointers(pointers) }
	override def withImageSettings(settings: ViewImageLabelSettings) =
		mapSettings { _.withImageSettings(settings) }
	override def withPromptPointer(p: Changing[LocalizedString]) = mapSettings { _.withPromptPointer(p) }
	override def withHintScaleFactor(scaling: Double): Repr = mapSettings { _.withHintScaleFactor(scaling) }
	
	
	// OTHER	--------------------
	
	def mapSettings(f: FieldSettings => FieldSettings) = withSettings(f(settings))
}

/**
  * Factory class used for constructing fields using contextual component creation information
  * @author Mikko Hilpinen
  * @since 01.06.2023, v1.1
  */
case class ContextualFieldFactory(hierarchy: ComponentHierarchy, context: VariableTextContext,
                                  settings: FieldSettings = FieldSettings.default)
	extends FieldSettingsWrapper[ContextualFieldFactory]
		with VariableTextContextualFactory[ContextualFieldFactory] with PartOfComponentHierarchy
{
	// IMPLEMENTED	--------------------
	
	override def self: ContextualFieldFactory = this
	
	override def withContext(context: VariableTextContext) = copy(context = context)
	override def withSettings(settings: FieldSettings) = copy(settings = settings)
	
	
	// OTHER    ------------------------
	
	/**
	  * Creates a new field
	  * @param emptyFlag A pointer that contains true when the content of this field is considered empty
	  *                       (of text / selection etc.)
	  * @param makeField A function for creating field contents (accepts contextual data)
	  * @param makeRightHintLabel A function for producing an additional right edge hint field (accepts created main
	  *                           field and component creation context, returns an open component or None
	  *                           if no label should be placed)
	  * @tparam C Type of wrapped component
	  * @return A new field
	  */
	def apply[C <: ReachComponent with Focusable](emptyFlag: Changing[Boolean])
	                                             (makeField: FieldCreationContext => C)
	                                             (makeRightHintLabel: ExtraFieldCreationContext[C] =>
		                                                 Option[OpenComponent[ReachComponent, Any]]) =
		new Field[C](hierarchy, context, emptyFlag, settings)(makeField)(makeRightHintLabel)
	
	/**
	  * Creates a new field
	  * @param emptyFlag A pointer that contains true when the content of this field is considered empty
	  *                       (of text / selection etc.)
	  * @param makeField A function for creating field contents (accepts contextual data)
	  * @tparam C Type of wrapped component
	  * @return A new field
	  */
	def withoutExtraLabel[C <: ReachComponent with Focusable](emptyFlag: Changing[Boolean])
	                                                         (makeField: FieldCreationContext => C) =
		apply(emptyFlag)(makeField) { _ => None }
}

/**
  * Used for defining field creation settings outside of the component building process
  * @author Mikko Hilpinen
  * @since 01.06.2023, v1.1
  */
case class FieldSetup(settings: FieldSettings = FieldSettings.default)
	extends FieldSettingsWrapper[FieldSetup]
		with FromContextComponentFactoryFactory[VariableTextContext, ContextualFieldFactory]
{
	// IMPLEMENTED	--------------------
	
	override def withContext(hierarchy: ComponentHierarchy, context: VariableTextContext) =
		ContextualFieldFactory(hierarchy, context, settings)
	
	override def withSettings(settings: FieldSettings) = copy(settings = settings)
}

object Field extends FieldSetup()
{
	// OTHER	--------------------
	
	def apply(settings: FieldSettings) = withSettings(settings)
}
/**
  * Wraps another component in an interactive container that indicates an input field
  * @author Mikko Hilpinen
  * @since 14.11.2020, v0.1
  * @tparam C Type of wrapped field
  */
// TODO: It would be more reasonable if isEmptyPointer/flag was nonEmptyPointer/flag - the problem is that the transition is hard
class Field[C <: ReachComponent with Focusable](override val hierarchy: ComponentHierarchy,
                                                context: VariableTextContext, emptyFlag: Flag,
                                                settings: FieldSettings = FieldSettings.default)
                                               (makeField: FieldCreationContext => C)
                                               (makeRightHintLabel: ExtraFieldCreationContext[C] =>
	                                                   Option[OpenComponent[ReachComponent, Any]])
	extends ReachComponentWrapper with FocusableWrapper with FocusableWithState with PartOfComponentHierarchy
{
	// ATTRIBUTES	------------------------------------------
	
	private val focusBorderWidth = (context.margins.verySmall / 2) max 3
	private val mouseExtraBorderWidth = context.margins.verySmall / 4
	private val defaultBorderWidth = focusBorderWidth / 3
	
	/**
	  * A tracker used for managing this field's focus state / -flag
	  */
	private val focusTracker = new FocusStateTracker()
	/**
	  * A flag that contains true while the mouse is hovering over this component
	  */
	private val mouseOverFlag = ResettableFlag()
	
	/**
	  * A pointer that contains the displayed hint text.
	  * Contains an error, if there is one, otherwise contains the hint, if there is one.
	  * None if neither are used.
	  *
	  * Always tied to this field's linked state, so that no changes occur while not linked.
	  */
	private lazy val localHintTextP = settings.hintPointer.notFixedWhere { _.isEmpty } match {
		case Some(hintP) =>
			settings.errorMessagePointer.notFixedWhere { _.isEmpty } match {
				// Case: May switch between a variable error and a variable hint
				case Some(errorP) =>
					val mergedP = errorP.flatMap { error =>
						if (error.isEmpty)
							hintP
						else
							Fixed(error)
					}
					Some(mergedP.viewWhile(linkedFlag))
					
				// Case: Only hint is specified
				case None => Some(hintP.viewWhile(linkedFlag))
			}
		// Case: Hint never specified => May still display the error message, if specified
		case None => settings.errorMessagePointer.notFixedWhere { _.isEmpty }.map { _.viewWhile(linkedFlag) }
	}
	/**
	  * A flag that contains true while the hint area should be displayed.
	  */
	private lazy val hintVisibleFlag: Flag = localHintTextP match {
		case Some(hintP) => hintP.strongMap { _.nonEmpty }
		case None => AlwaysFalse
	}
	
	/**
	  * A flag that contains true while this field highlights an error / is in the error state.
	  */
	private val hasErrorFlag: Flag = settings.errorMessagePointer.nonEmptyFlag
	// TODO: Add a state pointer and make error state pointer visible, also
	/**
	  * A pointer that contains the currently applied color highlighting.
	  * Based on:
	  *     1. Whether an error message is specified
	  *     1. Whether custom highlighting is applied
	  *     1. Whether this field has focus
	  *
	  * Might not update actively while not linked
	  */
	private val highlightStateP = {
		// Caches the alternative pointers so that they're not recreated when states change
		lazy val focusColorP = focusFlag.map { if (_) Some(settings.focusColorRole) else None }
		lazy val highlightColorP = settings.highlightPointer.flatMapWhile(linkedFlag) {
			// Case: Applies custom highlighting
			case Some(customHighlighting) => Fixed(Some(customHighlighting))
			// Case: No custom highlighting => Applies highlighting when focused
			case None => focusColorP
		}
		
		hasErrorFlag.fixedValue match {
			// Case: Fixed error-state => Applies simpler logic
			case Some(isError) =>
				if (isError)
					Fixed(Some(ColorRole.Failure))
				else
					highlightColorP
				
			case None =>
				hasErrorFlag.flatMapWhile(linkedFlag) { hasError =>
					// Case: Displays an error message => Error color
					if (hasError) Fixed(Some(ColorRole.Failure)) else highlightColorP
				}
		}
	}
	/**
	  * A pointer that contains the applied highlighting color.
	  * Contains None while no highlighting should be applied.
	  * Usable in the default context (i.e. not within inner background, if such is applicable)
	  */
	private val highlightColorP: Changing[Option[Color]] = makeHighlightColorPointerFor(context)
	/**
	  * A pointer that contains hint text color applicable in the standard context.
	  * Not applicable to text appearing under different background (i.e. under [[customInnerBgP]])
	  */
	private val hintTextColorP = makeTextColorPointerFor(context.textColorPointer, highlightColorP)
	/**
	  * Context used for the hint area. Applies the correct text color.
	  */
	private lazy val hintContext = contextToHintSize(context).withTextColorPointer(hintTextColorP)
	
	/**
	  * A pointer that contains the background color of the inner field area.
	  * None if the background should be the same as that of this component's context
	  * (i.e. no special background applied).
	  */
	private val customInnerBgP = {
		// Case: Inner background applied => Applies a more highlighted version when focused
		if (settings.fillBackground)
			Some(context.backgroundPointer
				.mergeWith(focusFlag, mouseOverFlag) { (bg, focus, mouseOver) =>
					var highlight = if (focus) 1.0 else 0.0
					if (mouseOver)
						highlight += 0.5
						
					bg.highlightedBy(1 + highlight)
				})
		// Case: No special inner background required
		else
			None
	}
	/**
	  * A special (color) context for the inner section of this field.
	  * None if the general context may be used.
	  */
	private val customInnerBaseContext = customInnerBgP.map(context.withBackgroundPointer)
	/**
	  * Context used within the inner field area (i.e. not within the hint area below).
	  * Counts the effects of stateful background highlighting, if the 'fillBackground' style is used.
	  */
	private val innerNormalTextContext = {
		val base = customInnerBaseContext.getOrElse(context)
		// Applies the enabled/disabled state effect
		enabledFlag.fixedValue match {
			case Some(fixedEnabled) =>
				// Case: Always enabled => No need to modify context
				if (fixedEnabled)
					base
				// Case: Always disabled
				else
					base.mapTextColor { _.timesAlpha(ComponentCreationDefaults.disabledAlphaMod) }
				
			// Case: Variable enabled state
			case None =>
				base.mapTextColorPointer {
					_.mergeWith(enabledFlag) { (color, enabled) =>
						if (enabled) color else color.timesAlpha(ComponentCreationDefaults.disabledAlphaMod)
					}
				}
		}
	}
	
	/**
	  * A pointer that contains the applied border
	  */
	private val borderP = {
		val borderWidthP = {
			if (settings.fillBackground)
				focusFlag.lightMap { if (_) focusBorderWidth else defaultBorderWidth }
			else
				focusFlag.mergeWith(mouseOverFlag) { (focus, mouseOver) =>
					val base = if (focus) focusBorderWidth else defaultBorderWidth
					if (mouseOver)
						(base + mouseExtraBorderWidth).round.toDouble
					else
						base
				}
		}
		// When using filled background style, only draws the bottom border, which varies in style based state
		if (settings.fillBackground)
			borderWidthP.mergeWith(hintTextColorP)(Border.bottom)
		// Otherwise, draws the border on all sides
		else
			borderWidthP.mergeWith(hintTextColorP)(Border.symmetric)
	}
	private val borderDrawer = BorderViewDrawer(borderP)
	
	private val (_wrapped, field) = {
		// Creates the main area first
		val openMainArea = makeInputArea()
		// Checks whether a separate hint area is required
		val component = makeHintArea(openMainArea.result) match {
			// Case: Both main and hint area used => uses a view stack
			case Some(openHintArea) =>
				val framedMainArea = Open.using(Framing) { makeContentFraming(_, openMainArea) }
				ViewStack(hierarchy)
					.withoutMargin(Pair(framedMainArea.withResult(AlwaysTrue), openHintArea)).parent
			
			// Case: Only main area used => uses framing only
			case None => makeContentFraming(Framing(hierarchy), openMainArea).parent
		}
		component -> openMainArea.result
	}
	private val repaintListener = ChangeListener.continuousOnAnyChange { repaint(High) }
	
	
	// INITIAL CODE	------------------------------------------
	
	focusFlag.addListener(repaintListener)
	customInnerBgP.foreach { _.addListenerWhile(linkedFlag)(repaintListener) }
	borderP.addListenerWhile(linkedFlag)(repaintListener)
	
	addMouseMoveListener(MouseMoveListener { e => mouseOverFlag.value = enabled && e.isOver(bounds) })
	
	
	// COMPUTED	----------------------------------------------
	
	/**
	  * @return The main component inside this wrapper
	  */
	def wrappedField = field
	
	/**
	  * @return A flag that contains true while this field is editable
	  */
	def enabledFlag = settings.enabledFlag
	/**
	  * @return Whether this field is currently enabled
	  */
	def enabled = enabledFlag.value
	/**
	  * @return Whether this field is currently disabled
	  */
	def disabled = !enabled
	
	/**
	  * A pointer to this field's current inner background color.
	  * May vary based on state.
	  */
	def innerBackgroundPointer = innerNormalTextContext.backgroundPointer
	
	
	// IMPLEMENTED	------------------------------------------
	
	override def focusFlag: Flag = focusTracker.focusFlag
	
	override protected def focusable = field
	override protected def wrapped: ConcreteReachComponent = _wrapped
	
	
	// OTHER	----------------------------------------------
	
	private def makeContentFraming[C2 <: ReachComponent](factory: FramingFactory, content: OpenComponent[C2, C]) =
	{
		// If extra icons are used, places them with the main content in a stack view
		val framingContent = {
			if (settings.iconPointers.exists { _.mayBeNonEmpty })
				Open.using(ViewStack) { stackF =>
					stackF.row.centered.withoutMargin(Vector(
						makeOpenViewImageLabel(settings.leftIconPointer, Direction2D.Right),
						content.withResult(AlwaysTrue),
						makeOpenViewImageLabel(settings.rightIconPointer, Direction2D.Left)
					))
				}
			else
				content
		}
		// Wraps the field component in a Framing (that applies border)
		// Top and side inset are increased if border is drawn on all sides
		val borderInsets =
			if (settings.fillBackground) Insets.bottom(focusBorderWidth) else Insets.symmetric(focusBorderWidth)
		// Draws background (optional) and border
		val drawers = customInnerBgP.map(makeBackgroundDrawer).emptyOrSingle :+ borderDrawer
		factory(borderInsets.fixed).withCustomDrawers(drawers)(framingContent).withResult(content.result)
	}
	
	private def makeViewImageLabel(hierarchy: ComponentHierarchy, pointer: Changing[SingleColorIcon],
	                               noMarginSide: Direction2D) =
		ViewImageLabel.withContext(hierarchy, innerNormalTextContext)
			.withSettings(settings.imageSettings).lowPriority.mapInsets { _ - noMarginSide }
			.iconPointer(pointer)
	private def makeOpenViewImageLabel(pointer: Changing[SingleColorIcon], noMarginSide: Direction2D) =
		Open { makeViewImageLabel(_, pointer, noMarginSide) }.withResult(pointer.strongMap { _.nonEmpty })
	
	// Assumes that the name-pointer is not always empty
	private def makeContentAndNameArea(fieldNamePointer: Changing[LocalizedString]) = {
		Open.using(ViewStack) { stackFactory =>
			stackFactory.withoutMargin.build(Mixed) { factories =>
				// Creates the field name label first
				// Field name is displayed when
				//      A. it is available AND B. The edit label has focus
				//      OR C. The edit label is not empty
				val nameShouldBeSeparateFlag = focusFlag || !emptyFlag
				val nameVisibleFlag = nameShouldBeSeparateFlag && fieldNamePointer.lightMap { _.nonEmpty }
				// When applying a custom background color, also needs a custom highlight & hint color
				val nameLabelContext = customInnerBaseContext match {
					case Some(base) =>
						val highlightColorP = makeHighlightColorPointerFor(base)
						contextToHintSize(base).mapTextColorPointer { p => makeTextColorPointerFor(p, highlightColorP) }
					
					case None => hintContext
				}
				val nameLabel = factories.next()(ViewTextLabel).withContext(nameLabelContext).text(fieldNamePointer)
				
				// When displaying only the input, accommodates name label size increase into the vertical insets
				// While displaying both, applies only half of the main text insets at top
				val contentContext = innerNormalTextContext.flatMapTextInsets { originalInsets =>
					// Text insets used when the field name is displayed on top of the content
					val comboTextInsets = originalInsets.mapTop { _ / 2 }
					// Text insets used when only the content is displayed. Scaled so that fills the same area.
					lazy val aloneTextInsets = {
						val requiredIncrease = comboTextInsets.vertical + nameLabel.stackSize.height -
							originalInsets.vertical
						originalInsets.mapVertical { _ + requiredIncrease / 2 }
					}
					nameVisibleFlag.lightMap { if (_) comboTextInsets else aloneTextInsets }
				}
				// While only the text label is being displayed, shows the field name as a prompt. Otherwise, may show
				// the other specified prompt (if defined)
				val promptStyleP = contentContext.hintTextDrawContextPointer
				val emptyTextP = promptStyleP.map { measureText(LocalizedString.empty, _) }
				// Only draws the name while it is not displayed elsewhere
				val namePromptP = {
					lazy val measuredNameP = fieldNamePointer.mergeWith(promptStyleP)(measureText)
					nameShouldBeSeparateFlag.flatMap { if (_) emptyTextP else measuredNameP }
				}
				val namePromptDrawer = TextViewDrawer(namePromptP, promptStyleP)
				
				// May also display another prompt while the field has focus and is empty / starting with the prompt
				// (not blocked by name or text)
				val promptDrawer = settings.promptPointer.notFixedWhere { _.isEmpty }.map { promptP =>
					lazy val measuredPromptP = promptP.mergeWith(promptStyleP)(measureText)
					val displayedPromptP = focusFlag.flatMap { if (_) measuredPromptP else emptyTextP }
					TextViewDrawer(displayedPromptP, promptStyleP)
				}
				
				val wrappedField = makeField(FieldCreationContext(factories.next().hierarchy,
					contentContext, focusTracker, Single(namePromptDrawer) ++ promptDrawer))
				
				// Displays one or both of the items
				Pair(nameLabel -> nameVisibleFlag, wrappedField -> AlwaysTrue) -> wrappedField
			}
		}
	}
	
	// Returns input area
	private def makeInputArea() = {
		// Input part may contain a name label, if enabled
		// Case: Field name is never displayed => Uses a simplified creation function
		if (settings.fieldNamePointer.existsFixed { _.isEmpty }) {
			// May draw a prompt while the field is empty (or starting with the prompt text)
			val promptDrawer = settings.promptPointer.notFixedWhere { _.isEmpty }.map { promptPointer =>
				val promptStylePointer = innerNormalTextContext.hintTextDrawContextPointer
				val displayedPromptPointer = promptPointer.mergeWith(promptStylePointer)(measureText)
				TextViewDrawer(displayedPromptPointer, promptStylePointer)
			}
			Open { hierarchy =>
				val field = makeField(
					FieldCreationContext(hierarchy, innerNormalTextContext, focusTracker, promptDrawer.emptyOrSingle))
				field -> field
			}
		}
		else
			makeContentAndNameArea(settings.fieldNamePointer)
	}
	
	// Returns the generated open component (if any), along with its visibility pointer (if applicable)
	private def makeHintArea(wrappedField: C): Option[OpenComponent[ReachComponent, Changing[Boolean]]] = {
		// In some cases, displays both message field and extra right side label
		// In other cases only the message field (which is hidden while empty)
		// The right side hint label expands to the left and not right
		val lazyRightContextP = Lazy {
			hintContext.mapTextInsets { _.mapRight { _.normalPriority }.mapLeft { _.expanding } }
				.withHorizontalTextAlignment(Far)
		}
		makeRightHintLabel(ExtraFieldCreationContext(wrappedField, lazyRightContextP)) match {
			// Case: A component displayed on the right side => Applies the hint text as well, if appropriate
			case Some(rightComponent) =>
				localHintTextP match {
					// Case: Hints are sometimes displayed
					case Some(hintTextPointer) =>
						val hintTextLabel = Open.using(ViewTextLabel) { _.withContext(hintContext)(hintTextPointer) }
						val stack = Open.using(ViewStack) {
							_.row
								.apply(Pair(
									hintTextLabel.withResult(hintVisibleFlag),
									rightComponent.withResult(AlwaysTrue)
								))
								.parent
						}
						Some(stack.withResult(AlwaysTrue))
						
					// Case: Only the right hint element should be displayed
					case None => Some(rightComponent.withResult(AlwaysTrue))
				}
			case None =>
				// Case: No additional hint should be displayed => May display a hint label still (occasionally)
				localHintTextP.map { hintTextPointer =>
					Open.using(ViewTextLabel) { _.withContext(hintContext)(hintTextPointer) }
						.withResult(hintVisibleFlag)
				}
		}
	}
	
	/**
	  * @param context Context to convert
	  * @return A version of the specified context, where text sizes are smaller.
	  *         Applicable for hint text.
	  */
	private def contextToHintSize(context: VariableTextContext) = context
		// Hint text is smaller and has smaller insets
		.mapTextInsets { original =>
			val midInsets = original.expandingHorizontallyAccordingTo(context.textAlignment)
				.mapVertical { _ * settings.hintScaleFactor }//.mapBottom { _ / 2 }
			if (settings.fillBackground)
				midInsets
			// Additional horizontal insets are added in outlined style
			else
				midInsets + Insets.horizontal(focusBorderWidth)
		}
		.mapFont { _ * settings.hintScaleFactor }.withShrinkingText
	
	/**
	  * Creates a highlight color -pointer that may be used in the specified color context
	  * @param context Color context, in which the resulting pointer is used
	  * @return A pointer that contains the applied highlighting color.
	  *         Aware of the enabled-state, as well as the applied highlighting.
	  */
	private def makeHighlightColorPointerFor(context: VariableColorContext) = {
		// Case: Always enabled => Applies contextual coloring with no need to handle the disabled color state
		if (enabledFlag.isAlwaysTrue)
			highlightStateP.flatMap {
				case Some(role) => context.colorPointer(role).lightMap { Some(_) }
				case None => Fixed.never
			}
		// Case: May become disabled => Creates additional disabled color pointers
		else {
			// Caches the generated pointers in order to avoid creating unnecessary replicates
			val colorPCache = Cache { role: ColorRole =>
				val defaultColorP = context.colorPointer(role)
				lazy val disabledColorP = defaultColorP.map { _.timesAlpha(ComponentCreationDefaults.disabledAlphaMod) }
				enabledFlag.flatMap { if (_) defaultColorP else disabledColorP }.lightMap { Some(_) }
			}
			highlightStateP.flatMap {
				case Some(role) => colorPCache(role)
				case None => Fixed.never
			}
		}
	}
	/**
	  * Creates a pointer for (hint) text color, which also applies highlighting, when necessary
	  * @param defaultTextColorP Text color -pointer from the applicable context.
	  *                          Does not apply hint or enabled alpha-change.
	  * @param highlightColorP Applied highlight color -pointer
	  * @return A pointer that contains the text color which should be applied in this context
	  */
	// The default color (pointer) is determined by the context, and may be variable
	// The enabled state also affects this
	private def makeTextColorPointerFor(defaultTextColorP: Changing[Color], highlightColorP: Changing[Option[Color]]) =
		enabledFlag.fixedValue match {
			// Case: Enabled-state doesn't change => Applies a static text color change
			//                                       Also uses a simpler final mapping
			case Some(enabled) =>
				val alphaMod = {
					// Case: Always enabled => Applies hint text color
					if (enabled)
						ComponentCreationDefaults.hintAlphaMod
					// Case: Always disabled => Applies disabled text color
					else
						ComponentCreationDefaults.disabledAlphaMod
				}
				highlightColorP.mergeWith(defaultTextColorP) { (highlight, default) =>
					highlight.getOrElse(default.timesAlpha(alphaMod))
				}
			
			// Case: Variable enabled-state => Modifies text alpha as necessary
			case None =>
				lazy val alphaModP = enabledFlag.lightMap {
					if (_) ComponentCreationDefaults.hintAlphaMod else ComponentCreationDefaults.disabledAlphaMod
				}
				lazy val defaultColorP = defaultTextColorP.mergeWith(alphaModP) { _.timesAlpha(_) }
				// The final color-pointer is determined by a combination of highlighting and the default text color
				highlightColorP.flatMap {
					case Some(color) => Fixed(color)
					case None => defaultColorP
				}
		}
	
	private def makeBackgroundDrawer(bgP: Changing[Color]) = BackgroundViewDrawer(View {
		if (enabledFlag.value)
			bgP.value
		else
			bgP.value.timesAlpha(ComponentCreationDefaults.disabledAlphaMod)
	})
	
	// TODO: This version doesn't take into account margin between lines
	private def measureText(text: LocalizedString, style: TextDrawContext) =
		MeasuredText(text.wrapped, hierarchy.fontMetricsWith(style.font), allowLineBreaks = style.allowLineBreaks)
}
