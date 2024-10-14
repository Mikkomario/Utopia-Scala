package utopia.reflection.component.swing.button

import utopia.firmament.context.text.StaticTextContext
import utopia.firmament.drawing.mutable.MutableCustomDrawableWrapper
import utopia.firmament.image.ButtonImageSet
import utopia.firmament.localization.LocalizedString
import utopia.firmament.model.GuiElementStatus
import utopia.firmament.model.stack.LengthExtensions._
import utopia.firmament.model.stack.{StackInsets, StackLength}
import utopia.genesis.text.Font
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Alignment
import utopia.reflection.component.swing.label.{ImageLabel, TextLabel}
import utopia.reflection.component.swing.template.StackableAwtComponentWrapperWrapper
import utopia.reflection.container.swing.AwtContainerRelated

object ImageAndTextButton
{
	/**
	  * Creates a new button
	  * @param images Images displayed in this button
	  * @param text Text displayed in this button
	  * @param font Font used in this button's text
	  * @param color This button's background color
	  * @param insets Insets around this button's contents
	  * @param borderWidth Width of border around this button's contents (in pixels)
	  * @param textAlignment Alignment used with this button's text (default = left)
	 *  @param textColor Color used for the text (default = black)
	  * @param hotKeys Hotkey indices that can be used for triggering this button (default = empty)
	  * @param hotKeyChars Characters on keyboard that can be used for triggering this button (default = empty)
	  * @param action Action that is performed when this button is triggered (call by name)
	  * @return A new button
	  */
	def apply(images: ButtonImageSet, text: LocalizedString, font: Font, color: Color, insets: StackInsets,
			  borderWidth: Double, textAlignment: Alignment = Alignment.Left, textColor: Color = Color.textBlack,
			  hotKeys: Set[Int] = Set(), hotKeyChars: Iterable[Char] = Set())(action: => Unit) =
	{
		val button = new ImageAndTextButton(images, text, font, color, insets, borderWidth, textAlignment, textColor,
			hotKeys, hotKeyChars)
		button.registerAction { () => action }
		button
	}
	
	/**
	  * Creates a new button using contextual information. An action for the button needs to be registered separately.
	  * @param images Images used in this button
	  * @param text Text displayed in this button
	  * @param hotKeys Hotkey indices that can be used for triggering this button (default = empty)
	  * @param hotKeyChars Characters on keyboard that can be used for triggering this button (default = empty)
	  * @param context Component creation context
	  * @return A new button
	  */
	def contextualWithoutAction(images: ButtonImageSet, text: LocalizedString, hotKeys: Set[Int] = Set(),
	                            hotKeyChars: Iterable[Char] = Set())
	                           (implicit context: StaticTextContext) =
		new ImageAndTextButton(images, text, context.font, context.background, context.textInsets,
			context.buttonBorderWidth, context.textAlignment, context.textColor, hotKeys, hotKeyChars)
	
	/**
	  * Creates a new button using contextual information
	  * @param images Images used in this button
	  * @param text Text displayed in this button
	  * @param hotKeys Hotkey indices that can be used for triggering this button (default = empty)
	  * @param hotKeyChars Characters on keyboard that can be used for triggering this button (default = empty)
	  * @param action Action performed when this button is pressed
	  * @param context Component creation context
	  * @return A new button
	  */
	def contextual(images: ButtonImageSet, text: LocalizedString, hotKeys: Set[Int] = Set(),
	               hotKeyChars: Iterable[Char] = Set())(action: => Unit)
	              (implicit context: StaticTextContext) =
	{
		val button = contextualWithoutAction(images, text, hotKeys, hotKeyChars)
		button.registerAction { () => action }
		button
	}
}

/**
  * This button implementation displays both an image and some text
  * @author Mikko Hilpinen
  * @since 1.8.2019, v1+
  * @param initialImages Images displayed in this button
  * @param initialText Text displayed in this button
  * @param font Font used in this button's text
  * @param color This button's background color
  * @param insets Insets around this button's contents
  * @param borderWidth Width of border around this button's contents (in pixels)
  * @param textAlignment Alignment used with this button's text
 *  @param textColor Color for this button's text (default = black)
  * @param hotKeys Hotkey indices that can be used for triggering this button (default = empty)
  * @param hotKeyChars Characters on keyboard that can be used for triggering this button (default = empty)
  */
class ImageAndTextButton(initialImages: ButtonImageSet, initialText: LocalizedString, font: Font, val color: Color,
						 insets: StackInsets, borderWidth: Double,
						 textAlignment: Alignment = Alignment.Left, textColor: Color = Color.textBlack,
						 hotKeys: Set[Int] = Set(), hotKeyChars: Iterable[Char] = Set())
	extends ButtonWithBackground(color, borderWidth) with StackableAwtComponentWrapperWrapper with AwtContainerRelated
		with MutableCustomDrawableWrapper
{
	// ATTRIBUTES	------------------------
	
	private var _images = initialImages
	
	private val imageLabel = new ImageLabel(initialImages.default)
	private val textLabel = new TextLabel(initialText, font, textColor,
		insets.withLeft(StackLength.fixedZero).mapRight { _.expanding }, initialAlignment = textAlignment)
	private val content = {
		val inside = imageLabel.framed(insets.mapRight { _ / 2 }).rowWith(Vector(textLabel), margin = StackLength.fixedZero)
		if (borderWidth > 0)
			inside.framed(StackInsets.symmetric(borderWidth.fixed))
		else
			inside
	}
	
	
	// INITIAL CODE	------------------------
	
	setup(hotKeys, hotKeyChars)
	
	
	// COMPUTED	----------------------------
	
	/**
	 * @return The currently used button image set
	 */
	def images = _images
	def images_=(newImages: ButtonImageSet) =
	{
		_images = newImages
		imageLabel.image = _images(state)
	}
	
	/**
	 * @return The text currently displayed on this button
	 */
	def text = textLabel.text
	def text_=(newText: LocalizedString) = textLabel.text = newText
	
	
	// IMPLEMENTED	------------------------
	
	override def drawable = content
	
	override def component = content.component
	
	override protected def wrapped = content
	
	override protected def updateStyleForState(newState: GuiElementStatus) = {
		super.updateStyleForState(newState)
		imageLabel.image = _images(newState)
	}
}
