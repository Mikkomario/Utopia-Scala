package utopia.reflection.component.reach.label

import utopia.flow.event.Changing
import utopia.genesis.color.Color
import utopia.genesis.image.Image
import utopia.reflection.color.{ColorRole, ColorShade}
import utopia.reflection.color.ColorShade.Standard
import utopia.reflection.component.context.TextContextLike
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.reach.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reflection.component.reach.hierarchy.ComponentHierarchy
import utopia.reflection.component.reach.template.ReachComponentWrapper
import utopia.reflection.component.reach.wrapper.{ComponentCreationResult, Open}
import utopia.reflection.container.reach.Stack
import utopia.reflection.image.SingleColorIcon
import utopia.reflection.localization.DisplayFunction
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.stack.{StackInsets, StackLength}
import utopia.reflection.text.Font

object ViewImageAndTextLabel extends ContextInsertableComponentFactoryFactory[TextContextLike,
	ViewImageAndTextLabelFactory, ContextualViewImageAndTextLabelFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new ViewImageAndTextLabelFactory(hierarchy)
}

class ViewImageAndTextLabelFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[TextContextLike, ContextualViewImageAndTextLabelFactory]
{
	override def withContext[N <: TextContextLike](context: N) =
		ContextualViewImageAndTextLabelFactory(this, context)
	
	/**
	  * Creates a new label which displays both image and text
	  * @param itemPointer A pointer to this label's (text-determining) content
	  * @param imagePointer A pointer to the image displayed on this label
	  * @param font Font used when drawing text
	  * @param displayFunction Display function used when converting the item to text (default = toString)
	  * @param textColor Color used when drawing text (default = standard black)
	  * @param alignment Alignment used for the <b>text</b> (the image will be placed with the opposite alignment,
	  *                  so that the two form a close pair) (default = Left)
	  * @param imageInsets Insets placed around the image (default = any, preferring 0)
	  * @param textInsets Insets placed around the text (default = any, preferring 0)
	  * @param betweenLinesMargin Vertical margin placed between text lines (default = 0)
	  * @param additionalDrawers Additional custom drawers assigned to this component
	  * @param allowLineBreaks Whether text should be allowed to use line breaks (default = true)
	  * @param allowImageUpscaling Whether image should be allowed to scale up to its source resolution (default = true)
	  * @param allowTextShrink Whether text should be allowed to shrink to conserve space (default = false)
	  * @param useLowPriorityImageSize Whether low priority size constraints should be used for the image part
	  *                                (default = false)
	  * @param forceEqualBreadth Whether the image and text should be forced to have the same breadth, no matter the
	  *                          alignment (default = false)
	  * @tparam A Type of content in this label
	  * @return A new label
	  */
	def apply[A](itemPointer: Changing[A], imagePointer: Changing[Image], font: Font,
				 displayFunction: DisplayFunction[A] = DisplayFunction.raw, textColor: Color = Color.textBlack,
				 alignment: Alignment = Alignment.Left, imageInsets: StackInsets = StackInsets.any,
				 textInsets: StackInsets = StackInsets.any, betweenLinesMargin: Double = 0.0,
				 additionalDrawers: Vector[CustomDrawer] = Vector(), allowLineBreaks: Boolean = true,
				 allowImageUpscaling: Boolean = true, allowTextShrink: Boolean = false,
				 useLowPriorityImageSize: Boolean = false, forceEqualBreadth: Boolean = false) =
		new ViewImageAndTextLabel[A](parentHierarchy, itemPointer, imagePointer, font, displayFunction, textColor,
			alignment, imageInsets, textInsets, betweenLinesMargin, additionalDrawers, allowLineBreaks,
			allowImageUpscaling, allowTextShrink, useLowPriorityImageSize, forceEqualBreadth)
}

case class ContextualViewImageAndTextLabelFactory[+N <: TextContextLike](factory: ViewImageAndTextLabelFactory, context: N)
	extends ContextualComponentFactory[N, TextContextLike, ContextualViewImageAndTextLabelFactory]
{
	implicit def c: TextContextLike = context
	
	override def withContext[N2 <: TextContextLike](newContext: N2) =
		copy(context = newContext)
	
	/**
	  * Creates a new label which displays both image and text
	  * @param itemPointer A pointer to this label's (text-determining) content
	  * @param imagePointer A pointer to the image displayed on this label
	  * @param displayFunction Display function used when converting the item to text (default = toString)
	  * @param imageInsets Insets placed around the image (default = any, preferring 0)
	  * @param additionalDrawers Additional custom drawers assigned to this component
	  * @param useLowPriorityImageSize Whether low priority size constraints should be used for the image part
	  *                                (default = false)
	  * @param forceEqualBreadth Whether the image and text should be forced to have the same breadth, no matter the
	  *                          alignment (default = false)
	  * @tparam A Type of content in this label
	  * @return A new label
	  */
	def apply[A](itemPointer: Changing[A], imagePointer: Changing[Image],
				 displayFunction: DisplayFunction[A] = DisplayFunction.raw, imageInsets: StackInsets = StackInsets.any,
				 additionalDrawers: Vector[CustomDrawer], useLowPriorityImageSize: Boolean = false,
				 forceEqualBreadth: Boolean = false) =
		factory[A](itemPointer, imagePointer, context.font, displayFunction, context.textColor, context.textAlignment,
			imageInsets, context.textInsets, context.betweenLinesMargin.optimal, additionalDrawers,
			context.allowLineBreaks, context.allowImageUpscaling, context.allowTextShrink, useLowPriorityImageSize,
			forceEqualBreadth)
	
	/**
	  * Creates a new label that contains both an image and text
	  * @param itemPointer A pointer to this label's (text-determining) content
	  * @param iconPointer A pointer to the displayed icon
	  * @param displayFunction A function for converting the displayed item to text (default = use toString)
	  * @param imageInsets Insets placed around the image (default = any, preferring 0)
	  * @param customDrawers Custom drawers assigned to this component (default = empty)
	  * @param useLowPriorityImageSize Whether image size constraints should be low priority (default = false)
	  * @param forceEqualBreadth Whether the image and the text should be forced to have equal width or height
	  *                          (depending on alignment) (default = false)
	  * @return A new label
	  */
	def withIcon[A](itemPointer: Changing[A], iconPointer: Changing[SingleColorIcon],
					displayFunction: DisplayFunction[A] = DisplayFunction.raw,
					imageInsets: StackInsets = StackInsets.any, customDrawers: Vector[CustomDrawer] = Vector(),
					useLowPriorityImageSize: Boolean = false, forceEqualBreadth: Boolean = false) =
		apply(itemPointer, iconPointer.map { _.singleColorImage }, displayFunction, imageInsets, customDrawers,
			useLowPriorityImageSize, forceEqualBreadth)
	
	/**
	  * Creates a new label that contains both an image and text
	  * @param itemPointer A pointer to this label's (text-determining) content
	  * @param iconPointer A pointer to the displayed icon
	  * @param rolePointer A pointer to the role the icon serves (determines icon color)
	  * @param preferredShade Preferred color shade to use (default = standard)
	  * @param displayFunction A function for converting the displayed item to text (default = use toString)
	  * @param imageInsets Insets placed around the image (default = any, preferring 0)
	  * @param customDrawers Custom drawers assigned to this component (default = empty)
	  * @param useLowPriorityImageSize Whether image size constraints should be low priority (default = false)
	  * @param forceEqualBreadth Whether the image and the text should be forced to have equal width or height
	  *                          (depending on alignment) (default = false)
	  * @return A new label
	  */
	def withColouredIcon[A](itemPointer: Changing[A], iconPointer: Changing[SingleColorIcon],
							rolePointer: Changing[ColorRole], preferredShade: ColorShade = Standard,
							displayFunction: DisplayFunction[A] = DisplayFunction.raw,
							imageInsets: StackInsets = StackInsets.any, customDrawers: Vector[CustomDrawer] = Vector(),
							useLowPriorityImageSize: Boolean = false, forceEqualBreadth: Boolean = false) =
		apply(itemPointer, iconPointer.mergeWith(rolePointer) { (icon, role) =>
			icon.asImageWithColor(context.color(role, preferredShade)) }, displayFunction, imageInsets, customDrawers,
			useLowPriorityImageSize, forceEqualBreadth)
}

/**
  * A pointer-based label that displays an image and a piece of text
  * @author Mikko Hilpinen
  * @since 9.11.2020, v2
  */
class ViewImageAndTextLabel[A](parentHierarchy: ComponentHierarchy, val itemPointer: Changing[A],
							   val imagePointer: Changing[Image], font: Font,
							   displayFunction: DisplayFunction[A] = DisplayFunction.raw,
							   textColor: Color = Color.textBlack, alignment: Alignment = Alignment.Left,
							   imageInsets: StackInsets = StackInsets.any, textInsets: StackInsets = StackInsets.any,
							   betweenLinesMargin: Double = 0.0, additionalDrawers: Vector[CustomDrawer] = Vector(),
							   allowLineBreaks: Boolean = true, allowImageUpscaling: Boolean = true,
							   allowTextShrink: Boolean = false, useLowPriorityImageSize: Boolean = false,
							   forceEqualBreadth: Boolean = false) extends ReachComponentWrapper
{
	// ATTRIBUTES	-------------------------------
	
	override protected val wrapped =
	{
		// Creates stack content (image and text label)
		val openItems = Open { hierarchy =>
			val imageLabel = ViewImageLabel(hierarchy).apply(imagePointer, Changing.wrap(imageInsets),
				Changing.wrap(alignment.opposite), allowUpscaling = allowImageUpscaling,
				useLowPrioritySize = useLowPriorityImageSize)
			val textLabel = ViewTextLabel(hierarchy).withStaticStyle(itemPointer, font, displayFunction, textColor,
				alignment, textInsets, betweenLinesMargin, allowLineBreaks = allowLineBreaks,
				allowTextShrink = allowTextShrink)
			ComponentCreationResult(imageLabel -> textLabel)
		}(parentHierarchy.top)
		// Wraps the components in a stack
		Stack(parentHierarchy).forPair(openItems, alignment, StackLength.fixedZero,
			customDrawers = additionalDrawers, forceFitLayout = forceEqualBreadth).parent
	}
}
