package utopia.echo.model.settings

import utopia.echo.model.tokenization.TokenCount
import utopia.flow.util.Mutate

/**
 * Common trait for mutable instances that have context size limits
 * @author Mikko Hilpinen
 * @since 24.02.2026, v1.5
 */
trait HasMutableContextSizeLimits extends HasContextSizeLimits
{
	// ABSTRACT ----------------------------
	
	def contextSizeLimits_=(newLimits: ContextSizeLimits): Unit
	
	
	// COMPUTED ----------------------------
	
	def maxContextSize_=(max: TokenCount) = updateContextSizeLimits { _.withMax(max) }
	def minContextSize_=(min: TokenCount) = updateContextSizeLimits { _.withMin(min) }
	def additionalContextSize_=(additional: TokenCount) = updateContextSizeLimits { _.withAdditional(additional) }
	
	
	// OTHER    ----------------------------
	
	/**
	 * @param min Minimum context size to use in thinking mode
	 */
	def setMinThinkingContextSize(min: TokenCount) = updateContextSizeLimits { _.withMinWhenThinking(min) }
	
	/**
	 * Modifies the applied context size limits
	 * @param f A function that yields a modified copy of the applied limits
	 */
	def updateContextSizeLimits(f: Mutate[ContextSizeLimits]) = contextSizeLimits = f(contextSizeLimits)
}
