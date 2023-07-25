package utopia.reach.component.button.image

import utopia.firmament.context.TextContext
import utopia.firmament.drawing.immutable.CustomDrawableFactory
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.drawing.view.ButtonBackgroundViewDrawer
import utopia.firmament.image.{ButtonImageSet, SingleColorIcon}
import utopia.firmament.localization.{DisplayFunction, LocalizedString}
import utopia.firmament.model.enumeration.GuiElementState.Disabled
import utopia.firmament.model.stack.{StackInsets, StackInsetsConvertible}
import utopia.firmament.model.{GuiElementStatus, HotKey}
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.flow.view.template.eventful.Changing
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.{Alignment, FromAlignmentFactory}
import utopia.paradigm.shape.shape2d.Point
import utopia.reach.component.button.{ButtonSettings, ButtonSettingsLike}
import utopia.reach.component.factory.contextual.VariableContextualFactory
import utopia.reach.component.factory.{FramedFactory, FromContextComponentFactoryFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.image.{ViewImageAndTextLabel, ViewImageLabelSettings}
import utopia.reach.component.template.{ButtonLike, ReachComponentWrapper}
import utopia.reach.cursor.Cursor
import utopia.reach.focus.FocusListener

/**
  * Common trait for view image and text button factories and settings
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 31.05.2023, v1.1
  */
trait ViewImageAndTextButtonSettingsLike[+Repr]
	extends ButtonSettingsLike[Repr] with FramedFactory[Repr] with CustomDrawableFactory[Repr]
{
	// ABSTRACT	--------------------
	
	protected def imageSettings: ViewImageLabelSettings
	protected def buttonSettings: ButtonSettings
	
	/**
	  * @param settings New button settings to use.
	  * @return Copy of this factory with the specified button settings
	  */
	def withButtonSettings(settings: ButtonSettings): Repr
	/**
	  * @param settings New image settings to use.
	  * @return Copy of this factory with the specified image settings
	  */
	def withImageSettings(settings: ViewImageLabelSettings): Repr
	
	
	// COMPUTED	--------------------
	
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
	def imageImageScalingPointer = imageSettings.imageScalingPointer
	/**
	  * uses low priority size from the wrapped view image label settings
	  */
	def imageUsesLowPrioritySize = imageSettings.usesLowPrioritySize
	
	
	// IMPLEMENTED  -----------------
	
	def enabledPointer = buttonSettings.enabledPointer
	def hotKeys = buttonSettings.hotKeys
	def focusListeners = buttonSettings.focusListeners
	
	def withEnabledPointer(p: Changing[Boolean]) = withButtonSettings(buttonSettings.withEnabledPointer(p))
	def withFocusListeners(listeners: Vector[FocusListener]) =
		withButtonSettings(buttonSettings.withFocusListeners(listeners))
	def withHotKeys(keys: Set[HotKey]) = withButtonSettings(buttonSettings.withHotKeys(keys))
	
	
	// OTHER	--------------------
	
	def mapButtonSettings(f: ButtonSettings => ButtonSettings) = withButtonSettings(f(buttonSettings))
	
	def mapImageAlignmentPointer(f: Changing[Alignment] => Changing[Alignment]) =
		withImageAlignmentPointer(f(imageAlignmentPointer))
	def mapImageColorOverlayPointer(f: Option[Changing[Color]] => Option[Changing[Color]]) =
		withImageColorOverlayPointer(f(imageColorOverlayPointer))
	def mapImageImageScalingPointer(f: Changing[Double] => Changing[Double]) =
		withImageImageScalingPointer(f(imageImageScalingPointer))
	def mapImageInsetsPointer(f: Changing[StackInsets] => Changing[StackInsets]) =
		withImageInsetsPointer(f(imageInsetsPointer))
	
	def mapImageSettings(f: ViewImageLabelSettings => ViewImageLabelSettings) =
		withImageSettings(f(imageSettings))
	
	/**
	  * @param p Pointer that determines the image drawing location within this component
	  * @return Copy of this factory with the specified image alignment pointer
	  */
	def withImageAlignmentPointer(p: Changing[Alignment]) =
		withImageSettings(imageSettings.withAlignmentPointer(p))
	def withImageAlignment(alignment: Alignment) = withImageAlignmentPointer(Fixed(alignment))
	/**
	  * @param p Pointer that, when defined, places a color overlay over the drawn image
	  * @return Copy of this factory with the specified image color overlay pointer
	  */
	def withImageColorOverlayPointer(p: Option[Changing[Color]]) =
		withImageSettings(imageSettings.withColorOverlayPointer(p))
	/**
	  * @param p Pointer that determines image scaling, in addition to the original image scaling
	  * @return Copy of this factory with the specified image image scaling pointer
	  */
	def withImageImageScalingPointer(p: Changing[Double]) =
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
}

object ViewImageAndTextButtonSettings
{
	// ATTRIBUTES	--------------------
	
	val default = apply(ViewImageLabelSettings(alignmentPointer = Fixed(Alignment.Right)))
}
/**
  * Combined settings used when constructing view image and text buttons
  * @author Mikko Hilpinen
  * @since 31.05.2023, v1.1
  */
case class ViewImageAndTextButtonSettings(imageSettings: ViewImageLabelSettings = ViewImageLabelSettings.default,
                                          buttonSettings: ButtonSettings = ButtonSettings.default,
                                          insets: StackInsets = StackInsets.any,
                                          customDrawers: Vector[CustomDrawer] = Vector.empty)
	extends ViewImageAndTextButtonSettingsLike[ViewImageAndTextButtonSettings]
{
	// IMPLEMENTED	--------------------
	
	override def withButtonSettings(settings: ButtonSettings) = copy(buttonSettings = settings)
	override def withImageSettings(settings: ViewImageLabelSettings) = copy(imageSettings = settings)
	override def withInsets(insets: StackInsetsConvertible): ViewImageAndTextButtonSettings =
		copy(insets = insets.toInsets)
	override def withCustomDrawers(drawers: Vector[CustomDrawer]): ViewImageAndTextButtonSettings =
		copy(customDrawers = drawers)
}

/**
  * Common trait for factories that wrap a view image and text button settings instance
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 31.05.2023, v1.1
  */
trait ViewImageAndTextButtonSettingsWrapper[+Repr] extends ViewImageAndTextButtonSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Settings wrapped by this instance
	  */
	protected def settings: ViewImageAndTextButtonSettings
	
	/**
	  * @return Copy of this factory with the specified settings
	  */
	def withSettings(settings: ViewImageAndTextButtonSettings): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def buttonSettings = settings.buttonSettings
	override def imageSettings = settings.imageSettings
	override def insets: StackInsets = settings.insets
	override def customDrawers: Vector[CustomDrawer] = settings.customDrawers
	
	override def withButtonSettings(settings: ButtonSettings) = mapSettings { _.withButtonSettings(settings) }
	override def withImageSettings(settings: ViewImageLabelSettings) =
		mapSettings { _.withImageSettings(settings) }
	override def withInsets(insets: StackInsetsConvertible): Repr = mapSettings { _.withInsets(insets) }
	override def withCustomDrawers(drawers: Vector[CustomDrawer]): Repr = mapSettings { _.withCustomDrawers(drawers) }
	
	
	// OTHER	--------------------
	
	def mapSettings(f: ViewImageAndTextButtonSettings => ViewImageAndTextButtonSettings) =
		withSettings(f(settings))
}

/**
  * Factory class used for constructing view image and text buttons using contextual component
  * creation information
  * @author Mikko Hilpinen
  * @since 31.05.2023, v1.1
  */
case class ContextualViewImageAndTextButtonFactory(parentHierarchy: ComponentHierarchy,
                                                   contextPointer: Changing[TextContext],
                                                   settings: ViewImageAndTextButtonSettings = ViewImageAndTextButtonSettings.default)
	extends ViewImageAndTextButtonSettingsWrapper[ContextualViewImageAndTextButtonFactory]
		with VariableContextualFactory[TextContext, ContextualViewImageAndTextButtonFactory]
		with FromAlignmentFactory[ContextualViewImageAndTextButtonFactory]
{
	// IMPLEMENTED  --------------------------
	
	override def withContextPointer(contextPointer: Changing[TextContext]) =
		copy(contextPointer = contextPointer)
	override def withSettings(settings: ViewImageAndTextButtonSettings) =
		copy(settings = settings)
	
	override def apply(alignment: Alignment) = copy(
		contextPointer = contextPointer.map { _.withTextAlignment(alignment) },
		settings = settings.withImageAlignment(alignment.opposite)
	)
	
	
	// OTHER    ------------------------------
	
	/**
	  * Creates a new button
	  * @param contentPointer (Textual) content to display on this button
	  * @param imagesPointer Pointer to the displayed button images
	  * @param displayFunction Function which converts the button content to text. Default = use .toString
	  * @param action Action that should be performed when this button is pressed.
	  *               Accepts the current content of this button.
	  * @tparam A Type of button content
	  * @return A new button
	  */
	def apply[A](contentPointer: Changing[A], imagesPointer: Changing[ButtonImageSet],
	             displayFunction: DisplayFunction[A] = DisplayFunction.raw)
	            (action: A => Unit) =
		new ViewImageAndTextButton[A](parentHierarchy, contextPointer, contentPointer, imagesPointer, settings,
			displayFunction)(action)
	
	/**
	  * Creates a new button
	  * @param contentPointer  (Textual) content to display on this button
	  * @param iconPointer Icon to display on this button
	  * @param displayFunction Function which converts the button content to text. Default = use .toString
	  * @param action          Action that should be performed when this button is pressed.
	  *                        Accepts the current content of this button.
	  * @tparam A Type of button content
	  * @return A new button
	  */
	def icon[A](contentPointer: Changing[A], iconPointer: Changing[SingleColorIcon],
	            displayFunction: DisplayFunction[A] = DisplayFunction.raw)
	           (action: A => Unit) =
		apply[A](contentPointer, iconPointer.mergeWith(contextPointer) { _.inButton.contextual(_) },
			displayFunction)(action)
	@deprecated("Please use .icon(...) instead", "v1.1")
	def withIcon[A](contentPointer: Changing[A], iconPointer: Changing[SingleColorIcon],
	                displayFunction: DisplayFunction[A] = DisplayFunction.raw)
	               (action: A => Unit) =
		icon[A](contentPointer, iconPointer, displayFunction)(action)
	
	/**
	  * Creates a new button
	  * @param textPointer Pointer to the text to display
	  * @param imagesPointer Pointer to the button images to use
	  * @param action Action to perform when this button is pressed
	  * @tparam U Arbitrary action result type
	  * @return A new button
	  */
	def text[U](textPointer: Changing[LocalizedString], imagesPointer: Changing[ButtonImageSet])
	           (action: => U) =
		apply[LocalizedString](textPointer, imagesPointer, DisplayFunction.identity) { _ => action }
	/**
	  * Creates a new button
	  * @param textPointer   Pointer to the text to display
	  * @param iconPointer Pointer to the icon to display on this button
	  * @param action        Action to perform when this button is pressed
	  * @tparam U Arbitrary action result type
	  * @return A new button
	  */
	def textAndIcon[U](textPointer: Changing[LocalizedString], iconPointer: Changing[SingleColorIcon])
	                         (action: => U) =
		icon(textPointer, iconPointer, DisplayFunction.identity) { _ => action }
	
	/**
	  * Creates a new button
	  * @param text   Text to display
	  * @param images Button images to use
	  * @param action        Action to perform when this button is pressed
	  * @tparam U Arbitrary action result type
	  * @return A new button
	  */
	def fixedText[U](text: LocalizedString, images: ButtonImageSet)(action: => U) =
		this.text(Fixed(text), Fixed(images))(action)
	/**
	  * Creates a new button
	  * @param text   Text to display
	  * @param icon Icon to display
	  * @param action Action to perform when this button is pressed
	  * @tparam U Arbitrary action result type
	  * @return A new button
	  */
	def fixedTextAndIcon[U](text: LocalizedString, icon: SingleColorIcon)(action: => U) =
		textAndIcon(Fixed(text), Fixed(icon))(action)
	
	/**
	  * Creates a new button with image and text
	  * @param text Text displayed on this button
	  * @param imagesPointer Pointer to the displayed image set
	  * @param action Action performed when this button is pressed (accepts current content)
	  * @return A new button
	  */
	@deprecated("Please use .fixedText or .text instead", "v1.1")
	def withStaticText(text: LocalizedString, imagesPointer: Changing[ButtonImageSet])(action: => Unit) =
		this.text(Fixed(text), imagesPointer)(action)
	/**
	  * Creates a new button with image and text
	  * @param text Text displayed on this button
	  * @param icon Icon displayed on this button
	  * @param action Action performed when this button is pressed (accepts current content)
	  * @return A new button
	  */
	@deprecated("Renamed to .fixedTextAndIcon(...)", "v1.1")
	def withStaticTextAndIcon(text: LocalizedString, icon: SingleColorIcon)(action: => Unit) =
		fixedTextAndIcon(text, icon)(action)
}

/**
  * Used for defining view image and text button creation settings outside of the component building process
  * @author Mikko Hilpinen
  * @since 31.05.2023, v1.1
  */
case class ViewImageAndTextButtonSetup(settings: ViewImageAndTextButtonSettings = ViewImageAndTextButtonSettings.default)
	extends ViewImageAndTextButtonSettingsWrapper[ViewImageAndTextButtonSetup]
		with FromContextComponentFactoryFactory[TextContext, ContextualViewImageAndTextButtonFactory]
{
	// IMPLEMENTED	--------------------
	
	override def withContext(hierarchy: ComponentHierarchy, context: TextContext) =
		ContextualViewImageAndTextButtonFactory(hierarchy, Fixed(context), settings)
	
	override def withSettings(settings: ViewImageAndTextButtonSettings) =
		copy(settings = settings)
	
	
	// OTHER    ----------------------
	
	def withContext(hierarchy: ComponentHierarchy, context: Changing[TextContext]) =
		ContextualViewImageAndTextButtonFactory(hierarchy, context, settings)
}

object ViewImageAndTextButton extends ViewImageAndTextButtonSetup()
{
	// OTHER	--------------------
	
	def apply(settings: ViewImageAndTextButtonSettings) = withSettings(settings)
}

/**
  * A pointer-based button that displays both an image and text
  * @author Mikko Hilpinen
  * @since 10.11.2020, v0.1
  */
class ViewImageAndTextButton[A](parentHierarchy: ComponentHierarchy, contextPointer: Changing[TextContext],
                                contentPointer: Changing[A], imagesPointer: Changing[ButtonImageSet],
                                settings: ViewImageAndTextButtonSettings,
                                displayFunction: DisplayFunction[A] = DisplayFunction.raw)
                               (action: A => Unit)
	extends ReachComponentWrapper with ButtonLike
{
	// ATTRIBUTES	-----------------------------
	
	private val baseStatePointer = new PointerWithEvents(GuiElementStatus.identity)
	override val statePointer = baseStatePointer
		.mergeWith(settings.enabledPointer) { (state, enabled) => state + (Disabled -> !enabled) }
	override val focusListeners = new ButtonDefaultFocusListener(baseStatePointer) +: settings.focusListeners
	override val focusId = hashCode()
	
	/**
	  * A pointer that refers to this button's main color
	  */
	val colorPointer = contextPointer.strongMapWhile(parentHierarchy.linkPointer) { _.background }
	
	override protected val wrapped = {
		// Adds additional text insets
		val labelContextPointer = contextPointer.strongMapWhile(parentHierarchy.linkPointer) { c =>
			c.mapTextInsets { _ + settings.insets.withoutSides(c.textAlignment.directions) + c.buttonBorderWidth }
		}
		// Adds additional image insets
		val labelImageSettings = settings.imageSettings
			.mapInsetsPointer { _.mergeWith(contextPointer) { (base, context) =>
				base + settings.insets.withoutSides(context.textAlignment.opposite.directions) +
					context.buttonBorderWidth
			} }
		
		val imagePointer = imagesPointer.mergeWith(statePointer) { _(_) }
		// TODO: Add forceEqualBreadth option (if needed)
		ViewImageAndTextLabel.withContext(parentHierarchy, labelContextPointer).withImageSettings(labelImageSettings)
			.withCustomDrawers(
				ButtonBackgroundViewDrawer(colorPointer, statePointer,
					contextPointer.strongMapWhile(parentHierarchy.linkPointer) { _.buttonBorderWidth }
				) +: settings.customDrawers
			)
			.apply(contentPointer, imagePointer, displayFunction)
	}
	
	
	// COMPUTED	----------------------------------
	
	/**
	  * @return This button's current background color
	  */
	def color = colorPointer.value
	
	
	// INITIAL CODE	------------------------------
	
	setup(baseStatePointer, settings.hotKeys)
	colorPointer.addContinuousAnyChangeListener { repaint() }
	
	
	// IMPLEMENTED	------------------------------
	
	override protected def trigger() = action(contentPointer.value)
	
	override def cursorToImage(cursor: Cursor, position: Point) = cursor.over(color)
}
