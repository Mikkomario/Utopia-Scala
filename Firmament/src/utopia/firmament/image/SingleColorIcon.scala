package utopia.firmament.image

import utopia.firmament.context.ComponentCreationDefaults
import utopia.firmament.context.color.StaticColorContext
import utopia.firmament.factory.FromContextShadeFactory
import utopia.firmament.model.StandardSizeAdjustable
import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.immutable.caching.cache.WeakCache
import utopia.flow.collection.template.MapAccess
import utopia.flow.operator.MaybeEmpty
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.image.Image
import utopia.paradigm.color.ColorShade.{Dark, Light}
import utopia.paradigm.color.{Color, ColorShade}
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
	
	implicit def iconToImage(icon: SingleColorIcon)(implicit context: StaticColorContext): Image = icon.contextual
	
	
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
	extends FromContextShadeFactory[Image] with Sized[SingleColorIcon] with MaybeEmpty[SingleColorIcon]
		with StandardSizeAdjustable[SingleColorIcon] with FromColorFactory[Image]
{
	// ATTRIBUTES	------------------------
	
	// Weakly caches the results of color overlay operations, since those are somewhat slow to compute
	private val paintedImageCache = original.notEmpty match {
		case Some(img) => WeakCache.weakValues[Color, Image] { c => img.withColorOverlay(c) }
		case None => MapAccess { _: Any => original }
	}
	// Also weakly caches the results of color pointer mappings, in order to avoid creating unnecessary pointers
	private val paintedImagePointerCache = {
		if (original.isEmpty)
			MapAccess {  _: Any => Fixed(original) }
		else
			WeakCache { colorP: Changing[Color] => colorP.map(paintedImageCache.apply) }
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
		val newIcon = original.fullSized
		if (original == newIcon)
			this
		else
			new SingleColorIcon(newIcon, standardSize)
	}
	
	
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
	  * @param colorPointer A pointer that contains the color overlay to apply
	  * @return A pointer that contains this image overlaid with the specified pointer's color
	  */
	def apply(colorPointer: Changing[Color]) = paintedImagePointerCache(colorPointer)
	
	/**
	  * @param backgroundPointer Background color pointer
	  * @return A pointer that contains a black or white version of this icon,
	  *         whichever is suitable against the specified background
	  */
	@deprecated("Deprecated for removal. Please use againstVariableBackground instead.", "v1.4")
	def against(backgroundPointer: Changing[Color]): Changing[Image] = againstVariableBackground(backgroundPointer)
	
	/**
	  * @param f A mapping function
	  * @return A mapped copy of this icon
	  */
	def map(f: Image => Image) = new SingleColorIcon(f(original), standardSize)
	
	
	// NESTED   --------------------------
	
	object IconAsButton extends FromContextShadeFactory[ButtonImageSet] with FromColorFactory[ButtonImageSet]
	{
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
	
	object IconInsideButton extends FromContextShadeFactory[ButtonImageSet] with FromColorFactory[ButtonImageSet]
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
		
		
		// IMPLEMENTED  ---------------------
		
		override def apply(shade: ColorShade): ButtonImageSet = shade match {
			case Dark => blackForButtons
			case Light => whiteForButtons
		}
		
		override def apply(color: Color): ButtonImageSet =
			ButtonImageSet(SingleColorIcon.this(color)).lowerAlphaOnDisabled
	}
}
