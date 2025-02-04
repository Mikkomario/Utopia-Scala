package utopia.reach.component.button.image

import utopia.firmament.context.ComponentCreationDefaults
import utopia.firmament.context.color.StaticColorContext
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.image.{ButtonImageEffect, ButtonImageSet, SingleColorIcon}
import utopia.firmament.model.HotKey
import utopia.firmament.model.stack.StackInsetsConvertible
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.image.Image
import utopia.paradigm.color.ColorLevel.Standard
import utopia.paradigm.color.{Color, ColorLevel, ColorRole}
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.shape.shape2d.Matrix2D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.reach.component.button.{AbstractButton, ButtonSettings, ButtonSettingsLike}
import utopia.reach.component.factory.contextual.{ColorContextualFactory, ContextualBackgroundAssignableFactory}
import utopia.reach.component.factory.{AppliesButtonImageEffectsFactory, ComponentFactoryFactory, FromContextComponentFactoryFactory, FromContextFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.image.{ImageLabelSettings, ImageLabelSettingsLike, ViewImageLabel}
import utopia.reach.component.template.{PartOfComponentHierarchy, ReachComponentWrapper}
import utopia.reach.cursor.Cursor
import utopia.reach.focus.FocusListener

/**
  * Common trait for image button factories and settings
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 31.05.2023, v1.1
  */
trait ImageButtonSettingsLike[+Repr]
	extends ButtonSettingsLike[Repr] with ImageLabelSettingsLike[Repr] with AppliesButtonImageEffectsFactory[Repr]
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
	
	
	// IMPLEMENTED	--------------------
	
	override def identity: Repr = self
	
	override def alignment = imageSettings.alignment
	override def colorOverlay = imageSettings.colorOverlay
	override def customDrawers = imageSettings.customDrawers
	override def enabledPointer = buttonSettings.enabledPointer
	override def focusListeners = buttonSettings.focusListeners
	override def hotKeys = buttonSettings.hotKeys
	override def imageScaling = imageSettings.imageScaling
	override def insets = imageSettings.insets
	override def usesLowPrioritySize = imageSettings.usesLowPrioritySize
	override def transformation: Option[Matrix2D] = imageSettings.transformation
	
	override def apply(alignment: Alignment): Repr = mapImageSettings { _.withAlignment(alignment) }
	override def withAlignment(alignment: Alignment) = withImageSettings(imageSettings.withAlignment(alignment))
	override def withColor(color: Option[Color]) = withImageSettings(imageSettings.withColor(color))
	override def withCustomDrawers(drawers: Seq[CustomDrawer]): Repr =
		withImageSettings(imageSettings.withCustomDrawers(drawers))
	override def withEnabledPointer(p: Changing[Boolean]) =
		withButtonSettings(buttonSettings.withEnabledPointer(p))
	override def withFocusListeners(listeners: Seq[FocusListener]) =
		withButtonSettings(buttonSettings.withFocusListeners(listeners))
	override def withHotKeys(keys: Set[HotKey]) = withButtonSettings(buttonSettings.withHotKeys(keys))
	override def withImageScaling(scaling: Double) = withImageSettings(imageSettings.withImageScaling(scaling))
	override def withInsets(insets: StackInsetsConvertible) = withImageSettings(imageSettings.withInsets(insets))
	override def withUseLowPrioritySize(lowPriority: Boolean) =
		withImageSettings(imageSettings.withUseLowPrioritySize(lowPriority))
	override def withTransformation(transformation: Option[Matrix2D]): Repr =
		mapImageSettings { _.withTransformation(transformation) }
	
	
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
  * @param imageEffects Effects applied to generated image sets
  * @author Mikko Hilpinen
  * @since 31.05.2023, v1.1
  */
case class ImageButtonSettings(buttonSettings: ButtonSettings = ButtonSettings.default,
                               imageSettings: ImageLabelSettings = ImageLabelSettings.default,
                               imageEffects: Seq[ButtonImageEffect] = ComponentCreationDefaults.asButtonImageEffects)
	extends ImageButtonSettingsLike[ImageButtonSettings]
{
	// IMPLEMENTED	--------------------
	
	override def self: ImageButtonSettings = this
	
	override def withButtonSettings(settings: ButtonSettings) = copy(buttonSettings = settings)
	override def withImageEffects(effects: Seq[ButtonImageEffect]) = copy(imageEffects = effects)
	override def withImageSettings(settings: ImageLabelSettings) = copy(imageSettings = settings)
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
	override def imageEffects = settings.imageEffects
	override def imageSettings = settings.imageSettings
	
	override def withButtonSettings(settings: ButtonSettings) = mapSettings { _.withButtonSettings(settings) }
	override def withCustomDrawers(drawers: Seq[CustomDrawer]): Repr = mapSettings { _.withCustomDrawers(drawers) }
	override def withImageEffects(effects: Seq[ButtonImageEffect]) = mapSettings { _.withImageEffects(effects) }
	override def withImageSettings(settings: ImageLabelSettings) = mapSettings { _.withImageSettings(settings) }
	
	
	// OTHER	--------------------
	
	def mapSettings(f: ImageButtonSettings => ImageButtonSettings) = withSettings(f(settings))
}

/**
  * Common trait for factories that are used for constructing image buttons
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 31.05.2023, v1.1
  */
trait ImageButtonFactoryLike[+Repr] extends ImageButtonSettingsWrapper[Repr] with PartOfComponentHierarchy
{
	// ABSTRACT	--------------------
	
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
		new ImageButton(hierarchy, images ++ settings.imageEffects, settings, allowsUpscaling)(action)
	/**
	  * Creates a new image button
	  * @param image The image to display on this button
	  * @param action Action that will be triggered when this button is pressed
	  * @return A new image button
	  */
	def apply[U](image: Image)(action: => U): ImageButton = apply(ButtonImageSet(image))(action)
}

/**
  * Factory class used for constructing image buttons using contextual component creation information
  * @author Mikko Hilpinen
  * @since 31.05.2023, v1.1
  */
case class ContextualImageButtonFactory(hierarchy: ComponentHierarchy, context: StaticColorContext,
                                        settings: ImageButtonSettings = ImageButtonSettings.default)
	extends ImageButtonFactoryLike[ContextualImageButtonFactory]
		with ColorContextualFactory[ContextualImageButtonFactory]
		with ContextualBackgroundAssignableFactory[StaticColorContext, ContextualImageButtonFactory]
{
	// IMPLICIT	-----------------------------
	
	private implicit def c: StaticColorContext = context
	
	
	// IMPLEMENTED	-------------------------
	
	override def self: ContextualImageButtonFactory = this
	override protected def allowsUpscaling: Boolean = context.allowImageUpscaling
	
	override def withContext(newContext: StaticColorContext) = copy(context = newContext)
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
		val image = color match {
			case Some(color) => icon(color)
			case None => icon.contextual
		}
		apply(image)(action)
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
case class ImageButtonFactory(hierarchy: ComponentHierarchy,
                              settings: ImageButtonSettings = ImageButtonSettings.default,
                              allowsUpscaling: Boolean = false)
	extends ImageButtonFactoryLike[ImageButtonFactory]
		with FromContextFactory[StaticColorContext, ContextualImageButtonFactory]
{
	// COMPUTED -------------------------------
	
	/**
	  * @return Copy of this factory that allows image scaling beyond their source resolutions
	  */
	def allowingUpscaling = copy(allowsUpscaling = true)
	
	
	// IMPLEMENTED	---------------------------
	
	override def self: ImageButtonFactory = this
	
	override def withContext(context: StaticColorContext) =
		ContextualImageButtonFactory(hierarchy, context, settings)
	
	override def withSettings(settings: ImageButtonSettings) = copy(settings = settings)
}

/**
  * Used for defining image button creation settings outside of the component building process
  * @author Mikko Hilpinen
  * @since 31.05.2023, v1.1
  */
case class ImageButtonSetup(settings: ImageButtonSettings = ImageButtonSettings.default)
	extends ImageButtonSettingsWrapper[ImageButtonSetup] with ComponentFactoryFactory[ImageButtonFactory]
		with FromContextComponentFactoryFactory[StaticColorContext, ContextualImageButtonFactory]
{
	// IMPLEMENTED	--------------------
	
	override def self: ImageButtonSetup = this
	
	override def apply(hierarchy: ComponentHierarchy) = ImageButtonFactory(hierarchy, settings)
	
	override def withContext(hierarchy: ComponentHierarchy, context: StaticColorContext) =
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
	extends AbstractButton(settings) with ReachComponentWrapper
{
	// ATTRIBUTES	-----------------------------
	
	override protected val wrapped = ViewImageLabel(parentHierarchy)
		.withSettings(settings.imageSettings)
		.withAllowUpscaling(allowUpscaling)
		.apply(statePointer.strongMap { state => images(state) })
	
	
	// INITIAL CODE	-----------------------------
	
	setup()
	
	
	// COMPUTED ---------------------------------
	
	/**
	  * The overall shade of this button (calculated based on the focused-state)
	  */
	def shade = images.focus.shade
	
	
	// IMPLEMENTED	-----------------------------
	
	override protected def trigger() = action
	
	override def cursorToImage(cursor: Cursor, position: Point) = cursor.over(shade)
}
