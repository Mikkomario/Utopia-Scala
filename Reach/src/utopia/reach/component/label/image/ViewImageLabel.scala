package utopia.reach.component.label.image

import utopia.firmament.context.ColorContext
import utopia.firmament.drawing.immutable.BackgroundDrawer
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.drawing.view.ImageViewDrawer
import utopia.firmament.image.SingleColorIcon
import utopia.firmament.model.enumeration.SizeCategory
import utopia.firmament.model.stack.{StackInsets, StackInsetsConvertible}
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.image.Image
import utopia.paradigm.color.{Color, ColorRole, ColorSet}
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.enumeration.Alignment.Center
import utopia.paradigm.transform.SizeAdjustable
import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.factory.contextual.{ColorContextualFactory, ContextualBackgroundAssignableFactory, ContextualFramedFactory}
import utopia.reach.component.factory.{BackgroundAssignable, FramedFactory, FromContextFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.CustomDrawReachComponent

trait ViewImageLabelSettingsLike[+Repr] extends ImageLabelSettingsLike[Repr]
{
	// ABSTRACT ---------------------
	
	protected def insetsPointer: Changing[StackInsets]
	protected def alignmentPointer: Changing[Alignment]
	protected def colorOverlayPointer: Option[Changing[Color]]
	protected def imageScalingPointer: Changing[Double]
	
	def withInsetsPointer(p: Changing[StackInsets]): Repr
	def withAlignmentPointer(p: Changing[Alignment]): Repr
	def withColorOverlayPointer(p: Option[Changing[Color]]): Repr
	def withImageScalingPointer(p: Changing[Double]): Repr
	
	
	// IMPLEMENTED  ----------------
	
	override protected def insets: StackInsets = insetsPointer.value
	override protected def imageScaling: Double = imageScalingPointer.value
	override protected def alignment: Alignment = alignmentPointer.value
	override protected def colorOverlay = colorOverlayPointer.map { _.value }
	
	override def apply(alignment: Alignment): Repr = withAlignmentPointer(Fixed(alignment))
	override def withImageScaling(scaling: Double): Repr = withImageScalingPointer(Fixed(scaling))
	override def withColor(color: Option[Color]): Repr = withColorOverlayPointer(color.map(Fixed.apply))
	
	override def withInsets(insets: StackInsetsConvertible): Repr = withInsetsPointer(Fixed(insets.toInsets))
	override def mapInsets(f: StackInsets => StackInsetsConvertible) =
		withInsetsPointer(insetsPointer.map { f(_).toInsets })
	
	
	// OTHER    --------------------
	
	def withColor(color: Changing[Color]): Repr = withColorOverlayPointer(Some(color))
}

object ViewImageLabelSettings
{
	val default = apply()
}
case class ViewImageLabelSettings(insetsPointer: Changing[StackInsets] = Fixed(StackInsets.any),
                                  alignmentPointer: Changing[Alignment] = Fixed(Center),
                                  imageScalingPointer: Changing[Double] = Fixed(1.0),
                                  colorOverlayPointer: Option[Changing[Color]] = None,
                                  customDrawers: Vector[CustomDrawer] = Vector.empty,
                                  usesLowPrioritySize: Boolean = false)
	extends ViewImageLabelSettingsLike[ViewImageLabelSettings]
{
	
	override def withInsetsPointer(p: Changing[StackInsets]): ViewImageLabelSettings = copy(insetsPointer = p)
	override def withAlignmentPointer(p: Changing[Alignment]): ViewImageLabelSettings = copy(alignmentPointer = p)
	override def withColorOverlayPointer(p: Option[Changing[Color]]): ViewImageLabelSettings =
		copy(colorOverlayPointer = p)
	override def withImageScalingPointer(p: Changing[Double]): ViewImageLabelSettings = copy(imageScalingPointer = p)
	override def withUseLowPrioritySize(lowPriority: Boolean): ViewImageLabelSettings =
		copy(usesLowPrioritySize = lowPriority)
	override def withCustomDrawers(drawers: Vector[CustomDrawer]): ViewImageLabelSettings = copy(customDrawers = drawers)
}

trait ViewImageLabelSettingsWrapper[+Repr] extends ViewImageLabelSettingsLike[Repr]
{
	// ABSTRACT ------------------------
	
	protected def settings: ViewImageLabelSettings
	protected def withSettings(settings: ViewImageLabelSettings): Repr
	
	
	// IMPLEMENTED  --------------------
	
	override protected def insetsPointer: Changing[StackInsets] = settings.insetsPointer
	override protected def alignmentPointer: Changing[Alignment] = settings.alignmentPointer
	override protected def colorOverlayPointer: Option[Changing[Color]] = settings.colorOverlayPointer
	override protected def imageScalingPointer: Changing[Double] = settings.imageScalingPointer
	override protected def usesLowPrioritySize: Boolean = settings.usesLowPrioritySize
	override def customDrawers: Vector[CustomDrawer] = settings.customDrawers
	
	override def withInsetsPointer(p: Changing[StackInsets]): Repr = mapSettings { _.withInsetsPointer(p) }
	override def withAlignmentPointer(p: Changing[Alignment]): Repr = mapSettings { _.withAlignmentPointer(p) }
	override def withColorOverlayPointer(p: Option[Changing[Color]]): Repr = mapSettings { _.withColorOverlayPointer(p) }
	override def withImageScalingPointer(p: Changing[Double]): Repr = mapSettings { _.withImageScalingPointer(p) }
	override def withUseLowPrioritySize(lowPriority: Boolean): Repr = mapSettings { _.withUseLowPrioritySize(lowPriority) }
	override def withCustomDrawers(drawers: Vector[CustomDrawer]): Repr = mapSettings { _.withCustomDrawers(drawers) }
	
	
	// OTHER    ----------------------
	
	def mapSettings(f: ViewImageLabelSettings => ViewImageLabelSettings) = withSettings(f(settings))
}

trait ViewImageLabelFactoryLike[+Repr]
	extends ViewImageLabelSettingsWrapper[Repr] with FramedFactory[Repr] with SizeAdjustable[Repr]
{
	// ABSTRACT ------------------------------
	
	protected def parentHierarchy: ComponentHierarchy
	
	protected def allowsUpscaling: Boolean
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param imagePointer A pointer to an image
	  * @return Copy of this label that uses the specified pointer
	  */
	def apply(imagePointer: Changing[Image]) = {
		val scaledImagePointer = imagePointer.mergeWith(imageScalingPointer) { _ * _ }
		val imgPointer = colorOverlayPointer match {
			case Some(p) => scaledImagePointer.mergeWith(p) { _.withColorOverlay(_) }
			case None => scaledImagePointer
		}
		new ViewImageLabel(parentHierarchy, imgPointer, insetsPointer, alignmentPointer, customDrawers,
			allowsUpscaling, usesLowPrioritySize)
	}
	def apply(image: Image): ViewImageLabel = apply(Fixed(image))
}

case class ViewImageLabelFactory(parentHierarchy: ComponentHierarchy,
                                 settings: ViewImageLabelSettings = ViewImageLabelSettings.default,
                                 allowsUpscaling: Boolean = false)
	extends ViewImageLabelFactoryLike[ViewImageLabelFactory] with BackgroundAssignable[ViewImageLabelFactory]
		with FromContextFactory[ColorContext, ContextualViewImageLabelFactory]
{
	override def self: ViewImageLabelFactory = this
	
	override protected def withSettings(settings: ViewImageLabelSettings): ViewImageLabelFactory =
		copy(settings = settings)
	
	override def withBackground(background: Color): ViewImageLabelFactory = withCustomDrawer(BackgroundDrawer(background))
	
	override def withContext(context: ColorContext): ContextualViewImageLabelFactory =
		ContextualViewImageLabelFactory(parentHierarchy, context, settings)
	
	override def *(mod: Double): ViewImageLabelFactory = withInsetsScaledBy(mod).withImageScaledBy(mod)
}

case class ContextualViewImageLabelFactory(parentHierarchy: ComponentHierarchy, context: ColorContext,
                                           settings: ViewImageLabelSettings = ViewImageLabelSettings.default)
	extends ViewImageLabelFactoryLike[ContextualViewImageLabelFactory]
		with ContextualFramedFactory[ContextualViewImageLabelFactory]
		with ContextualBackgroundAssignableFactory[ColorContext, ContextualViewImageLabelFactory]
		with ColorContextualFactory[ContextualViewImageLabelFactory]
{
	// IMPLEMENTED  ---------------------------
	
	override def self: ContextualViewImageLabelFactory = this
	
	override protected def allowsUpscaling: Boolean = context.allowImageUpscaling
	
	override def withContext(context: ColorContext): ContextualViewImageLabelFactory = copy(context = context)
	override protected def withSettings(settings: ViewImageLabelSettings): ContextualViewImageLabelFactory =
		copy(settings = settings)
	
	
	// OTHER    ------------------------------
	
	def withInsetSizePointer(sizePointer: Changing[SizeCategory]) =
		withInsetsPointer(sizePointer.map { context.scaledStackMargin(_).toInsets })
	
	def withColor(color: ColorSet): ContextualViewImageLabelFactory = withColor(context.color(color))
	def withColor(color: ColorRole): ContextualViewImageLabelFactory = withColor(context.color(color))
	def withColorPointer(colorPointer: Changing[ColorRole]) =
		withColor(colorPointer.map(context.color.apply))
	
	/**
	  * @param iconPointer A pointer to the icon to display
	  * @return A new image label that displays the specified icon
	  */
	def iconPointer(iconPointer: Changing[SingleColorIcon]) = {
		implicit val ct: ColorContext = context
		colorOverlayPointer match {
			case Some(p) => withoutColorOverlay(iconPointer.mergeWith(p) { _(_) })
			case None => apply(iconPointer.map { _.contextual })
		}
	}
	/**
	  * @param icon    The icon to display
	  * @return A new image label that displays the specified icon
	  */
	def icon(icon: SingleColorIcon) = iconPointer(Fixed(icon))
	
	def apply(image: Either[Image, SingleColorIcon]): ViewImageLabel = image match {
		case Right(ic) => icon(ic)
		case Left(img) => apply(img)
	}
	def pointer(p: Either[Changing[Image], Changing[SingleColorIcon]]) = p match {
		case Right(p) => iconPointer(p)
		case Left(p) => apply(p)
	}
}

case class ViewImageLabelSetup(settings: ViewImageLabelSettings)
	extends ViewImageLabelSettingsWrapper[ViewImageLabelSetup] with Cff[ViewImageLabelFactory]
{
	override protected def withSettings(settings: ViewImageLabelSettings): ViewImageLabelSetup =
		copy(settings = settings)
	
	override def apply(hierarchy: ComponentHierarchy) = ViewImageLabelFactory(hierarchy, settings)
}

object ViewImageLabel extends ViewImageLabelSetup(ViewImageLabelSettings.default)

/**
  * A pointer-based label that draws an image
  * @author Mikko Hilpinen
  * @since 28.10.2020, v0.1
  */
class ViewImageLabel(override val parentHierarchy: ComponentHierarchy, imagePointer: Changing[Image],
                     insetsPointer: Changing[StackInsets], alignmentPointer: Changing[Alignment],
                     additionalCustomDrawers: Vector[CustomDrawer] = Vector(),
                     override val allowUpscaling: Boolean = true, override val useLowPrioritySize: Boolean = false)
	extends CustomDrawReachComponent with ImageLabel
{
	// ATTRIBUTES	---------------------------------
	
	val customDrawers = ImageViewDrawer(imagePointer, insetsPointer, alignmentPointer, useUpscaling = allowUpscaling) +:
		additionalCustomDrawers
	
	
	// INITIAL CODE	---------------------------------
	
	// Reacts to changes in the pointers
	imagePointer.addContinuousListener { change =>
		if (change.equalsBy { _.size } && change.equalsBy { _.sourceResolution })
			repaint()
		else
			revalidate()
	}
	insetsPointer.addContinuousListener { _ => revalidate() }
	alignmentPointer.addContinuousListener { _ => repaint() }
	
	
	// COMPUTED	-------------------------------------
	
	/**
	  * @return Current alignment used when positioning the image in this label
	  */
	def alignment = alignmentPointer.value
	
	
	// IMPLEMENTED	---------------------------------
	
	override def image = imagePointer.value
	
	override def insets = insetsPointer.value
	
	override def updateLayout() = ()
}
