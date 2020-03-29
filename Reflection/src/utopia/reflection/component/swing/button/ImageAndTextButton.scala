package utopia.reflection.component.swing.button

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.genesis.color.Color
import utopia.reflection.component.drawing.mutable.BorderDrawer
import utopia.reflection.component.swing.StackableAwtComponentWrapperWrapper
import utopia.reflection.component.swing.label.{ImageLabel, TextLabel}
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.{Alignment, Border, StackInsets, StackLength}
import utopia.reflection.text.Font
import utopia.reflection.util.ComponentContext
import utopia.reflection.shape.LengthExtensions._

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
	  * @param action Action that is performed when this button is triggered
	  * @return A new button
	  */
	def apply(images: ButtonImageSet, text: LocalizedString, font: Font, color: Color, insets: StackInsets,
			  borderWidth: Double, textAlignment: Alignment = Alignment.Left, textColor: Color = Color.textBlack)(
		action: () => Unit) =
	{
		val button = new ImageAndTextButton(images, text, font, color, insets, borderWidth, textAlignment, textColor)
		button.registerAction(action)
		button
	}
	
	/**
	  * Creates a new button using contextual information
	  * @param images Images used in this button
	  * @param text Text displayed in this button
	  * @param action Action performed when this button is pressed
	  * @param context Component creation context
	  * @return A new button
	  */
	def contextual(images: ButtonImageSet, text: LocalizedString)(action: () => Unit)(implicit context: ComponentContext) =
		apply(images, text, context.font, context.buttonBackground, context.insets, context.borderWidth,
			context.textAlignment, context.textColor)(action)
	
	/**
	 * Creates a new button using contextual information. An action for the button needs to be registered separately.
	 * @param images Images used in this button
	 * @param text Text displayed in this button
	 * @param context Component creation context
	 * @return A new button
	 */
	def contextualWithoutAction(images: ButtonImageSet, text: LocalizedString)(implicit context: ComponentContext) =
		new ImageAndTextButton(images, text, context.font, context.buttonBackground, context.insets,
			context.borderWidth, context.textAlignment, context.textColor)
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
  */
class ImageAndTextButton(initialImages: ButtonImageSet, initialText: LocalizedString, font: Font, val color: Color,
						 insets: StackInsets, borderWidth: Double,
						 textAlignment: Alignment = Alignment.Left, textColor: Color = Color.textBlack)
	extends StackableAwtComponentWrapperWrapper with ButtonLike
{
	// ATTRIBUTES	------------------------
	
	private var _images = initialImages
	
	private val imageLabel = new ImageLabel(initialImages.defaultImage)
	private val textLabel = new TextLabel(initialText, font, textColor,
		insets.withLeft(StackLength.fixedZero).mapRight { _.expanding }, initialAlignment = textAlignment)
	private val content =
	{
		val inside = imageLabel.framed(insets).rowWith(Vector(textLabel), margin = StackLength.fixedZero)
		if (borderWidth > 0)
			inside.framed(StackInsets.symmetric(borderWidth.fixed))
		else
			inside
	}
	
	private val borderPointer = new PointerWithEvents(makeBorder(color))
	
	
	// INITIAL CODE	------------------------
	
	content.background = color
	setHandCursor()
	content.component.setFocusable(true)
	initializeListeners()
	
	// Adds border drawing
	if (borderWidth > 0)
	{
		content.addCustomDrawer(new BorderDrawer(borderPointer))
		borderPointer.addListener { _ => content.repaint() }
	}
	
	
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
	
	override protected def wrapped = content
	
	override protected def updateStyleForState(newState: ButtonState) =
	{
		val newColor = newState.modify(color)
		background = newColor
		imageLabel.image = _images(newState)
		borderPointer.value = makeBorder(newColor)
	}
	
	
	// OTHER	----------------------------
	
	private def makeBorder(baseColor: Color) = Border.raised(borderWidth, baseColor, 0.5)
}
