package utopia.reach.component.button.image

import utopia.firmament.context.ComponentCreationDefaults
import utopia.firmament.context.text.StaticTextContext
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.drawing.view.ButtonBackgroundViewDrawer
import utopia.firmament.image.{ButtonImageEffect, ButtonImageSet, SingleColorIcon}
import utopia.firmament.localization.{DisplayFunction, LocalizedString}
import utopia.firmament.model.HotKey
import utopia.firmament.model.enumeration.SizeCategory
import utopia.flow.util.EitherExtensions._
import utopia.flow.util.NotEmpty
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Flag
import utopia.genesis.image.Image
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.reach.component.button.{AbstractButton, ButtonSettings, ButtonSettingsLike}
import utopia.reach.component.factory.UnresolvedFramedFactory.UnresolvedStackInsets
import utopia.reach.component.factory.contextual.TextContextualFactory
import utopia.reach.component.factory.{AppliesButtonImageEffectsFactory, FromContextComponentFactoryFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.image.{ImageAndTextLabel, ImageAndTextLabelSettings, ImageAndTextLabelSettingsLike, ImageLabelSettings}
import utopia.reach.component.template.{PartOfComponentHierarchy, ReachComponentWrapper}
import utopia.reach.cursor.Cursor
import utopia.reach.focus.FocusListener

/**
  * Common trait for image and text button factories and settings
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 01.06.2023, v1.1
  */
trait ImageAndTextButtonSettingsLike[+Repr]
	extends ButtonSettingsLike[Repr] with ImageAndTextLabelSettingsLike[ImageLabelSettings, Repr]
		with AppliesButtonImageEffectsFactory[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Wrapped general button settings
	  */
	def buttonSettings: ButtonSettings
	/**
	  * Wrapped settings for label construction
	  */
	def labelSettings: ImageAndTextLabelSettings
	
	/**
	  * Wrapped general button settings
	  * @param settings New button settings to use.
	  * Wrapped general button settings
	  * @return Copy of this factory with the specified button settings
	  */
	def withButtonSettings(settings: ButtonSettings): Repr
	/**
	  * Wrapped settings for label construction
	  * @param settings New label settings to use.
	  * Wrapped settings for label construction
	  * @return Copy of this factory with the specified label settings
	  */
	def withLabelSettings(settings: ImageAndTextLabelSettings): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def customDrawers = labelSettings.customDrawers
	override def enabledFlag = buttonSettings.enabledFlag
	override def focusListeners = buttonSettings.focusListeners
	override def forceEqualBreadth = labelSettings.forceEqualBreadth
	override def hotKeys: Set[HotKey] = buttonSettings.hotKeys
	override def imageSettings = labelSettings.imageSettings
	override def isHint = labelSettings.isHint
	override def separatingMargin: Option[SizeCategory] = labelSettings.separatingMargin
	override def insets: UnresolvedStackInsets = labelSettings.insets
	
	override def withInsets(insets: UnresolvedStackInsets): Repr = mapLabelSettings { _.withInsets(insets) }
	override def withSeparatingMargin(margin: Option[SizeCategory]): Repr =
		mapLabelSettings { _.withSeparatingMargin(margin) }
	override def withCustomDrawers(drawers: Seq[CustomDrawer]) =
		withLabelSettings(labelSettings.withCustomDrawers(drawers))
	override def withEnabledFlag(p: Flag) =
		withButtonSettings(buttonSettings.withEnabledFlag(p))
	override def withFocusListeners(listeners: Seq[FocusListener]) =
		withButtonSettings(buttonSettings.withFocusListeners(listeners))
	override def withForceEqualBreadth(force: Boolean) =
		withLabelSettings(labelSettings.withForceEqualBreadth(force))
	override def withHotKeys(keys: Set[HotKey]): Repr = withButtonSettings(buttonSettings.withHotKeys(keys))
	override def withImageSettings(settings: ImageLabelSettings) =
		withLabelSettings(labelSettings.withImageSettings(settings))
	override def withIsHint(hint: Boolean) = withLabelSettings(labelSettings.withIsHint(hint))
	
	override protected def _withMargins(separatingMargin: Option[SizeCategory], insets: UnresolvedStackInsets): Repr =
		mapLabelSettings { _.copy(separatingMargin = separatingMargin, insets = insets) }
	
	
	// OTHER	--------------------
	
	def mapButtonSettings(f: ButtonSettings => ButtonSettings) = withButtonSettings(f(buttonSettings))
	def mapLabelSettings(f: ImageAndTextLabelSettings => ImageAndTextLabelSettings) =
		withLabelSettings(f(labelSettings))
}

object ImageAndTextButtonSettings
{
	// ATTRIBUTES	--------------------
	
	val default = apply()
}

/**
  * Combined settings used when constructing image and text buttons
  * @param buttonSettings Wrapped general button settings
  * @param labelSettings Wrapped settings for label construction
  * @param imageEffects Effects applied to generated image sets
  * @author Mikko Hilpinen
  * @since 01.06.2023, v1.1
  */
case class ImageAndTextButtonSettings(buttonSettings: ButtonSettings = ButtonSettings.default,
                                      labelSettings: ImageAndTextLabelSettings = ImageAndTextLabelSettings.default,
                                      imageEffects: Seq[ButtonImageEffect] = ComponentCreationDefaults.inButtonImageEffects)
	extends ImageAndTextButtonSettingsLike[ImageAndTextButtonSettings]
{
	// IMPLEMENTED	--------------------
	
	override def self: ImageAndTextButtonSettings = this
	
	override def withButtonSettings(settings: ButtonSettings) = copy(buttonSettings = settings)
	override def withImageEffects(effects: Seq[ButtonImageEffect]) = copy(imageEffects = effects)
	override def withLabelSettings(settings: ImageAndTextLabelSettings) = copy(labelSettings = settings)
}

/**
  * Common trait for factories that wrap a image and text button settings instance
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 01.06.2023, v1.1
  */
trait ImageAndTextButtonSettingsWrapper[+Repr] extends ImageAndTextButtonSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Settings wrapped by this instance
	  */
	protected def settings: ImageAndTextButtonSettings
	/**
	  * @return Copy of this factory with the specified settings
	  */
	protected def withSettings(settings: ImageAndTextButtonSettings): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def buttonSettings: ButtonSettings = settings.buttonSettings
	override def imageEffects = settings.imageEffects
	override def labelSettings = settings.labelSettings
	
	override def withButtonSettings(settings: ButtonSettings): Repr =
		mapSettings { _.withButtonSettings(settings) }
	override def withImageEffects(effects: Seq[ButtonImageEffect]) =
		mapSettings { _.withImageEffects(effects) }
	override def withLabelSettings(settings: ImageAndTextLabelSettings) =
		mapSettings { _.withLabelSettings(settings) }
	
	
	// OTHER	--------------------
	
	def mapSettings(f: ImageAndTextButtonSettings => ImageAndTextButtonSettings) = withSettings(f(settings))
}

/**
  * Factory class used for constructing image and text buttons using contextual component creation information
  * @author Mikko Hilpinen
  * @since 01.06.2023, v1.1
  */
case class ContextualImageAndTextButtonFactory(hierarchy: ComponentHierarchy, context: StaticTextContext,
                                               settings: ImageAndTextButtonSettings = ImageAndTextButtonSettings.default)
	extends ImageAndTextButtonSettingsWrapper[ContextualImageAndTextButtonFactory]
		with TextContextualFactory[ContextualImageAndTextButtonFactory] with PartOfComponentHierarchy
{
	// IMPLEMENTED  ------------------------
	
	override def self: ContextualImageAndTextButtonFactory = this
	
	override def withContext(newContext: StaticTextContext) = copy(context = newContext)
	override def withSettings(settings: ImageAndTextButtonSettings): ContextualImageAndTextButtonFactory =
		copy(settings = settings)
	
	
	// OTHER    ----------------------------
	
	/**
	  * Creates a new button with both image and text
	  * @param image                    Image displayed on this button.
	  *                                 Either Left: Image or Right: Icon
	  * @param text                     Text displayed on this button
	  * @param action                   Action called whenever this button is triggered
	  * @return A new button
	  */
	def apply(image: Either[Image, SingleColorIcon], text: LocalizedString)(action: => Unit) =
		_apply(Left(image), text)(action)
	/**
	  * Creates a new button with both image and text
	  * @param image  Image displayed on this button
	  * @param text   Text displayed on this button
	  * @param action Action called whenever this button is triggered
	  * @return A new button
	  */
	def apply(image: Image, text: LocalizedString)(action: => Unit): ImageAndTextButton =
		apply(Left(image), text)(action)
	/**
	  * Creates a new button with both image and text
	  * @param icon  Icon displayed on this button
	  * @param text   Text displayed on this button
	  * @param action Action called whenever this button is triggered
	  * @return A new button
	  */
	def apply(icon: SingleColorIcon, text: LocalizedString)(action: => Unit): ImageAndTextButton =
		apply(Right(icon), text)(action)
	/**
	  * Creates a new button with an image-set, plus static text
	  * @param imageSet Image set that determines the images to display in different button states
	  * @param text Text to display on this button
	  * @param action Action called whenever this button is triggered
	  * @return A new button
	  */
	def apply(imageSet: ButtonImageSet, text: LocalizedString)(action: => Unit) =
		_apply(Right(imageSet), text)(action)
	
	/**
	  * Creates a new button with both image and text
	  * @param icon Icon displayed on this button
	  * @param text Text displayed on this button
	  * @param action Action called whenever this button is triggered
	  * @return A new button
	  */
	@deprecated("Please use .apply(SingleColorIcon, LocalizedString)(=> Unit) instead", "v1.1")
	def withIcon(icon: SingleColorIcon, text: LocalizedString)(action: => Unit) =
		apply(icon, text)(action)
	
	/**
	  * Creates a new button with both image and text
	  * @param image                    Image displayed on this button.
	  *                                 Either
	  *                                     Left/Left: A static image,
	  *                                     Left/Right: A static icon, or
	  *                                     Right: A button image set
	  * @param text                     Text displayed on this button
	  * @param action                   Action called whenever this button is triggered
	  * @return A new button
	  */
	private def _apply(image: Either[Either[Image, SingleColorIcon], ButtonImageSet], text: LocalizedString)
	                  (action: => Unit) =
		new ImageAndTextButton(hierarchy, context, image, text, settings)(action)
}

/**
  * Used for defining image and text button creation settings outside of the component building process
  * @author Mikko Hilpinen
  * @since 01.06.2023, v1.1
  */
case class ImageAndTextButtonSetup(settings: ImageAndTextButtonSettings = ImageAndTextButtonSettings.default)
	extends ImageAndTextButtonSettingsWrapper[ImageAndTextButtonSetup]
		with FromContextComponentFactoryFactory[StaticTextContext, ContextualImageAndTextButtonFactory]
{
	// IMPLEMENTED	--------------------
	
	override def self: ImageAndTextButtonSetup = this
	
	override def withContext(hierarchy: ComponentHierarchy,
	                         context: StaticTextContext): ContextualImageAndTextButtonFactory =
		ContextualImageAndTextButtonFactory(hierarchy, context, settings)
	override def withSettings(settings: ImageAndTextButtonSettings): ImageAndTextButtonSetup =
		copy(settings = settings)
}

object ImageAndTextButton extends ImageAndTextButtonSetup()
{
	// OTHER	--------------------
	
	def apply(settings: ImageAndTextButtonSettings) = withSettings(settings)
}

/**
  * A button which displays both an image and some text
  * @author Mikko Hilpinen
  * @since 10.11.2020, v0.1
  */
class ImageAndTextButton(override val hierarchy: ComponentHierarchy, context: StaticTextContext,
                         image: Either[Either[Image, SingleColorIcon], ButtonImageSet], text: LocalizedString,
                         settings: ImageAndTextButtonSettings = ImageAndTextButtonSettings.default)
                        (action: => Unit)
	extends AbstractButton(settings) with ReachComponentWrapper
{
	// ATTRIBUTES	-----------------------------
	
	// Applies the button image effects, if applicable
	private val appliedImage = image match {
		// Case: Starts from a static icon or an image => Converts to a button image set, if effects are present
		case Left(imageOrIcon) =>
			NotEmpty(settings.imageEffects) match {
				// Case: Effects applied => Converts to an image set
				case Some(effects) =>
					// Converts the icon to an image, if needed
					val image = imageOrIcon.leftOrMap { icon =>
						settings.imageColorOverlay match {
							// Case: Color-overlay applied
							case Some(color) => icon(color)
							// Case: Black or white icon
							case None => icon.contextual(context)
						}
					}
					Right(ButtonImageSet(image) ++ effects)
				// Case: No effects applied => Keeps as a static image or icon
				case None => Left(imageOrIcon)
			}
		// Case: Button image set used => Applies effects on top of the set
		case Right(imageSet) => Right(imageSet ++ settings.imageEffects)
	}
	
	override protected val wrapped = {
		// Adds space for the borders
		val borderWidth = context.buttonBorderWidth
		val appliedLabelSettings = settings.labelSettings
			.mapInsets { _.map { _.mapBoth { _.more } { _ + borderWidth } } }
		// Prepares the component factory
		val factory = ImageAndTextLabel(appliedLabelSettings)
			.withContext(hierarchy, context)
			// Adds state-based background-drawing
			.withCustomBackgroundDrawer(
				ButtonBackgroundViewDrawer(Fixed(context.background), statePointer, Fixed(borderWidth)))
		appliedImage match {
			// Case: Image won't change => Constructs an immutable label
			case Left(staticImage) => factory(staticImage, text)
			// Case: Image is defined as a set and will changed based on the state => Constructs a view-label
			case Right(imageSet) =>
				factory.toViewFactory(Fixed(text), statePointer.map(imageSet.apply), DisplayFunction.identity)
		}
	}
	
	
	// INITIAL CODE	------------------------------
	
	setup()
	
	
	// IMPLEMENTED	------------------------------
	
	override protected def trigger() = action
	
	override def cursorToImage(cursor: Cursor, position: Point) = cursor.contextual(context)
}
