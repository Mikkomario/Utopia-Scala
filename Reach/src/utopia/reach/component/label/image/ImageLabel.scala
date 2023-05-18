package utopia.reach.component.label.image

import utopia.firmament.component.stack.CachingStackable
import utopia.firmament.context.ColorContext
import utopia.firmament.drawing.immutable.{BackgroundDrawer, CustomDrawableFactory, ImageDrawer}
import utopia.firmament.drawing.template.{CustomDrawable, CustomDrawer}
import utopia.firmament.image.SingleColorIcon
import utopia.firmament.model.enumeration.SizeCategory
import utopia.firmament.model.stack.{StackInsets, StackInsetsConvertible, StackLength, StackSize}
import utopia.genesis.image.Image
import utopia.paradigm.color.{Color, ColorRole, ColorSet}
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.{Alignment, Axis2D, Direction2D, FromAlignmentFactory}
import utopia.paradigm.transform.SizeAdjustable
import utopia.reach.component.factory.{BackgroundAssignable, ColorContextualFactory, ComponentFactoryFactory, ContextualBackgroundAssignableFactory, FromContextFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{CustomDrawReachComponent, ReachComponentLike}

object ImageLabel extends ComponentFactoryFactory[ImageLabelFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = ImageLabelFactory(hierarchy)
}

trait ImageLabelFactoryLike[+Repr]
	extends CustomDrawableFactory[Repr] with FromAlignmentFactory[Repr] with SizeAdjustable[Repr]
{
	// ABSTRACT ---------------------------
	
	protected def parentHierarchy: ComponentHierarchy
	
	protected def imageScaling: Double
	protected def insets: StackInsets
	protected def alignment: Alignment
	protected def colorOverlay: Option[Color]
	protected def allowsUpscaling: Boolean
	protected def usesLowPrioritySize: Boolean
	
	/**
	  * @param scaling Scaling to apply to the drawn image (in addition to the image's original scaling)
	  * @return Copy of this factory that scales the images by that amount
	  */
	def withImageScaling(scaling: Double): Repr
	/**
	  * @param insets Insets to place around the image
	  * @return Copy of this factory with the specified insets
	  */
	def withInsets(insets: StackInsetsConvertible): Repr
	/**
	  * @param color Color overlay to place around the drawn images
	  * @return Copy of this factory with the specified color overlay
	  */
	def withColor(color: Color): Repr
	/**
	  * @param allow Whether images should be allowed to scale beyond their original resolution
	  * @return Copy of this factory with the specified setting in use
	  */
	def withAllowUpscaling(allow: Boolean): Repr
	/**
	  * @param lowPriority Whether low priority image size should be used
	  * @return Copy of this factory with the specified setting in use
	  */
	def withUseLowPrioritySize(lowPriority: Boolean): Repr
	
	
	// COMPUTED ---------------------------
	
	/**
	  * @return Copy of this factory that doesn't place any insets around the image
	  */
	def withoutInsets = withInsets(StackInsets.zero)
	/**
	  * @return Copy of this factory that doesn't place any horizontal insets around the image
	  */
	def withoutHorizontalInsets = withoutInsetsAlong(X)
	/**
	  * @return Copy of this factory that doesn't place any vertical insets around the image
	  */
	def withoutVerticalInsets = withoutInsetsAlong(Y)
	
	/**
	  * @return Copy of this factory that allows image scaling beyond its original resolution
	  */
	def allowingImageUpscaling = withAllowUpscaling(allow = true)
	/**
	  * @return Copy of this factory that uses low priority image sizes
	  */
	def lowPriority = withUseLowPrioritySize(lowPriority = true)
	
	
	// OTHER    ---------------------------
	
	def mapInsets(f: StackInsets => StackInsetsConvertible) = withInsets(f(insets))
	def mapInsetsAlong(axis: Axis2D)(f: StackLength => StackLength) = mapInsets { _.mapAxis(axis)(f) }
	def mapInset(side: Direction2D)(f: StackLength => StackLength) = mapInsets { _.mapSide(side)(f) }
	def withoutInsetsAlong(axis: Axis2D) = mapInsets { _ - axis }
	def withoutInset(direction: Direction2D) = mapInsets { _ - direction }
	
	/**
	  * Creates a new image label
	  * @param image              Drawn image
	  * @return A new label that displays the specified image
	  */
	def apply(image: Image): ImageLabel = {
		val img = colorOverlay match {
			case Some(c) => image.withColorOverlay(c)
			case None => image
		}
		new _ImageLabel(parentHierarchy, img * imageScaling, insets, alignment, customDrawers, allowsUpscaling,
			usesLowPrioritySize)
	}
	
	
	// NESTED   --------------------------------------
	
	// A static image label implementation
	private class _ImageLabel(override val parentHierarchy: ComponentHierarchy, override val image: Image,
	                          override val insets: StackInsets = StackInsets.zero, alignment: Alignment = Alignment.Center,
	                          additionalCustomDrawers: Vector[CustomDrawer] = Vector(), override val allowUpscaling: Boolean = true,
	                          override val useLowPrioritySize: Boolean = false)
		extends CustomDrawReachComponent with ImageLabel
	{
		// ATTRIBUTES	------------------------------
		
		override val customDrawers = ImageDrawer(image * imageScaling, insets, alignment,
			useUpscaling = allowUpscaling) +: additionalCustomDrawers
		
		
		// IMPLEMENTED	------------------------------
		
		override def updateLayout() = ()
	}
}

case class ImageLabelFactory(parentHierarchy: ComponentHierarchy, insets: StackInsets = StackInsets.any,
                             imageScaling: Double = 1.0, alignment: Alignment = Alignment.Center,
                             customDrawers: Vector[CustomDrawer] = Vector.empty, colorOverlay: Option[Color] = None,
                             allowsUpscaling: Boolean = false, usesLowPrioritySize: Boolean = false)
	extends ImageLabelFactoryLike[ImageLabelFactory] with BackgroundAssignable[ImageLabelFactory]
		with FromContextFactory[ColorContext, ContextualImageLabelFactory]
{
	// IMPLEMENTED  ----------------------------
	
	override def self: ImageLabelFactory = this
	
	override def withImageScaling(scaling: Double): ImageLabelFactory = copy(imageScaling = scaling)
	override def withInsets(insets: StackInsetsConvertible): ImageLabelFactory = copy(insets = insets.toInsets)
	override def withAllowUpscaling(allow: Boolean): ImageLabelFactory = copy(allowsUpscaling = allow)
	override def withUseLowPrioritySize(lowPriority: Boolean): ImageLabelFactory =
		copy(usesLowPrioritySize = lowPriority)
	override def withCustomDrawers(drawers: Vector[CustomDrawer]): ImageLabelFactory = copy(customDrawers = drawers)
	override def withColor(color: Color): ImageLabelFactory = copy(colorOverlay = Some(color))
	override def apply(alignment: Alignment) = copy(alignment = alignment)
	
	override def *(mod: Double): ImageLabelFactory = copy(imageScaling = imageScaling * mod, insets = insets * mod)
	
	override def withBackground(background: Color): ImageLabelFactory = withCustomDrawer(BackgroundDrawer(background))
	
	override def withContext(context: ColorContext): ContextualImageLabelFactory =
		ContextualImageLabelFactory(parentHierarchy, context, imageScaling, alignment, customDrawers, colorOverlay,
			allowsUpscaling = allowsUpscaling, usesLowPrioritySize = usesLowPrioritySize)
}

case class ContextualImageLabelFactory(parentHierarchy: ComponentHierarchy, context: ColorContext,
                                       imageScaling: Double = 1.0, alignment: Alignment = Alignment.Center,
                                       customDrawers: Vector[CustomDrawer] = Vector.empty,
                                       colorOverlay: Option[Color] = None,
                                       customInsets: Option[Either[StackInsets, SizeCategory]] = None,
                                       allowsUpscaling: Boolean = false, usesLowPrioritySize: Boolean = false)
	extends ImageLabelFactoryLike[ContextualImageLabelFactory]
		with ColorContextualFactory[ContextualImageLabelFactory]
		with ContextualBackgroundAssignableFactory[ColorContext, ContextualImageLabelFactory]
{
	// IMPLEMENTED  ----------------------------
	
	override def self: ContextualImageLabelFactory = this
	
	override protected def insets: StackInsets = customInsets match {
		case Some(Left(custom)) => custom
		case Some(Right(size)) => context.scaledStackMargin(size).toInsets
		case None => StackInsets.any
	}
	
	override def allowingImageUpscaling: ContextualImageLabelFactory =
		copy(context = context.allowingImageUpscaling, allowsUpscaling = true)
	
	override def withImageScaling(scaling: Double): ContextualImageLabelFactory = copy(imageScaling = scaling)
	override def withInsets(insets: StackInsetsConvertible): ContextualImageLabelFactory =
		copy(customInsets = Some(Left(insets.toInsets)))
	override def apply(alignment: Alignment) = copy(alignment = alignment)
	override def withAllowUpscaling(allow: Boolean): ContextualImageLabelFactory = copy(allowsUpscaling = allow)
	override def withUseLowPrioritySize(lowPriority: Boolean): ContextualImageLabelFactory =
		copy(usesLowPrioritySize = lowPriority)
	override def withCustomDrawers(drawers: Vector[CustomDrawer]): ContextualImageLabelFactory =
		copy(customDrawers = drawers)
	override def withColor(color: Color): ContextualImageLabelFactory = copy(colorOverlay = Some(color))
	
	override def withContext(context: ColorContext): ContextualImageLabelFactory = copy(context = context)
	
	
	// OTHER    --------------------------------
	
	def withInsets(insetSize: SizeCategory) = copy(customInsets = Some(Right(insetSize)))
	
	def withColor(color: ColorSet): ContextualImageLabelFactory = withColor(context.color(color))
	def withColor(color: ColorRole): ContextualImageLabelFactory = withColor(context.color(color))
	
	/**
	  * @param icon Icon to draw
	  * @return A label that displays that icon
	  */
	def apply(icon: SingleColorIcon): ImageLabel = colorOverlay match {
		case Some(c) => copy(colorOverlay = None)(icon(c))
		case None => apply(icon.contextual(context))
	}
}

/**
  * A common trait for image label implementations
  * @author Mikko Hilpinen
  * @since 27.10.2020, v0.1
  */
trait ImageLabel extends ReachComponentLike with CustomDrawable with CachingStackable
{
	// ABSTRACT	----------------------------
	
	/**
	  * @return The image drawn on this label
	  */
	def image: Image
	/**
	  * @return Insets placed around the image
	  */
	def insets: StackInsets
	
	/**
	  * @return Whether image should be allowed to scale beyond it's original size
	  *         (while still respecting source resolution)
	  */
	def allowUpscaling: Boolean
	/**
	  * @return Whether this label should use lower priority size constraints
	  */
	def useLowPrioritySize: Boolean
	
	
	// IMPLEMENTED	------------------------
	
	override def calculatedStackSize = {
		val raw = {
			if (allowUpscaling)
				StackSize.downscaling(image.size.ceil, image.sourceResolution)
			else
				StackSize.downscaling(image.size.ceil)
		}
		if (useLowPrioritySize) raw.shrinking else raw
	}
}
