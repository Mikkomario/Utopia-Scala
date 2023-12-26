package utopia.reach.component.input

import utopia.firmament.context.{BaseContext, ComponentCreationDefaults, TextContext}
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.drawing.view.{BackgroundViewDrawer, BorderViewDrawer, TextViewDrawer}
import utopia.firmament.image.SingleColorIcon
import utopia.firmament.localization.LocalizedString
import utopia.firmament.model.stack.LengthExtensions._
import utopia.firmament.model.stack.StackInsets
import utopia.firmament.model.{Border, TextDrawContext}
import utopia.flow.collection.immutable.Pair
import utopia.flow.event.listener.ChangeListener
import utopia.flow.operator.enumeration.End.{First, Last}
import utopia.flow.operator.enumeration.End
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.immutable.eventful.{AlwaysFalse, AlwaysTrue, Fixed}
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.graphics.MeasuredText
import utopia.paradigm.color.{Color, ColorRole}
import utopia.paradigm.enumeration.LinearAlignment.Far
import utopia.paradigm.enumeration.{Alignment, Direction2D}
import utopia.paradigm.shape.shape2d.insets.Insets
import utopia.reach.component.factory.contextual.VariableContextualFactory
import utopia.reach.component.factory.{FromContextComponentFactoryFactory, Mixed}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.image.{ViewImageLabel, ViewImageLabelSettings}
import utopia.reach.component.label.text.ViewTextLabel
import utopia.reach.component.template.focus.{Focusable, FocusableWithPointer, FocusableWrapper}
import utopia.reach.component.template.{ReachComponent, ReachComponentLike, ReachComponentWrapper}
import utopia.reach.component.wrapper.{ComponentCreationResult, Open, OpenComponent}
import utopia.reach.container.ReachCanvas
import utopia.reach.container.multi.ViewStack
import utopia.reach.container.wrapper.{Framing, FramingFactory}
import utopia.reach.drawing.Priority.High
import utopia.reach.focus.{FocusChangeEvent, FocusChangeListener}

/**
  * A set of context variables provided when creating field contents
  * @param parentHierarchy   Component hierarchy to use
  * @param contextPointer    Pointer that contains teh field creation context
  * @param focusListener     Focus listener to assign to the created component
  * @param promptDrawers     Custom drawers to assign for prompt drawing
  */
case class FieldCreationContext(parentHierarchy: ComponentHierarchy, contextPointer: Changing[TextContext],
                                focusListener: FocusChangeListener, promptDrawers: Vector[CustomDrawer])

/**
  * A set of context variables provided when creating an additional right side label
  * @param content           Field content
  * @param lazyContextPointer    Pointer to the context prevalent in this area (lazy)
  * @tparam C Type of field contents
  */
case class ExtraFieldCreationContext[C](content: C, lazyContextPointer: Lazy[Changing[TextContext]])
{
	/**
	  * @return Pointer to the context prevalent in this area
	  */
	def contextPointer = lazyContextPointer.value
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
	def withImageCustomDrawers(drawers: Vector[CustomDrawer]) =
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
	
	val default = apply()
}

/**
  * Combined settings used when constructing fields
  * @param fieldNamePointer    Pointer that determines the name of this field displayed on this field
  * @param promptPointer       Pointer that determines what text is displayed while there is no input
  * @param hintPointer         Pointer that determines the hint to show underneath this field
  * @param errorMessagePointer Pointer that determines an error message to display underneath this field
  * @param highlightPointer    Pointer that determines external highlight state/style to apply
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
                         highlightPointer: Changing[Option[ColorRole]] = Fixed(None),
                         focusColorRole: ColorRole = ColorRole.Secondary, hintScaleFactor: Double = 0.7,
                         iconPointers: Pair[Changing[SingleColorIcon]] = Pair.twice(SingleColorIcon.alwaysEmpty),
                         imageSettings: ViewImageLabelSettings = ViewImageLabelSettings.default,
                         fillBackground: Boolean = ComponentCreationDefaults.useFillStyleFields)
	extends FieldSettingsLike[FieldSettings]
{
	// IMPLEMENTED	--------------------
	
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
case class ContextualFieldFactory(parentHierarchy: ComponentHierarchy, contextPointer: Changing[TextContext],
                                  settings: FieldSettings = FieldSettings.default)
	extends FieldSettingsWrapper[ContextualFieldFactory]
		with VariableContextualFactory[TextContext, ContextualFieldFactory]
{
	// IMPLEMENTED	--------------------
	
	override def withContextPointer(contextPointer: Changing[TextContext]) =
		copy(contextPointer = contextPointer)
	override def withSettings(settings: FieldSettings) = copy(settings = settings)
	
	
	// OTHER    ------------------------
	
	/**
	  * Creates a new field
	  * @param isEmptyPointer A pointer that contains true when the content of this field is considered empty
	  *                       (of text / selection etc.)
	  * @param makeField A function for creating field contents (accepts contextual data)
	  * @param makeRightHintLabel A function for producing an additional right edge hint field (accepts created main
	  *                           field and component creation context, returns an open component or None
	  *                           if no label should be placed)
	  * @tparam C Type of wrapped component
	  * @return A new field
	  */
	def apply[C <: ReachComponentLike with Focusable](isEmptyPointer: Changing[Boolean])
	                                                 (makeField: FieldCreationContext => C)
	                                                 (makeRightHintLabel: ExtraFieldCreationContext[C] =>
		                                                 Option[OpenComponent[ReachComponentLike, Any]]) =
		new Field[C](parentHierarchy, contextPointer, isEmptyPointer, settings)(makeField)(makeRightHintLabel)
	
	/**
	  * Creates a new field
	  * @param isEmptyPointer A pointer that contains true when the content of this field is considered empty
	  *                       (of text / selection etc.)
	  * @param makeField A function for creating field contents (accepts contextual data)
	  * @tparam C Type of wrapped component
	  * @return A new field
	  */
	def withoutExtraLabel[C <: ReachComponentLike with Focusable](isEmptyPointer: Changing[Boolean])
	                                                             (makeField: FieldCreationContext => C) =
		apply(isEmptyPointer)(makeField) { _ => None }
}

/**
  * Used for defining field creation settings outside of the component building process
  * @author Mikko Hilpinen
  * @since 01.06.2023, v1.1
  */
case class FieldSetup(settings: FieldSettings = FieldSettings.default)
	extends FieldSettingsWrapper[FieldSetup]
		with FromContextComponentFactoryFactory[TextContext, ContextualFieldFactory]
{
	// IMPLEMENTED	--------------------
	
	override def withContext(hierarchy: ComponentHierarchy, context: TextContext) =
		ContextualFieldFactory(hierarchy, Fixed(context), settings)
	
	override def withSettings(settings: FieldSettings) = copy(settings = settings)
	
	
	// OTHER	--------------------
	
	/**
	  * @return A new field factory that uses the specified (variable) context
	  */
	def withContext(hierarchy: ComponentHierarchy, context: Changing[TextContext]) =
		ContextualFieldFactory(hierarchy, context, settings)
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
class Field[C <: ReachComponentLike with Focusable](parentHierarchy: ComponentHierarchy,
                                                    contextPointer: Changing[TextContext],
                                                    isEmptyPointer: Changing[Boolean],
                                                    settings: FieldSettings = FieldSettings.default)
                                                   (makeField: FieldCreationContext => C)
                                                   (makeRightHintLabel: ExtraFieldCreationContext[C] =>
	                                                   Option[OpenComponent[ReachComponentLike, Any]])
	extends ReachComponentWrapper with FocusableWrapper with FocusableWithPointer
{
	// ATTRIBUTES	------------------------------------------
	
	private implicit def c: ReachCanvas = parentHierarchy.top
	
	private val _focusPointer = new EventfulPointer(false)
	
	private lazy val uncoloredHintContextPointer = contextPointer.mapWhile(parentHierarchy.linkPointer) { context =>
		context
			// Hint text is smaller and has smaller insets
			.mapTextInsets { original =>
				val midInsets = original.expandingHorizontallyAccordingTo(context.textAlignment)
					.mapVertical { _ * settings.hintScaleFactor }//.mapBottom { _ / 2 }
				if (settings.fillBackground)
					midInsets
				// Additional horizontal insets are added in outlined style
				else
					midInsets + Insets.horizontal(focusBorderWidthFrom(context))
			}
			.mapFont { _ * settings.hintScaleFactor }.withShrinkingText
	}
	
	// Displays an error if there is one, otherwise displays the hint (provided there is one). None if neither is used.
	private lazy val actualHintTextPointer = settings.hintPointer.notFixedWhere { _.isEmpty } match {
		case Some(hint) =>
			settings.errorMessagePointer.notFixedWhere { _.isEmpty } match {
				case Some(error) => Some(hint.mergeWith(error) { (hint, error) => error.notEmpty getOrElse hint })
				case None => Some(hint)
			}
		case None => settings.errorMessagePointer.notFixedWhere { _.isEmpty }
	}
	private lazy val hintVisibilityPointer = actualHintTextPointer match {
		case Some(hintTextPointer) => hintTextPointer.strongMap { _.nonEmpty }
		case None => AlwaysFalse
	}
	
	// A pointer to whether this field currently highlights an error
	private val errorStatePointer = settings.errorMessagePointer.strongMap { _.nonEmpty }
	// TODO: Add a state pointer and make error state pointer visible, also
	// Pointer that determines highlighting color
	private val highlightStatePointer = errorStatePointer
		.mergeWith(settings.highlightPointer, _focusPointer) { (isError, custom, hasFocus) =>
			// Case: Error is displayed => Highlights error
			if (isError)
				Some(ColorRole.Failure)
			// Case: No error => Highlights using custom highlighting (from outside), or focus, if applicable
			else
				custom.orElse { if (hasFocus) Some(settings.focusColorRole) else None }
		}
	// If a separate background color is used for this component, it depends from this component's state
	private val innerContextPointer = {
		// TODO: Handle mouse over state (highlights one more time)
		if (settings.fillBackground)
			contextPointer.mergeWithWhile(_focusPointer, parentHierarchy.linkPointer) { (context, hasFocus) =>
				context.mapBackground { _.highlightedBy(if (hasFocus) 2 else 1) }
			}
		else
			contextPointer
	}
	/**
	  * A pointer to this field's current inner background color. May vary based on state.
	  */
	// TODO: See if this really needs to be exposed
	val innerBackgroundPointer = innerContextPointer.strongMap { _.background }
	private val highlightColorPointer = highlightStatePointer.mergeWith(innerContextPointer) { (state, context) =>
		state.map { s => context.color(s) }
	}
	
	private val editTextColorPointer = innerContextPointer.strongMap { _.textColor }
	private val contentColorPointer: Changing[Color] = highlightColorPointer
		.mergeWith(editTextColorPointer) { (highlight, default) =>
			highlight match {
				case Some(color) => color: Color
				case None => default.timesAlpha(0.66)
			}
		}
	// Hint text colouring is affected by the displayed error, as well as possible highlighting
	// The actual display color is adjusted based on context background
	// TODO: Check whether the timesAlpha -portion is unnecessary (or harmful)
	private lazy val hintContextPointer = uncoloredHintContextPointer
		.mergeWith(highlightStatePointer) { (context, highlight) =>
			highlight match {
				// Case: Highlighting applied
				case Some(colorRole) => context.withTextColor(colorRole)
				// Case: Default hint color
				case None => context.mapTextColor { _.timesAlpha(0.66) }
			}
		}
	
	private val borderPointer = {
		// When using filled background style, only draws the bottom border which varies in style based state
		if (settings.fillBackground) {
			contentColorPointer.mergeWithWhile(_focusPointer, contextPointer, parentHierarchy.linkPointer) { (color, focus, context) =>
				Border.bottom(if (focus) focusBorderWidthFrom(context) else defaultBorderWidthFrom(context), color)
			}
		}
		else
			contentColorPointer.mergeWithWhile(_focusPointer, contextPointer, parentHierarchy.linkPointer) { (color, focus, context) =>
				Border.symmetric(if (focus) focusBorderWidthFrom(context) else defaultBorderWidthFrom(context), color)
			}
	}
	private val borderDrawer = BorderViewDrawer(borderPointer)
	
	private val (_wrapped, field) = {
		// Creates the main area first
		val openMainArea = makeInputArea()
		// Checks whether a separate hint area is required
		val component = makeHintArea(openMainArea.result) match {
			// Case: Both main and hint area used => uses a view stack
			case Some(openHintArea) =>
				val framedMainArea = Open.using(Framing) { makeContentFraming(_, openMainArea) }
				ViewStack(parentHierarchy)
					.withoutMargin(Vector(framedMainArea.withResult(AlwaysTrue), openHintArea)).parent
			// Case: Only main area used => uses framing only
			case None => makeContentFraming(Framing(parentHierarchy), openMainArea).parent
		}
		component -> openMainArea.result
	}
	private val repaintListener = ChangeListener.continuousOnAnyChange { repaint(High) }
	
	
	// INITIAL CODE	------------------------------------------
	
	_focusPointer.addListener(repaintListener)
	innerBackgroundPointer.addListener(repaintListener)
	borderPointer.addListener(repaintListener)
	
	
	// COMPUTED	----------------------------------------------
	
	/**
	  * @return The main component inside this wrapper
	  */
	def wrappedField = field
	
	
	// IMPLEMENTED	------------------------------------------
	
	/**
	  * @return A pointer to this component's focus state
	  */
	override def focusPointer = _focusPointer.readOnly
	
	override protected def focusable = field
	
	override protected def wrapped: ReachComponent = _wrapped
	
	
	// OTHER	----------------------------------------------
	
	private def makeContentFraming[C2 <: ReachComponentLike](factory: FramingFactory, content: OpenComponent[C2, C]) =
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
		// TODO: At this time, these insets are static. Once ViewFraming is available, use variable insets
		val borderInsets = {
			if (settings.fillBackground)
				Insets.bottom(focusBorderWidthFrom(contextPointer.value))
			else
				Insets.symmetric(focusBorderWidthFrom(contextPointer.value))
		}
		// Draws background (optional) and border
		val drawers = {
			if (settings.fillBackground)
				Vector(makeBackgroundDrawer(), borderDrawer)
			else
				Vector(borderDrawer)
		}
		factory(borderInsets.fixed).withCustomDrawers(drawers)(framingContent).withResult(content.result)
	}
	
	private def makeViewImageLabel(hierarchy: ComponentHierarchy, pointer: Changing[SingleColorIcon],
	                               noMarginSide: Direction2D) =
		ViewImageLabel.withContextPointer(hierarchy, innerContextPointer)
			.withSettings(settings.imageSettings).lowPriority.mapInsets { _ - noMarginSide }
			.iconPointer(pointer)
	private def makeOpenViewImageLabel(pointer: Changing[SingleColorIcon], noMarginSide: Direction2D) =
		Open { makeViewImageLabel(_, pointer, noMarginSide) }.withResult(pointer.strongMap { _.nonEmpty })
	
	private def makeContentAndNameArea(fieldNamePointer: Changing[LocalizedString]) = {
		Open.using(ViewStack) { stackFactory =>
			stackFactory.withoutMargin.build(Mixed) { factories =>
				// Creates the field name label first
				// Field name is displayed when
				// a) it is available AND
				// b) The edit label has focus OR c) The edit label is empty
				val nameShouldBeSeparatePointer = _focusPointer.mergeWith(isEmptyPointer) { _ || !_ }
				val nameVisibilityPointer = fieldNamePointer.mergeWith(nameShouldBeSeparatePointer) { _.nonEmpty && _ }
				// TODO: Name label might have wrong text color because of background highlighting - Needs a different context if so
				val nameLabel = factories.next()(ViewTextLabel).withContextPointer(hintContextPointer)
					.text(fieldNamePointer)
				
				// When displaying only the input, accommodates name label size increase into the vertical insets
				// While displaying both, applies only half of the main text insets at top
				val contentContextPointer = innerContextPointer.mergeWith(nameVisibilityPointer) { (context, nameIsVisible) =>
					val comboTextInsets = context.textInsets.mapTop { _ / 2 }
					val newInsets = {
						if (nameIsVisible)
							comboTextInsets
						else {
							val requiredIncrease = comboTextInsets.vertical + nameLabel.stackSize.height -
								context.textInsets.vertical
							context.textInsets.mapVertical { _ + requiredIncrease / 2 }
						}
					}
					context.withTextInsets(newInsets)
				}
				// While only the text label is being displayed, shows the field name as a prompt. Otherwise may show
				// the other specified prompt (if defined)
				val promptStylePointer = contentContextPointer.map { TextDrawContext.contextualHint(_) }
				// TODO: Should most likely be variable (now uses initial style only)
				val emptyText = measureText(LocalizedString.empty, promptStylePointer.value)
				// Only draws the name while it is not displayed elsewhere
				val namePromptPointer = fieldNamePointer
					.mergeWith(nameShouldBeSeparatePointer, promptStylePointer) { (name, isSeparate, style) =>
						if (isSeparate) emptyText else measureText(name, style)
					}
				val namePromptDrawer = TextViewDrawer(namePromptPointer, promptStylePointer)
				
				// May also display another prompt while the field has focus and is empty / starting with the prompt
				// (not blocked by name or text)
				val promptDrawer = settings.promptPointer.notFixedWhere { _.isEmpty }.map { promptPointer =>
					val promptContentPointer = promptPointer.mergeWith(promptStylePointer)(measureText)
					val displayedPromptPointer = promptContentPointer.mergeWith(_focusPointer) { (prompt, focus) =>
						if (focus) prompt else emptyText }
					TextViewDrawer(displayedPromptPointer, promptStylePointer)
				}
				
				val wrappedField = makeField(FieldCreationContext(factories.next().parentHierarchy,
					contentContextPointer, FocusTracker, Vector(namePromptDrawer) ++ promptDrawer))
				
				// Displays one or both of the items
				Vector(ComponentCreationResult(nameLabel, nameVisibilityPointer),
					ComponentCreationResult(wrappedField, AlwaysTrue)) -> wrappedField
			}
		}
	}
	
	// Returns input area + editable label
	private def makeInputArea() = {
		// Input part may contain a name label, if enabled
		if (settings.fieldNamePointer.existsFixed { _.isEmpty }) {
			// May draw a prompt while the field is empty (or starting with the prompt text)
			val promptDrawer = settings.promptPointer.notFixedWhere { _.isEmpty }.map { promptPointer =>
				val promptStylePointer = innerContextPointer.map { TextDrawContext.contextualHint(_) }
				val displayedPromptPointer = promptPointer.mergeWith(promptStylePointer)(measureText)
				TextViewDrawer(displayedPromptPointer, promptStylePointer)
			}
			Open { hierarchy =>
				val field = makeField(FieldCreationContext(hierarchy, innerContextPointer, FocusTracker,
					promptDrawer.toVector))
				field -> field
			}
		}
		else
			makeContentAndNameArea(settings.fieldNamePointer)
	}
	
	// Returns the generated open component (if any), along with its visibility pointer (if applicable)
	private def makeHintArea(wrappedField: C): Option[OpenComponent[ReachComponentLike, Changing[Boolean]]] = {
		// In some cases, displays both message field and extra right side label
		// In other cases only the message field (which is hidden while empty)
		// The right side hint label expands to the left and not right
		val rightContextPointer = Lazy { hintContextPointer.map { context =>
			context.mapTextInsets { _.mapRight { _.withDefaultPriority }.mapLeft { _.expanding } }
				.withHorizontalTextAlignment(Far)
		} }
		makeRightHintLabel(ExtraFieldCreationContext(wrappedField, rightContextPointer)) match {
			case Some(rightComponent) =>
				actualHintTextPointer match {
					// Case: Hints are sometimes displayed
					case Some(hintTextPointer) =>
						// Places caps to stack equal to horizontal content margin
						// Removed 19.6.2023, because seemed to look better without - Add back or modify if necessary
						/*
						val capPointer = hintContextPointer.map { context =>
							(context.textInsets.horizontal / 2 +
								(if (settings.fillBackground) 0 else defaultBorderWidthFrom(context)))
								.notExpanding
						}*/
						val hintLabel = Open.using(ViewTextLabel) { _.withContextPointer(hintContextPointer)(hintTextPointer) }
						val stack = Open.using(ViewStack) {
							_.row
								.apply(Vector(
									hintLabel.withResult(hintVisibilityPointer),
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
				actualHintTextPointer.map { hintTextPointer =>
					Open.using(ViewTextLabel) { _.withContextPointer(hintContextPointer)(hintTextPointer) }
						.withResult(hintVisibilityPointer)
				}
		}
	}
	
	private def makeBackgroundDrawer() = BackgroundViewDrawer(innerBackgroundPointer)
	
	// TODO: This version doesn't take into account margin between lines
	private def measureText(text: LocalizedString, style: TextDrawContext) =
		MeasuredText(text.string, parentHierarchy.fontMetricsWith(style.font), allowLineBreaks = style.allowLineBreaks)
	
	private def focusBorderWidthFrom(context: BaseContext) = (context.margins.verySmall / 2) max 3
	private def defaultBorderWidthFrom(context: BaseContext) = focusBorderWidthFrom(context) / 3
	
	
	// NESTED	-----------------------------------
	
	private object FocusTracker extends FocusChangeListener
	{
		// Updates focus status
		override def onFocusChangeEvent(event: FocusChangeEvent) = _focusPointer.value = event.hasFocus
	}
}
