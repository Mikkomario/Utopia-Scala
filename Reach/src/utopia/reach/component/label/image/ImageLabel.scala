package utopia.reach.component.label.image

import utopia.firmament.component.image.ImageComponent
import utopia.firmament.context.ColorContext
import utopia.firmament.drawing.immutable.{BackgroundDrawer, CustomDrawableFactory, ImageDrawer}
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.factory.FramedFactory
import utopia.firmament.image.SingleColorIcon
import utopia.firmament.model.stack.{StackInsets, StackInsetsConvertible}
import utopia.flow.collection.immutable.Empty
import utopia.flow.util.Mutate
import utopia.flow.view.immutable.eventful.Fixed
import utopia.genesis.image.Image
import utopia.paradigm.color.{Color, ColorRole, ColorSet}
import utopia.paradigm.enumeration.{Alignment, FromAlignmentFactory}
import utopia.paradigm.shape.shape2d.Matrix2D
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.paradigm.transform.{LinearSizeAdjustable, LinearTransformable}
import utopia.reach.component.factory.contextual.{ColorContextualFactory, ContextualBackgroundAssignableFactory, ContextualFramedFactory}
import utopia.reach.component.factory.{BackgroundAssignable, ComponentFactoryFactory, FromContextFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{CustomDrawReachComponent, PartOfComponentHierarchy, ReachComponentLike}

/**
  * Common trait for image label factories and settings
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 31.05.2023, v1.1
  */
trait ImageLabelSettingsLike[+Repr]
	extends CustomDrawableFactory[Repr] with FramedFactory[Repr] with FromAlignmentFactory[Repr]
		with LinearTransformable[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Alignment used when drawing the image within this label
	  */
	def alignment: Alignment
	/**
	  * Scaling applied to the drawn images
	  */
	def imageScaling: Double
	/**
	  * @return Transformation applied to the drawn images.
	  *         None if no transformation should be applied.
	  */
	def transformation: Option[Matrix2D]
	/**
	  * Color overlay applied over drawn images
	  */
	def colorOverlay: Option[Color]
	/**
	  * Whether low priority size constraints should be used
	  */
	def usesLowPrioritySize: Boolean
	
	/**
	  * Alignment used when drawing the image within this label
	  * @param alignment New alignment to use.
	  *                  Alignment used when drawing the image within this label
	  * @return Copy of this factory with the specified alignment
	  */
	def withAlignment(alignment: Alignment): Repr
	/**
	  * Color overlay applied over drawn images
	  * @param color Color overlay to place around the drawn images. None if no overlay shall be used.
	  * @return Copy of this factory with the specified color overlay
	  */
	def withColor(color: Option[Color]): Repr
	/**
	  * Scaling applied to the drawn images
	  * @param scaling Scaling to apply to the drawn image (in addition to the image's original scaling)
	  * @return Copy of this factory that scales the images by that amount
	  */
	def withImageScaling(scaling: Double): Repr
	/**
	  * @param transformation Transformation to apply
	  * @return Copy of this factory with the specified transformation in place
	  */
	def withTransformation(transformation: Option[Matrix2D]): Repr
	/**
	  * Whether low priority size constraints should be used
	  * @param lowPriority Whether low priority image size should be used
	  * @return Copy of this factory with the specified setting in use
	  */
	def withUseLowPrioritySize(lowPriority: Boolean): Repr
	
	
	// COMPUTED	--------------------
	
	/**
	  * Copy of this factory that doesn't place any color overlay
	  */
	def withoutColorOverlay = withColor(None)
	
	/**
	  * Copy of this factory that uses low priority image sizes
	  */
	def lowPriority = withUseLowPrioritySize(lowPriority = true)
	
	
	// IMPLEMENTED  ----------------
	
	override def transformedWith(transformation: Matrix2D): Repr = mapTransformation {
		case Some(t) => Some(t * transformation)
		case None => Some(transformation)
	}
	
	
	// OTHER	--------------------
	
	def mapAlignment(f: Alignment => Alignment) = withAlignment(f(alignment))
	def mapImageScaling(f: Double => Double) = withImageScaling(f(imageScaling))
	
	def withTransformation(transformation: Matrix2D): Repr = withTransformation(Some(transformation))
	def mapTransformation(f: Mutate[Option[Matrix2D]]) = withTransformation(f(transformation))
	
	/**
	  * @param color Color overlay to add on created image labels
	  * @return Copy of this factory with specified color overlay
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
	// ATTRIBUTES   ------------------------
	
	val default = apply()
	
	
	// OTHER    ----------------------------
	
	/**
	  * @param settings Image label settings or related settings
	  * @return Image label settings from the specified settings
	  */
	def from(settings: ImageLabelSettingsLike[_]) = settings match {
		case s: ImageLabelSettings => s
		case s => apply(s.insets, s.alignment, s.imageScaling, s.transformation, s.colorOverlay, s.customDrawers,
			s.usesLowPrioritySize)
	}
}
case class ImageLabelSettings(insets: StackInsets = StackInsets.any, alignment: Alignment = Alignment.Center,
                              imageScaling: Double = 1.0, transformation: Option[Matrix2D] = None,
                              colorOverlay: Option[Color] = None, customDrawers: Seq[CustomDrawer] = Empty,
                              usesLowPrioritySize: Boolean = false)
	extends ImageLabelSettingsLike[ImageLabelSettings]
{
	// COMPUTED ----------------------------
	
	/**
	  * @return A set of view-image-label-settings that match these settings
	  */
	def toViewSettings = ViewImageLabelSettings(customDrawers, Fixed(insets), Fixed(alignment),
		colorOverlay.map(Fixed.apply), Fixed(imageScaling), Fixed(transformation),
		usesLowPrioritySize = usesLowPrioritySize)
	
	
	// IMPLEMENTED  ------------------------
	
	override def identity: ImageLabelSettings = this
	
	override def withTransformation(transformation: Option[Matrix2D]): ImageLabelSettings =
		copy(transformation = transformation)
	override def withInsets(insets: StackInsetsConvertible): ImageLabelSettings = copy(insets = insets.toInsets)
	override def withImageScaling(scaling: Double): ImageLabelSettings = copy(imageScaling = scaling)
	override def withColor(color: Option[Color]): ImageLabelSettings = copy(colorOverlay = color)
	override def withUseLowPrioritySize(lowPriority: Boolean): ImageLabelSettings =
		copy(usesLowPrioritySize = lowPriority)
	override def withCustomDrawers(drawers: Seq[CustomDrawer]): ImageLabelSettings = copy(customDrawers = drawers)
	override def apply(alignment: Alignment) = copy(alignment = alignment)
}

trait ImageLabelSettingsWrapper[+Repr] extends ImageLabelSettingsLike[Repr]
{
	// ABSTRACT ---------------------------
	
	protected def settings: ImageLabelSettings
	def withSettings(settings: ImageLabelSettings): Repr
	
	
	// IMPLEMENTED  ----------------------
	
	override def insets: StackInsets = settings.insets
	override def imageScaling: Double = settings.imageScaling
	override def alignment: Alignment = settings.alignment
	override def colorOverlay: Option[Color] = settings.colorOverlay
	override def usesLowPrioritySize: Boolean = settings.usesLowPrioritySize
	override def customDrawers: Seq[CustomDrawer] = settings.customDrawers
	override def transformation: Option[Matrix2D] = settings.transformation
	
	override def withInsets(insets: StackInsetsConvertible): Repr = mapSettings { _.withInsets(insets) }
	override def withImageScaling(scaling: Double): Repr = mapSettings { _.withImageScaling(scaling) }
	override def withColor(color: Option[Color]): Repr = mapSettings { _.withColor(color) }
	override def withUseLowPrioritySize(lowPriority: Boolean): Repr = mapSettings { _.withUseLowPrioritySize(lowPriority) }
	override def apply(alignment: Alignment): Repr = mapSettings { _(alignment) }
	override def withCustomDrawers(drawers: Seq[CustomDrawer]): Repr = mapSettings { _.withCustomDrawers(drawers) }
	override def withTransformation(transformation: Option[Matrix2D]): Repr =
		mapSettings { _.withTransformation(transformation) }
	
	
	// OTHER    --------------------------
	
	def mapSettings(f: ImageLabelSettings => ImageLabelSettings) = withSettings(f(settings))
}

trait ImageLabelFactoryLike[+Repr, +VF]
	extends ImageLabelSettingsWrapper[Repr] with LinearSizeAdjustable[Repr] with PartOfComponentHierarchy
{
	// ABSTRACT ---------------------------
	
	protected def allowsUpscaling: Boolean
	
	/**
	  * @return A view (pointer) -based copy of this factory
	  */
	def toViewFactory: VF
	
	
	// IMPLEMENTED  -----------------------
	
	override def identity: Repr = self
	
	
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
		new _ImageLabel(parentHierarchy, img * imageScaling, transformation, insets, alignment, customDrawers,
			allowsUpscaling, usesLowPrioritySize)
	}
	
	
	// NESTED   --------------------------------------
	
	// A static image label implementation
	private class _ImageLabel(override val parentHierarchy: ComponentHierarchy, image: Image,
	                          transformation: Option[Matrix2D] = None,
	                          override val insets: StackInsets = StackInsets.zero, alignment: Alignment = Alignment.Center,
	                          additionalCustomDrawers: Seq[CustomDrawer] = Empty, override val allowUpscaling: Boolean = true,
	                          override val useLowPrioritySize: Boolean = false)
		extends CustomDrawReachComponent with ImageLabel
	{
		// ATTRIBUTES	------------------------------
		
		override val customDrawers = ImageDrawer
			.copy(insets = insets, alignment = alignment, transformation = transformation, upscales = allowUpscaling)
			.apply(image * imageScaling) +:
			additionalCustomDrawers
		
		override val visualImageSize: Size = transformation match {
			case Some(t) => (image.bounds * t).size
			case None => image.size
		}
		
		
		// IMPLEMENTED	------------------------------
		
		override def updateLayout() = ()
		
		override def imageScaling: Vector2D = image.scaling
	}
}

case class ImageLabelFactory(parentHierarchy: ComponentHierarchy,
                              settings: ImageLabelSettings = ImageLabelSettings.default,
                              allowsUpscaling: Boolean = false)
	extends ImageLabelFactoryLike[ImageLabelFactory, ViewImageLabelFactory] with BackgroundAssignable[ImageLabelFactory]
		with FromContextFactory[ColorContext, ContextualImageLabelFactory]
{
	// IMPLEMENTED  ----------------------------
	
	override def self: ImageLabelFactory = this
	
	override def toViewFactory: ViewImageLabelFactory =
		ViewImageLabelFactory(parentHierarchy, settings.toViewSettings,
			allowUpscalingPointer = Fixed(allowsUpscaling))
	
	override def *(mod: Double): ImageLabelFactory = withImageScaledBy(mod).withInsetsScaledBy(mod)
	
	override def withSettings(settings: ImageLabelSettings): ImageLabelFactory = copy(settings = settings)
	
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
	extends ImageLabelFactoryLike[ContextualImageLabelFactory, ContextualViewImageLabelFactory]
		with ColorContextualFactory[ContextualImageLabelFactory]
		with ContextualBackgroundAssignableFactory[ColorContext, ContextualImageLabelFactory]
		with ContextualFramedFactory[ContextualImageLabelFactory]
{
	// IMPLEMENTED  ----------------------------
	
	override def self: ContextualImageLabelFactory = this
	
	override protected def allowsUpscaling: Boolean = context.allowImageUpscaling
	
	override def toViewFactory: ContextualViewImageLabelFactory =
		ContextualViewImageLabelFactory(parentHierarchy, Fixed(context), settings.toViewSettings)
	
	override def withSettings(settings: ImageLabelSettings): ContextualImageLabelFactory =
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
	
	override def identity: ImageLabelSetup = this
	
	override def withSettings(settings: ImageLabelSettings): ImageLabelSetup = copy(settings = settings)
	
	override def apply(hierarchy: ComponentHierarchy) = ImageLabelFactory(hierarchy, settings)
}

object ImageLabel extends ComponentFactoryFactory[ImageLabelFactory] with ImageLabelSettingsWrapper[ImageLabelSetup]
{
	override protected def settings: ImageLabelSettings = ImageLabelSettings.default
	override def identity: ImageLabelSetup = ImageLabelSetup(settings)
	
	override def withSettings(settings: ImageLabelSettings): ImageLabelSetup = ImageLabelSetup(settings)
	
	override def apply(hierarchy: ComponentHierarchy) = ImageLabelFactory(hierarchy)
}

/**
  * A common trait for image label implementations
  * @author Mikko Hilpinen
  * @since 27.10.2020, v0.1
  */
trait ImageLabel extends ReachComponentLike with ImageComponent