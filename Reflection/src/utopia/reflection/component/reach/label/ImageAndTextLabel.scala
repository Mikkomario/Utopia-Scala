package utopia.reflection.component.reach.label

import utopia.genesis.color.Color
import utopia.genesis.image.Image
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.reach.hierarchy.ComponentHierarchy
import utopia.reflection.component.reach.template.ReachComponentWrapper
import utopia.reflection.component.reach.wrapper.{ComponentCreationResult, Open}
import utopia.reflection.container.reach.Stack
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.stack.{StackInsets, StackLength}
import utopia.reflection.text.Font

/**
  * A label that displays both image and text
  * @author Mikko Hilpinen
  * @since 29.10.2020, v2
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
		// Creates stack content in open state first
		val openItems = Open { hierarchy =>
			val textLabel = TextLabel(hierarchy).apply(text, font, textColor, alignment, textInsets, betweenLinesMargin,
				allowLineBreaks = allowLineBreaks, allowTextShrink = allowTextShrink)
			val imageLabel = ImageLabel(hierarchy).apply(image, imageInsets, alignment.opposite,
				allowUpscaling = allowImageUpscaling, useLowPrioritySize = useLowPriorityImageSize)
			ComponentCreationResult(imageLabel -> textLabel)
		}(parentHierarchy.top)
		// Wraps the components in a stack
		Stack(parentHierarchy).forPair(openItems, alignment, StackLength.fixedZero, customDrawers = additionalDrawers,
			forceFitLayout = forceEqualBreadth)
	}.parent
}
