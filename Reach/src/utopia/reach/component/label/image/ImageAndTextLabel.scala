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
import utopia.reach.component.factory.FromContextComponentFactoryFactory.Ccff
import utopia.reach.component.factory.contextual.{ContextualBackgroundAssignableFactory, TextContextualFactory}
import utopia.reach.component.factory.{FromContextFactory, Mixed}
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

trait ImageAndTextLabelSettingsLike[+Repr] extends CustomDrawableFactory[Repr]
{
	// ABSTRACT -----------------------
	
	protected def imageSettings: ImageLabelSettings
	protected def forceEqualBreadth: Boolean
	protected def isHint: Boolean
	
	/**
	  * @param settings New image settings to use
	  * @return Copy of these settings with the specified underlying image settings
	  */
	def withImageSettings(settings: ImageLabelSettings): Repr
	/**
	  * @param force Whether Fit layout should be forced
	  * @return Copy of this factory with specified setting
	  */
	def withForceEqualBreadth(force: Boolean): Repr
	/**
	  * @param isHint Whether text should be drawn as a hint (partially transparent)
	  * @return Copy of this factory with the specified setting
	  */
	def withIsHint(isHint: Boolean): Repr
	
	
	// COMPUTED -----------------------
	
	/**
	  * @return Copy of this factory that forces Fit layout
	  */
	def forcingEqualBreadth = withForceEqualBreadth(force = true)
	/**
	  * @return Copy of this factory that draws text as a hint
	  */
	def hint = withIsHint(isHint = true)
	
	def imageAlignment = imageSettings.alignment
	
	
	// OTHER    ----------------------
	
	def mapImageSettings(f: ImageLabelSettings => ImageLabelSettings) = withImageSettings(f(imageSettings))
	
	def withImageColor(color: Color) = mapImageSettings { _.withColor(color) }
	def withImageInsets(insets: StackInsetsConvertible) = mapImageSettings { _.withInsets(insets) }
	def mapImageInsets(f: StackInsets => StackInsetsConvertible) = mapImageSettings { _.mapInsets(f) }
	
	def withImageAlignment(alignment: Alignment) = mapImageSettings { _.withAlignment(alignment) }
}

object ImageAndTextLabelSettings
{
	val default = apply(ImageLabelSettings(alignment = Alignment.Right))
}
case class ImageAndTextLabelSettings(imageSettings: ImageLabelSettings = ImageLabelSettings.default,
                                     customDrawers: Vector[CustomDrawer] = Vector.empty,
                                     forceEqualBreadth: Boolean = false, isHint: Boolean = false)
	extends ImageAndTextLabelSettingsLike[ImageAndTextLabelSettings]
{
	override def withForceEqualBreadth(force: Boolean): ImageAndTextLabelSettings = copy(forceEqualBreadth = force)
	override def withIsHint(isHint: Boolean): ImageAndTextLabelSettings = copy(isHint = isHint)
	override def withImageSettings(settings: ImageLabelSettings): ImageAndTextLabelSettings =
		copy(imageSettings = settings)
	override def withCustomDrawers(drawers: Vector[CustomDrawer]): ImageAndTextLabelSettings =
		copy(customDrawers = customDrawers)
}

trait ImageAndTextLabelSettingsWrapper[+Repr] extends ImageAndTextLabelSettingsLike[Repr]
{
	// ABSTRACT ---------------------
	
	protected def settings: ImageAndTextLabelSettings
	protected def withSettings(settings: ImageAndTextLabelSettings): Repr
	
	
	// IMPLEMENTED  ----------------
	
	override protected def imageSettings: ImageLabelSettings = settings.imageSettings
	override protected def forceEqualBreadth: Boolean = settings.forceEqualBreadth
	override protected def isHint: Boolean = settings.isHint
	override def customDrawers = settings.customDrawers
	
	override def withCustomDrawers(drawers: Vector[CustomDrawer]): Repr = mapSettings { _.withCustomDrawers(drawers) }
	override def withForceEqualBreadth(force: Boolean): Repr = mapSettings { _.withForceEqualBreadth(force) }
	override def withIsHint(isHint: Boolean): Repr = mapSettings { _.withIsHint(isHint) }
	override def withImageSettings(settings: ImageLabelSettings): Repr = mapSettings { _.withImageSettings(settings) }
	
	
	// OTHER    --------------------
	
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
	
	override protected def withSettings(settings: ImageAndTextLabelSettings): ContextualImageAndTextLabelFactory =
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
		new ImageAndTextLabel(parentHierarchy, context, image, text, imageSettings, customDrawers, forceEqualBreadth)
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

case class ImageAndTextLabelSetup(settings: ImageAndTextLabelSettings)
	extends Ccff[TextContext, ContextualImageAndTextLabelFactory]
		with ImageAndTextLabelSettingsWrapper[ImageAndTextLabelSetup]
{
	override protected def withSettings(settings: ImageAndTextLabelSettings): ImageAndTextLabelSetup =
		copy(settings = settings)
	
	override def withContext(hierarchy: ComponentHierarchy, context: TextContext): ContextualImageAndTextLabelFactory =
		ContextualImageAndTextLabelFactory(hierarchy, context, settings)
}

object ImageAndTextLabel extends ImageAndTextLabelSetup(ImageAndTextLabelSettings.default)
{
	def apply(settings: ImageAndTextLabelSettings) = withSettings(settings)
}

/**
  * A label that displays both image and text
  * @author Mikko Hilpinen
  * @since 29.10.2020, v0.1
  */
class ImageAndTextLabel(parentHierarchy: ComponentHierarchy, context: TextContext,
                        image: Either[Image, SingleColorIcon], text: LocalizedString,
                        imageSettings: ImageLabelSettings = ImageLabelSettings.default,
                        additionalDrawers: Vector[CustomDrawer] = Vector(), forceEqualBreadth: Boolean = false)
	extends ReachComponentWrapper
{
	// ATTRIBUTES	------------------------------
	
	override protected val wrapped = {
		// If one of the provided items is empty, only creates one component
		if (image.either.isEmpty)
			TextLabel(parentHierarchy).withContext(context).withAdditionalCustomDrawers(additionalDrawers).apply(text)
		else if (text.isEmpty)
			ImageLabel.withSettings(imageSettings)
				.withAlignment(context.textAlignment.opposite).withAdditionalCustomDrawers(additionalDrawers)
				.apply(parentHierarchy).withContext(context).apply(image)
		else
			// Wraps the components in a stack
			Stack(parentHierarchy).withContext(context).withCustomDrawers(additionalDrawers)
				.buildPair(Mixed, context.textAlignment, forceEqualBreadth) { factories =>
					Pair(
						factories(TextLabel)(text),
						factories(ImageLabel.withSettings(imageSettings))(image))
				}
				.parent
	}
}
