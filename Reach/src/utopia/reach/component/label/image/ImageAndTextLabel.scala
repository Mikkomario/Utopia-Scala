package utopia.reach.component.label.image

import utopia.flow.datastructure.immutable.Pair
import utopia.paradigm.color.Color
import utopia.genesis.image.Image
import utopia.reach.component.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.text.TextLabel
import utopia.reach.component.template.ReachComponentWrapper
import utopia.reach.component.wrapper.{ComponentCreationResult, Open}
import utopia.reach.container.multi.stack.Stack
import utopia.reflection.color.ColorShade.Standard
import utopia.reflection.color.{ColorRole, ColorShade}
import utopia.reflection.component.context.TextContextLike
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.image.SingleColorIcon
import utopia.reflection.localization.LocalizedString
import utopia.paradigm.enumeration.Alignment
import utopia.reflection.shape.stack.{StackInsets, StackLength}
import utopia.reflection.text.Font

object ImageAndTextLabel extends ContextInsertableComponentFactoryFactory[TextContextLike, ImageAndTextLabelFactory,
	ContextualImageAndTextLabelFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new ImageAndTextLabelFactory(hierarchy)
}

class ImageAndTextLabelFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[TextContextLike, ContextualImageAndTextLabelFactory]
{
	override def withContext[N <: TextContextLike](context: N) =
		ContextualImageAndTextLabelFactory(this, context)
	
	/**
	  * Creates a new label that contains both an image and text
	  * @param image Image displayed on this label
	  * @param text Text displayed on this label
	  * @param font Font used when drawing text
	  * @param textColor Color used when drawing text (default = standard black)
	  * @param alignment Alignment used for placing the image (default = left)
	  * @param imageInsets Insets placed around the image (default = any, preferring 0)
	  * @param textInsets Insets placed around the text (default = any, preferring 0)
	  * @param betweenLinesMargin Margin placed between text lines, if there are many (default = 0)
	  * @param customDrawers Custom drawers assigned to this component (default = empty)
	  * @param allowLineBreaks Whether line breaks in text should be applied (default = true)
	  * @param allowImageUpscaling Whether image should be allowed to scale up to its original resolution
	  *                            (default = true)
	  * @param allowTextShrink Whether text should be allowed to shrink below its normal size (default = false)
	  * @param useLowPriorityImageSize Whether image size constraints should be low priority (default = false)
	  * @param forceEqualBreadth Whether the image and the text should be forced to have equal width or height
	  *                          (depending on alignment) (default = false)
	  * @return A new label
	  */
	def apply(image: Image, text: LocalizedString, font: Font, textColor: Color = Color.textBlack,
			  alignment: Alignment = Alignment.Left, imageInsets: StackInsets = StackInsets.any,
			  textInsets: StackInsets = StackInsets.any, betweenLinesMargin: Double = 0.0,
			  customDrawers: Vector[CustomDrawer] = Vector(), allowLineBreaks: Boolean = true,
			  allowImageUpscaling: Boolean = true, allowTextShrink: Boolean = false,
			  useLowPriorityImageSize: Boolean = false, forceEqualBreadth: Boolean = false) =
		new ImageAndTextLabel(parentHierarchy, image, text, font, textColor, alignment, imageInsets, textInsets,
			betweenLinesMargin, customDrawers, allowLineBreaks, allowImageUpscaling, allowTextShrink,
			useLowPriorityImageSize, forceEqualBreadth)
}

case class ContextualImageAndTextLabelFactory[+N <: TextContextLike](factory: ImageAndTextLabelFactory, context: N)
	extends ContextualComponentFactory[N, TextContextLike, ContextualImageAndTextLabelFactory]
{
	private implicit def c: TextContextLike = context
	
	override def withContext[N2 <: TextContextLike](newContext: N2) =
		copy(context = newContext)
	
	/**
	  * Creates a new label that contains both an image and text
	  * @param image Image displayed on this label
	  * @param text Text displayed on this label
	  * @param imageInsets Insets placed around the image (default = any, preferring 0)
	  * @param customDrawers Custom drawers assigned to this component (default = empty)
	  * @param useLowPriorityImageSize Whether image size constraints should be low priority (default = false)
	  * @param forceEqualBreadth Whether the image and the text should be forced to have equal width or height
	  *                          (depending on alignment) (default = false)
	  * @return A new label
	  */
	def apply(image: Image, text: LocalizedString, imageInsets: StackInsets = StackInsets.any,
			  customDrawers: Vector[CustomDrawer] = Vector(), useLowPriorityImageSize: Boolean = false,
			  forceEqualBreadth: Boolean = false) =
		factory(image, text, context.font, context.textColor, context.textAlignment, imageInsets, context.textInsets,
			context.betweenLinesMargin.optimal, customDrawers, context.allowLineBreaks, context.allowImageUpscaling,
			context.allowTextShrink, useLowPriorityImageSize, forceEqualBreadth)
	
	/**
	  * Creates a new label that contains both an image and text
	  * @param icon Icon displayed on this label
	  * @param text Text displayed on this label
	  * @param imageInsets Insets placed around the image (default = any, preferring 0)
	  * @param customDrawers Custom drawers assigned to this component (default = empty)
	  * @param useLowPriorityImageSize Whether image size constraints should be low priority (default = false)
	  * @param forceEqualBreadth Whether the image and the text should be forced to have equal width or height
	  *                          (depending on alignment) (default = false)
	  * @return A new label
	  */
	def withIcon(icon: SingleColorIcon, text: LocalizedString, imageInsets: StackInsets = StackInsets.any,
				 customDrawers: Vector[CustomDrawer] = Vector(), useLowPriorityImageSize: Boolean = false,
				 forceEqualBreadth: Boolean = false) =
		apply(icon.singleColorImage, text, imageInsets, customDrawers, useLowPriorityImageSize, forceEqualBreadth)
	
	/**
	  * Creates a new label that contains both an image and text
	  * @param icon Icon displayed on this label
	  * @param text Text displayed on this label
	  * @param role Role that determines the image color
	  * @param preferredShade Preferred color shade to use (default = standard)
	  * @param imageInsets Insets placed around the image (default = any, preferring 0)
	  * @param customDrawers Custom drawers assigned to this component (default = empty)
	  * @param useLowPriorityImageSize Whether image size constraints should be low priority (default = false)
	  * @param forceEqualBreadth Whether the image and the text should be forced to have equal width or height
	  *                          (depending on alignment) (default = false)
	  * @return A new label
	  */
	def withColouredIcon(icon: SingleColorIcon, text: LocalizedString, role: ColorRole,
						 preferredShade: ColorShade = Standard, imageInsets: StackInsets = StackInsets.any,
						 customDrawers: Vector[CustomDrawer] = Vector(), useLowPriorityImageSize: Boolean = false,
						 forceEqualBreadth: Boolean = false) =
		apply(icon.asImageWithColor(context.color(role, preferredShade)), text, imageInsets, customDrawers,
			useLowPriorityImageSize, forceEqualBreadth)
}

/**
  * A label that displays both image and text
  * @author Mikko Hilpinen
  * @since 29.10.2020, v0.1
  */
class ImageAndTextLabel(parentHierarchy: ComponentHierarchy, image: Image, text: LocalizedString, font: Font,
						textColor: Color = Color.textBlack, alignment: Alignment = Alignment.Left,
						imageInsets: StackInsets = StackInsets.any, textInsets: StackInsets = StackInsets.any,
						betweenLinesMargin: Double = 0.0, additionalDrawers: Vector[CustomDrawer] = Vector(),
						allowLineBreaks: Boolean = true, allowImageUpscaling: Boolean = true,
						allowTextShrink: Boolean = false, useLowPriorityImageSize: Boolean = false,
						forceEqualBreadth: Boolean = false)
	extends ReachComponentWrapper
{
	// ATTRIBUTES	------------------------------
	
	override protected val wrapped =
	{
		def makeTextLabel(hierarchy: ComponentHierarchy) = TextLabel(hierarchy).apply(text, font, textColor,
			alignment, textInsets, betweenLinesMargin, allowLineBreaks = allowLineBreaks,
			allowTextShrink = allowTextShrink)
		def makeImageLabel(hierarchy: ComponentHierarchy) = ImageLabel(hierarchy).apply(image, imageInsets,
			alignment.opposite, allowUpscaling = allowImageUpscaling, useLowPrioritySize = useLowPriorityImageSize)
		
		// If one of the provided items is empty, only creates one component
		if (image.isEmpty)
			makeTextLabel(parentHierarchy)
		else if (text.isEmpty)
			makeImageLabel(parentHierarchy)
		else
		{
			// Creates stack content in open state first
			val openItems = Open { hierarchy =>
				ComponentCreationResult(Pair(makeImageLabel(hierarchy), makeTextLabel(hierarchy)))
			}(parentHierarchy.top)
			// Wraps the components in a stack
			Stack(parentHierarchy).forPair(openItems, alignment, StackLength.fixedZero,
				customDrawers = additionalDrawers, forceFitLayout = forceEqualBreadth).parent
		}
	}
}
