package utopia.reflection.component.swing.label

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.genesis.color.Color
import utopia.genesis.image.Image
import utopia.genesis.shape.Axis.{X, Y}
import utopia.reflection.component.{RefreshableWithPointer, TextComponent}
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.swing.{StackableAwtComponentWrapperWrapper, SwingComponentRelated}
import utopia.reflection.container.stack.StackLayout
import utopia.reflection.container.stack.StackLayout.{Leading, Trailing}
import utopia.reflection.container.swing.Stack
import utopia.reflection.localization.DisplayFunction
import utopia.reflection.shape.Alignment.{Bottom, Top}
import utopia.reflection.shape.{Alignment, StackInsets, StackLength}
import utopia.reflection.text.Font
import utopia.reflection.util.ComponentContext

object ImageAndTextLabel
{
	/**
	  * Creates a new image and text label using a component creation context
	  * @param pointer Content pointer
	  * @param displayFunction Function for creating text display
	  * @param itemToImage Function for creating image display
	  * @param context Component creation context (implicit)
	  * @tparam A Type of displayed item
	  * @return A new label
	  */
	def contextualWithPointer[A](pointer: PointerWithEvents[A],
								 displayFunction: DisplayFunction[A] = DisplayFunction.raw)(itemToImage: A => Image)
								(implicit context: ComponentContext) =
	{
		val label = new ImageAndTextLabel[A](pointer, context.font, displayFunction, context.insets, context.insets,
			context.textAlignment, context.textColor, context.textHasMinWidth, context.allowImageUpscaling)(itemToImage)
		context.setBorderAndBackground(label)
		label
	}
	
	/**
	  * Creates a new image and text label using a component creation context
	  * @param item Initially displayed item
	  * @param displayFunction Function for creating text display
	  * @param itemToImage Function for creating image display
	  * @param context Component creation context (implicit)
	  * @tparam A Type of displayed item
	  * @return A new label
	  */
	def contextual[A](item: A, displayFunction: DisplayFunction[A] = DisplayFunction.raw)(itemToImage: A => Image)
					 (implicit context: ComponentContext) =
		contextualWithPointer(new PointerWithEvents(item), displayFunction)(itemToImage)
}

/**
  * Used for displaying items with an image + text combination
  * @author Mikko Hilpinen
  * @since 19.3.2020, v1
  * @param contentPointer Pointer used for holding the displayed item
  * @param initialFont Font used when displaying item text
  * @param displayFunction Function used for displaying an item as text (default = toString)
  * @param textInsets Insets used in text display (default = any insets)
  * @param imageInsets Insets used in image display (default = any insets)
  * @param alignment Alignment used for placing the items + text alignment (default = left)
  * @param initialTextColor Text color used initially (default = black)
  * @param textHasMinWidth Whether text should always be fully displayed (default = true)
  * @param allowImageUpscaling Whether image should be allowed to scale up (default = false)
  * @param itemToImageFunction Function used for selecting proper image for each item
  */
class ImageAndTextLabel[A](override val contentPointer: PointerWithEvents[A], initialFont: Font,
						   displayFunction: DisplayFunction[A] = DisplayFunction.raw,
						   textInsets: StackInsets = StackInsets.any, imageInsets: StackInsets = StackInsets.any,
						   alignment: Alignment = Alignment.Left,
						   initialTextColor: Color = Color.textBlack, textHasMinWidth: Boolean = true,
						   allowImageUpscaling: Boolean = false)(itemToImageFunction: A => Image)
	extends StackableAwtComponentWrapperWrapper with RefreshableWithPointer[A] with TextComponent with SwingComponentRelated
{
	// ATTRIBUTES	-------------------------
	
	private val textLabel = new ItemLabel[A](contentPointer, displayFunction, initialFont, initialTextColor,
		textInsets, alignment, textHasMinWidth)
	private val imageLabel = new ImageLabel(itemToImageFunction(contentPointer.value), allowUpscaling = allowImageUpscaling)
	
	private val view =
	{
		val wrappedImageLabel = imageLabel.framed(imageInsets)
		// Determines stack layout based on alignment
		val (direction, items) = alignment.vertical match
		{
			case Top => Y -> Vector(textLabel, wrappedImageLabel)
			case Bottom => Y -> Vector(wrappedImageLabel, textLabel)
			case _ =>
				alignment.horizontal match
				{
					case Alignment.Left => X -> Vector(wrappedImageLabel, textLabel)
					case Alignment.Right => X -> Vector(textLabel, wrappedImageLabel)
					case _ => Y -> Vector(wrappedImageLabel, textLabel)
				}
				
		}
		val layout = alignment.horizontal match
		{
			case Alignment.Left => Leading
			case Alignment.Right => Trailing
			case _ => StackLayout.Center
		}
		Stack.withItems(items, direction, StackLength.fixedZero, layout = layout)
	}
	
	
	// INITIAL CODE	-------------------------
	
	// Whenever content updates, image also updates
	addContentListener { e => imageLabel.image = itemToImageFunction(e.newValue) }
	
	
	// IMPLEMENTED	-------------------------
	
	override def component = view.component
	
	def text = textLabel.text
	
	override def drawContext = textLabel.drawContext
	
	override def drawContext_=(newContext: TextDrawContext) = textLabel.drawContext = newContext
	
	override protected def wrapped = view
	
	
	// OTHER	----------------------------
	
	/**
	  * Refreshes the text display in this label. Useful if you use display functions that rely on an external mutable
	  * state
	  */
	def refreshText() = textLabel.refreshText()
	
	/**
	  * Refreshes the image display in this label without changing content. Useful if your item to image function
	  * relies on an external mutable state
	  */
	def refreshImage() = imageLabel.image = itemToImageFunction(content)
}
