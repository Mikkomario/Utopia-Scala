package utopia.firmament.context.text

import utopia.firmament.context.color.ColorContextCopyable
import utopia.firmament.model.enumeration.SizeCategory
import utopia.firmament.model.stack.{StackInsets, StackLength}
import utopia.firmament.model.stack.LengthExtensions._
import utopia.genesis.text.Font
import utopia.paradigm.enumeration.{Alignment, Axis2D, Direction2D, LinearAlignment}
import utopia.paradigm.enumeration.Alignment.Center
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.LinearAlignment.Middle
import utopia.paradigm.shape.template.HasDimensions

/**
  * Common trait for text context implementations that can create copies of themselves
  * @author Mikko Hilpinen
  * @since 05.10.2024, v1.3.2
  */
trait TextContextCopyable[+Repr] extends ColorContextCopyable[Repr, Repr] with TextContextPropsView
{
	// ABSTRACT ------------------------
	
	/**
	  * @return Copy of this context that uses the default prompt font
	  */
	def withDefaultPromptFont: Repr
	
	/**
	  * @param font Font to use with prompts
	  * @return A copy of this context that uses the specified font for prompts
	  */
	def withPromptFont(font: Font): Repr
	
	/**
	  * @param alignment Text alignment to use
	  * @return A copy of this context with the specified text alignment
	  */
	def withTextAlignment(alignment: Alignment): Repr
	
	/**
	  * @param insets Text insets to apply
	  * @return A copy of this context with the specified text insets in use
	  */
	def withTextInsets(insets: StackInsets): Repr
	
	/**
	  * @param threshold A width threshold after which automated line-splitting should occur.
	  *                  None if no automated line-splitting should occur.
	  * @return Copy of this context with the specified split-threshold.
	  */
	def withLineSplitThreshold(threshold: Option[Double]): Repr
	/**
	  * @param margin Margin to place between text lines, where applicable
	  * @return Copy of this context that uses the specified margin
	  */
	def withMarginBetweenLines(margin: StackLength): Repr
	
	/**
	  * @param allowLineBreaks Whether line breaks should be allowed within text components
	  * @return Copy of this context with the specified setting in use
	  */
	def withAllowLineBreaks(allowLineBreaks: Boolean): Repr
	/**
	  * @param allowTextShrink Whether the text should be allowed to be downscaled in order to conserve space
	  * @return Copy of this context with the specified setting in use
	  */
	def withAllowTextShrink(allowTextShrink: Boolean): Repr
	
	/**
	  * @param f A mapping function for text insets
	  * @return A modified copy of this context
	  */
	def mapTextInsets(f: StackInsets => StackInsets): Repr
	
	
	// COMPUTED ----------------------------
	
	/**
	  * @return Copy of this context with text centered horizontally and vertically
	  */
	def withCenteredText = withTextAlignment(Center)
	/**
	  * @return Copy of this context where text is centered horizontally
	  */
	def withHorizontallyCenteredText = withTextAlignedToCenterAlong(X)
	/**
	  * @return Copy of this context with vertically centered text
	  */
	def withVerticallyCenteredText = withTextAlignedToCenterAlong(Y)
	
	/**
	  * @return Copy of this context where all text insets are set to zero
	  */
	def withoutTextInsets = withTextInsets(StackInsets.zero)
	/**
	  * @return Copy of this context where horizontal text insets have been set to zero
	  */
	def withoutHorizontalTextInsets = withoutTextInsetsAlong(X)
	/**
	  * @return Copy of this context where vertical text insets have been set to zero
	  */
	def withoutVerticalTextInsets = withoutTextInsetsAlong(Y)
	def withTextInsetsHalved = withTextInsetsScaledBy(0.5)
	def withHorizontalTextInsetsHalved = withHorizontalTextInsetsScaledBy(0.5)
	def withVerticalTextInsetsHalved = withVerticalTextInsetsScaledBy(0.5)
	
	/**
	  * @return Copy of this context where text expands easily to right
	  */
	def withTextExpandingToRight = withTextExpandingTo(Direction2D.Right)
	/**
	  * @return Copy of this context where text expands easily horizontally
	  */
	def withHorizontallyExpandingText = withTextExpandingAlong(X)
	
	/**
	  * @return Copy of this context with no automatic line-splitting in use
	  */
	def withoutAutomatedLineSplitting = withLineSplitThreshold(None)
	
	/**
	  * @return Copy of this context without any additional margin between text lines
	  */
	def withoutMarginBetweenLines = withMarginBetweenLines(StackLength.fixedZero)
	def withSmallMarginBetweenLines = withMarginBetweenLines(margins.small.downscaling)
	def withSmallerMarginBetweenLines = mapMarginBetweenLines { _.smaller(margins.adjustment) }
	def withLargerMarginBetweenLines = mapMarginBetweenLines { _.larger(margins.adjustment) }
	
	/**
	  * @return Copy of this context that only allows single line components
	  */
	def singleLine = withAllowLineBreaks(false)
	/**
	  * @return Copy of this context that allows multi-line components
	  */
	def manyLines = withAllowLineBreaks(true)
	
	/**
	  * @return Context copy that allows shrinking text
	  */
	def withShrinkingText = if (allowTextShrink) self else withAllowTextShrink(true)
	/**
	  * @return Context copy that doesn't allow shrinking text
	  */
	def withoutShrinkingText = if (allowTextShrink) withAllowTextShrink(false) else self
	
	
	// IMPLEMENTED  ----------------------
	
	override def forTextComponents: Repr = self
	
	
	// OTHER    --------------------------
	
	/**
	  * @param axis Targeted axis
	  * @param alignment New text alignment to use
	  * @return A copy of this context with that alignment in place
	  */
	def withTextAlignmentAlong(axis: Axis2D, alignment: LinearAlignment) =
		mapTextAlignment { _.withDimension(axis, alignment) }
	/**
	  * @param alignment Horizontal text alignment to use
	  * @return A copy of this context with that alignment in place
	  */
	def withHorizontalTextAlignment(alignment: LinearAlignment) = withTextAlignmentAlong(X, alignment)
	/**
	  * @param alignment Vertical text alignment to use
	  * @return A copy of this context with that alignment in place
	  */
	def withVerticalTextAlignment(alignment: LinearAlignment) = withTextAlignmentAlong(Y, alignment)
	
	/**
	  * @param direction Direction to which text should be aligned
	  * @return A copy of this context where text is aligned to that direction
	  */
	def withTextAlignedTo(direction: Direction2D) = mapTextAlignment { _.toDirection(direction) }
	/**
	  * @param axis Targeted axis
	  * @return Copy of this context where text is aligned to center along the specified axis
	  */
	def withTextAlignedToCenterAlong(axis: Axis2D) = mapTextAlignment { _.withDimension(axis, Middle) }
	
	/**
	  * @param f A mapping function for text alignment
	  * @return A modified copy of this context
	  */
	def mapTextAlignment(f: Alignment => Alignment) = withTextAlignment(f(textAlignment))
	/**
	  * @param axis Targeted axis
	  * @param f A mapping function for text alignment along the specified axis
	  * @return A modified copy of this context
	  */
	def mapTextAlignmentAlong(axis: Axis2D)(f: LinearAlignment => LinearAlignment) =
		mapTextAlignment { _.mapDimension(axis)(f) }
	/**
	  * @param f A mapping function for horizontal text alignment
	  * @return A modified copy of this context
	  */
	def mapHorizontalTextAlignment(f: LinearAlignment => LinearAlignment) = mapTextAlignmentAlong(X)(f)
	/**
	  * @param f A mapping function for vertical text alignment
	  * @return A modified copy of this context
	  */
	def mapVerticalTextAlignment(f: LinearAlignment => LinearAlignment) = mapTextAlignmentAlong(Y)(f)
	
	def mapTextInsetsAlong(axis: Axis2D)(f: StackLength => StackLength) = mapTextInsets { _.mapAxis(axis)(f) }
	def mapHorizontalTextInsets(f: StackLength => StackLength) = mapTextInsetsAlong(X)(f)
	def mapVerticalTextInsets(f: StackLength => StackLength) = mapTextInsetsAlong(Y)(f)
	def mapTextInset(side: Direction2D)(f: StackLength => StackLength) = mapTextInsets { _.mapSide(side)(f) }
	
	/**
	  * @param axis Targeted axis
	  * @return A copy of this context where text easily expands along that axis
	  */
	def withTextExpandingAlong(axis: Axis2D) = mapTextInsets { _.expandingAlong(axis) }
	/**
	  * @param direction Targeted direction
	  * @return A copy of this context where text easily expands towards that direction
	  */
	def withTextExpandingTo(direction: Direction2D) = mapTextInsets { _.expandingTowards(direction) }
	
	/**
	  * @param scaling A scaling modifier
	  * @return A copy of this context with text insets scaled by the specified factor
	  */
	def withTextInsetsScaledBy(scaling: Double) = mapTextInsets { _ * scaling }
	/**
	  * @param scaling A two-dimensional scaling modifier
	  * @return Copy of this context with text insets multiplied with the specified scaling modifier
	  */
	def withTextInsetsScaledBy(scaling: HasDimensions[Double]) =
		mapTextInsets { _.mergeWith(scaling) { (insets, scaling) => insets.map { _ * scaling } } }
	def withHorizontalTextInsetsScaledBy(scaling: Double) = mapHorizontalTextInsets { _ * scaling }
	def withVerticalTextInsetsScaledBy(scaling: Double) = mapVerticalTextInsets { _ * scaling }
	def withTextInsetScaledBy(side: Direction2D, scaling: Double) = mapTextInset(side) { _ * scaling }
	/**
	  * @param insetsSize New text inset sizes to apply on all sides (relative to margins)
	  * @return A copy of this context with specified inset sizes
	  */
	def withTextInsets(insetsSize: SizeCategory): Repr = {
		val medium = margins.medium
		val targetScale = insetsSize.scaling(margins.adjustment)
		mapTextInsets { _.map { current =>
			val currentScale = current.optimal / medium
			current * (targetScale/currentScale)
		} }
	}
	/**
	  * @param axis Targeted axis
	  * @param insetsSize New text inset sizes to apply along the specified axis (relative to margins)
	  * @return A copy of this context with specified inset sizes
	  */
	def withTextInsetsAlong(axis: Axis2D, insetsSize: SizeCategory) = {
		// WET WET
		val medium = margins.medium
		val targetScale = insetsSize.scaling(margins.adjustment)
		mapTextInsets {
			_.mapAxis(axis) { current =>
				val currentScale = current.optimal / medium
				current * (targetScale / currentScale)
			}
		}
	}
	/**
	  * @param insetsSize New horizontal text inset sizes to apply (relative to margins)
	  * @return A copy of this context with specified inset sizes
	  */
	def withHorizontalTextInsets(insetsSize: SizeCategory) = withTextInsetsAlong(X, insetsSize)
	/**
	  * @param insetsSize New vertical text inset sizes to apply (relative to margins)
	  * @return A copy of this context with specified inset sizes
	  */
	def withVerticalTextInsets(insetsSize: SizeCategory) = withTextInsetsAlong(Y, insetsSize)
	
	/**
	  * @param axis Targeted axis
	  * @return A copy of this context where text insets along that axis are fixed to zero
	  */
	def withoutTextInsetsAlong(axis: Axis2D) = mapTextInsets { _.withoutAxis(axis) }
	/**
	  * @param direction Targeted direction
	  * @return A copy of this context where text insets don't include the specified direction
	  */
	def withoutTextInsetsTowards(direction: Direction2D) = mapTextInsets { _ - direction }
	
	/**
	  * @param threshold A width threshold after which lines should be split.
	  * @return A copy of this context with the specified line-split threshold.
	  */
	def withLineSplitThreshold(threshold: Double): Repr = withLineSplitThreshold(Some(threshold))
	
	/**
	  * @param f A mapping function for between-lines margin
	  * @return A modified copy of this context
	  */
	def mapMarginBetweenLines(f: StackLength => StackLength) = withMarginBetweenLines(f(betweenLinesMargin))
	/**
	  * @param margin Margin to place between the lines (relative to margins)
	  * @return A copy of this context with that between lines -margin
	  */
	def withMarginBetweenLines(margin: SizeCategory) = {
		val targetScaling = margin.scaling(margins.adjustment)
		val current = betweenLinesMargin
		val currentScaling = current.optimal / margins.medium
		current * (targetScaling/currentScaling)
	}
}
