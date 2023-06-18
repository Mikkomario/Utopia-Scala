package utopia.reach.component.button.image

import utopia.firmament.context.ColorContext
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.image.{ButtonImageSet, SingleColorIcon}
import utopia.firmament.model.enumeration.GuiElementState.Disabled
import utopia.firmament.model.stack.StackInsets
import utopia.firmament.model.{GuiElementStatus, HotKey}
import utopia.flow.view.immutable.eventful.{AlwaysFalse, AlwaysTrue, Fixed}
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.flow.view.template.eventful.Changing
import utopia.paradigm.color.ColorLevel.Standard
import utopia.paradigm.color.{Color, ColorLevel, ColorRole, ColorShade}
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.shape.shape2d.Point
import utopia.reach.component.button.{ButtonSettings, ButtonSettingsLike}
import utopia.reach.component.factory.contextual.VariableContextualFactory
import utopia.reach.component.factory.{ComponentFactoryFactory, FromVariableContextComponentFactoryFactory, FromVariableContextFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.image.{ViewImageLabel, ViewImageLabelSettings, ViewImageLabelSettingsLike}
import utopia.reach.component.template.{ButtonLike, ReachComponentWrapper}
import utopia.reach.cursor.Cursor
import utopia.reach.focus.FocusListener

/**
  * Common trait for view image button factories and settings
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 01.06.2023, v1.1
  */
trait ViewImageButtonSettingsLike[+Repr] extends ButtonSettingsLike[Repr] with ViewImageLabelSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	def buttonSettings: ButtonSettings
	def imageSettings: ViewImageLabelSettings
	
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
	
	
	// IMPLEMENTED	--------------------
	
	def enabledPointer = buttonSettings.enabledPointer
	def hotKeys = buttonSettings.hotKeys
	def focusListeners = buttonSettings.focusListeners
	def customDrawers = imageSettings.customDrawers
	def insetsPointer = imageSettings.insetsPointer
	def alignmentPointer = imageSettings.alignmentPointer
	def colorOverlayPointer = imageSettings.colorOverlayPointer
	def imageScalingPointer = imageSettings.imageScalingPointer
	def usesLowPrioritySize = imageSettings.usesLowPrioritySize
	
	def withAlignmentPointer(p: Changing[Alignment]) = withImageSettings(imageSettings
		.withAlignmentPointer(p))
	def withColorOverlayPointer(p: Option[Changing[Color]]) =
		withImageSettings(imageSettings.withColorOverlayPointer(p))
	def withCustomDrawers(drawers: Vector[CustomDrawer]) =
		withImageSettings(imageSettings.withCustomDrawers(drawers))
	def withEnabledPointer(p: Changing[Boolean]) = withButtonSettings(buttonSettings.withEnabledPointer(p))
	def withFocusListeners(listeners: Vector[FocusListener]) =
		withButtonSettings(buttonSettings.withFocusListeners(listeners))
	def withHotKeys(keys: Set[HotKey]) = withButtonSettings(buttonSettings.withHotKeys(keys))
	def withImageScalingPointer(p: Changing[Double]) = withImageSettings(imageSettings
		.withImageScalingPointer(p))
	def withInsetsPointer(p: Changing[StackInsets]) = withImageSettings(imageSettings.withInsetsPointer(p))
	def withUseLowPrioritySize(lowPriority: Boolean) =
		withImageSettings(imageSettings.withUseLowPrioritySize(lowPriority))
	
	
	// OTHER	--------------------
	
	def mapButtonSettings(f: ButtonSettings => ButtonSettings) = withButtonSettings(f(buttonSettings))
	def mapImageSettings(f: ViewImageLabelSettings => ViewImageLabelSettings) =
		withImageSettings(f(imageSettings))
}

object ViewImageButtonSettings
{
	// ATTRIBUTES	--------------------
	
	val default = apply()
}

/**
  * Combined settings used when constructing view image buttons
  * @author Mikko Hilpinen
  * @since 01.06.2023, v1.1
  */
case class ViewImageButtonSettings(buttonSettings: ButtonSettings = ButtonSettings.default,
                                   imageSettings: ViewImageLabelSettings = ViewImageLabelSettings.default)
	extends ViewImageButtonSettingsLike[ViewImageButtonSettings]
{
	// IMPLEMENTED	--------------------
	
	override def self: ViewImageButtonSettings = this
	
	override def withButtonSettings(settings: ButtonSettings) = copy(buttonSettings = settings)
	override def withImageSettings(settings: ViewImageLabelSettings) = copy(imageSettings = settings)
	
	override def *(mod: Double): ViewImageButtonSettings = mapImageSettings { _ * mod }
}

/**
  * Common trait for factories that wrap a view image button settings instance
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 01.06.2023, v1.1
  */
trait ViewImageButtonSettingsWrapper[+Repr] extends ViewImageButtonSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Settings wrapped by this instance
	  */
	protected def settings: ViewImageButtonSettings
	
	/**
	  * @return Copy of this factory with the specified settings
	  */
	def withSettings(settings: ViewImageButtonSettings): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def buttonSettings = settings.buttonSettings
	override def imageSettings = settings.imageSettings
	
	override def withButtonSettings(settings: ButtonSettings) = mapSettings { _.withButtonSettings(settings) }
	override def withImageSettings(settings: ViewImageLabelSettings) =
		mapSettings { _.withImageSettings(settings) }
	
	
	// OTHER	--------------------
	
	def mapSettings(f: ViewImageButtonSettings => ViewImageButtonSettings) = withSettings(f(settings))
}

/**
  * Common trait for factories that are used for constructing view image buttons
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 01.06.2023, v1.1
  */
trait ViewImageButtonFactoryLike[+Repr] extends ViewImageButtonSettingsWrapper[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * The component hierarchy, to which created view image buttons will be attached
	  */
	protected def parentHierarchy: ComponentHierarchy
	/**
	  * @return Pointer that determines whether the drawn images should be allowed to scale
	  *         beyond their original source resolution
	  */
	protected def allowsUpscalingPointer: Changing[Boolean]
	
	
	// OTHER	--------------------
	
	/**
	  * Creates a new view image button
	  * @param images Pointer that determines the image/images to display on this button
	  * @param action The action performed when this button is triggered
	  * @return A new view image button
	  */
	def apply[U](images: Changing[ButtonImageSet])(action: => U) =
		new ViewImageButton(parentHierarchy, images, settings, allowsUpscalingPointer)(action)
	/**
	  * Creates a new view image button
	  * @param images Image/images to display on this button
	  * @param action The action performed when this button is triggered
	  * @return A new view image button
	  */
	def apply[U](images: ButtonImageSet)(action: => U): ViewImageButton =
		apply(Fixed(images))(action)
}

/**
  * Factory class used for constructing view image buttons using contextual component creation information
  * @author Mikko Hilpinen
  * @since 01.06.2023, v1.1
  */
case class ContextualViewImageButtonFactory(parentHierarchy: ComponentHierarchy,
                                            contextPointer: Changing[ColorContext],
                                            settings: ViewImageButtonSettings = ViewImageButtonSettings.default)
	extends ViewImageButtonFactoryLike[ContextualViewImageButtonFactory]
		with VariableContextualFactory[ColorContext, ContextualViewImageButtonFactory]
{
	// IMPLEMENTED	-------------------------
	
	override def self: ContextualViewImageButtonFactory = this
	
	override protected def allowsUpscalingPointer: Changing[Boolean] = contextPointer.map { _.allowImageUpscaling }
	
	override def withContextPointer(contextPointer: Changing[ColorContext]) =
		copy(contextPointer = contextPointer)
	override def withSettings(settings: ViewImageButtonSettings) = copy(settings = settings)
	
	override def *(mod: Double): ContextualViewImageButtonFactory =
		copy(contextPointer = contextPointer.map { _ * mod }, settings = settings * mod)
	
	
	// OTHER	-----------------------------
	
	/**
	  * Creates a new button
	  * @param iconPointer Pointer that determines the icon to display on this button
	  * @param action Action to perform when this button is triggered
	  * @tparam U Arbitrary action result type
	  * @return A new button
	  */
	def icon[U](iconPointer: Changing[SingleColorIcon])(action: => U) =
		apply(iconPointer.mergeWith(contextPointer) { _.asButton.contextual(_) })(action)
	/**
	  * Creates a new button
	  * @param icon The icon to display on this button
	  * @param action      Action to perform when this button is triggered
	  * @tparam U Arbitrary action result type
	  * @return A new button
	  */
	def icon[U](icon: SingleColorIcon)(action: => U): ViewImageButton =
		this.icon(Fixed(icon))(action)
	
	/**
	  * Creates a new button
	  * @param iconPointer Pointer that determines the icon to display on this button
	  * @param rolePointer Pointer that determines the color (role) to apply to the icon
	  * @param preferredShade Preferred color shade to use (default = Standard)
	  * @param action      Action to perform when this button is triggered
	  * @tparam U Arbitrary action result type
	  * @return A new button
	  */
	def coloredIcon[U](iconPointer: Changing[SingleColorIcon], rolePointer: Changing[ColorRole],
	                   preferredShade: ColorLevel = Standard)
	                  (action: => U) =
		apply[U](iconPointer.mergeWith(contextPointer, rolePointer) { (icon, context, role) =>
			icon.asButton(context.color.preferring(preferredShade)(role))
		})(action)
	/**
	  * Creates a new button
	  * @param icon    The icon to display on this button
	  * @param role    The color (role) to apply to the icon
	  * @param preferredShade Preferred color shade to use (default = Standard)
	  * @param action         Action to perform when this button is triggered
	  * @tparam U Arbitrary action result type
	  * @return A new button
	  */
	def coloredIcon[U](icon: SingleColorIcon, role: ColorRole, preferredShade: ColorLevel)
	                  (action: => U): ViewImageButton =
		coloredIcon(Fixed(icon), Fixed(role), preferredShade)(action)
	/**
	  * Creates a new button
	  * @param icon           The icon to display on this button
	  * @param role           The color (role) to apply to the icon
	  * @param action         Action to perform when this button is triggered
	  * @tparam U Arbitrary action result type
	  * @return A new button
	  */
	def coloredIcon[U](icon: SingleColorIcon, role: ColorRole)(action: => U): ViewImageButton =
		coloredIcon(icon, role, Standard)(action)
	
	/**
	  * Creates a new button
	  * @param iconPointer              A pointer to the icon used in this button
	  * @param action                   Action performed each time this button is triggered
	  * @return A new button
	  */
	@deprecated("Renamed to .icon(...)", "v1.1")
	def withIcon(iconPointer: Changing[SingleColorIcon])(action: => Unit) =
		icon(iconPointer)(action)
	/**
	  * Creates a new button
	  * @param iconPointer              A pointer to the icon used in this button
	  * @param rolePointer              A pointer to the role this button serves / the color set it should use
	  * @param preferredShade           Preferred color shade to use (default = standard)
	  * @param action                   Action performed each time this button is triggered
	  * @return A new button
	  */
	@deprecated("Renamed to .coloredIcon(...)", "v1.1")
	def withColouredIcon(iconPointer: Changing[SingleColorIcon], rolePointer: Changing[ColorRole],
	                     preferredShade: ColorLevel = Standard)
	                    (action: => Unit) =
		coloredIcon(iconPointer, rolePointer, preferredShade)(action)
}

/**
  * Factory class that is used for constructing view image buttons without using contextual information
  * @author Mikko Hilpinen
  * @since 01.06.2023, v1.1
  */
case class ViewImageButtonFactory(parentHierarchy: ComponentHierarchy,
                                  settings: ViewImageButtonSettings = ViewImageButtonSettings.default,
                                  allowsUpscalingPointer: Changing[Boolean] = AlwaysFalse)
	extends ViewImageButtonFactoryLike[ViewImageButtonFactory]
		with FromVariableContextFactory[ColorContext, ContextualViewImageButtonFactory]
{
	// COMPTUTED    --------------------
	
	/**
	  * @return Copy of this factory that allows the drawn images to scale beyond their source resolution
	  */
	def allowingUpscaling = copy(allowsUpscalingPointer = AlwaysTrue)
	
	
	// IMPLEMENTED	--------------------
	
	override def self: ViewImageButtonFactory = this
	
	override def withContextPointer(context: Changing[ColorContext]) =
		ContextualViewImageButtonFactory(parentHierarchy, context, settings)
	
	override def withSettings(settings: ViewImageButtonSettings) = copy(settings = settings)
	
	override def *(mod: Double): ViewImageButtonFactory = mapSettings { _ * mod }
}

/**
  * Used for defining view image button creation settings outside of the component building process
  * @author Mikko Hilpinen
  * @since 01.06.2023, v1.1
  */
case class ViewImageButtonSetup(settings: ViewImageButtonSettings = ViewImageButtonSettings.default)
	extends ViewImageButtonSettingsWrapper[ViewImageButtonSetup]
		with ComponentFactoryFactory[ViewImageButtonFactory]
		with FromVariableContextComponentFactoryFactory[ColorContext, ContextualViewImageButtonFactory]
{
	// IMPLEMENTED	--------------------
	
	override def self: ViewImageButtonSetup = this
	
	override def apply(hierarchy: ComponentHierarchy) = ViewImageButtonFactory(hierarchy, settings)
	
	override def withContextPointer(hierarchy: ComponentHierarchy,
	                                context: Changing[ColorContext]): ContextualViewImageButtonFactory =
		ContextualViewImageButtonFactory(hierarchy, context, settings)
	override def withSettings(settings: ViewImageButtonSettings) = copy(settings = settings)
	
	override def *(mod: Double): ViewImageButtonSetup = mapSettings { _ * mod }
	
	
	// OTHER	--------------------
	
	/**
	  * @return A new view image button factory that uses the specified (variable) context
	  */
	def withContext(hierarchy: ComponentHierarchy, context: Changing[ColorContext]) =
		ContextualViewImageButtonFactory(hierarchy, context, settings)
}

object ViewImageButton extends ViewImageButtonSetup()
{
	// OTHER	--------------------
	
	def apply(settings: ViewImageButtonSettings) = withSettings(settings)
}

/**
  * A button that only draws images and whose state is dependent from a number of pointers
  * @author Mikko Hilpinen
  * @since 29.10.2020, v0.1
  */
class ViewImageButton(parentHierarchy: ComponentHierarchy, imagesPointer: Changing[ButtonImageSet],
                      settings: ViewImageButtonSettings, allowUpscalingPointer: Changing[Boolean] = AlwaysTrue)
                     (action: => Unit)
	extends ReachComponentWrapper with ButtonLike
{
	// ATTRIBUTES	-----------------------------
	
	private val baseStatePointer = new PointerWithEvents(GuiElementStatus.identity)
	
	override val statePointer = baseStatePointer
		.mergeWith(settings.enabledPointer) { (base, enabled) => base + (Disabled -> !enabled) }
	override protected val wrapped = ViewImageLabel(parentHierarchy)
		.withSettings(settings.imageSettings)
		.copy(allowUpscalingPointer = allowUpscalingPointer)
		.apply(statePointer.mergeWith(imagesPointer) { (state, images) => images(state) })
	override val focusListeners = new ButtonDefaultFocusListener(baseStatePointer) +: settings.focusListeners
	override val focusId = hashCode()
	
	/**
	  * A pointer to this button's current overall shade (based on the focused-state)
	  */
	val shadePointer = imagesPointer.lazyMap { images =>
		ColorShade.forLuminosity(images.focusImage.pixels.averageLuminosity) }
	
	
	// INITIAL CODE	-----------------------------
	
	setup(baseStatePointer, settings.hotKeys)
	
	
	// COMPUTED	---------------------------------
	
	/**
	  * @return The current overall shade of this button (based on the focused-state)
	  */
	def shade = shadePointer.value
	
	
	// IMPLEMENTED	-----------------------------
	
	override protected def trigger() = action
	
	override def cursorToImage(cursor: Cursor, position: Point) = cursor(shade)
}
