package utopia.firmament.context

import utopia.firmament.localization.Localizer
import utopia.firmament.model.Margins
import utopia.firmament.model.enumeration.SizeCategory
import utopia.firmament.model.enumeration.SizeCategory.{Large, Medium, Small, VeryLarge, VerySmall}
import utopia.firmament.model.stack.{LengthPriority, StackLength}
import utopia.flow.operator.ScopeUsable
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.text.Font
import utopia.paradigm.color.{Color, ColorLevel, ColorRole, ColorScheme}
import utopia.paradigm.enumeration.ColorContrastStandard
import utopia.paradigm.enumeration.ColorContrastStandard.Enhanced
import utopia.paradigm.transform.{Adjustment, SizeAdjustable}

/**
  * A trait common for basic component context implementations
  * @author Mikko Hilpinen
  * @since 27.4.2020, Reflection v1.2
  * @tparam Repr This context type
  * @tparam ColorSensitive A color sensitive version of this context
  */
trait BaseContextLike[+Repr, +ColorSensitive] extends Any with ScopeUsable[Repr] with SizeAdjustable[Repr]
{
	// ABSTRACT	-------------------------
	
	/**
	  * @return Actor handler used for distributing action events
	  */
	def actorHandler: ActorHandler
	
	/**
	  * @return A localizer used in this context
	  */
	def localizer: Localizer
	/**
	  * @return Used font
	  */
	def font: Font
	
	/**
	  * @return The color scheme to be used
	  */
	def colors: ColorScheme
	/**
	  * @return Color contrast standard being applied
	  */
	def contrastStandard: ColorContrastStandard
	
	/**
	  * @return Used margins
	  */
	def margins: Margins
	/**
	  * @return The default margin placed between items in a stack
	  */
	def stackMargin: StackLength
	/**
	  * @return The margin placed between items in a stack when they are more closely related
	  */
	def smallStackMargin: StackLength
	
	/**
	  * @return Whether images and icons should be allowed to scale above their original resolution. When this is
	  *         enabled, images will fill the desired screen space but they will be blurry.
	  */
	def allowImageUpscaling: Boolean
	
	/**
	  * @param font New font to use
	  * @return A copy of this context with that font
	  */
	def withFont(font: Font): Repr
	/**
	  * @param standard Color contrast standard to follow
	  * @return A copy of this context that uses the specified color contrast standards
	  */
	def withColorContrastStandard(standard: ColorContrastStandard): Repr
	/**
	  * @param margins Margins to use
	  * @return A copy of this context with those margins
	  */
	def withMargins(margins: Margins): Repr
	/**
	  * @param stackMargin New stack margins to apply
	  * @return A copy of this context with those margins
	  */
	def withStackMargin(stackMargin: StackLength): Repr
	@deprecated("Renamed to withStackMargin", "v1.1")
	def withStackMargins(stackMargin: StackLength): Repr = withStackMargin(stackMargin)
	/**
	  * @param allowImageUpscaling Whether image upscaling should be allowed
	  * @return A copy of this context with that setting
	  */
	def withAllowImageUpscaling(allowImageUpscaling: Boolean): Repr
	
	/**
	  * @param background A background color
	  * @return A copy of this context against the specified background color
	  */
	def against(background: Color): ColorSensitive
	
	
	// COMPUTED -------------------------
	
	/**
	  * @return Smallest allowed (stack) margin
	  */
	def minMargin = stackMargin.min
	/**
	  * @return Optimal (stack) margin
	  */
	def optimalMargin = stackMargin.optimal
	/**
	  * @return Largest allowed (stack) margin
	  */
	def maxMargin = stackMargin.max
	
	/**
	  * @return Button border width to use by default
	  */
	def buttonBorderWidth = margins.verySmall
	
	/**
	  * @return A copy of this context that uses bold font
	  */
	def bold = mapFont { _.bold }
	/**
	  * @return A copy of this context that uses italic font
	  */
	def italic = mapFont { _.italic }
	
	/**
	  * @return A copy of this context with strict color contrast standards being applied
	  */
	def withEnhancedColorContrast = withColorContrastStandard(Enhanced)
	
	/**
	  * @return A copy of this context that doesn't allow any space between stacked items
	  */
	def withoutStackMargin = withStackMargin(StackLength.fixedZero)
	/**
	  * @return A copy of this context where the minimum (stack) margin is 0 px
	  */
	def withoutMinMargin = mapStackMargin { _.noMin }
	/**
	  * @return A copy of this context where there is no maximum (stack) margin defined
	  */
	def withoutMaxMargin = if (stackMargin.hasMax) mapStackMargin { _.noMax } else self
	/**
	  * @return A copy of this context where (stack) margins expand easily
	  */
	def withExpandingMargin = mapStackMargin { _.expanding }
	/**
	  * @return A copy of this context where (stack) margins shrink easily
	  */
	def withShrinkingMargin = mapStackMargin { _.shrinking }
	/**
	  * @return A copy of this context where (stack) margins shrink and expand easily
	  */
	def withLowPriorityMargin = mapStackMargin { _.withLowPriority }
	/**
	  * @return A copy of this context where (stack) margins don't shrink or expand easily
	  */
	def withNormalPriorityMargin = mapStackMargin { _.withPriority(LengthPriority.Normal) }
	/**
	  * @return A copy of this context with smaller stack margins in place
	  */
	def withSmallerStackMargin = mapStackMargin { _.smaller(margins.adjustment) }
	/**
	  * @return A copy of this context with larger stack margins in place
	  */
	def withLargerStackMargin = mapStackMargin { _.larger(margins.adjustment) }
	/**
	  * @return A copy of this context with medium (i.e. standard) stack margins in place
	  */
	def withMediumStackMargin = withStackMargin(Medium)
	/**
	  * @return A copy of this context with small stack margins in place
	  */
	def withSmallStackMargin = withStackMargin(Small)
	/**
	  * @return A copy of this context with very small stack margins in place
	  */
	def withVerySmallStackMargin = withStackMargin(VerySmall)
	/**
	  * @return A copy of this context with large stack margins in place
	  */
	def withLargeStackMargin = withStackMargin(Large)
	/**
	  * @return A copy of this context with very large stack margins in place
	  */
	def withVeryLargeStackMargin = withStackMargin(VeryLarge)
	
	/**
	  * @return A copy of this context with image-upscaling allowed
	  */
	def allowingImageUpscaling =
		if (allowImageUpscaling) self else withAllowImageUpscaling(true)
	/**
	  * @return A copy of this context with image-upscaling not allowed
	  */
	def withoutImageUpscaling =
		if (allowImageUpscaling) withAllowImageUpscaling(false) else self
	/**
	  * @return Copy of this context that doesn't allow image upscaling
	  */
	def withUpscalingImages = withAllowImageUpscaling(true)
	/**
	  * @return Copy of this context that allows image upscaling
	  */
	def withoutUpscalingImages = withAllowImageUpscaling(false)
	
	/**
	  * @param adj Implicit adjustments to apply
	  * @return A copy of this context with larger font
	  */
	def withLargerFont(implicit adj: Adjustment) = mapFont { _.larger }
	/**
	  * @param adj Implicit adjustments to apply
	  * @return A copy of this context with larger font
	  */
	def withSmallerFont(implicit adj: Adjustment) = mapFont { _.smaller }
	
	@deprecated("Replaced with stackMargin", "v1.0")
	def defaultStackMargin = stackMargin
	@deprecated("Replaced with smallStackMargin", "v1.0")
	def relatedItemsStackMargin = smallStackMargin
	
	
	// OTHER    -------------------------
	
	/**
	  * @param scaling Targeted stack margin size
	  * @return Stack margin of this context, scaled to the specified size
	  */
	def scaledStackMargin(scaling: SizeCategory) = {
		val currentLevel = stackMargin.optimal / margins.medium
		val targetLevel = margins(scaling) / margins.medium
		if (currentLevel == targetLevel) stackMargin else stackMargin * (targetLevel / currentLevel)
	}
	
	/**
	  * @param f A mapping function for the font used
	  * @return A mapped copy of this context
	  */
	def mapFont(f: Font => Font) = withFont(f(font))
	/**
	  * @param f A mapping function for margins to use
	  * @return A mapped copy of this context
	  */
	def mapMargins(f: Margins => Margins) = withMargins(f(margins))
	/**
	  * @param f A mapping function for stack margins to use
	  * @return A mapped copy of this context
	  */
	def mapStackMargin(f: StackLength => StackLength) = withStackMargin(f(stackMargin))
	@deprecated("Renamed to mapStackMargin", "v1.1")
	def mapStackMargins(f: StackLength => StackLength) = mapStackMargin(f)
	
	/**
	  * @param scaling A scaling to apply to the used font
	  * @return A copy of this context with scaled font
	  */
	def withFontScaledBy(scaling: Double) = mapFont { _ * scaling }
	/**
	  * @param scaling A scaling to apply to standard (non-stack) margins
	  * @return A copy of this context with scaled margins
	  */
	def withMarginsScaledBy(scaling: Double) = mapMargins { _ * scaling }
	/**
	  * @param scaling Scaling to apply to stack margins
	  * @return A copy of this context with scaled stack margins
	  */
	def withStackMarginScaledBy(scaling: Double) = mapStackMargin { _ * scaling }
	@deprecated("Renamed to withStackMarginScaledBy", "v1.1")
	def withStackMarginsScaledBy(scaling: Double) = withStackMarginScaledBy(scaling)
	
	/**
	  * @param minMargin New minimum (stack) margin to use
	  * @return A copy of this context with that margin applying
	  */
	def withMinMargin(minMargin: Double) = mapStackMargin { _.withMin(minMargin) }
	/**
	  * @param optimalMargin New optimal (stack) margin to use
	  * @return A copy of this context with that margin applying
	  */
	def withOptimalMargin(optimalMargin: Double) = mapStackMargin { _.withOptimal(optimalMargin) }
	/**
	  * @param maxMargin New maximum margin to use
	  * @return A copy of this context with that margin applying
	  */
	def withMaxMargin(maxMargin: Double) = mapStackMargin { _.withMax(maxMargin) }
	
	/**
	  * @param size New stack margins size category to use
	  * @return A copy of this context with those stack margins in use
	  */
	def withStackMargin(size: SizeCategory): Repr = withStackMargin(scaledStackMargin(size))
	
	/**
	  * @param color Background color role
	  * @param shade Background color shade
	  * @return A copy of this context that assumes the specified color combination as the background color.
	  *         Assumes that this context's color scheme is used.
	  */
	def against(color: ColorRole, shade: ColorLevel): ColorSensitive = against(colors(color)(shade))
	
	@deprecated("Replaced with .against(Color)", "v1.0")
	def inContextWithBackground(color: Color) = against(color)
}
