package utopia.reach.component.label.image

import utopia.firmament.context.ColorContext
import utopia.firmament.drawing.immutable.BackgroundDrawer
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.drawing.view.ImageViewDrawer
import utopia.firmament.image.SingleColorIcon
import utopia.firmament.model.enumeration.SizeCategory
import utopia.firmament.model.stack.{StackInsets, StackInsetsConvertible}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.caching.cache.WeakCache
import utopia.flow.operator.LinearScalable
import utopia.flow.view.immutable.eventful.{AlwaysFalse, AlwaysTrue, Fixed}
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.image.Image
import utopia.paradigm.color.{Color, ColorRole, ColorSet}
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.transform.SizeAdjustable
import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.factory.contextual.VariableBackgroundRoleAssignableFactory
import utopia.reach.component.factory.{BackgroundAssignable, FramedFactory, FromVariableContextComponentFactoryFactory, FromVariableContextFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.CustomDrawReachComponent

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
	  * Pointer that determines the insets placed around the image
	  */
	protected def insetsPointer: Changing[StackInsets]
	/**
	  * Pointer that determines the image drawing location within this component
	  */
	protected def alignmentPointer: Changing[Alignment]
	/**
	  * Pointer that, when defined, places a color overlay over the drawn image
	  */
	protected def colorOverlayPointer: Option[Changing[Color]]
	/**
	  * Pointer that determines image scaling, in addition to the original image scaling
	  */
	protected def imageScalingPointer: Changing[Double]
	
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
	  * Pointer that determines image scaling, in addition to the original image scaling
	  * @return Copy of this factory with the specified image scaling pointer
	  */
	def withImageScalingPointer(p: Changing[Double]): Repr
	/**
	  * Pointer that determines the insets placed around the image
	  * @return Copy of this factory with the specified insets pointer
	  */
	def withInsetsPointer(p: Changing[StackInsets]): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def alignment: Alignment = alignmentPointer.value
	override def colorOverlay = colorOverlayPointer.map { _.value }
	override def imageScaling: Double = imageScalingPointer.value
	override def insets: StackInsets = insetsPointer.value
	
	override def apply(alignment: Alignment): Repr = withAlignmentPointer(Fixed(alignment))
	
	override def mapInsets(f: StackInsets => StackInsetsConvertible) =
		withInsetsPointer(insetsPointer.map { f(_).toInsets })
	
	override def withColor(color: Option[Color]): Repr = withColorOverlayPointer(color.map(Fixed.apply))
	override def withImageScaling(scaling: Double): Repr = withImageScalingPointer(Fixed(scaling))
	override def withInsets(insets: StackInsetsConvertible): Repr = withInsetsPointer(Fixed(insets.toInsets))
	
	
	// OTHER	--------------------
	
	def mapAlignmentPointer(f: Changing[Alignment] => Changing[Alignment]) =
		withAlignmentPointer(f(alignmentPointer))
	def mapColorOverlayPointer(f: Option[Changing[Color]] => Option[Changing[Color]]) =
		withColorOverlayPointer(f(colorOverlayPointer))
	def mapImageScalingPointer(f: Changing[Double] => Changing[Double]) =
		withImageScalingPointer(f(imageScalingPointer))
	def mapInsetsPointer(f: Changing[StackInsets] => Changing[StackInsets]) = withInsetsPointer(f(insetsPointer))
	
	def withColorOverlayPointer(pointer: Changing[Color]): Repr = withColorOverlayPointer(Some(pointer))
	def withColor(color: Changing[Color]): Repr = withColorOverlayPointer(Some(color))
}

object ViewImageLabelSettings
{
	val default = apply()
	
	/**
	  * @param staticSettings Fixed image settings
	  * @return View image label settings based on those settings
	  */
	def apply(staticSettings: ImageLabelSettings): ViewImageLabelSettings =
		apply(staticSettings.customDrawers, Fixed(staticSettings.insets), Fixed(staticSettings.alignment),
			staticSettings.colorOverlay.map { Fixed(_) }, Fixed(staticSettings.imageScaling),
			staticSettings.usesLowPrioritySize)
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
case class ViewImageLabelSettings(customDrawers: Vector[CustomDrawer] = Vector.empty,
                                  insetsPointer: Changing[StackInsets] = Fixed(StackInsets.any),
                                  alignmentPointer: Changing[Alignment] = Fixed(Alignment.Center),
                                  colorOverlayPointer: Option[Changing[Color]] = None,
                                  imageScalingPointer: Changing[Double] = Fixed(1.0),
                                  usesLowPrioritySize: Boolean = false)
	extends ViewImageLabelSettingsLike[ViewImageLabelSettings]
{
	// IMPLEMENTED	--------------------
	
	override def self: ViewImageLabelSettings = this
	
	override def withAlignmentPointer(p: Changing[Alignment]): ViewImageLabelSettings = copy(alignmentPointer = p)
	override def withColorOverlayPointer(p: Option[Changing[Color]]): ViewImageLabelSettings =
		copy(colorOverlayPointer = p)
	override def withCustomDrawers(drawers: Vector[CustomDrawer]): ViewImageLabelSettings =
		copy(customDrawers = drawers)
	override def withImageScalingPointer(p: Changing[Double]): ViewImageLabelSettings =
		copy(imageScalingPointer = p)
	override def withInsetsPointer(p: Changing[StackInsets]): ViewImageLabelSettings = copy(insetsPointer = p)
	override def withUseLowPrioritySize(lowPriority: Boolean): ViewImageLabelSettings =
		copy(usesLowPrioritySize = lowPriority)
	
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
	override def customDrawers: Vector[CustomDrawer] = settings.customDrawers
	override def imageScalingPointer: Changing[Double] = settings.imageScalingPointer
	override def insetsPointer: Changing[StackInsets] = settings.insetsPointer
	override def usesLowPrioritySize: Boolean = settings.usesLowPrioritySize
	
	override def withAlignmentPointer(p: Changing[Alignment]): Repr =
		mapSettings { _.withAlignmentPointer(p) }
	override def withColorOverlayPointer(p: Option[Changing[Color]]): Repr =
		mapSettings { _.withColorOverlayPointer(p) }
	override def withCustomDrawers(drawers: Vector[CustomDrawer]): Repr =
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
	extends ViewImageLabelSettingsWrapper[Repr] with FramedFactory[Repr] with SizeAdjustable[Repr]
{
	// ABSTRACT ------------------------------
	
	/**
	  * The component hierarchy, to which created view image labels will be attached
	  */
	protected def parentHierarchy: ComponentHierarchy
	
	protected def allowUpscalingPointer: Changing[Boolean]
	
	
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
	protected def _imageOrIcon(pointer: Either[(Changing[SingleColorIcon], Changing[ColorContext]), Changing[Image]]) = {
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
						if (imagePointer.isChanging || p.isChanging) {
							val cache = WeakCache[(Image, Color), Image] { case (img, c) => img.withColorOverlay(c) }
							imagePointer.mergeWith(p) { (image, color) => cache(image -> color) }
						}
						// Case: Static image & color => Uses a static pointer
						else
							Fixed(imagePointer.value.withColorOverlay(p.value))
				}
			// Case: No color overlay => If using icons, colors according to the context background
			case None =>
				pointer.rightOrMap { case (iconPointer, contextPointer) =>
					iconPointer.mergeWith(contextPointer) { (icon, context) => icon.contextual(context) }
				}
		}
		// Applies image scaling, also
		val scaledImagePointer = coloredPointer.mergeWith(imageScalingPointer) { _ * _ }
		new ViewImageLabel(parentHierarchy, scaledImagePointer, insetsPointer, alignmentPointer, allowUpscalingPointer,
			customDrawers, usesLowPrioritySize)
	}
}

/**
  * Factory class used for constructing view image labels using contextual component creation information
  * @author Mikko Hilpinen
  * @since 30.05.2023, v1.1
  */
case class ContextualViewImageLabelFactory(parentHierarchy: ComponentHierarchy, contextPointer: Changing[ColorContext],
                                           settings: ViewImageLabelSettings = ViewImageLabelSettings.default,
                                           drawBackground: Boolean = false)
	extends ViewImageLabelFactoryLike[ContextualViewImageLabelFactory]
		with VariableBackgroundRoleAssignableFactory[ColorContext, ContextualViewImageLabelFactory]
{
	// IMPLEMENTED  ---------------------------
	
	override def self: ContextualViewImageLabelFactory = this
	
	override protected def allowUpscalingPointer: Changing[Boolean] = contextPointer
		.mapWhile(parentHierarchy.linkPointer) { _.allowImageUpscaling }
	
	override def withContextPointer(p: Changing[ColorContext]): ContextualViewImageLabelFactory =
		copy(contextPointer = p)
	override def withSettings(settings: ViewImageLabelSettings): ContextualViewImageLabelFactory =
		copy(settings = settings)
	
	override protected def withVariableBackgroundContext(newContextPointer: Changing[ColorContext],
	                                                     backgroundDrawer: CustomDrawer): ContextualViewImageLabelFactory =
		copy(contextPointer = newContextPointer,
			settings = settings.withCustomDrawers(backgroundDrawer +: settings.customDrawers), drawBackground = true)
	
	override def *(mod: Double): ContextualViewImageLabelFactory =
		copy(contextPointer = contextPointer.mapWhile(parentHierarchy.linkPointer) { _ * mod },
			settings = settings * mod)
	
	override def apply(imagePointer: Changing[Image]) = iconOrImagePointer(Right(imagePointer))
	
	
	// OTHER    ------------------------------
	
	def withInsetSizePointer(sizePointer: Changing[SizeCategory]) =
		withInsetsPointer(contextPointer.mergeWith(sizePointer) { _.scaledStackMargin(_).toInsets })
	
	def withColor(color: ColorSet): ContextualViewImageLabelFactory =
		withColorOverlayPointer(contextPointer.mapWhile(parentHierarchy.linkPointer) { _.color(color) })
	def withColor(color: ColorRole): ContextualViewImageLabelFactory =
		withColorOverlayPointer(contextPointer.mapWhile(parentHierarchy.linkPointer) { _.color(color) })
	def withColorPointer(colorPointer: Changing[ColorRole]) =
		withColorOverlayPointer(contextPointer.mergeWith(colorPointer) { _.color(_) })
	
	/**
	  * @param pointer Either Left) An icon pointer, or
	  *                       Right) An image pointer
	  * @return A label that displays the specified icon or image
	  */
	def iconOrImagePointer(pointer: Either[Changing[SingleColorIcon], Changing[Image]]) = {
		val label = _imageOrIcon(pointer.mapLeft { _ -> contextPointer })
		// If background drawing is enabled, repaints when background color changes
		if (drawBackground)
			contextPointer.addContinuousListener { e =>
				if (e.values.isAsymmetricBy { _.background })
					label.repaint()
			}
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
  * @param allowUpscalingPointer Pointer that determines whether drawn images should be allowed to scale
  *                               beyond their source resolution
  * @author Mikko Hilpinen
  * @since 30.05.2023, v1.1
  */
case class ViewImageLabelFactory(parentHierarchy: ComponentHierarchy,
                                 settings: ViewImageLabelSettings = ViewImageLabelSettings.default,
                                 allowUpscalingPointer: Changing[Boolean] = AlwaysFalse)
	extends ViewImageLabelFactoryLike[ViewImageLabelFactory] with BackgroundAssignable[ViewImageLabelFactory]
		with FromVariableContextFactory[ColorContext, ContextualViewImageLabelFactory]
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
	override def withContextPointer(context: Changing[ColorContext]) =
		ContextualViewImageLabelFactory(parentHierarchy, context, settings)
	
	override def *(mod: Double): ViewImageLabelFactory = withInsetsScaledBy(mod).withImageScaledBy(mod)
	
	
	// OTHER	--------------------
	
	/**
	  * @param allow Whether drawn images should be allowed to scale beyond their source resolution
	  * @return Copy of this factory with the specified allows upscaling
	  */
	def withAllowUpscaling(allow: Boolean) = copy(allowUpscalingPointer = Fixed(allow))
}

/**
  * Used for defining view image label creation settings outside of the component building process
  * @author Mikko Hilpinen
  * @since 30.05.2023, v1.1
  */
case class ViewImageLabelSetup(settings: ViewImageLabelSettings = ViewImageLabelSettings.default)
	extends ViewImageLabelSettingsWrapper[ViewImageLabelSetup]
		with Cff[ViewImageLabelFactory]
		with FromVariableContextComponentFactoryFactory[ColorContext, ContextualViewImageLabelFactory]
{
	override def self: ViewImageLabelSetup = this
	override def *(mod: Double): ViewImageLabelSetup = mapSettings { _ * mod }
	
	override def withContextPointer(hierarchy: ComponentHierarchy, context: Changing[ColorContext]): ContextualViewImageLabelFactory =
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
class ViewImageLabel(override val parentHierarchy: ComponentHierarchy, imagePointer: Changing[Image],
                     insetsPointer: Changing[StackInsets], alignmentPointer: Changing[Alignment],
                     allowUpscalingPointer: Changing[Boolean] = AlwaysTrue,
                     additionalCustomDrawers: Vector[CustomDrawer] = Vector(),
                     override val useLowPrioritySize: Boolean = false)
	extends CustomDrawReachComponent with ImageLabel
{
	// ATTRIBUTES	---------------------------------
	
	val customDrawers = additionalCustomDrawers :+
		ImageViewDrawer(imagePointer, insetsPointer, alignmentPointer, useUpscaling = allowUpscaling)
	
	
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
	allowUpscalingPointer.addContinuousAnyChangeListener { revalidate() }
	
	
	// COMPUTED	-------------------------------------
	
	/**
	  * @return Current alignment used when positioning the image in this label
	  */
	def alignment = alignmentPointer.value
	
	
	// IMPLEMENTED	---------------------------------
	
	override def image = imagePointer.value
	override def insets = insetsPointer.value
	override def allowUpscaling: Boolean = allowUpscalingPointer.value
	
	override def updateLayout() = ()
}
