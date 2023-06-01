package utopia.reach.component.button.image

import utopia.firmament.context.TextContext
import utopia.firmament.drawing.view.ButtonBackgroundViewDrawer
import utopia.firmament.image.SingleColorIcon
import utopia.firmament.localization.LocalizedString
import utopia.firmament.model.stack.{StackInsets, StackInsetsConvertible}
import utopia.firmament.model.{GuiElementStatus, HotKey}
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.genesis.image.Image
import utopia.paradigm.shape.shape2d.Point
import utopia.reach.component.factory.FromContextComponentFactoryFactory.Ccff
import utopia.reach.component.factory.contextual.TextContextualFactory
import utopia.reach.component.factory.{FocusListenableFactory, FramedFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.image.{ImageAndTextLabel, ImageAndTextLabelSettings, ImageAndTextLabelSettingsWrapper}
import utopia.reach.component.template.{ButtonLike, ReachComponentWrapper}
import utopia.reach.cursor.Cursor
import utopia.reach.focus.FocusListener

trait ImageAndTextButtonSettingsLike[+Repr]
	extends ImageAndTextLabelSettingsWrapper[Repr] with FramedFactory[Repr] with FocusListenableFactory[Repr]
{
	// ABSTRACT -----------------------
	
	protected def hotKeys: Set[HotKey]
	
	/**
	  * @param hotKeys Hotkeys that trigger this button
	  * @return Copy of this factory with the specified hotkeys
	  */
	def withHotKeys(hotKeys: Set[HotKey]): Repr
	
	
	// OTHER    -----------------------
	
	def withAdditionalHotKeys(keys: IterableOnce[HotKey]) = withHotKeys(hotKeys ++ keys)
	def withHotKey(key: HotKey) = withHotKeys(hotKeys + key)
	def triggeredByKeyWithIndex(keyIndex: Int) = withHotKey(HotKey.keyWithIndex(keyIndex))
	def triggeredByCharKey(char: Char) = withHotKey(HotKey.character(char))
}

object ImageAndTextButtonSettings
{
	val default = apply()
}
case class ImageAndTextButtonSettings(settings: ImageAndTextLabelSettings = ImageAndTextLabelSettings.default,
                                      insets: StackInsets = StackInsets.any, hotKeys: Set[HotKey] = Set(),
                                      focusListeners: Vector[FocusListener] = Vector())
	extends ImageAndTextButtonSettingsLike[ImageAndTextButtonSettings]
{
	override def withHotKeys(hotKeys: Set[HotKey]): ImageAndTextButtonSettings = copy(hotKeys = hotKeys)
	override def withSettings(settings: ImageAndTextLabelSettings): ImageAndTextButtonSettings =
		copy(settings = settings)
	override def withFocusListeners(listeners: Vector[FocusListener]): ImageAndTextButtonSettings =
		copy(focusListeners = listeners)
	override def withInsets(insets: StackInsetsConvertible): ImageAndTextButtonSettings = copy(insets = insets.toInsets)
}

trait ImageAndTextButtonSettingsWrapper[+Repr] extends ImageAndTextButtonSettingsLike[Repr]
{
	// ABSTRACT --------------------------
	
	protected def buttonSettings: ImageAndTextButtonSettings
	protected def withSettings(settings: ImageAndTextButtonSettings): Repr
	
	
	// IMPLEMENTED  ----------------------
	
	override protected def settings: ImageAndTextLabelSettings = buttonSettings.settings
	override protected def hotKeys: Set[HotKey] = buttonSettings.hotKeys
	override protected def focusListeners: Vector[FocusListener] = buttonSettings.focusListeners
	override protected def insets: StackInsets = buttonSettings.insets
	
	override def withHotKeys(hotKeys: Set[HotKey]): Repr = mapButtonSettings { _.withHotKeys(hotKeys) }
	override protected def withSettings(settings: ImageAndTextLabelSettings): Repr =
		mapButtonSettings { _.withSettings(settings) }
	override def withFocusListeners(listeners: Vector[FocusListener]): Repr =
		mapButtonSettings { _.withFocusListeners(listeners) }
	override def withInsets(insets: StackInsetsConvertible): Repr = mapButtonSettings { _.withInsets(insets) }
	
	
	// OTHER    -------------------------
	
	def mapButtonSettings(f: ImageAndTextButtonSettings => ImageAndTextButtonSettings) =
		withSettings(f(buttonSettings))
}

case class ContextualImageAndTextButtonFactory(parentHierarchy: ComponentHierarchy, context: TextContext,
                                               buttonSettings: ImageAndTextButtonSettings = ImageAndTextButtonSettings.default)
	extends TextContextualFactory[ContextualImageAndTextButtonFactory]
		with ImageAndTextButtonSettingsWrapper[ContextualImageAndTextButtonFactory]
{
	// IMPLEMENTED  ------------------------
	
	override def self: ContextualImageAndTextButtonFactory = this
	
	override def withContext(newContext: TextContext) = copy(context = newContext)
	
	override protected def withSettings(settings: ImageAndTextButtonSettings): ContextualImageAndTextButtonFactory =
		copy(buttonSettings = settings)
	
	
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
		new ImageAndTextButton(parentHierarchy, context, image, text, settings, insets, hotKeys, focusListeners)(action)
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

case class ImageAndTextButtonSetup(buttonSettings: ImageAndTextButtonSettings)
	extends ImageAndTextButtonSettingsWrapper[ImageAndTextButtonSetup]
		with Ccff[TextContext, ContextualImageAndTextButtonFactory]
{
	override protected def withSettings(settings: ImageAndTextButtonSettings): ImageAndTextButtonSetup =
		copy(buttonSettings = settings)
	
	override def withContext(hierarchy: ComponentHierarchy, context: TextContext): ContextualImageAndTextButtonFactory =
		ContextualImageAndTextButtonFactory(hierarchy, context, buttonSettings)
}

object ImageAndTextButton extends ImageAndTextButtonSetup(ImageAndTextButtonSettings.default)
{
	def apply(settings: ImageAndTextButtonSettings) = withSettings(settings)
}

/**
  * A button which displays both an image and some text
  * @author Mikko Hilpinen
  * @since 10.11.2020, v0.1
  */
class ImageAndTextButton(parentHierarchy: ComponentHierarchy, context: TextContext, image: Either[Image, SingleColorIcon],
                         text: LocalizedString, settings: ImageAndTextLabelSettings = ImageAndTextLabelSettings.default,
                         commonInsets: StackInsets = StackInsets.zero,
                         hotKeys: Set[HotKey] = Set(), additionalFocusListeners: Seq[FocusListener] = Vector())
                        (action: => Unit)
	extends ReachComponentWrapper with ButtonLike
{
	// ATTRIBUTES	-----------------------------
	
	private val _statePointer = new PointerWithEvents(GuiElementStatus.identity)
	
	override val focusListeners = new ButtonDefaultFocusListener(_statePointer) +: additionalFocusListeners
	override val focusId = hashCode()
	
	override protected val wrapped = {
		val alignment = context.textAlignment
		val borderWidth = context.buttonBorderWidth
		val actualContext = context.mapTextInsets { _/2 + commonInsets.withoutSides(alignment.directions) + borderWidth }
		ImageAndTextLabel(settings).mapImageInsets { _/2 + commonInsets.withoutSides(alignment.directions) + borderWidth }
			.withContext(parentHierarchy, actualContext)
			.withCustomDrawers(
				ButtonBackgroundViewDrawer(Fixed(context.background), statePointer, Fixed(borderWidth)) +: settings.customDrawers)
			.apply(image, text)
	}
	
	
	// INITIAL CODE	------------------------------
	
	setup(_statePointer, hotKeys)
	
	
	// IMPLEMENTED	------------------------------
	
	override def statePointer = _statePointer.view
	
	override protected def trigger() = action
	
	override def cursorToImage(cursor: Cursor, position: Point) = cursor.contextual(context)
}
