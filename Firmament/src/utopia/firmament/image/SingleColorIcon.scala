package utopia.firmament.image

import utopia.firmament.context.{ColorContext, ComponentCreationDefaults}
import utopia.firmament.model.StandardSizeAdjustable
import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.immutable.caching.cache.WeakCache
import utopia.flow.collection.template.MapAccess
import utopia.flow.operator.MaybeEmpty
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.immutable.eventful.Fixed
import utopia.genesis.image.Image
import utopia.paradigm.color.ColorShade.{Dark, Light}
import utopia.paradigm.color.{Color, ColorShade, FromShadeFactory}
import utopia.paradigm.shape.shape2d.vector.size.{Size, Sized}

import scala.language.implicitConversions

object SingleColorIcon
{
	// ATTRIBUTES   ------------------------
	
	/**
	  * An empty icon
	  */
	val empty = apply(Image.empty, Size.zero)
	/**
	 * A pointer that always contains an empty icon
	 */
	val alwaysEmpty = Fixed(empty)
	
	
	// IMPLICIT ---------------------------
	
	implicit def iconToImage(icon: SingleColorIcon)(implicit context: ColorContext): Image = icon.contextual
	
	
	// OTHER    ---------------------------
	
	/**
	  * @param image The image to wrap
	  * @return An icon based on that image
	  */
	def apply(image: Image): SingleColorIcon = apply(image, image.size)
}

/**
  * Used for displaying single color icons in various contexts
  * @author Mikko Hilpinen
  * @since 4.5.2020, Reflection v1.2
  */
case class SingleColorIcon(original: Image, standardSize: Size)
	extends Sized[SingleColorIcon] with FromShadeFactory[Image] with MaybeEmpty[SingleColorIcon]
		with StandardSizeAdjustable[SingleColorIcon] with FromColorFactory[Image]
{
	// ATTRIBUTES	------------------------
	
	private val paintedImageCache = original.notEmpty match {
		case Some(img) => WeakCache[Color, Image] { c => img.withColorOverlay(c) }
		case None => MapAccess { _: Any => original }
	}
	
	private val _blackAndWhite = Pair(
		Lazy { original.mapIfNotEmpty { _.withAlpha(0.88) } },
		Lazy { original.mapIfNotEmpty { _.withColorOverlay(Color.white) } }
	)
	private val _inButtonSets = _blackAndWhite
		.map { _.map { ButtonImageSet(_) ++ ComponentCreationDefaults.inButtonImageEffects } }
	private val _highlightingInButtonSets = _blackAndWhite
		.map { _.map { ButtonImageSet(_) ++ ComponentCreationDefaults.asButtonImageEffects } }
	
	override protected lazy val relativeToStandardSize: Double = {
		if (standardSize.dimensions.exists { _ == 0.0 })
			1.0
		else
			(size/standardSize).xyPair.sum/2.0
	}
	
	
	// COMPUTED	---------------------------
	
	/**
	  * A black version of this icon
	  */
	def black = _blackAndWhite.first.value
	/**
	  * A white version of this icon
	  */
	def white = _blackAndWhite.second.value
	/**
	  * @return The black and white versions of this icon (view)
	  */
	def blackAndWhite = _blackAndWhite.view.map { _.value }
	
	/**
	  * A version of this icon for light image + text buttons
	  */
	def blackForButtons = _inButtonSets.first.value
	/**
	  * A version of this icon for dark image + text buttons
	  */
	def whiteForButtons = _inButtonSets.second.value
	/**
	  * A version of this icon for black individual buttons
	  */
	def blackIndividualButton = _highlightingInButtonSets.first.value
	/**
	  * A version of this icon for white individual buttons
	  */
	def whiteIndividualButton = _highlightingInButtonSets.second.value
	
	/**
	  * @return Access to methods that generate button image sets suitable for icon buttons
	  */
	def asButton = IconAsButton
	/**
	  * @return Access to methods that generate images suitable to be used inside buttons
	  */
	def inButton = IconInsideButton
	
	/**
	  * @return A full size version of this icon where icon size matches the source resolution
	  */
	def fullSize = {
		val newIcon = original.withOriginalSize
		if (original == newIcon)
			this
		else
			new SingleColorIcon(newIcon, standardSize)
	}
	
	/**
	  * @param context Implicit component creation context
	  * @return A black or a white icon, whichever is better suited to the current context
	  */
	def contextual(implicit context: ColorContext) = against(context.background)
	
	
	// IMPLEMENTED  -----------------------
	
	override def self = this
	
	override def isEmpty: Boolean = original.isEmpty
	
	override def size = original.size
	
	override def withSize(size: Size) = map { _.withSize(size) }
	
	/**
	  * @param shade Targeted shade
	  * @return This icon converted to a black or white image matching that shade
	  */
	override def apply(shade: ColorShade) = shade match {
		case Light => white
		case Dark => black
	}
	/**
	  * @param color A color
	  * @return This icon painted with that color
	  */
	override def apply(color: Color) = paintedImageCache(color)
	
	override def *(mod: Double): SingleColorIcon = map { _ * mod }
	
	
	// OTHER	---------------------------
	
	/**
	  * @param f A mapping function
	  * @return A mapped copy of this icon
	  */
	def map(f: Image => Image) = new SingleColorIcon(f(original), standardSize)
	
	/**
	  * @param color Target icon color
	  * @return A button image set to be used in buttons without text
	  */
	@deprecated("Replaced with .forButton.apply(Color)")
	def asIndividualButtonWithColor(color: Color) = {
		val colored = asImageWithColor(color)
		if (color.relativeLuminance < 0.6)
			ButtonImageSet.brightening(colored)
		else
			ButtonImageSet.darkening(colored)
	}
	/**
	  * @param shade a color shade
	  * @return A version of this icon that matches that shade
	  */
	@deprecated("Replaced with .apply(ColorShade)", "v1.0")
	def withShade(shade: ColorShade) = shade match {
		case ColorShade.Light => white
		case ColorShade.Dark => black
	}
	/**
	  * @param color Button background color
	  * @return Images used with specified button background
	  */
	@deprecated("Replaced with .forButton.against(Color)", "v1.0")
	def forButtonWithBackground(color: Color) = inButton.against(color)
	/**
	  * @param iconColor New color of the icon
	  * @return An icon image with specified color overlay
	  */
	@deprecated("Replaced with .apply(Color)", "v1.0")
	def asImageWithColor(iconColor: Color) = paintedImageCache(iconColor)
	/**
	  * @param background Background color
	  * @return A version of this icon (black or white) that is better against that background
	  */
	@deprecated("Replaced with .against(Color)", "v1.0")
	def singleColorImageAgainst(background: Color) = background.shade match {
		case Dark => white
		case Light => black
	}
	
	
	// NESTED   --------------------------
	
	object IconAsButton extends FromShadeFactory[ButtonImageSet] with FromColorFactory[ButtonImageSet]
	{
		// COMPUTED --------------------------
		
		/**
		  * @param context Implicit button creation context
		  * @return A set of black or white images that may be used in a button
		  */
		def contextual(implicit context: ColorContext) = against(context.background)
		
		
		// IMPLEMENTED  ----------------------
		
		/**
		  * @param shade Targeted color shade
		  * @return A button image set with black or white icons matching that shade
		  */
		override def apply(shade: ColorShade) = shade match {
			case Light => whiteIndividualButton
			case Dark => blackIndividualButton
		}
		
		/**
		  * @param color Target icon color
		  * @return A button image set that uses icons with that color
		  */
		override def apply(color: Color) = {
			val colored = SingleColorIcon.this.apply(color)
			ButtonImageSet(colored).lowerAlphaOnDisabled.highlighting
		}
	}
	
	object IconInsideButton extends FromShadeFactory[ButtonImageSet] with FromColorFactory[ButtonImageSet]
	{
		// COMPUTED -------------------------
		
		/**
		  * @return A button image set based on the white version of this icon
		  */
		def white = apply(Light)
		/**
		  * @return A button-image-set based on the black version of this icon
		  */
		def black = apply(Dark)
		
		/**
		  * @param context Component creation context
		  * @return A button image set based on this icon, where only alpha values may change
		  */
		def contextual(implicit context: ColorContext) = against(context.background)
		
		
		// IMPLEMENTED  ---------------------
		
		override def apply(shade: ColorShade): ButtonImageSet = shade match {
			case Dark => blackForButtons
			case Light => whiteForButtons
		}
		
		override def apply(color: Color): ButtonImageSet =
			ButtonImageSet(SingleColorIcon.this(color)).lowerAlphaOnDisabled
	}
}
