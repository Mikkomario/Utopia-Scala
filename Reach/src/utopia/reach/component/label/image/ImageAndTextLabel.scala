package utopia.reach.component.label.image

import utopia.firmament.context.TextContext
import utopia.firmament.drawing.immutable.CustomDrawableFactory
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.image.SingleColorIcon
import utopia.firmament.localization.LocalizedString
import utopia.firmament.model.stack.{StackInsets, StackInsetsConvertible}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.genesis.image.Image
import utopia.paradigm.color.ColorLevel.Standard
import utopia.paradigm.color.{Color, ColorLevel, ColorRole}
import utopia.paradigm.enumeration.{Alignment, FromAlignmentFactory}
import utopia.reach.component.factory.contextual.{ContextualBackgroundAssignableFactory, TextContextualFactory}
import utopia.reach.component.factory.{FromContextComponentFactoryFactory, FromContextFactory, Mixed}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.text.TextLabel
import utopia.reach.component.template.ReachComponentWrapper
import utopia.reach.container.multi.Stack

class ImageAndTextLabelFactory(parentHierarchy: ComponentHierarchy)
	extends FromContextFactory[TextContext, ContextualImageAndTextLabelFactory]
{
	override def withContext(context: TextContext) =
		ContextualImageAndTextLabelFactory(parentHierarchy, context)
}

/**
  * Common trait for image and text label factories and settings
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 01.06.2023, v1.1
  */
trait ImageAndTextLabelSettingsLike[+Repr] extends CustomDrawableFactory[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Settings that affect the wrapped image label
	  */
	def imageSettings: ImageLabelSettings
	
	/**
	  * Whether the text and image should be forced to have equal width or height, depending on the layout
	  */
	def forceEqualBreadth: Boolean
	/**
	  * Whether this factory constructs hint labels. Affects text opacity.
	  */
	def isHint: Boolean
	
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
	def withImageSettings(settings: ImageLabelSettings): Repr
	/**
	  * Whether this factory constructs hint labels. Affects text opacity.
	  * @param isHint Whether text should be drawn as a hint (partially transparent)
	  * @return Copy of this factory with the specified setting
	  */
	def withIsHint(isHint: Boolean): Repr
	
	
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
	def imageColorOverlay = imageSettings.colorOverlay
	/**
	  * uses low priority size from the wrapped image label settings
	  */
	def imageUsesLowPrioritySize = imageSettings.usesLowPrioritySize
	
	/**
	  * Copy of this factory that forces Fit layout
	  */
	def forcingEqualBreadth = withForceEqualBreadth(force = true)
	/**
	  * Copy of this factory that draws text as a hint
	  */
	def hint = withIsHint(isHint = true)
	
	
	// OTHER	--------------------
	
	def mapImageInsets(f: StackInsets => StackInsetsConvertible) = mapImageSettings { _.mapInsets(f) }
	def mapImageSettings(f: ImageLabelSettings => ImageLabelSettings) = withImageSettings(f(imageSettings))
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
	def withImageCustomDrawers(drawers: Vector[CustomDrawer]) =
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
	
	val default = apply()
}
/**
  * Combined settings used when constructing image and text labels
  * @param customDrawers     Custom drawers to assign to created components
  * @param imageSettings     Settings that affect the wrapped image label
  * @param forceEqualBreadth Whether the text and image should be forced to have equal width or height,
  *                          depending on the layout
  * @param isHint            Whether this factory constructs hint labels. Affects text opacity.
  * @author Mikko Hilpinen
  * @since 01.06.2023, v1.1
  */
case class ImageAndTextLabelSettings(customDrawers: Vector[CustomDrawer] = Vector.empty,
                                     imageSettings: ImageLabelSettings = ImageLabelSettings.default,
                                     forceEqualBreadth: Boolean = false,
                                     isHint: Boolean = false)
	extends ImageAndTextLabelSettingsLike[ImageAndTextLabelSettings]
{
	// IMPLEMENTED	--------------------
	
	override def withCustomDrawers(drawers: Vector[CustomDrawer]): ImageAndTextLabelSettings =
		copy(customDrawers = customDrawers)
	override def withForceEqualBreadth(force: Boolean): ImageAndTextLabelSettings =
		copy(forceEqualBreadth = force)
	override def withImageSettings(settings: ImageLabelSettings): ImageAndTextLabelSettings =
		copy(imageSettings = settings)
	override def withIsHint(isHint: Boolean): ImageAndTextLabelSettings = copy(isHint = isHint)
}

/**
  * Common trait for factories that wrap a image and text label settings instance
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 01.06.2023, v1.1
  */
trait ImageAndTextLabelSettingsWrapper[+Repr] extends ImageAndTextLabelSettingsLike[Repr]
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
	override def forceEqualBreadth: Boolean = settings.forceEqualBreadth
	override def imageSettings: ImageLabelSettings = settings.imageSettings
	override def isHint: Boolean = settings.isHint
	
	override def withCustomDrawers(drawers: Vector[CustomDrawer]): Repr =
		mapSettings { _.withCustomDrawers(drawers) }
	override def withForceEqualBreadth(force: Boolean): Repr = mapSettings { _.withForceEqualBreadth(force) }
	override def withImageSettings(settings: ImageLabelSettings): Repr =
		mapSettings { _.withImageSettings(settings) }
	override def withIsHint(isHint: Boolean): Repr = mapSettings { _.withIsHint(isHint) }
	
	
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
		new ImageAndTextLabel(parentHierarchy, context, image, text, settings)
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
		ContextualImageAndTextLabelFactory(hierarchy, context, settings)
	
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
                        settings: ImageAndTextLabelSettings = ImageAndTextLabelSettings.default)
	extends ReachComponentWrapper
{
	// ATTRIBUTES	------------------------------
	
	override protected val wrapped = {
		// If one of the provided items is empty, only creates one component
		if (image.either.isEmpty)
			TextLabel(parentHierarchy).withContext(context)
				.withIsHint(settings.isHint).withAdditionalCustomDrawers(settings.customDrawers)
				.apply(text)
		else if (text.isEmpty)
			ImageLabel.withSettings(settings.imageSettings)
				.withAdditionalCustomDrawers(settings.customDrawers)
				.apply(parentHierarchy).withContext(context)
				.apply(image)
		else
			// Wraps the components in a stack
			Stack(parentHierarchy).withContext(context)
				.withCustomDrawers(settings.customDrawers)
				.buildPair(Mixed, context.textAlignment, settings.forceEqualBreadth) { factories =>
					Pair(
						factories(TextLabel).withIsHint(settings.isHint)(text),
						factories(ImageLabel.withSettings(settings.imageSettings))(image))
				}
				.parent
	}
}
