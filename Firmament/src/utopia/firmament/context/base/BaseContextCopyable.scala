package utopia.firmament.context.base

import utopia.firmament.model.Margins
import utopia.firmament.model.enumeration.SizeCategory
import utopia.firmament.model.enumeration.SizeCategory.{Large, Medium, Small, VeryLarge, VerySmall}
import utopia.firmament.model.stack.{LengthPriority, StackLength}
import utopia.flow.operator.ScopeUsable
import utopia.genesis.text.Font
import utopia.paradigm.color.{Color, ColorLevel, ColorRole}
import utopia.paradigm.enumeration.ColorContrastStandard
import utopia.paradigm.enumeration.ColorContrastStandard.Enhanced
import utopia.paradigm.transform.{Adjustment, LinearSizeAdjustable}

/**
  * Used for creating new copies of self based on base contextual property-assignments
  * @tparam Repr Implementing context type
  * @tparam ColorSensitive A color-sensitive version of this context
  * @author Mikko Hilpinen
  * @since 29.09.2024, v1.3.2
  */
trait BaseContextCopyable[+Repr, +ColorSensitive]
	extends BaseContextPropsView with ScopeUsable[Repr] with LinearSizeAdjustable[Repr]
{
	// ABSTRACT -------------------------
	
	/**
	  * @return Current state of this context as a static context instance
	  */
	def current: StaticBaseContext
	/**
	  * @return This context as a variable context instance
	  */
	def toVariableContext: VariableBaseContext
	
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
	/**
	  * @param allowImageUpscaling Whether image upscaling should be allowed
	  * @return A copy of this context with that setting
	  */
	def withAllowImageUpscaling(allowImageUpscaling: Boolean): Repr
	
	/**
	  * @param size New stack margins size category to use
	  * @return A copy of this context with those stack margins in use
	  */
	def withStackMargin(size: SizeCategory): Repr
	
	/**
	  * @param background A background color
	  * @return A copy of this context against the specified background color
	  */
	def against(background: Color): ColorSensitive
	
	/**
	  * @param f A mapping function for the font used
	  * @return A mapped copy of this context
	  */
	def mapFont(f: Font => Font): Repr
	/**
	  * @param f A mapping function for stack margins to use
	  * @return A mapped copy of this context
	  */
	def mapStackMargin(f: StackLength => StackLength): Repr
	
	
	// COMPUTED --------------------------------
	
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
	  * @return A copy of this context where there is no maximum (stack) margin defined
	  */
	def withoutMaxMargin: Repr = mapStackMargin { _.noMax }
	/**
	  * @return A copy of this context that doesn't allow any space between stacked items
	  */
	def withoutStackMargin = withStackMargin(StackLength.fixedZero)
	/**
	  * @return A copy of this context where the minimum (stack) margin is 0 px
	  */
	def withoutMinMargin = mapStackMargin { _.noMin }
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
	def withLowPriorityMargin = mapStackMargin { _.lowPriority }
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
	def allowingImageUpscaling = withAllowImageUpscaling(true)
	/**
	  * @return A copy of this context with image-upscaling not allowed
	  */
	def withoutImageUpscaling = withAllowImageUpscaling(false)
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
	
	
	// OTHER    ------------------------------
	
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
	  * @param f A mapping function for margins to use
	  * @return A mapped copy of this context
	  */
	def mapMargins(f: Margins => Margins) = withMargins(f(margins))
	
	/**
	  * @param color Background color role
	  * @param shade Background color shade
	  * @return A copy of this context that assumes the specified color combination as the background color.
	  *         Assumes that this context's color scheme is used.
	  */
	def against(color: ColorRole, shade: ColorLevel): ColorSensitive = against(colors(color)(shade))
}
