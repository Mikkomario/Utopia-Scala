package utopia.reflection.component.swing.button

import utopia.paradigm.color.Color
import utopia.reflection.component.context.ButtonContextLike
import utopia.reflection.component.drawing.mutable.CustomDrawableWrapper
import utopia.reflection.component.swing.label.{ImageLabel, TextLabel}
import utopia.reflection.component.swing.template.StackableAwtComponentWrapperWrapper
import utopia.reflection.container.swing.AwtContainerRelated
import utopia.reflection.event.ButtonState
import utopia.reflection.localization.LocalizedString
import utopia.paradigm.enumeration.Alignment
import utopia.reflection.text.Font
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.shape.stack.{StackInsets, StackLength}

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
	                            hotKeyChars: Iterable[Char] = Set())(implicit context: ButtonContextLike) =
		new ImageAndTextButton(images, text, context.font, context.buttonColor, context.textInsets,
			context.borderWidth, context.textAlignment, context.textColor, hotKeys, hotKeyChars)
	
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
	               hotKeyChars: Iterable[Char] = Set())(action: => Unit)(implicit context: ButtonContextLike) =
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
		with CustomDrawableWrapper
{
	// ATTRIBUTES	------------------------
	
	private var _images = initialImages
	
	private val imageLabel = new ImageLabel(initialImages.defaultImage)
	private val textLabel = new TextLabel(initialText, font, textColor,
		insets.withLeft(StackLength.fixedZero).mapRight { _.expanding }, initialAlignment = textAlignment)
	private val content =
	{
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
	
	override protected def updateStyleForState(newState: ButtonState) =
	{
		super.updateStyleForState(newState)
		imageLabel.image = _images(newState)
	}
}
