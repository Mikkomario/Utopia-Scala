package utopia.reach.component.label.image

import utopia.firmament.context.color.{ColorContextPropsView, VariableColorContext}
import utopia.firmament.drawing.immutable.BackgroundDrawer
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.drawing.view.ViewImageDrawer
import utopia.firmament.factory.FramedFactory
import utopia.firmament.image.SingleColorIcon
import utopia.firmament.model.enumeration.SizeCategory
import utopia.firmament.model.stack.{StackInsets, StackInsetsConvertible}
import utopia.flow.collection.immutable.Empty
import utopia.flow.collection.immutable.caching.cache.{WeakKeysCache, WeakValuesCache}
import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeResponse.Continue
import utopia.flow.operator.Identity
import utopia.flow.operator.combine.LinearScalable
import utopia.flow.util.EitherExtensions._
import utopia.flow.util.Mutate
import utopia.flow.view.immutable.eventful.{AlwaysFalse, AlwaysTrue, Fixed}
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.graphics.Priority
import utopia.genesis.image.{Image, ImageView}
import utopia.paradigm.color.{Color, ColorRole, ColorSet}
import utopia.paradigm.enumeration.{Alignment, Axis2D}
import utopia.paradigm.shape.shape2d.Matrix2D
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.paradigm.transform.LinearSizeAdjustable
import utopia.reach.component.factory.ComponentFactories.CF
import utopia.reach.component.factory.ContextualComponentFactories.CCF
import utopia.reach.component.factory.contextual.{VariableBackgroundRoleAssignableFactory, VariableColorContextualFactory}
import utopia.reach.component.factory.{BackgroundAssignable, FromContextFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{ConcreteCustomDrawReachComponent, PartOfComponentHierarchy}

/**
  * Common trait for view image label factories and settings
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 30.05.2023, v1.1
  */
trait ViewImageLabelSettingsLike[+Repr] extends ImageLabelSettingsLike[Repr] with LinearScalable[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * A pointer that, if specified, overrides the image size for the purposes of component layout.
	  * 'imageScalingPointer' and 'insetsPointer' will be applied after this effect.
	  */
	def customSizePointer: Option[Changing[Size]]
	/**
	  * Pointer that determines the insets placed around the image
	  */
	def insetsPointer: Changing[StackInsets]
	/**
	  * Pointer that determines the image drawing location within this component
	  */
	def alignmentPointer: Changing[Alignment]
	/**
	  * Pointer that, when defined, places a color overlay over the drawn image
	  */
	def colorOverlayPointer: Option[Changing[Color]]
	/**
	  * Pointer that determines image scaling, in addition to the original image scaling
	  */
	def imageScalingPointer: Changing[Double]
	/**
	  * @return Pointer that determines image transformation to apply.
	  *         Contains None if no transformation should be applied.
	  */
	def transformationPointer: Changing[Option[Matrix2D]]
	/**
	  * A priority used when requesting label repaints.
	  * Note: Doesn't affect updates involving label size changes.
	  */
	def repaintPriority: Priority
	/**
	  * Whether this label should shrink when the drawn image shrinks
	  */
	def allowsShrinking: Boolean
	
	/**
	  * Pointer that determines the image drawing location within this component
	  * @return Copy of this factory with the specified alignment pointer
	  */
	def withAlignmentPointer(p: Changing[Alignment]): Repr
	/**
	  * Pointer that, when defined, places a color overlay over the drawn image
	  * @return Copy of this factory with the specified color overlay pointer
	  */
	def withColorOverlayPointer(p: Option[Changing[Color]]): Repr
	/**
	  * A pointer that, if specified, overrides the image size for the purposes of component layout.
	  * 'imageScalingPointer' and 'insetsPointer' will be applied after this effect.
	  * @param p New custom size pointer to use.
	  *          A pointer that, if specified, overrides the image size for the purposes of component
	  *          layout.
	  *          'imageScalingPointer' and 'insetsPointer' will be applied after this effect.
	  * @return Copy of this factory with the specified custom size pointer
	  */
	def withCustomSizePointer(p: Option[Changing[Size]]): Repr
	/**
	  * Pointer that determines image scaling, in addition to the original image scaling
	  * @return Copy of this factory with the specified image scaling pointer
	  */
	def withImageScalingPointer(p: Changing[Double]): Repr
	/**
	  * Pointer that determines the insets placed around the image
	  * @return Copy of this factory with the specified insets pointer
	  */
	def withInsetsPointer(p: Changing[StackInsets]): Repr
	/**
	  * @param p New Transformation pointer to assign
	  * @return Cop of this factory with the specified transformation pointer
	  */
	def withTransformationPointer(p: Changing[Option[Matrix2D]]): Repr
	/**
	  * A priority used when requesting label repaints.
	  * Note: Doesn't affect updates involving label size changes.
	  * @param priority New repaint priority to use.
	  *                 A priority used when requesting label repaints.
	  *                 Note: Doesn't affect updates involving label size changes.
	  * @return Copy of this factory with the specified repaint priority
	  */
	def withRepaintPriority(priority: Priority): Repr
	/**
	  * Whether this label should shrink when the drawn image shrinks
	  * @param allowShrink New allows shrinking to use.
	  *                    Whether this label should shrink when the drawn image shrinks
	  * @return Copy of this factory with the specified allows shrinking
	  */
	def withAllowShrinking(allowShrink: Boolean): Repr
	
	
	// COMPUTED ------------------------
	
	/**
	  * @return A copy of this factory that allows the created labels to shrink in (optimal image) size
	  */
	def allowingShrinking = withAllowShrinking(true)
	/**
	  * @return A copy of this factory that creates labels that never shrink in their (optimal) image size
	  */
	def notShrinking = withAllowShrinking(false)
	
	/**
	  * @return A copy of this factory that applies a higher repaint priority
	  */
	def faster = mapRepaintPriority { _.more }
	/**
	  * @return A copy of this factory that applies a much higher repaint priority
	  */
	def muchFaster = mapRepaintPriority { _.step(2) }
	/**
	  * @return A copy of this factory that applies a lower repaint priority
	  */
	def slower = mapRepaintPriority { _.less }
	/**
	  * @return A copy of this factory that applies a much lower repaint priority
	  */
	def muchSlower = mapRepaintPriority { _.step(-2) }
	
	/**
	  * @return Unchanging copy of these settings, which may be used for constructing immutable image labels.
	  */
	def toImageLabelSettings = ImageLabelSettings.from(this)
	
	
	// IMPLEMENTED	--------------------
	
	override def identity: Repr = self
	
	override def alignment: Alignment = alignmentPointer.value
	override def colorOverlay = colorOverlayPointer.map { _.value }
	override def imageScaling: Double = imageScalingPointer.value
	override def insets: StackInsets = insetsPointer.value
	override def transformation: Option[Matrix2D] = transformationPointer.value
	
	override def apply(alignment: Alignment): Repr = withAlignmentPointer(Fixed(alignment))
	
	override def mapInsets(f: StackInsets => StackInsetsConvertible) =
		withInsetsPointer(insetsPointer.map { f(_).toInsets })
	override def mapTransformation(f: Mutate[Option[Matrix2D]]) = mapTransformationPointer { _.map(f) }
	
	override def withColor(color: Option[Color]): Repr = withColorOverlayPointer(color.map(Fixed.apply))
	override def withImageScaling(scaling: Double): Repr = withImageScalingPointer(Fixed(scaling))
	override def withInsets(insets: StackInsetsConvertible): Repr = withInsetsPointer(Fixed(insets.toInsets))
	override def withTransformation(transformation: Option[Matrix2D]): Repr =
		withTransformationPointer(Fixed(transformation))
	
	
	// OTHER	--------------------
	
	def withCustomSizePointer(pointer: Changing[Size]): Repr = withCustomSizePointer(Some(pointer))
	def withCustomSize(size: Size) = withCustomSizePointer(Fixed(size))
	
	def withColorOverlayPointer(pointer: Changing[Color]): Repr = withColorOverlayPointer(Some(pointer))
	def withColor(color: Changing[Color]): Repr = withColorOverlayPointer(Some(color))
	
	def mapAlignmentPointer(f: Changing[Alignment] => Changing[Alignment]) =
		withAlignmentPointer(f(alignmentPointer))
	def mapColorOverlayPointer(f: Option[Changing[Color]] => Option[Changing[Color]]) =
		withColorOverlayPointer(f(colorOverlayPointer))
	def mapImageScalingPointer(f: Changing[Double] => Changing[Double]) =
		withImageScalingPointer(f(imageScalingPointer))
	def mapInsetsPointer(f: Changing[StackInsets] => Changing[StackInsets]) = withInsetsPointer(f(insetsPointer))
	def mapTransformationPointer(f: Mutate[Changing[Option[Matrix2D]]]) =
		withTransformationPointer(f(transformationPointer))
	def mapRepaintPriority(f: Mutate[Priority]) = withRepaintPriority(f(repaintPriority))
}

object ViewImageLabelSettings
{
	// ATTRIBUTES   --------------------------
	
	val default = apply()
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param staticSettings Fixed image settings
	  * @return View image label settings based on those settings
	  */
	def apply(staticSettings: ImageLabelSettings): ViewImageLabelSettings =
		apply(staticSettings.customDrawers, Fixed(staticSettings.insets), Fixed(staticSettings.alignment),
			staticSettings.colorOverlay.map { Fixed(_) },
			imageScalingPointer = Fixed(staticSettings.imageScaling),
			transformationPointer = Fixed(staticSettings.transformation),
			usesLowPrioritySize = staticSettings.usesLowPrioritySize)
}
/**
  * Combined settings used when constructing view image labels
  * @param insetsPointer       Pointer that determines the insets placed around the image
  * @param alignmentPointer    Pointer that determines the image drawing location within this component
  * @param colorOverlayPointer Pointer that, when defined, places a color overlay over the drawn image
  * @param imageScalingPointer Pointer that determines image scaling,
  *                                                        in addition to the original image scaling
  * @param usesLowPrioritySize Whether this label should use low priority size constraints
  * @author Mikko Hilpinen
  * @since 30.05.2023, v1.1
  */
case class ViewImageLabelSettings(customDrawers: Seq[CustomDrawer] = Empty,
                                  insetsPointer: Changing[StackInsets] = Fixed(StackInsets.any),
                                  alignmentPointer: Changing[Alignment] = Fixed(Alignment.Center),
                                  colorOverlayPointer: Option[Changing[Color]] = None,
                                  customSizePointer: Option[Changing[Size]] = None,
                                  imageScalingPointer: Changing[Double] = Fixed(1.0),
                                  repaintPriority: Priority = Priority.Normal,
                                  transformationPointer: Changing[Option[Matrix2D]] = Fixed.never,
                                  usesLowPrioritySize: Boolean = false, allowsShrinking: Boolean = true)
	extends ViewImageLabelSettingsLike[ViewImageLabelSettings]
{
	// IMPLEMENTED	--------------------
	
	override def self: ViewImageLabelSettings = this
	
	override def withTransformationPointer(p: Changing[Option[Matrix2D]]): ViewImageLabelSettings =
		copy(transformationPointer = p)
	override def withAlignmentPointer(p: Changing[Alignment]): ViewImageLabelSettings = copy(alignmentPointer = p)
	override def withColorOverlayPointer(p: Option[Changing[Color]]): ViewImageLabelSettings =
		copy(colorOverlayPointer = p)
	override def withCustomDrawers(drawers: Seq[CustomDrawer]): ViewImageLabelSettings =
		copy(customDrawers = drawers)
	override def withImageScalingPointer(p: Changing[Double]): ViewImageLabelSettings =
		copy(imageScalingPointer = p)
	override def withInsetsPointer(p: Changing[StackInsets]): ViewImageLabelSettings = copy(insetsPointer = p)
	override def withUseLowPrioritySize(lowPriority: Boolean): ViewImageLabelSettings =
		copy(usesLowPrioritySize = lowPriority)
	override def withCustomSizePointer(p: Option[Changing[Size]]) = copy(customSizePointer = p)
	override def withRepaintPriority(priority: Priority) = copy(repaintPriority = priority)
	override def withAllowShrinking(allowShrink: Boolean) = copy(allowsShrinking = allowShrink)
	
	override def *(mod: Double): ViewImageLabelSettings =
		copy(insetsPointer = insetsPointer.map { _ * mod }, imageScalingPointer = imageScalingPointer.map { _ * mod })
}

/**
  * Common trait for factories that wrap a view image label settings instance
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 30.05.2023, v1.1
  */
trait ViewImageLabelSettingsWrapper[+Repr] extends ViewImageLabelSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Settings wrapped by this instance
	  */
	protected def settings: ViewImageLabelSettings
	/**
	  * @return Copy of this factory with the specified settings
	  */
	def withSettings(settings: ViewImageLabelSettings): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def alignmentPointer: Changing[Alignment] = settings.alignmentPointer
	override def colorOverlayPointer: Option[Changing[Color]] = settings.colorOverlayPointer
	override def customDrawers: Seq[CustomDrawer] = settings.customDrawers
	override def imageScalingPointer: Changing[Double] = settings.imageScalingPointer
	override def insetsPointer: Changing[StackInsets] = settings.insetsPointer
	override def usesLowPrioritySize: Boolean = settings.usesLowPrioritySize
	override def transformationPointer: Changing[Option[Matrix2D]] = settings.transformationPointer
	override def customSizePointer: Option[Changing[Size]] = settings.customSizePointer
	override def repaintPriority: Priority = settings.repaintPriority
	override def allowsShrinking: Boolean = settings.allowsShrinking
	
	
	override def withCustomSizePointer(p: Option[Changing[Size]]): Repr = mapSettings { _.withCustomSizePointer(p) }
	override def withRepaintPriority(priority: Priority): Repr = mapSettings { _.withRepaintPriority(priority) }
	override def withAllowShrinking(allowShrink: Boolean): Repr = mapSettings { _.withAllowShrinking(allowShrink) }
	override def withTransformationPointer(p: Changing[Option[Matrix2D]]): Repr =
		mapSettings { _.withTransformationPointer(p) }
	override def withAlignmentPointer(p: Changing[Alignment]): Repr =
		mapSettings { _.withAlignmentPointer(p) }
	override def withColorOverlayPointer(p: Option[Changing[Color]]): Repr =
		mapSettings { _.withColorOverlayPointer(p) }
	override def withCustomDrawers(drawers: Seq[CustomDrawer]): Repr =
		mapSettings { _.withCustomDrawers(drawers) }
	override def withImageScalingPointer(p: Changing[Double]): Repr =
		mapSettings { _.withImageScalingPointer(p) }
	override def withInsetsPointer(p: Changing[StackInsets]): Repr = mapSettings { _.withInsetsPointer(p) }
	override def withUseLowPrioritySize(lowPriority: Boolean): Repr =
		mapSettings { _.withUseLowPrioritySize(lowPriority) }
	
	
	// OTHER	--------------------
	
	def mapSettings(f: ViewImageLabelSettings => ViewImageLabelSettings) = withSettings(f(settings))
	
	def withSettings(settings: ImageLabelSettings): Repr = withSettings(ViewImageLabelSettings(settings))
}

/**
  * Common trait for factories that are used for constructing view image labels
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 30.05.2023, v1.1
  */
trait ViewImageLabelFactoryLike[+Repr]
	extends ViewImageLabelSettingsWrapper[Repr] with FramedFactory[Repr] with LinearSizeAdjustable[Repr]
		with PartOfComponentHierarchy
{
	// ABSTRACT ------------------------------
	
	protected def allowUpscalingFlag: Changing[Boolean]
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param imagePointer A pointer to an image
	  * @return A label that displays image from the specified pointer
	  */
	def apply(imagePointer: Changing[Image]) = _imageOrIcon(Right(imagePointer))
	/**
	  * @param image An image
	  * @return A label that displays the specified static image
	  */
	def apply(image: Image): ViewImageLabel = apply(Fixed(image))
	
	/**
	  * Creates a new image label that is either based on contextual icons or simply images
	  * @param pointer Either a icon or an image pointer.
	  *                The icon pointer, if present, must be companied by a context pointer.
	  * @return A new view image label where appropriate image-altering functions have been applied
	  */
	protected def _imageOrIcon(pointer: Either[(Changing[SingleColorIcon], ColorContextPropsView), Changing[Image]]) = {
		// Applies to color overlay, if applicable
		val coloredPointer = colorOverlayPointer match {
			// Case: Color overlay used
			case Some(p) =>
				pointer match {
					// Case: Using icons => Applies the color overlay over the icon
					case Left((iconPointer, _)) => iconPointer.mergeWith(p) { _(_) }
					// Case: Using images => Applies color overlay over the image (optimized)
					case Right(imagePointer) =>
						// Case: Variables are involved => Caches color overlay results in a separate weak cache
						if (imagePointer.mayChange || p.mayChange) {
							val cache = WeakKeysCache { img: Image =>
								WeakValuesCache { color: Color => img.withColorOverlay(color) }
							}
							imagePointer.mergeWith(p) { (image, color) => cache(image)(color) }
						}
						// Case: Static image & color => Uses a static pointer
						else
							Fixed(imagePointer.value.withColorOverlay(p.value))
				}
			// Case: No color overlay => If using icons, colors according to the context background
			case None =>
				pointer.rightOrMap { case (iconPointer, context) =>
					iconPointer.flatMap { _.variableContextual(context) }
				}
		}
		// Applies image scaling, also
		val scaledImagePointer = coloredPointer.mergeWith(imageScalingPointer) { _ * _ }
		new ViewImageLabel(hierarchy, scaledImagePointer, settings, allowUpscalingFlag)
	}
}

/**
  * Factory class used for constructing view image labels using contextual component creation information
  * @author Mikko Hilpinen
  * @since 30.05.2023, v1.1
  */
case class ContextualViewImageLabelFactory(hierarchy: ComponentHierarchy, context: VariableColorContext,
                                           settings: ViewImageLabelSettings = ViewImageLabelSettings.default,
                                           drawBackground: Boolean = false)
	extends ViewImageLabelFactoryLike[ContextualViewImageLabelFactory]
		with VariableBackgroundRoleAssignableFactory[VariableColorContext, ContextualViewImageLabelFactory]
		with VariableColorContextualFactory[ContextualViewImageLabelFactory]
{
	// IMPLEMENTED  ---------------------------
	
	override def self: ContextualViewImageLabelFactory = this
	
	override protected def allowUpscalingFlag: Changing[Boolean] = context.allowImageUpscalingFlag
	
	override def withContext(c: VariableColorContext): ContextualViewImageLabelFactory = copy(context = c)
	override def withSettings(settings: ViewImageLabelSettings): ContextualViewImageLabelFactory =
		copy(settings = settings)
	
	override protected def withVariableBackgroundContext(newContext: VariableColorContext,
	                                                     backgroundDrawer: CustomDrawer): ContextualViewImageLabelFactory =
		copy(context = newContext,
			settings = settings.withCustomDrawers(backgroundDrawer +: settings.customDrawers), drawBackground = true)
	
	override def *(mod: Double): ContextualViewImageLabelFactory =
		copy(context = context * mod, settings = settings * mod)
	
	override def apply(imagePointer: Changing[Image]) = iconOrImagePointer(Right(imagePointer))
	
	
	// OTHER    ------------------------------
	
	def withInsetSizePointer(sizePointer: Changing[SizeCategory]) =
		withInsetsPointer(sizePointer.flatMap { context.scaledStackMarginPointer(_).map { _.toInsets } })
	
	def withColor(color: ColorSet): ContextualViewImageLabelFactory =
		withColorOverlayPointer(context.colorPointer(color))
	def withColor(color: ColorRole): ContextualViewImageLabelFactory =
		withColorOverlayPointer(context.colorPointer(color))
	def withColorPointer(colorPointer: Changing[ColorRole]) =
		withColorOverlayPointer(colorPointer.flatMap(context.colorPointer.apply))
	
	/**
	  * @param pointer Either
	  *                     - Left: An icon pointer, or
	  *                     - Right: An image pointer
	  * @return A label that displays the specified icon or image
	  */
	def iconOrImagePointer(pointer: Either[Changing[SingleColorIcon], Changing[Image]]) = {
		val label = _imageOrIcon(pointer.mapLeft { _ -> context })
		// If background drawing is enabled, repaints when background color changes
		if (drawBackground)
			context.backgroundPointer.addContinuousAnyChangeListener { label.repaint() }
		label
	}
	/**
	  * @param i An icon (Left), or an image (Right)
	  * @return A label that displays the specified icon or image
	  */
	def iconOrImage(i: Either[SingleColorIcon, Image]) =
		iconOrImagePointer(i.mapBoth { Fixed(_) } { Fixed(_) })
	
	/**
	  * @param iconPointer A pointer to the icon to display
	  * @return A new image label that displays the specified icon
	  */
	def iconPointer(iconPointer: Changing[SingleColorIcon]) = iconOrImagePointer(Left(iconPointer))
	/**
	  * @param icon The icon to display
	  * @return A new image label that displays the specified icon
	  */
	def icon(icon: SingleColorIcon) = iconPointer(Fixed(icon))
}

/**
  * Factory class that is used for constructing view image labels without using contextual information
  * @param allowUpscalingFlag Pointer that determines whether drawn images should be allowed to scale
  *                               beyond their source resolution
  * @author Mikko Hilpinen
  * @since 30.05.2023, v1.1
  */
case class ViewImageLabelFactory(hierarchy: ComponentHierarchy,
                                 settings: ViewImageLabelSettings = ViewImageLabelSettings.default,
                                 allowUpscalingFlag: Changing[Boolean] = AlwaysFalse)
	extends ViewImageLabelFactoryLike[ViewImageLabelFactory] with BackgroundAssignable[ViewImageLabelFactory]
		with FromContextFactory[VariableColorContext, ContextualViewImageLabelFactory]
{
	// COMPUTED ------------------------
	
	/**
	  * @return Copy of this factory that allows image upscaling
	  */
	def allowingUpscaling = withAllowUpscaling(allow = true)
	
	
	// IMPLEMENTED	--------------------
	
	override def self: ViewImageLabelFactory = this
	
	override def withSettings(settings: ViewImageLabelSettings): ViewImageLabelFactory = copy(settings = settings)
	override def withBackground(background: Color): ViewImageLabelFactory =
		withCustomDrawer(BackgroundDrawer(background))
	override def withContext(context: VariableColorContext) =
		ContextualViewImageLabelFactory(hierarchy, context, settings)
	
	override def *(mod: Double): ViewImageLabelFactory = withInsetsScaledBy(mod).withImageScaledBy(mod)
	
	
	// OTHER	--------------------
	
	/**
	  * @param allow Whether drawn images should be allowed to scale beyond their source resolution
	  * @return Copy of this factory with the specified allows upscaling
	  */
	def withAllowUpscaling(allow: Boolean) = copy(allowUpscalingFlag = Fixed(allow))
}

/**
  * Used for defining view image label creation settings outside of the component building process
  * @author Mikko Hilpinen
  * @since 30.05.2023, v1.1
  */
case class ViewImageLabelSetup(settings: ViewImageLabelSettings = ViewImageLabelSettings.default)
	extends ViewImageLabelSettingsWrapper[ViewImageLabelSetup]
		with CF[ViewImageLabelFactory] with CCF[VariableColorContext, ContextualViewImageLabelFactory]
{
	override def self: ViewImageLabelSetup = this
	override def *(mod: Double): ViewImageLabelSetup = mapSettings { _ * mod }
	
	override def withContext(hierarchy: ComponentHierarchy, context: VariableColorContext): ContextualViewImageLabelFactory =
		ContextualViewImageLabelFactory(hierarchy, context, settings)
	override def withSettings(settings: ViewImageLabelSettings): ViewImageLabelSetup = copy(settings = settings)
	override def apply(hierarchy: ComponentHierarchy) = ViewImageLabelFactory(hierarchy, settings)
}

object ViewImageLabel extends ViewImageLabelSetup()
{
	// OTHER	--------------------
	
	def apply(settings: ViewImageLabelSettings) = withSettings(settings)
}

/**
  * A pointer-based label that draws an image
  * @author Mikko Hilpinen
  * @since 28.10.2020, v0.1
  */
class ViewImageLabel(override val hierarchy: ComponentHierarchy, imageP: Changing[ImageView],
                     settings: ViewImageLabelSettings, allowUpscalingFlag: Changing[Boolean] = AlwaysTrue)
	extends ConcreteCustomDrawReachComponent with ImageLabel
{
	// ATTRIBUTES	---------------------------------
	
	private val localImageP = imageP.viewWhile(hierarchy.linkedFlag)
	private val localTransformationP = settings.transformationPointer.viewWhile(hierarchy.linkedFlag)
	/**
	  * A pointer that caches the size of the drawn image, including the effects of a possible transformation.
	  * Takes into account, whether image size is allowed to decrease.
	  */
	private val visualImageSizeP: Changing[Size] = {
		val allowShrink = settings.allowsShrinking
		settings.customSizePointer match {
			// Case: Applies a custom size => Applies the "no shrinking" constraint, if appropriate
			case Some(customSizeP) =>
				if (allowShrink || customSizeP.isFixed) customSizeP else noShrinking(customSizeP.viewWhile(linkedFlag))
			
			// Case: No custom size => Calculates image size based on image bounds & applied transform,
			//                         applying "no shrinking" constraint, if appropriate
			// When applying "no shrinking", uses strongly mapped pointers,
			// because the constraint's incrementalMap function is not optimized.
			case None =>
				localTransformationP.fixedValue match {
					// Case: Never transforms => Uses image size
					case Some(None) =>
						if (allowShrink)
							localImageP.lightMap { _.size }
						else
							noShrinking(localImageP.strongMap { _.size })
						
					// Case: Applies a static transformation => Merges image bounds with this transformation
					case Some(Some(t)) =>
						if (allowShrink)
							localImageP.map { _.bounds }.map { b => (b * t).size }
						else
							noShrinking(localImageP.strongMap { _.bounds }.strongMap { b => (b * t).size })
							
					// Case: Applies a variable transformation => Combines image bounds & transformation
					case None =>
						if (allowShrink) {
							val boundsP = localImageP.map { _.bounds }
							boundsP.mergeWith(localTransformationP) { (b, t) =>
								t match {
									case Some(t) => (b * t).size
									case None => b.size
								}
							}
						}
						else {
							val boundsP = localImageP.strongMap { _.bounds }
							val transformedBoundsP = boundsP.strongMergeWith(localTransformationP) { (b, t) =>
								t match {
									case Some(t) => (b * t).size
									case None => b.size
								}
							}
							noShrinking(transformedBoundsP)
						}
				}
		}
	}
	
	override val customDrawers = settings.customDrawers :+
		ViewImageDrawer.copy(transformationView = settings.transformationPointer, insetsPointer = settings.insetsPointer,
			alignmentView = settings.alignmentPointer, upscales = allowUpscaling).apply(localImageP)
	// Repainting is delayed until all change listeners have been informed
	private val repaintListener = ChangeListener.triggerAfterEffect { repaint(settings.repaintPriority) }
	private val revalidateListener = ChangeListener.triggerAfterEffect { revalidate() }
	
	
	// INITIAL CODE	---------------------------------
	
	// Reacts to changes in the pointers
	// Applies either a revalidation or a repaint when image, transformation and/or size changes
	// Case: Using a custom size pointer => Image & transformation -pointers only trigger repaints
	if (settings.customSizePointer.isDefined) {
		localImageP.addListener(repaintListener)
		localTransformationP.addListener(repaintListener)
		visualImageSizeP.addListener(revalidateListener)
	}
	// Case: Not using a custom size -pointer
	//       => Image & transform changes may trigger a repaint or a revalidation
	else {
		localImageP.addListener { change =>
			// Checks whether the image's size is changed, taking the "no shrinking" constraint into account
			val hasSameSize = {
				if (settings.allowsShrinking)
					change.equalsBy { _.size } && change.equalsBy { _.maxScaling }
				// Case: "No shrinking" applied => Only checks for size increases
				else {
					val sizes = change.values.map { _.size }
					Axis2D.values.forall { axis => sizes.mapAndMerge { _(axis) } { _ >= _ } } &&
						change.values.mapAndMerge { _.maxScaling } { (before, after) =>
							before.forall { before => after.exists { _ <= before } }
						}
				}
			}
			// Case: Size stayed the same => Only repaints
			if (hasSameSize)
				Continue.and { repaint(settings.repaintPriority) }
			// Case: Size changed => Revalidates the component hierarchy
			else
				Continue.and { revalidate() }
		}
		// Case: Shrinking allowed => Transformation changes are assumed to always trigger size changes
		if (settings.allowsShrinking)
			localTransformationP.addListener(revalidateListener)
		// Case: Shrinking not allowed
		//       => Handles transformation changes with repaint and only revalidates
		//          when the size is confirmed to change
		//          (Has the possible side effect of repainting twice)
		else {
			localTransformationP.addListener(repaintListener)
			visualImageSizeP.addListener(revalidateListener)
		}
	}
	settings.insetsPointer.addListenerWhile(hierarchy.linkedFlag)(revalidateListener)
	settings.alignmentPointer.addListenerWhile(hierarchy.linkedFlag)(repaintListener)
	allowUpscalingFlag.addListenerWhile(hierarchy.linkedFlag)(revalidateListener)
	
	
	// COMPUTED	-------------------------------------
	
	/**
	  * @return Current alignment used when positioning the image in this label
	  */
	def alignment = settings.alignmentPointer.value
	
	
	// IMPLEMENTED	---------------------------------
	
	override def useLowPrioritySize: Boolean = settings.usesLowPrioritySize
	
	override def visualImageSize: Size = visualImageSizeP.value
	override def maxScaling = localImageP.value.maxScaling
	
	override def insets = settings.insetsPointer.value
	override def allowUpscaling: Boolean = allowUpscalingFlag.value
	
	override def updateLayout() = ()
	
	
	// OTHER    -------------------------------------
	
	/**
	  * Applies a "no shrinkin" effect to a size pointer
	  * @param sizePointer Size pointer to map
	  * @return A mapping of the specified pointer, which never decreases in width or height
	  */
	private def noShrinking(sizePointer: Changing[Size]) =
		sizePointer.incrementalMap(Identity) { (oldSize, change) => oldSize bottomRight change.newValue }
}
