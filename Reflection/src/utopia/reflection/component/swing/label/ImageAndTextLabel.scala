package utopia.reflection.component.swing.label

import utopia.firmament.component.display.RefreshableWithPointer
import utopia.firmament.component.text.MutableStyleTextComponent
import utopia.firmament.context.TextContext
import utopia.firmament.model.TextDrawContext
import utopia.firmament.model.enumeration.StackLayout
import utopia.firmament.model.enumeration.StackLayout.{Leading, Trailing}
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.genesis.graphics.MeasuredText
import utopia.genesis.image.Image
import utopia.genesis.text.Font
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.LinearAlignment.{Close, Far}
import utopia.reflection.component.swing.template.{StackableAwtComponentWrapperWrapper, SwingComponentRelated}
import utopia.reflection.container.swing.layout.multi.Stack
import utopia.firmament.localization.DisplayFunction
import utopia.firmament.model.stack.{StackInsets, StackLength}

object ImageAndTextLabel
{
	/**
	  * Creates a new image and text label using a component creation context
	  * @param pointer Content pointer
	  * @param displayFunction Function for creating text display
	  * @param imageInsets Insets placed around image. If None, contextual text insets will be used (default)
	  * @param itemToImage Function for creating image display
	  * @param context Component creation context (implicit)
	  * @tparam A Type of displayed item
	  * @return A new label
	  */
	def contextualWithPointer[A](pointer: PointerWithEvents[A], displayFunction: DisplayFunction[A] = DisplayFunction.raw,
								 imageInsets: Option[StackInsets] = None)
								(itemToImage: A => Image)(implicit context: TextContext) =
	{
		new ImageAndTextLabel[A](pointer, context.font, displayFunction, context.textInsets,
			imageInsets.getOrElse(context.textInsets), context.textAlignment, context.textColor, !context.allowTextShrink,
			context.allowImageUpscaling)(itemToImage)
	}
	
	/**
	  * Creates a new image and text label using a component creation context
	  * @param item Initially displayed item
	  * @param displayFunction Function for creating text display
	  * @param imageInsets Insets placed around image. If None, contextual text insets will be used (default)
	  * @param itemToImage Function for creating image display
	  * @param context Component creation context (implicit)
	  * @tparam A Type of displayed item
	  * @return A new label
	  */
	def contextual[A](item: A, displayFunction: DisplayFunction[A] = DisplayFunction.raw,
					  imageInsets: Option[StackInsets] = None)(itemToImage: A => Image)
					 (implicit context: TextContext) =
		contextualWithPointer(new PointerWithEvents(item), displayFunction, imageInsets)(itemToImage)
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
  * @param hasMinWidth Whether text should always be fully displayed (default = true)
  * @param allowImageUpscaling Whether image should be allowed to scale up (default = false)
  * @param itemToImageFunction Function used for selecting proper image for each item
  */
class ImageAndTextLabel[A](override val contentPointer: PointerWithEvents[A], initialFont: Font,
						   displayFunction: DisplayFunction[A] = DisplayFunction.raw,
						   textInsets: StackInsets = StackInsets.any, imageInsets: StackInsets = StackInsets.any,
						   alignment: Alignment = Alignment.Left,
						   initialTextColor: Color = Color.textBlack, hasMinWidth: Boolean = true,
						   allowImageUpscaling: Boolean = false)(itemToImageFunction: A => Image)
	extends StackableAwtComponentWrapperWrapper with RefreshableWithPointer[A]
		with MutableStyleTextComponent with SwingComponentRelated
{
	// ATTRIBUTES	-------------------------
	
	private val textLabel = new ItemLabel[A](contentPointer, displayFunction, initialFont, initialTextColor,
		textInsets, alignment, hasMinWidth)
	private val imageLabel = new ImageLabel(itemToImageFunction(contentPointer.value), allowUpscaling = allowImageUpscaling)
	
	private val view = {
		val wrappedImageLabel = imageLabel.framed(imageInsets)
		// Determines stack layout based on alignment
		val (direction, items) = alignment.vertical match {
			case Close => Y -> Vector(textLabel, wrappedImageLabel)
			case Far => Y -> Vector(wrappedImageLabel, textLabel)
			case _ =>
				alignment.horizontal match {
					case Close => X -> Vector(wrappedImageLabel, textLabel)
					case Far => X -> Vector(textLabel, wrappedImageLabel)
					case _ => Y -> Vector(wrappedImageLabel, textLabel)
				}
		}
		val layout = {
			if (direction == X)
				StackLayout.Center
			else {
				alignment.horizontal match {
					case Close => Leading
					case Far => Trailing
					case _ => StackLayout.Center
				}
			}
		}
		Stack.withItems(items, direction, StackLength.fixedZero, layout = layout)
	}
	
	
	// INITIAL CODE	-------------------------
	
	// Whenever content updates, image also updates
	contentPointer.addContinuousListener { e => imageLabel.image = itemToImageFunction(e.newValue) }
	
	
	// IMPLEMENTED	-------------------------
	
	override def allowTextShrink: Boolean = textLabel.allowTextShrink
	
	override def measuredText: MeasuredText = textLabel.measuredText
	
	override def component = view.component
	
	def text = textLabel.text
	
	override def textDrawContext = textLabel.textDrawContext
	
	override def textDrawContext_=(newContext: TextDrawContext) = textLabel.textDrawContext = newContext
	
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
