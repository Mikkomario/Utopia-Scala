package utopia.reach.component.label.image

import utopia.flow.datastructure.immutable.Pair
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.event.{ChangeListener, ChangingLike, Fixed}
import utopia.paradigm.color.Color
import utopia.genesis.image.Image
import utopia.reach.component.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.text.ViewTextLabel
import utopia.reach.component.template.ReachComponentWrapper
import utopia.reach.component.wrapper.Open
import utopia.reach.container.multi.stack.Stack
import utopia.reach.util.Priority.Low
import utopia.reflection.color.ColorShade.Standard
import utopia.reflection.color.{ColorRole, ColorShade}
import utopia.reflection.component.context.TextContextLike
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.drawing.view.BackgroundViewDrawer
import utopia.reflection.component.template.layout.stack.ConstrainableWrapper
import utopia.reflection.image.SingleColorIcon
import utopia.reflection.localization.DisplayFunction
import utopia.paradigm.enumeration.Alignment
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
	  * @param fontPointer A pointer to the font used in the text
	  * @param displayFunction Display function used when converting the item to text (default = toString)
	  * @param textColorPointer A pointer to the color used when drawing text (default = always standard black)
	  * @param imageInsetsPointer A pointer to insets placed around the image (default = any, preferring 0)
	  * @param textInsetsPointer A pointer to insets placed around the text (default = any, preferring 0)
	  * @param alignment Alignment used for the <b>text</b> (the image will be placed with the opposite alignment,
	  *                  so that the two form a close pair) (default = Left)
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
	def apply[A](itemPointer: ChangingLike[A], imagePointer: ChangingLike[Image], fontPointer: ChangingLike[Font],
				 textColorPointer: ChangingLike[Color] = Fixed(Color.textBlack),
				 imageInsetsPointer: ChangingLike[StackInsets] = Fixed(StackInsets.any),
				 textInsetsPointer: ChangingLike[StackInsets] = Fixed(StackInsets.any),
				 alignment: Alignment = Alignment.Left,
				 displayFunction: DisplayFunction[A] = DisplayFunction.raw,
				 betweenLinesMargin: Double = 0.0, additionalDrawers: Vector[CustomDrawer] = Vector(),
				 allowLineBreaks: Boolean = true, allowImageUpscaling: Boolean = true,
				 allowTextShrink: Boolean = false, useLowPriorityImageSize: Boolean = false,
				 forceEqualBreadth: Boolean = false) =
		new ViewImageAndTextLabel[A](parentHierarchy, itemPointer, imagePointer, fontPointer, textColorPointer,
			imageInsetsPointer, textInsetsPointer, alignment, displayFunction, betweenLinesMargin, additionalDrawers,
			allowLineBreaks, allowImageUpscaling, allowTextShrink, useLowPriorityImageSize, forceEqualBreadth)
}

case class ContextualViewImageAndTextLabelFactory[+N <: TextContextLike](factory: ViewImageAndTextLabelFactory, context: N)
	extends ContextualComponentFactory[N, TextContextLike, ContextualViewImageAndTextLabelFactory]
{
	implicit def c: TextContextLike = context
	
	override def withContext[N2 <: TextContextLike](newContext: N2) =
		copy(context = newContext)
	
	/**
	  * @return A copy of this factory that doesn't utilize component creation context
	  */
	def withoutContext = factory
	
	/**
	  * Creates a new label which displays both image and text
	  * @param itemPointer A pointer to this label's (text-determining) content
	  * @param imagePointer A pointer to the image displayed on this label
	  * @param fontPointer A pointer to the font used in the text (default = determined by context)
	  * @param textColorPointer A pointer to the color used when drawing text (default = determined by context)
	  * @param imageInsetsPointer A pointer to the insets placed around the image (default = always any, preferring zero)
	  * @param textInsetsPointer A pointer to the insets placed around the text (default = determined by context)
	  * @param displayFunction Display function used when converting the item to text (default = toString)
	  * @param additionalDrawers Additional custom drawers assigned to this component
	  * @param useLowPriorityImageSize Whether low priority size constraints should be used for the image part
	  *                                (default = false)
	  * @param forceEqualBreadth Whether the image and text should be forced to have the same breadth, no matter the
	  *                          alignment (default = false)
	  * @tparam A Type of content in this label
	  * @return A new label
	  */
	def withChangingStyle[A](itemPointer: ChangingLike[A], imagePointer: ChangingLike[Image],
							 fontPointer: ChangingLike[Font] = Fixed(context.font),
							 textColorPointer: ChangingLike[Color] = Fixed(context.textColor),
							 imageInsetsPointer: ChangingLike[StackInsets] = Fixed(StackInsets.any),
							 textInsetsPointer: ChangingLike[StackInsets] = Fixed(context.textInsets),
							 displayFunction: DisplayFunction[A] = DisplayFunction.raw,
							 additionalDrawers: Vector[CustomDrawer] = Vector(),
							 useLowPriorityImageSize: Boolean = false, forceEqualBreadth: Boolean = false) =
		factory[A](itemPointer, imagePointer, fontPointer, textColorPointer, imageInsetsPointer, textInsetsPointer,
			context.textAlignment, displayFunction, context.betweenLinesMargin.optimal, additionalDrawers,
			context.allowLineBreaks, context.allowImageUpscaling, context.allowTextShrink, useLowPriorityImageSize,
			forceEqualBreadth)
	
	/**
	  * Creates a new label which displays both image and text
	  * @param itemPointer A pointer to this label's (text-determining) content
	  * @param imagePointer A pointer to the image displayed on this label
	  * @param imageInsets Insets placed around the image (default = any, preferring 0)
	  * @param displayFunction Display function used when converting the item to text (default = toString)
	  * @param additionalDrawers Additional custom drawers assigned to this component
	  * @param useLowPriorityImageSize Whether low priority size constraints should be used for the image part
	  *                                (default = false)
	  * @param forceEqualBreadth Whether the image and text should be forced to have the same breadth, no matter the
	  *                          alignment (default = false)
	  * @tparam A Type of content in this label
	  * @return A new label
	  */
	def apply[A](itemPointer: ChangingLike[A], imagePointer: ChangingLike[Image], imageInsets: StackInsets = StackInsets.any,
				 displayFunction: DisplayFunction[A] = DisplayFunction.raw,
				 additionalDrawers: Vector[CustomDrawer], useLowPriorityImageSize: Boolean = false,
				 forceEqualBreadth: Boolean = false) =
		withChangingStyle[A](itemPointer, imagePointer, imageInsetsPointer = Fixed(imageInsets),
			displayFunction = displayFunction, additionalDrawers = additionalDrawers,
			useLowPriorityImageSize = useLowPriorityImageSize, forceEqualBreadth = forceEqualBreadth)
	
	/**
	  * Creates a new label that contains both an image and text
	  * @param itemPointer A pointer to this label's (text-determining) content
	  * @param iconPointer A pointer to the displayed icon
	  * @param imageInsets Insets placed around the image (default = any, preferring 0)
	  * @param displayFunction A function for converting the displayed item to text (default = use toString)
	  * @param customDrawers Custom drawers assigned to this component (default = empty)
	  * @param useLowPriorityImageSize Whether image size constraints should be low priority (default = false)
	  * @param forceEqualBreadth Whether the image and the text should be forced to have equal width or height
	  *                          (depending on alignment) (default = false)
	  * @return A new label
	  */
	def withIcon[A](itemPointer: ChangingLike[A], iconPointer: ChangingLike[SingleColorIcon],
					imageInsets: StackInsets = StackInsets.any,
					displayFunction: DisplayFunction[A] = DisplayFunction.raw,
					customDrawers: Vector[CustomDrawer] = Vector(), useLowPriorityImageSize: Boolean = false,
					forceEqualBreadth: Boolean = false) =
		apply(itemPointer, iconPointer.map { _.singleColorImage }, imageInsets, displayFunction, customDrawers,
			useLowPriorityImageSize, forceEqualBreadth)
	
	/**
	  * Creates a new label that contains both an image and text
	  * @param itemPointer A pointer to this label's (text-determining) content
	  * @param iconPointer A pointer to the displayed icon
	  * @param rolePointer A pointer to the role the icon serves (determines icon color)
	  * @param preferredShade Preferred color shade to use (default = standard)
	  * @param imageInsets Insets placed around the image (default = any, preferring 0)
	  * @param displayFunction A function for converting the displayed item to text (default = use toString)
	  * @param customDrawers Custom drawers assigned to this component (default = empty)
	  * @param useLowPriorityImageSize Whether image size constraints should be low priority (default = false)
	  * @param forceEqualBreadth Whether the image and the text should be forced to have equal width or height
	  *                          (depending on alignment) (default = false)
	  * @return A new label
	  */
	def withColouredIcon[A](itemPointer: ChangingLike[A], iconPointer: ChangingLike[SingleColorIcon],
							rolePointer: ChangingLike[ColorRole], preferredShade: ColorShade = Standard,
							imageInsets: StackInsets = StackInsets.any,
							displayFunction: DisplayFunction[A] = DisplayFunction.raw,
							customDrawers: Vector[CustomDrawer] = Vector(), useLowPriorityImageSize: Boolean = false,
							forceEqualBreadth: Boolean = false) =
		apply(itemPointer, iconPointer.mergeWith(rolePointer) { (icon, role) =>
			icon.asImageWithColor(context.color(role, preferredShade)) }, imageInsets, displayFunction, customDrawers,
			useLowPriorityImageSize, forceEqualBreadth)
	
	/**
	  * Creates a new label which displays both image and text
	  * @param itemPointer A pointer to this label's (text-determining) content
	  * @param iconPointer A pointer to the displayed icon
	  * @param rolePointer A pointer to the color role used in label background
	  * @param preferredShade Preferred color shade to use (default = standard)
	  * @param imageInsets Insets placed around the image (default = any, preferring 0)
	  * @param displayFunction Display function used when converting the item to text (default = toString)
	  * @param customDrawers Custom drawers assigned to this component (default = empty)
	  * @param useLowPriorityImageSize Whether low priority size constraints should be used for the image part
	  *                                (default = false)
	  * @param forceEqualBreadth Whether the image and text should be forced to have the same breadth, no matter the
	  *                          alignment (default = false)
	  * @tparam A Type of content in this label
	  * @return A new label
	  */
	def withIconAndChangingBackground[A](itemPointer: ChangingLike[A], iconPointer: ChangingLike[SingleColorIcon],
										 rolePointer: ChangingLike[ColorRole], preferredShade: ColorShade = Standard,
										 imageInsets: StackInsets = StackInsets.any,
										 displayFunction: DisplayFunction[A] = DisplayFunction.raw,
										 customDrawers: Vector[CustomDrawer] = Vector(),
										 useLowPriorityImageSize: Boolean = false, forceEqualBreadth: Boolean = false) =
	{
		val backgroundPointer = rolePointer.map { context.color(_, preferredShade) }
		val backgroundDrawer = BackgroundViewDrawer(backgroundPointer.map { c => c })
		val imagePointer = iconPointer.mergeWith(backgroundPointer) { _.singleColorImageAgainst(_) }
		val label = withChangingStyle(itemPointer, imagePointer,
			textColorPointer = backgroundPointer.map { _.defaultTextColor }, imageInsetsPointer = Fixed(imageInsets),
			displayFunction = displayFunction, additionalDrawers = backgroundDrawer +: customDrawers,
			useLowPriorityImageSize = useLowPriorityImageSize, forceEqualBreadth = forceEqualBreadth)
		// Repaints this component whenever background color changes
		backgroundPointer.addAnyChangeListener { label.repaint(Low) }
		label
	}
}

/**
  * A pointer-based label that displays an image and a piece of text
  * @author Mikko Hilpinen
  * @since 9.11.2020, v0.1
  */
class ViewImageAndTextLabel[A](parentHierarchy: ComponentHierarchy, val itemPointer: ChangingLike[A],
							   val imagePointer: ChangingLike[Image], fontPointer: ChangingLike[Font],
							   textColorPointer: ChangingLike[Color] = Fixed(Color.textBlack),
							   imageInsetsPointer: ChangingLike[StackInsets] = Fixed(StackInsets.any),
							   textInsetsPointer: ChangingLike[StackInsets] = Fixed(StackInsets.any),
							   alignment: Alignment = Alignment.Left,
							   displayFunction: DisplayFunction[A] = DisplayFunction.raw,
							   betweenLinesMargin: Double = 0.0, additionalDrawers: Vector[CustomDrawer] = Vector(),
							   allowLineBreaks: Boolean = true, allowImageUpscaling: Boolean = true,
							   allowTextShrink: Boolean = false, useLowPriorityImageSize: Boolean = false,
							   forceEqualBreadth: Boolean = false)
	extends ReachComponentWrapper with ConstrainableWrapper
{
	// ATTRIBUTES	-------------------------------
	
	private val stylePointer = new PointerWithEvents[TextDrawContext](updatedStyle)
	private val updateStyleListener: ChangeListener[Any] = _ => stylePointer.value = updatedStyle
	
	override protected val wrapped =
	{
		// Creates stack content (image and text label)
		val openItems = Open { hierarchy =>
			val imageLabel = ViewImageLabel(hierarchy).apply(imagePointer, imageInsetsPointer,
				Fixed(alignment.opposite), allowUpscaling = allowImageUpscaling,
				useLowPrioritySize = useLowPriorityImageSize)
			val textLabel = ViewTextLabel(hierarchy).apply(itemPointer, stylePointer, displayFunction,
				allowTextShrink = allowTextShrink)
			Pair(imageLabel, textLabel)
		}(parentHierarchy.top)
		// Wraps the components in a stack
		Stack(parentHierarchy).forPair(openItems, alignment, StackLength.fixedZero,
			customDrawers = additionalDrawers, forceFitLayout = forceEqualBreadth).parent
	}
	
	
	// INITIAL CODE	--------------------------------
	
	fontPointer.addListener(updateStyleListener)
	textColorPointer.addListener(updateStyleListener)
	textInsetsPointer.addListener(updateStyleListener)
	
	
	// COMPUTED	-------------------------------------
	
	private def updatedStyle = TextDrawContext(fontPointer.value, textColorPointer.value, alignment,
		textInsetsPointer.value, betweenLinesMargin, allowLineBreaks)
}
