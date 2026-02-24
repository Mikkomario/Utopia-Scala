package utopia.echo.model.settings

import utopia.flow.util.Mutate

/**
 * Common trait for mutable instances that have context size limits
 * @author Mikko Hilpinen
 * @since 24.02.2026, v1.4.1
 */
trait HasMutableContextSizeLimits extends HasContextSizeLimits
{
	// ABSTRACT ----------------------------
	
	def contextSizeLimits_=(newLimits: ContextSizeLimits): Unit
	
	
	// COMPUTED ----------------------------
	
	def maxContextSize_=(max: Int) = updateContextSizeLimits { _.withMax(max) }
	def minContextSize_=(min: Int) = updateContextSizeLimits { _.withMin(min) }
	def additionalContextSize_=(additional: Int) = updateContextSizeLimits { _.withAdditional(additional) }
	
	
	// OTHER    ----------------------------
	
	/**
	 * @param min Minimum context size to use in thinking mode
	 */
	def setMinThinkingContextSize(min: Int) = updateContextSizeLimits { _.withMinWhenThinking(min) }
	
	/**
	 * Modifies the applied context size limits
	 * @param f A function that yields a modified copy of the applied limits
	 */
	def updateContextSizeLimits(f: Mutate[ContextSizeLimits]) = contextSizeLimits = f(contextSizeLimits)
}
