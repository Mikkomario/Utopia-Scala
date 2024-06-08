package utopia.reach.component.label.image

import utopia.firmament.context.TextContext
import utopia.firmament.drawing.immutable.CustomDrawableFactory
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.image.SingleColorIcon
import utopia.firmament.localization.LocalizedString
import utopia.firmament.model.enumeration.SizeCategory
import utopia.firmament.model.enumeration.SizeCategory.{Small, VerySmall}
import utopia.firmament.model.stack.{StackInsets, StackInsetsConvertible, StackLength}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Pair}
import utopia.flow.view.immutable.eventful.Fixed
import utopia.genesis.image.Image
import utopia.paradigm.color.ColorLevel.Standard
import utopia.paradigm.color.{Color, ColorLevel, ColorRole}
import utopia.paradigm.enumeration.{Alignment, FromAlignmentFactory}
import utopia.reach.component.factory.UnresolvedFramedFactory.UnresolvedStackInsets
import utopia.reach.component.factory.contextual.{ContextualBackgroundAssignableFactory, TextContextualFactory}
import utopia.reach.component.factory.{FromContextComponentFactoryFactory, FromContextFactory, Mixed, UnresolvedFramedFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.image.ImageAndTextLabelSettings.defaultImageSettings
import utopia.reach.component.label.text.TextLabel
import utopia.reach.component.template.ReachComponentWrapper
import utopia.reach.container.multi.Stack

/**
  * Common trait for image and text label factories and settings
  * @tparam ImgSettings Type of image label settings wrapped
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 01.06.2023, v1.1
  */
trait ImageAndTextLabelSettingsLike[ImgSettings <: ImageLabelSettingsLike[ImgSettings], +Repr]
	extends CustomDrawableFactory[Repr] with FromAlignmentFactory[Repr] with UnresolvedFramedFactory[Repr]
{
	import UnresolvedFramedFactory.sides
	
	// ABSTRACT	--------------------
	
	/**
	  * Settings that affect the wrapped image label
	  */
	def imageSettings: ImgSettings
	
	/**
	  * The total margin placed between the image and the text. None if no margin is placed.
	  */
	def separatingMargin: Option[SizeCategory]
	/**
	  * Whether the text and image should be forced to have equal width or height, depending on the layout
	  */
	def forceEqualBreadth: Boolean
	/**
	  * Whether this factory constructs hint labels. Affects text opacity.
	  */
	def isHint: Boolean
	
	/**
	  * The total margin placed between the image and the text. None if no margin is placed.
	  * @param margin New separating margin to use.
	  * The total margin placed between the image and the text. None if no margin is placed.
	  * @return Copy of this factory with the specified separating margin
	  */
	def withSeparatingMargin(margin: Option[SizeCategory]): Repr
	/**
	  * Whether the text and image should be forced to have equal width or height, depending on the layout
	  * @param force Whether Fit layout should be forced
	  * @return Copy of this factory with specified setting
	  */
	def withForceEqualBreadth(force: Boolean): Repr
	/**
	  * Settings that affect the wrapped image label
	  * @param settings New image settings to use
	  * @return Copy of these settings with the specified underlying image settings
	  */
	def withImageSettings(settings: ImgSettings): Repr
	/**
	  * Whether this factory constructs hint labels. Affects text opacity.
	  * @param isHint Whether text should be drawn as a hint (partially transparent)
	  * @return Copy of this factory with the specified setting
	  */
	def withIsHint(isHint: Boolean): Repr
	
	/**
	  * Modifies both the separating margin, as well as the insets at the same time
	  * @param separatingMargin Separating margin to place
	  * @param insets (Common) insets to place
	  * @return Copy of this factory with the specified margins
	  */
	protected def _withMargins(separatingMargin: Option[SizeCategory], insets: UnresolvedStackInsets): Repr
	
	
	// COMPUTED	--------------------
	
	/**
	  * custom drawers from the wrapped image label settings
	  */
	def imageCustomDrawers = imageSettings.customDrawers
	/**
	  * insets from the wrapped image label settings
	  */
	def imageInsets = imageSettings.insets
	/**
	  * alignment from the wrapped image label settings
	  */
	def imageAlignment = imageSettings.alignment
	/**
	  * image scaling from the wrapped image label settings
	  */
	def imageScaling = imageSettings.imageScaling
	/**
	  * color overlay from the wrapped image label settings
	  */
	// FiXME: Nullpointer here
	def imageColorOverlay = imageSettings.colorOverlay
	/**
	  * uses low priority size from the wrapped image label settings
	  */
	def imageUsesLowPrioritySize = imageSettings.usesLowPrioritySize
	
	/**
	  * @return Copy of this factory that doesn't place any margin between the image and the text
	  */
	def withoutSeparatingMargin = withSeparatingMargin(None)
	/**
	  * Copy of this factory that forces Fit layout
	  */
	def forcingEqualBreadth = withForceEqualBreadth(force = true)
	/**
	  * Copy of this factory that draws text as a hint
	  */
	def hint = withIsHint(isHint = true)
	
	/**
	  * @return Copy of this factory that uses low-priority constraints for the image size
	  */
	def withLowPriorityImageSize = withImageUsesLowPrioritySize(lowPriority = true)
	/**
	  * @return Copy of this factory that doesn't place any insets around the image
	  */
	def withoutImageInsets = withImageInsets(StackInsets.zero)
	
	/**
	  * @return Copy of this factory with no margins placed between the image and the text, and no margins placed
	  *         ouside of them (except for the specified text insets and image insets)
	  */
	def withoutMargins = _withMargins(None, sides.empty)
	
	
	// IMPLEMENTED  ----------------
	
	override def apply(alignment: Alignment): Repr = withImageAlignment(alignment.opposite)
	
	
	// OTHER	--------------------
	
	def withSeparatingMargin(margin: SizeCategory): Repr = withSeparatingMargin(Some(margin))
	/**
	  * @param margin The size of the margin to place between the image and the text, as well as around both
	  * @return Copy of this factory with the specified margin applied
	  */
	def withMargin(margin: SizeCategory) = _withMargins(Some(margin), sides.symmetric(Left(margin)))
	
	def mapImageInsets(f: StackInsets => StackInsetsConvertible) = mapImageSettings { _.mapInsets(f) }
	def mapImageSettings(f: ImgSettings => ImgSettings) = withImageSettings(f(imageSettings))
	def mapImageAlignment(f: Alignment => Alignment) = withImageAlignment(f(imageAlignment))
	def mapImageScaling(f: Double => Double) = withImageScaling(f(imageScaling))
	
	/**
	  * @param alignment Alignment used when drawing the image within this label
	  * @return Copy of this factory with the specified image alignment
	  */
	def withImageAlignment(alignment: Alignment) = withImageSettings(imageSettings.withAlignment(alignment))
	/**
	  * @param color Color overlay applied over drawn images
	  * @return Copy of this factory with the specified image color overlay
	  */
	def withImageColorOverlay(color: Option[Color]) = withImageSettings(imageSettings.withColor(color))
	/**
	  * @param drawers Custom drawers to assign to created components
	  * @return Copy of this factory with the specified image custom drawers
	  */
	def withImageCustomDrawers(drawers: Seq[CustomDrawer]) =
		withImageSettings(imageSettings.withCustomDrawers(drawers))
	/**
	  * @param scaling Scaling applied to the drawn images
	  * @return Copy of this factory with the specified image image scaling
	  */
	def withImageScaling(scaling: Double) = withImageSettings(imageSettings.withImageScaling(scaling))
	/**
	  * @param insets Insets to place around created components
	  * @return Copy of this factory with the specified image insets
	  */
	def withImageInsets(insets: StackInsets) = withImageSettings(imageSettings.withInsets(insets))
	/**
	  * @param lowPriority Whether low priority size constraints should be used
	  * @return Copy of this factory with the specified image uses low priority size
	  */
	def withImageUsesLowPrioritySize(lowPriority: Boolean) =
		withImageSettings(imageSettings.withUseLowPrioritySize(lowPriority))
}

object ImageAndTextLabelSettings
{
	// ATTRIBUTES	--------------------
	
	private val defaultInsets = UnresolvedFramedFactory.sides.horizontal(Left(Small)).withVertical(Left(VerySmall))
	private val defaultImageSettings = ImageLabelSettings.default.right
	
	val default = apply()
}
/**
  * Combined settings used when constructing image and text labels
  * @param customDrawers     Custom drawers to assign to created components
  * @param imageSettings     Settings that affect the wrapped image label
  * @param separatingMargin The total margin placed between the image and the text. None if no margin is placed.
  * @param forceEqualBreadth Whether the text and image should be forced to have equal width or height,
  *                          depending on the layout
  * @param isHint            Whether this factory constructs hint labels. Affects text opacity.
  * @author Mikko Hilpinen
  * @since 01.06.2023, v1.1
  */
case class ImageAndTextLabelSettings(customDrawers: Seq[CustomDrawer] = Empty,
                                     imageSettings: ImageLabelSettings = defaultImageSettings,
                                     separatingMargin: Option[SizeCategory] = Some(Small),
                                     insets: UnresolvedStackInsets = ImageAndTextLabelSettings.defaultInsets,
                                     forceEqualBreadth: Boolean = false,
                                     isHint: Boolean = false)
	extends ImageAndTextLabelSettingsLike[ImageLabelSettings, ImageAndTextLabelSettings]
{
	// COMPUTED ------------------------
	
	/**
	  * @return A copy of these settings that may be used in constructing pointer-based image-and-text labels
	  */
	def toViewSettings = ViewImageAndTextLabelSettings(customDrawers, imageSettings.toViewSettings, separatingMargin,
		insets, Fixed(isHint), forceEqualBreadth = forceEqualBreadth)
	
	
	// IMPLEMENTED	--------------------
	
	override def withInsets(insets: UnresolvedStackInsets): ImageAndTextLabelSettings = copy(insets = insets)
	override def withCustomDrawers(drawers: Seq[CustomDrawer]): ImageAndTextLabelSettings =
		copy(customDrawers = drawers)
	override def withForceEqualBreadth(force: Boolean): ImageAndTextLabelSettings =
		copy(forceEqualBreadth = force)
	override def withImageSettings(settings: ImageLabelSettings): ImageAndTextLabelSettings =
		copy(imageSettings = settings)
	override def withIsHint(isHint: Boolean): ImageAndTextLabelSettings = copy(isHint = isHint)
	override def withSeparatingMargin(margin: Option[SizeCategory]) =
		copy(separatingMargin = margin)
	
	override protected def _withMargins(separatingMargin: Option[SizeCategory],
	                                    insets: UnresolvedStackInsets): ImageAndTextLabelSettings =
		copy(separatingMargin = separatingMargin, insets = insets)
}

/**
  * Common trait for factories that wrap a image and text label settings instance
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 01.06.2023, v1.1
  */
trait ImageAndTextLabelSettingsWrapper[+Repr] extends ImageAndTextLabelSettingsLike[ImageLabelSettings, Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Settings wrapped by this instance
	  */
	protected def settings: ImageAndTextLabelSettings
	
	/**
	  * @return Copy of this factory with the specified settings
	  */
	def withSettings(settings: ImageAndTextLabelSettings): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def customDrawers = settings.customDrawers
	override def separatingMargin = settings.separatingMargin
	override def forceEqualBreadth: Boolean = settings.forceEqualBreadth
	override def imageSettings: ImageLabelSettings = settings.imageSettings
	override def isHint: Boolean = settings.isHint
	override def insets: UnresolvedStackInsets = settings.insets
	
	override def withInsets(insets: UnresolvedStackInsets): Repr = mapSettings { _.withInsets(insets) }
	override def withCustomDrawers(drawers: Seq[CustomDrawer]): Repr =
		mapSettings { _.withCustomDrawers(drawers) }
	override def withForceEqualBreadth(force: Boolean): Repr = mapSettings { _.withForceEqualBreadth(force) }
	override def withImageSettings(settings: ImageLabelSettings): Repr =
		mapSettings { _.withImageSettings(settings) }
	override def withIsHint(isHint: Boolean): Repr = mapSettings { _.withIsHint(isHint) }
	override def withSeparatingMargin(margin: Option[SizeCategory]) =
		mapSettings { _.withSeparatingMargin(margin) }
	
	override protected def _withMargins(separatingMargin: Option[SizeCategory], insets: UnresolvedStackInsets): Repr =
		mapSettings { _.copy(separatingMargin = separatingMargin, insets = insets) }
	
	
	// OTHER	--------------------
	
	def mapSettings(f: ImageAndTextLabelSettings => ImageAndTextLabelSettings) = withSettings(f(settings))
}

case class ContextualImageAndTextLabelFactory(parentHierarchy: ComponentHierarchy, context: TextContext,
                                              settings: ImageAndTextLabelSettings = ImageAndTextLabelSettings.default)
	extends TextContextualFactory[ContextualImageAndTextLabelFactory]
		with ImageAndTextLabelSettingsWrapper[ContextualImageAndTextLabelFactory]
		with ContextualBackgroundAssignableFactory[TextContext, ContextualImageAndTextLabelFactory]
		with FromAlignmentFactory[ContextualImageAndTextLabelFactory]
{
	// COMPUTED --------------------
	
	/**
	  * @return A factory resembling this factory, which may be used for constructing view-based labels
	  */
	def toViewFactory: ContextualViewImageAndTextLabelFactory =
		ContextualViewImageAndTextLabelFactory(parentHierarchy, Fixed(context), settings.toViewSettings)
	
	private def resolveInsets = resolveInsetsIn(context)
	
	
	// IMPLEMENTED  ----------------
	
	override def self: ContextualImageAndTextLabelFactory = this
	
	override def withSettings(settings: ImageAndTextLabelSettings): ContextualImageAndTextLabelFactory =
		copy(settings = settings)
	
	override def withContext(newContext: TextContext) =
		copy(context = newContext)
	
	// By default, uses opposite alignments for text and image
	override def apply(alignment: Alignment) = copy(
		context = context.withTextAlignment(alignment),
		settings = settings.withImageAlignment(alignment.opposite)
	)
	override def withTextAlignment(alignment: Alignment) = apply(alignment)
	
	
	// OTHER    --------------------
	
	/**
	  * @param color Color to overlay the drawn image with
	  * @return Copy of this factory with the specified color overlay
	  */
	def withImageColor(color: ColorRole) =
		mapImageSettings { _.withColor(context.color(color)) }
	
	/**
	  * Creates a new label that contains both an image and text
	  * @param image                   Image displayed on this label.
	  *                                Either Left: Image or Right: Icon
	  * @param text                    Text displayed on this label
	  * @return A new label
	  */
	def apply(image: Either[Image, SingleColorIcon], text: LocalizedString) =
		new ImageAndTextLabel(parentHierarchy, context, image, text, settings, resolveInsets)
	
	/**
	  * Creates a new label that contains both an image and text
	  * @param image Image displayed on this label
	  * @param text  Text displayed on this label
	  * @return A new label
	  */
	def apply(image: Image, text: LocalizedString): ImageAndTextLabel = apply(Left(image), text)
	/**
	  * Creates a new label that contains both an image and text
	  * @param icon Icon displayed on this label
	  * @param text  Text displayed on this label
	  * @return A new label
	  */
	def apply(icon: SingleColorIcon, text: LocalizedString): ImageAndTextLabel = apply(Right(icon), text)
	
	/**
	  * Creates a new label that contains both an image and text
	  * @param icon Icon displayed on this label
	  * @param text Text displayed on this label
	  * @return A new label
	  */
	@deprecated("Replaced with .apply(SingleColorIcon, LocalizedString)", "v1.1")
	def withIcon(icon: SingleColorIcon, text: LocalizedString) = apply(icon, text)
	/**
	  * Creates a new label that contains both an image and text
	  * @param icon Icon displayed on this label
	  * @param text Text displayed on this label
	  * @param role Role that determines the image color
	  * @param preferredShade Preferred color shade to use (default = standard)
	  * @return A new label
	  */
	@deprecated("Please use .mapImageSettings { _.withColor(ColorRole) }.apply(SingleColorIcon, LocalizedString) instead", "v1.1")
	def withColouredIcon(icon: SingleColorIcon, text: LocalizedString, role: ColorRole,
	                     preferredShade: ColorLevel = Standard) =
		mapImageSettings { _.withColor(context.color.preferring(preferredShade)(role)) }.apply(icon, text)
}

@deprecated("Deprecated for removal", "v1.2")
class ImageAndTextLabelFactory(parentHierarchy: ComponentHierarchy)
	extends FromContextFactory[TextContext, ContextualImageAndTextLabelFactory]
{
	override def withContext(context: TextContext) =
		ContextualImageAndTextLabelFactory(parentHierarchy, context)
}

/**
  * Used for defining image and text label creation settings outside of the component building process
  * @author Mikko Hilpinen
  * @since 01.06.2023, v1.1
  */
case class ImageAndTextLabelSetup(settings: ImageAndTextLabelSettings = ImageAndTextLabelSettings.default)
	extends ImageAndTextLabelSettingsWrapper[ImageAndTextLabelSetup]
		with FromContextComponentFactoryFactory[TextContext, ContextualImageAndTextLabelFactory]
{
	// IMPLEMENTED	--------------------
	
	override def withContext(hierarchy: ComponentHierarchy,
	                         context: TextContext): ContextualImageAndTextLabelFactory =
		ContextualImageAndTextLabelFactory(hierarchy, context, settings.withImageAlignment(context.textAlignment.opposite))
	
	override def withSettings(settings: ImageAndTextLabelSettings): ImageAndTextLabelSetup =
		copy(settings = settings)
}

object ImageAndTextLabel extends ImageAndTextLabelSetup()
{
	// OTHER	--------------------
	
	def apply(settings: ImageAndTextLabelSettings) = withSettings(settings)
}

/**
  * A label that displays both image and text
  * @author Mikko Hilpinen
  * @since 29.10.2020, v0.1
  */
class ImageAndTextLabel(parentHierarchy: ComponentHierarchy, context: TextContext,
                        image: Either[Image, SingleColorIcon], text: LocalizedString,
                        settings: ImageAndTextLabelSettings = ImageAndTextLabelSettings.default,
                        commonInsets: StackInsets)
	extends ReachComponentWrapper
{
	// ATTRIBUTES	------------------------------
	
	// TODO: Apply the hint to the image label also
	override protected val wrapped = {
		// If one of the provided items is empty, only creates one component
		if (image.either.isEmpty)
			TextLabel(parentHierarchy).withContext(context.mapTextInsets { _ max commonInsets })
				.withIsHint(settings.isHint).withAdditionalCustomDrawers(settings.customDrawers)
				.apply(text)
		else if (text.isEmpty)
			ImageLabel.withSettings(settings.imageSettings)
				.withAdditionalCustomDrawers(settings.customDrawers)
				.mapInsets { _ max commonInsets }
				.apply(parentHierarchy).withContext(context)
				.apply(image)
		else {
			val textAlignment = context.textAlignment
			val imageAlignment = textAlignment.opposite
			// Applies the common insets and removes insets between the text and the image
			val appliedImageSettings = settings.imageSettings.mapInsets { default =>
				StackInsets.fromFunction { dir =>
					if (imageAlignment.movesTowards(dir))
						StackLength.fixedZero
					else
						default(dir) max commonInsets(dir)
				}
			}
			// WET WET
			val appliedContext = context.mapTextInsets { default =>
				StackInsets.fromFunction { dir =>
					if (textAlignment.movesTowards(dir))
						StackLength.fixedZero
					else
						default(dir) max commonInsets(dir)
				}
			}
			
			// Wraps the components in a stack
			Stack(parentHierarchy).withContext(appliedContext)
				.withCustomDrawers(settings.customDrawers).withMargin(settings.separatingMargin)
				.buildPair(Mixed, context.textAlignment, settings.forceEqualBreadth) { factories =>
					Pair(
						factories(ImageLabel.withSettings(appliedImageSettings))(image),
						factories(TextLabel).withIsHint(settings.isHint)(text)
					)
				}
				.parent
		}
	}
}
