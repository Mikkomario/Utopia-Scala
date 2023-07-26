package utopia.reach.component.button.image

import utopia.firmament.context.TextContext
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.drawing.view.ButtonBackgroundViewDrawer
import utopia.firmament.image.SingleColorIcon
import utopia.firmament.localization.LocalizedString
import utopia.firmament.model.enumeration.GuiElementState.Disabled
import utopia.firmament.model.stack.{StackInsets, StackInsetsConvertible}
import utopia.firmament.model.{GuiElementStatus, HotKey}
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.image.Image
import utopia.paradigm.shape.shape2d.Point
import utopia.reach.component.button.{ButtonSettings, ButtonSettingsLike}
import utopia.reach.component.factory.contextual.TextContextualFactory
import utopia.reach.component.factory.{FramedFactory, FromContextComponentFactoryFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.image.{ImageAndTextLabel, ImageAndTextLabelSettings, ImageAndTextLabelSettingsLike, ImageLabelSettings}
import utopia.reach.component.template.{ButtonLike, ReachComponentWrapper}
import utopia.reach.cursor.Cursor
import utopia.reach.focus.FocusListener

/**
  * Common trait for image and text button factories and settings
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 01.06.2023, v1.1
  */
trait ImageAndTextButtonSettingsLike[+Repr]
	extends FramedFactory[Repr] with ButtonSettingsLike[Repr] with ImageAndTextLabelSettingsLike[Repr]
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
	  *                 Wrapped general button settings
	  * @return Copy of this factory with the specified button settings
	  */
	def withButtonSettings(settings: ButtonSettings): Repr
	/**
	  * Wrapped settings for label construction
	  * @param settings New label settings to use.
	  *                 Wrapped settings for label construction
	  * @return Copy of this factory with the specified label settings
	  */
	def withLabelSettings(settings: ImageAndTextLabelSettings): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def customDrawers = labelSettings.customDrawers
	override def enabledPointer = buttonSettings.enabledPointer
	override def focusListeners = buttonSettings.focusListeners
	override def forceEqualBreadth = labelSettings.forceEqualBreadth
	override def imageSettings = labelSettings.imageSettings
	override def isHint = labelSettings.isHint
	override def hotKeys: Set[HotKey] = buttonSettings.hotKeys
	
	override def withCustomDrawers(drawers: Vector[CustomDrawer]) =
		withLabelSettings(labelSettings.withCustomDrawers(drawers))
	override def withEnabledPointer(p: Changing[Boolean]) =
		withButtonSettings(buttonSettings.withEnabledPointer(p))
	override def withFocusListeners(listeners: Vector[FocusListener]) =
		withButtonSettings(buttonSettings.withFocusListeners(listeners))
	override def withForceEqualBreadth(force: Boolean) =
		withLabelSettings(labelSettings.withForceEqualBreadth(force))
	override def withHotKeys(keys: Set[HotKey]): Repr = withButtonSettings(buttonSettings.withHotKeys(keys))
	override def withImageSettings(settings: ImageLabelSettings) =
		withLabelSettings(labelSettings.withImageSettings(settings))
	override def withIsHint(hint: Boolean) = withLabelSettings(labelSettings.withIsHint(hint))
	
	
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
  * @param insets         Insets to place around created components
  * @param buttonSettings Wrapped general button settings
  * @param labelSettings  Wrapped settings for label construction
  * @author Mikko Hilpinen
  * @since 01.06.2023, v1.1
  */
case class ImageAndTextButtonSettings(insets: StackInsets = StackInsets.any,
                                      buttonSettings: ButtonSettings = ButtonSettings.default,
                                      labelSettings: ImageAndTextLabelSettings = ImageAndTextLabelSettings.default)
	extends ImageAndTextButtonSettingsLike[ImageAndTextButtonSettings]
{
	// IMPLEMENTED	--------------------
	
	override def withButtonSettings(settings: ButtonSettings) = copy(buttonSettings = settings)
	override def withInsets(insets: StackInsetsConvertible) = copy(insets = insets.toInsets)
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
	def settings: ImageAndTextButtonSettings
	
	/**
	  * @return Copy of this factory with the specified settings
	  */
	protected def withSettings(settings: ImageAndTextButtonSettings): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def labelSettings = settings.labelSettings
	override def buttonSettings: ButtonSettings = settings.buttonSettings
	override def insets: StackInsets = settings.insets
	
	override def withInsets(insets: StackInsetsConvertible) = mapSettings { _.withInsets(insets) }
	override def withButtonSettings(settings: ButtonSettings): Repr = mapSettings { _.withButtonSettings(settings) }
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
case class ContextualImageAndTextButtonFactory(parentHierarchy: ComponentHierarchy, context: TextContext,
                                               settings: ImageAndTextButtonSettings = ImageAndTextButtonSettings.default)
	extends ImageAndTextButtonSettingsWrapper[ContextualImageAndTextButtonFactory]
		with TextContextualFactory[ContextualImageAndTextButtonFactory]
{
	// IMPLEMENTED  ------------------------
	
	override def self: ContextualImageAndTextButtonFactory = this
	
	override def withContext(newContext: TextContext) = copy(context = newContext)
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
		new ImageAndTextButton(parentHierarchy, context, image, text, settings)(action)
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
	  * Creates a new button with both image and text
	  * @param icon Icon displayed on this button
	  * @param text Text displayed on this button
	  * @param action Action called whenever this button is triggered
	  * @return A new button
	  */
	@deprecated("Please use .apply(SingleColorIcon, LocalizedString)(=> Unit) instead", "v1.1")
	def withIcon(icon: SingleColorIcon, text: LocalizedString)(action: => Unit) =
		apply(icon, text)(action)
}

/**
  * Used for defining image and text button creation settings outside of the component building process
  * @author Mikko Hilpinen
  * @since 01.06.2023, v1.1
  */
case class ImageAndTextButtonSetup(settings: ImageAndTextButtonSettings = ImageAndTextButtonSettings.default)
	extends ImageAndTextButtonSettingsWrapper[ImageAndTextButtonSetup]
		with FromContextComponentFactoryFactory[TextContext, ContextualImageAndTextButtonFactory]
{
	// IMPLEMENTED	--------------------
	
	override def withContext(hierarchy: ComponentHierarchy,
	                         context: TextContext): ContextualImageAndTextButtonFactory =
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
class ImageAndTextButton(parentHierarchy: ComponentHierarchy, context: TextContext, image: Either[Image, SingleColorIcon],
                         text: LocalizedString,
                         settings: ImageAndTextButtonSettings = ImageAndTextButtonSettings.default)
                        (action: => Unit)
	extends ReachComponentWrapper with ButtonLike
{
	// ATTRIBUTES	-----------------------------
	
	private val baseStatePointer = new EventfulPointer(GuiElementStatus.identity)
	override val statePointer = baseStatePointer
		.mergeWith(settings.enabledPointer) { (state, enabled) => state + (Disabled -> !enabled) }
	
	override val focusListeners = new ButtonDefaultFocusListener(baseStatePointer) +: settings.focusListeners
	override val focusId = hashCode()
	
	override protected val wrapped = {
		val alignment = context.textAlignment
		val borderWidth = context.buttonBorderWidth
		val actualContext = context.mapTextInsets { original =>
			val smallDirections = context.textAlignment.directions
			original.mapWithDirection { (side, len) =>
				if (smallDirections.contains(side))
					len/2
				else
					len + borderWidth + settings.insets(side)
			}
		}
		// val actualContext = context.mapTextInsets { _/2 + settings.insets.withoutSides(alignment.directions) + borderWidth }
		ImageAndTextLabel(settings.labelSettings)
			.withContext(parentHierarchy, actualContext)
			.mapImageInsets { original =>
				val smallDirections = context.textAlignment.opposite.directions
				original.mapWithDirection { (side, len) =>
					if (smallDirections.contains(side))
						len/2
					else
						len + borderWidth + settings.insets(side)
				}
			}
			.withCustomBackgroundDrawer(
				ButtonBackgroundViewDrawer(Fixed(context.background), statePointer, Fixed(borderWidth)))
			.apply(image, text)
	}
	
	
	// INITIAL CODE	------------------------------
	
	setup(baseStatePointer, settings.hotKeys)
	
	
	// IMPLEMENTED	------------------------------
	
	override protected def trigger() = action
	
	override def cursorToImage(cursor: Cursor, position: Point) = cursor.contextual(context)
}
