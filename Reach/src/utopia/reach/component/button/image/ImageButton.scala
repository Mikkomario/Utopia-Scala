package utopia.reach.component.button.image

import utopia.firmament.context.ColorContext
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.image.{ButtonImageSet, SingleColorIcon}
import utopia.firmament.model.enumeration.GuiElementState.Disabled
import utopia.firmament.model.stack.StackInsetsConvertible
import utopia.firmament.model.{GuiElementStatus, HotKey}
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.flow.view.template.eventful.Changing
import utopia.paradigm.color.ColorLevel.Standard
import utopia.paradigm.color.{Color, ColorLevel, ColorRole, ColorShade}
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.shape.shape2d.Point
import utopia.reach.component.button.{ButtonSettings, ButtonSettingsLike}
import utopia.reach.component.factory.contextual.{ColorContextualFactory, ContextualBackgroundAssignableFactory}
import utopia.reach.component.factory.{ComponentFactoryFactory, FromContextComponentFactoryFactory, FromContextFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.image.{ImageLabelSettings, ImageLabelSettingsLike, ViewImageLabel}
import utopia.reach.component.template.{ButtonLike, ReachComponentWrapper}
import utopia.reach.cursor.Cursor
import utopia.reach.focus.FocusListener

/**
  * Common trait for image button factories and settings
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 31.05.2023, v1.1
  */
trait ImageButtonSettingsLike[+Repr] extends ButtonSettingsLike[Repr] with ImageLabelSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	def buttonSettings: ButtonSettings
	def imageSettings: ImageLabelSettings
	
	/**
	  * @param settings New button settings to use.
	  * @return Copy of this factory with the specified button settings
	  */
	def withButtonSettings(settings: ButtonSettings): Repr
	/**
	  * @param settings New image settings to use.
	  * @return Copy of this factory with the specified image settings
	  */
	def withImageSettings(settings: ImageLabelSettings): Repr
	
	
	// IMPLEMENTED  ----------------
	
	def customDrawers = imageSettings.customDrawers ++ buttonSettings.customDrawers
	def enabledPointer = buttonSettings.enabledPointer
	def hotKeys = buttonSettings.hotKeys
	def focusListeners = buttonSettings.focusListeners
	def insets = imageSettings.insets
	def alignment = imageSettings.alignment
	def imageScaling = imageSettings.imageScaling
	def colorOverlay = imageSettings.colorOverlay
	def usesLowPrioritySize = imageSettings.usesLowPrioritySize
	
	override def apply(alignment: Alignment): Repr = mapImageSettings { _.withAlignment(alignment) }
	
	def withColor(color: Option[Color]) = withImageSettings(imageSettings.withColor(color))
	def withEnabledPointer(p: Changing[Boolean]) = withButtonSettings(buttonSettings.withEnabledPointer(p))
	def withFocusListeners(listeners: Vector[FocusListener]) =
		withButtonSettings(buttonSettings.withFocusListeners(listeners))
	def withHotKeys(keys: Set[HotKey]) = withButtonSettings(buttonSettings.withHotKeys(keys))
	def withImageScaling(scaling: Double) = withImageSettings(imageSettings.withImageScaling(scaling))
	def withInsets(insets: StackInsetsConvertible) = withImageSettings(imageSettings.withInsets(insets))
	def withUseLowPrioritySize(lowPriority: Boolean) =
		withImageSettings(imageSettings.withUseLowPrioritySize(lowPriority))
	
	
	// OTHER	--------------------
	
	def mapButtonSettings(f: ButtonSettings => ButtonSettings) = withButtonSettings(f(buttonSettings))
	def mapImageSettings(f: ImageLabelSettings => ImageLabelSettings) = withImageSettings(f(imageSettings))
}

object ImageButtonSettings
{
	// ATTRIBUTES	--------------------
	
	val default = apply()
}
/**
  * Combined settings used when constructing image buttons
  * @author Mikko Hilpinen
  * @since 31.05.2023, v1.1
  */
case class ImageButtonSettings(buttonSettings: ButtonSettings = ButtonSettings.default,
                               imageSettings: ImageLabelSettings = ImageLabelSettings.default)
	extends ImageButtonSettingsLike[ImageButtonSettings]
{
	// IMPLEMENTED	--------------------
	
	override def withButtonSettings(settings: ButtonSettings) = copy(buttonSettings = settings)
	override def withImageSettings(settings: ImageLabelSettings) = copy(imageSettings = settings)
	
	override def withCustomDrawers(drawers: Vector[CustomDrawer]): ImageButtonSettings = {
		copy(
			buttonSettings = buttonSettings.withCustomDrawers(drawers.filterNot(imageSettings.customDrawers.contains)),
			imageSettings = imageSettings.withCustomDrawers(imageSettings.customDrawers.filter(drawers.contains))
		)
	}
}

/**
  * Common trait for factories that wrap a image button settings instance
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 31.05.2023, v1.1
  */
trait ImageButtonSettingsWrapper[+Repr] extends ImageButtonSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Settings wrapped by this instance
	  */
	protected def settings: ImageButtonSettings
	
	/**
	  * @return Copy of this factory with the specified settings
	  */
	def withSettings(settings: ImageButtonSettings): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def buttonSettings = settings.buttonSettings
	override def imageSettings = settings.imageSettings
	
	override def withButtonSettings(settings: ButtonSettings) = mapSettings { _.withButtonSettings(settings) }
	override def withImageSettings(settings: ImageLabelSettings) =
		mapSettings { _.withImageSettings(settings) }
	
	override def withCustomDrawers(drawers: Vector[CustomDrawer]): Repr = mapSettings { _.withCustomDrawers(drawers) }
	
	
	// OTHER	--------------------
	
	def mapSettings(f: ImageButtonSettings => ImageButtonSettings) = withSettings(f(settings))
}

/**
  * Common trait for factories that are used for constructing image buttons
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 31.05.2023, v1.1
  */
trait ImageButtonFactoryLike[+Repr] extends ImageButtonSettingsWrapper[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * The component hierarchy, to which created image buttons will be attached
	  */
	protected def parentHierarchy: ComponentHierarchy
	
	/**
	  * @return Whether images should be allowed to scale beyond their source resolution
	  */
	protected def allowsUpscaling: Boolean
	
	
	// OTHER	--------------------
	
	/**
	  * Creates a new image button
	  * @param images Images to display on this button
	  * @param action Action that will be triggered when this button is pressed
	  * @return A new image button
	  */
	def apply[U](images: ButtonImageSet)(action: => U) =
		new ImageButton(parentHierarchy, images, settings, allowsUpscaling)(action)
}

/**
  * Factory class used for constructing image buttons using contextual component creation information
  * @author Mikko Hilpinen
  * @since 31.05.2023, v1.1
  */
case class ContextualImageButtonFactory(parentHierarchy: ComponentHierarchy, context: ColorContext,
                                        settings: ImageButtonSettings = ImageButtonSettings.default)
	extends ImageButtonFactoryLike[ContextualImageButtonFactory]
		with ColorContextualFactory[ContextualImageButtonFactory]
		with ContextualBackgroundAssignableFactory[ColorContext, ContextualImageButtonFactory]
{
	// IMPLICIT	-----------------------------
	
	private implicit def c: ColorContext = context
	
	
	// IMPLEMENTED	-------------------------
	
	override def self: ContextualImageButtonFactory = this
	override protected def allowsUpscaling: Boolean = context.allowImageUpscaling
	
	override def withContext(newContext: ColorContext) = copy(context = newContext)
	override def withSettings(settings: ImageButtonSettings) = copy(settings = settings)
	
	
	// OTHER	-----------------------------
	
	/**
	  * Creates a new button
	  * @param icon Icon that forms this button
	  * @param color Color (role) to apply to the icon (optional)
	  * @param preferredShade Preferred color shade to use (default = Standard).
	  *                       Only used when 'color' is specified.
	  * @param action Action performed when this button is triggered
	  * @tparam U Arbitrary action result type
	  * @return A new button
	  */
	def icon[U](icon: SingleColorIcon, color: Option[ColorRole] = None, preferredShade: ColorLevel = Standard)
	           (action: => U) =
	{
		val images = color match {
			case Some(color) => icon.asButton(context.color.preferring(preferredShade)(color))
			case None => icon.asButton.contextual
		}
		apply(images)(action)
	}
	
	/**
	  * Creates a new button
	  * @param icon                     Icon used in this button
	  * @param action                   Action performed each time this button is triggered
	  * @return A new button
	  */
	@deprecated("Renamed to .icon(SingleColorIcon)", "v1.1")
	def withIcon(icon: SingleColorIcon)(action: => Unit) =
		this.icon(icon)(action)
	/**
	  * Creates a new button
	  * @param icon                     Icon used in this button
	  * @param role                     The role of this button / the colour used in this button
	  * @param preferredShade           Preferred color shade to use (default = standard)
	  * @param action                   Action performed each time this button is triggered
	  * @return A new button
	  */
	@deprecated("Please use .icon(...) instead", "v1.1")
	def withColouredIcon(icon: SingleColorIcon, role: ColorRole, preferredShade: ColorLevel = Standard)
	                    (action: => Unit) =
		this.icon(icon, Some(role), preferredShade)(action)
}

/**
  * Factory class that is used for constructing image buttons without using contextual information
  * @author Mikko Hilpinen
  * @since 31.05.2023, v1.1
  */
case class ImageButtonFactory(parentHierarchy: ComponentHierarchy,
                              settings: ImageButtonSettings = ImageButtonSettings.default,
                              allowsUpscaling: Boolean = false)
	extends ImageButtonFactoryLike[ImageButtonFactory]
		with FromContextFactory[ColorContext, ContextualImageButtonFactory]
{
	// COMPUTED -------------------------------
	
	/**
	  * @return Copy of this factory that allows image scaling beyond their source resolutions
	  */
	def allowingUpscaling = copy(allowsUpscaling = true)
	
	
	// IMPLEMENTED	---------------------------
	
	override def withContext(context: ColorContext) =
		ContextualImageButtonFactory(parentHierarchy, context, settings)
	
	override def withSettings(settings: ImageButtonSettings) = copy(settings = settings)
}

/**
  * Used for defining image button creation settings outside of the component building process
  * @author Mikko Hilpinen
  * @since 31.05.2023, v1.1
  */
case class ImageButtonSetup(settings: ImageButtonSettings = ImageButtonSettings.default)
	extends ImageButtonSettingsWrapper[ImageButtonSetup] with ComponentFactoryFactory[ImageButtonFactory]
		with FromContextComponentFactoryFactory[ColorContext, ContextualImageButtonFactory]
{
	// IMPLEMENTED	--------------------
	
	override def apply(hierarchy: ComponentHierarchy) = ImageButtonFactory(hierarchy, settings)
	
	override def withContext(hierarchy: ComponentHierarchy, context: ColorContext) =
		ContextualImageButtonFactory(hierarchy, context, settings)
	
	override def withSettings(settings: ImageButtonSettings) = copy(settings = settings)
}

object ImageButton extends ImageButtonSetup()
{
	// OTHER	--------------------
	
	def apply(settings: ImageButtonSettings) = withSettings(settings)
}

/**
  * A button that only draws an image
  * @author Mikko Hilpinen
  * @since 29.10.2020, v0.1
  */
class ImageButton(parentHierarchy: ComponentHierarchy, images: ButtonImageSet, settings: ImageButtonSettings,
                  allowUpscaling: Boolean = true)
                 (action: => Unit)
	extends ReachComponentWrapper with ButtonLike
{
	// ATTRIBUTES	-----------------------------
	
	private val baseStatePointer = new PointerWithEvents(GuiElementStatus.identity)
	override val statePointer = baseStatePointer
		.mergeWith(settings.enabledPointer) { (base, enabled) => base + (Disabled -> !enabled) }
	
	override protected val wrapped = ViewImageLabel(parentHierarchy)
		.withSettings(settings.imageSettings)
		.withAdditionalCustomDrawers(settings.buttonSettings.customDrawers)
		.copy(allowsUpscaling = allowUpscaling)
		.apply(statePointer.map { state => images(state) })
	override val focusListeners = new ButtonDefaultFocusListener(baseStatePointer) +: settings.focusListeners
	
	override val focusId = hashCode()
	
	/**
	  * The overall shade of this button (calculated based on the focused-state)
	  */
	lazy val shade = ColorShade.forLuminosity(images.focusImage.pixels.averageLuminosity)
	
	
	// INITIAL CODE	-----------------------------
	
	setup(baseStatePointer, settings.hotKeys)
	
	
	// IMPLEMENTED	-----------------------------
	
	override protected def trigger() = action
	
	override def cursorToImage(cursor: Cursor, position: Point) = cursor.over(shade)
}
