package utopia.reach.component.label.image

import utopia.firmament.component.stack.CachingStackable
import utopia.firmament.context.ColorContext
import utopia.firmament.drawing.immutable.{BackgroundDrawer, CustomDrawableFactory, ImageDrawer}
import utopia.firmament.drawing.template.{CustomDrawable, CustomDrawer}
import utopia.firmament.image.SingleColorIcon
import utopia.firmament.model.stack.{StackInsets, StackInsetsConvertible, StackSize}
import utopia.genesis.image.Image
import utopia.paradigm.color.{Color, ColorRole, ColorSet}
import utopia.paradigm.enumeration.{Alignment, FromAlignmentFactory}
import utopia.paradigm.transform.SizeAdjustable
import utopia.reach.component.factory.contextual.{ColorContextualFactory, ContextualBackgroundAssignableFactory, ContextualFramedFactory}
import utopia.reach.component.factory.{BackgroundAssignable, ComponentFactoryFactory, FramedFactory, FromContextFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{CustomDrawReachComponent, ReachComponentLike}

trait ImageLabelSettingsLike[+Repr]
	extends CustomDrawableFactory[Repr] with FromAlignmentFactory[Repr] with FramedFactory[Repr]
{
	// ABSTRACT ---------------------------
	
	protected def imageScaling: Double
	protected def alignment: Alignment
	protected def colorOverlay: Option[Color]
	protected def usesLowPrioritySize: Boolean
	
	/**
	  * @param scaling Scaling to apply to the drawn image (in addition to the image's original scaling)
	  * @return Copy of this factory that scales the images by that amount
	  */
	def withImageScaling(scaling: Double): Repr
	/**
	  * @param color Color overlay to place around the drawn images. None if no overlay shall be used.
	  * @return Copy of this factory with the specified color overlay
	  */
	def withColor(color: Option[Color]): Repr
	/**
	  * @param lowPriority Whether low priority image size should be used
	  * @return Copy of this factory with the specified setting in use
	  */
	def withUseLowPrioritySize(lowPriority: Boolean): Repr
	
	
	// COMPUTED ---------------------------
	
	/**
	  * @return Copy of this factory that doesn't place any color overlay
	  */
	def withoutColorOverlay = withColor(None)
	
	/**
	  * @return Copy of this factory that uses low priority image sizes
	  */
	def lowPriority = withUseLowPrioritySize(lowPriority = true)
	
	
	// OTHER    -------------------------
	
	/**
	  * @param color Color overlay to place around the drawn images
	  * @return Copy of this factory with the specified color overlay
	  */
	def withColor(color: Color): Repr = withColor(Some(color))
	
	/**
	  * @param scaling A scaling modifier, which is applied over the existing scaling modifier, if present
	  * @return Copy of this factory that uses scaling
	  */
	def withImageScaledBy(scaling: Double) = withImageScaling(imageScaling * scaling)
}

object ImageLabelSettings
{
	val default = apply()
}
case class ImageLabelSettings(insets: StackInsets = StackInsets.any, alignment: Alignment = Alignment.Center,
                              imageScaling: Double = 1.0, colorOverlay: Option[Color] = None,
                              customDrawers: Vector[CustomDrawer] = Vector.empty, usesLowPrioritySize: Boolean = false)
	extends ImageLabelSettingsLike[ImageLabelSettings]
{
	override def withInsets(insets: StackInsetsConvertible): ImageLabelSettings = copy(insets = insets.toInsets)
	override def withImageScaling(scaling: Double): ImageLabelSettings = copy(imageScaling = scaling)
	override def withColor(color: Option[Color]): ImageLabelSettings = copy(colorOverlay = color)
	override def withUseLowPrioritySize(lowPriority: Boolean): ImageLabelSettings =
		copy(usesLowPrioritySize = lowPriority)
	override def withCustomDrawers(drawers: Vector[CustomDrawer]): ImageLabelSettings = copy(customDrawers = drawers)
	override def apply(alignment: Alignment) = copy(alignment = alignment)
}

trait ImageLabelSettingsWrapper[+Repr] extends ImageLabelSettingsLike[Repr]
{
	// ABSTRACT ---------------------------
	
	protected def settings: ImageLabelSettings
	protected def withSettings(settings: ImageLabelSettings): Repr
	
	
	// IMPLEMENTED  ----------------------
	
	override protected def insets: StackInsets = settings.insets
	override protected def imageScaling: Double = settings.imageScaling
	override protected def alignment: Alignment = settings.alignment
	override protected def colorOverlay: Option[Color] = settings.colorOverlay
	override protected def usesLowPrioritySize: Boolean = settings.usesLowPrioritySize
	override def customDrawers: Vector[CustomDrawer] = settings.customDrawers
	
	override def withInsets(insets: StackInsetsConvertible): Repr = mapSettings { _.withInsets(insets) }
	override def withImageScaling(scaling: Double): Repr = mapSettings { _.withImageScaling(scaling) }
	override def withColor(color: Option[Color]): Repr = mapSettings { _.withColor(color) }
	override def withUseLowPrioritySize(lowPriority: Boolean): Repr = mapSettings { _.withUseLowPrioritySize(lowPriority) }
	override def apply(alignment: Alignment): Repr = mapSettings { _(alignment) }
	override def withCustomDrawers(drawers: Vector[CustomDrawer]): Repr = mapSettings { _.withCustomDrawers(drawers) }
	
	
	// OTHER    --------------------------
	
	def mapSettings(f: ImageLabelSettings => ImageLabelSettings) = withSettings(f(settings))
}

trait ImageLabelFactoryLike[+Repr]
	extends ImageLabelSettingsWrapper[Repr] with SizeAdjustable[Repr]
{
	// ABSTRACT ---------------------------
	
	protected def parentHierarchy: ComponentHierarchy
	
	protected def allowsUpscaling: Boolean
	
	
	// OTHER    ---------------------------
	
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

case class ImageLabelFactory(parentHierarchy: ComponentHierarchy,
                             settings: ImageLabelSettings = ImageLabelSettings.default,
                             allowsUpscaling: Boolean = false)
	extends ImageLabelFactoryLike[ImageLabelFactory] with BackgroundAssignable[ImageLabelFactory]
		with FromContextFactory[ColorContext, ContextualImageLabelFactory]
{
	// IMPLEMENTED  ----------------------------
	
	override def self: ImageLabelFactory = this
	
	override def *(mod: Double): ImageLabelFactory = withImageScaledBy(mod).withInsetsScaledBy(mod)
	
	override protected def withSettings(settings: ImageLabelSettings): ImageLabelFactory = copy(settings = settings)
	
	override def withBackground(background: Color): ImageLabelFactory = withCustomDrawer(BackgroundDrawer(background))
	
	override def withContext(context: ColorContext): ContextualImageLabelFactory =
		ContextualImageLabelFactory(parentHierarchy, context, settings)
	
	
	// COMPUTED ---------------------------
	
	/**
	  * @return Copy of this factory that allows image scaling beyond its original resolution
	  */
	def allowingImageUpscaling = copy(allowsUpscaling = true)
}

case class ContextualImageLabelFactory(parentHierarchy: ComponentHierarchy, context: ColorContext,
                                       settings: ImageLabelSettings = ImageLabelSettings.default)
	extends ImageLabelFactoryLike[ContextualImageLabelFactory]
		with ColorContextualFactory[ContextualImageLabelFactory]
		with ContextualBackgroundAssignableFactory[ColorContext, ContextualImageLabelFactory]
		with ContextualFramedFactory[ContextualImageLabelFactory]
{
	// IMPLEMENTED  ----------------------------
	
	override def self: ContextualImageLabelFactory = this
	
	override protected def allowsUpscaling: Boolean = context.allowImageUpscaling
	
	override protected def withSettings(settings: ImageLabelSettings): ContextualImageLabelFactory =
		copy(settings = settings)
	
	override def withContext(context: ColorContext): ContextualImageLabelFactory = copy(context = context)
	
	
	// OTHER    --------------------------------
	
	def withColor(color: ColorSet): ContextualImageLabelFactory = withColor(context.color(color))
	def withColor(color: ColorRole): ContextualImageLabelFactory = withColor(context.color(color))
	
	/**
	  * @param icon Icon to draw
	  * @return A label that displays that icon
	  */
	def apply(icon: SingleColorIcon): ImageLabel = colorOverlay match {
		case Some(c) => withoutColorOverlay(icon(c))
		case None => apply(icon.contextual(context))
	}
	def apply(image: Either[Image, SingleColorIcon]): ImageLabel = image match {
		case Right(icon) => apply(icon)
		case Left(img) => apply(img)
	}
}

case class ImageLabelSetup(settings: ImageLabelSettings)
	extends ImageLabelSettingsWrapper[ImageLabelSetup] with ComponentFactoryFactory[ImageLabelFactory]
{
	// IMPLEMENTED  ---------------------
	
	override protected def withSettings(settings: ImageLabelSettings): ImageLabelSetup = copy(settings = settings)
	
	override def apply(hierarchy: ComponentHierarchy) = ImageLabelFactory(hierarchy, settings)
}

object ImageLabel extends ComponentFactoryFactory[ImageLabelFactory] with ImageLabelSettingsWrapper[ImageLabelSetup]
{
	override protected def settings: ImageLabelSettings = ImageLabelSettings.default
	
	override def withSettings(settings: ImageLabelSettings): ImageLabelSetup = ImageLabelSetup(settings)
	
	override def apply(hierarchy: ComponentHierarchy) = ImageLabelFactory(hierarchy)
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
