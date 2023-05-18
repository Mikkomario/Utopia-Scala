package utopia.reach.component.label.image

import utopia.firmament.context.ColorContext
import utopia.firmament.drawing.immutable.{BackgroundDrawer, CustomDrawableFactory}
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.image.Image
import utopia.reach.component.factory.{BackgroundAssignable, ComponentFactoryFactory, FramedFactory, FromContextFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.CustomDrawReachComponent
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.drawing.view.ImageViewDrawer
import utopia.firmament.image.SingleColorIcon
import utopia.paradigm.enumeration.{Alignment, FromAlignmentFactory}
import utopia.firmament.model.stack.{StackInsets, StackInsetsConvertible}
import utopia.paradigm.color.ColorLevel.Standard
import utopia.paradigm.color.{Color, ColorLevel, ColorRole}
import utopia.paradigm.enumeration.Alignment.Center
import utopia.reach.component.factory.contextual.{ColorContextualFactory, ContextualBackgroundAssignableFactory, ContextualFramedFactory}

object ViewImageLabel extends ComponentFactoryFactory[ViewImageLabelFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = ViewImageLabelFactory(hierarchy)
}

trait ViewImageLabelFactoryLike[+Repr] extends CustomDrawableFactory[Repr] with FramedFactory[Repr]
	with FromAlignmentFactory[Repr]
{
	// ABSTRACT ------------------------------
	
	protected def parentHierarchy: ComponentHierarchy
	
	protected def insetsPointer: Changing[StackInsets]
	protected def alignmentPointer: Changing[Alignment]
	protected def allowsUpscaling: Boolean
	protected def usesLowPrioritySize: Boolean
	
	def withInsetsPointer(p: Changing[StackInsets]): Repr
	def withAlignmentPointer(p: Changing[Alignment]): Repr
	// WET WET (from ImageLabelFactoryLike)
	def withAllowsUpscaling(allow: Boolean): Repr
	def withUseLowPrioritySize(lowPriority: Boolean): Repr
	
	
	// COMPUTED ------------------------------
	
	def lowPriority = withUseLowPrioritySize(lowPriority = true)
	def allowingUpscaling = withAllowsUpscaling(allow = true)
	
	
	// IMPLEMENTED  --------------------------
	
	override protected def insets: StackInsets = insetsPointer.value
	
	override def withInsets(insets: StackInsetsConvertible): Repr = withInsetsPointer(Fixed(insets.toInsets))
	override def mapInsets(f: StackInsets => StackInsetsConvertible) =
		withInsetsPointer(insetsPointer.map { f(_).toInsets })
	
	override def apply(alignment: Alignment): Repr = withAlignmentPointer(Fixed(alignment))
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param imagePointer A pointer to an image
	  * @return Copy of this label that uses the specified pointer
	  */
	def apply(imagePointer: Changing[Image]) =
		new ViewImageLabel(parentHierarchy, imagePointer, insetsPointer, alignmentPointer, customDrawers,
			allowsUpscaling, usesLowPrioritySize)
}

case class ViewImageLabelFactory(parentHierarchy: ComponentHierarchy,
                                 insetsPointer: Changing[StackInsets] = Fixed(StackInsets.any),
                                 alignmentPointer: Changing[Alignment] = Fixed(Center),
                                 customDrawers: Vector[CustomDrawer] = Vector.empty,
                                 allowsUpscaling: Boolean = false, usesLowPrioritySize: Boolean = false)
	extends ViewImageLabelFactoryLike[ViewImageLabelFactory] with BackgroundAssignable[ViewImageLabelFactory]
		with FromContextFactory[ColorContext, ContextualViewImageLabelFactory]
{
	override def withInsetsPointer(p: Changing[StackInsets]): ViewImageLabelFactory = copy(insetsPointer = p)
	override def withAlignmentPointer(p: Changing[Alignment]): ViewImageLabelFactory = copy(alignmentPointer = p)
	override def withAllowsUpscaling(allow: Boolean): ViewImageLabelFactory = copy(allowsUpscaling = allow)
	override def withUseLowPrioritySize(lowPriority: Boolean): ViewImageLabelFactory =
		copy(usesLowPrioritySize = lowPriority)
	override def withCustomDrawers(drawers: Vector[CustomDrawer]): ViewImageLabelFactory = copy(customDrawers = drawers)
	
	override def withBackground(background: Color): ViewImageLabelFactory = withCustomDrawer(BackgroundDrawer(background))
	
	override def withContext(context: ColorContext): ContextualViewImageLabelFactory =
		ContextualViewImageLabelFactory(parentHierarchy, context, insetsPointer, alignmentPointer, customDrawers,
			allowsUpscaling, usesLowPrioritySize)
}

case class ContextualViewImageLabelFactory(parentHierarchy: ComponentHierarchy, context: ColorContext,
                                           insetsPointer: Changing[StackInsets] = Fixed(StackInsets.any),
                                           alignmentPointer: Changing[Alignment] = Fixed(Center),
                                           customDrawers: Vector[CustomDrawer] = Vector.empty,
                                           allowsUpscaling: Boolean = false, usesLowPrioritySize: Boolean = false)
	extends ViewImageLabelFactoryLike[ContextualViewImageLabelFactory]
		with ContextualFramedFactory[ContextualViewImageLabelFactory]
		with ContextualBackgroundAssignableFactory[ColorContext, ContextualViewImageLabelFactory]
		with ColorContextualFactory[ContextualViewImageLabelFactory]
{
	// IMPLEMENTED  ---------------------------
	
	override def self: ContextualViewImageLabelFactory = this
	
	override def withInsetsPointer(p: Changing[StackInsets]): ContextualViewImageLabelFactory = copy(insetsPointer = p)
	override def withAlignmentPointer(p: Changing[Alignment]): ContextualViewImageLabelFactory = copy(alignmentPointer = p)
	override def withAllowsUpscaling(allow: Boolean): ContextualViewImageLabelFactory = copy(allowsUpscaling = allow)
	override def withUseLowPrioritySize(lowPriority: Boolean): ContextualViewImageLabelFactory =
		copy(usesLowPrioritySize = lowPriority)
	override def withContext(context: ColorContext): ContextualViewImageLabelFactory = copy(context = context)
	override def withCustomDrawers(drawers: Vector[CustomDrawer]): ContextualViewImageLabelFactory =
		copy(customDrawers = drawers)
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param iconPointer A pointer to the icon to display
	  * @param color Color (role) to use when drawing the icon (optional).
	  *              If not specified, the icon will be drawn black or white.
	  * @param preferredShade Preferred color shade to use, when color has been specified (default = Standard)
	  * @return A new image label that displays the specified icon
	  */
	def iconPointer(iconPointer: Changing[SingleColorIcon], color: Option[ColorRole] = None,
	             preferredShade: ColorLevel = Standard) =
	{
		implicit val ct: ColorContext = context
		apply(color match {
			case Some(c) => iconPointer.map { _.apply(c, preferredShade) }
			case None => iconPointer.map { _.contextual }
		})
	}
	/**
	  * @param icon    The icon to display
	  * @param color   Color (role) to use when drawing the icon (optional).
	  *                If not specified, the icon will be drawn black or white.
	  * @param preferredShade Preferred color shade to use, when color has been specified (default = Standard)
	  * @return A new image label that displays the specified icon
	  */
	def icon(icon: SingleColorIcon, color: Option[ColorRole] = None, preferredShade: ColorLevel = Standard) =
		iconPointer(Fixed(icon), color, preferredShade)
}

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
