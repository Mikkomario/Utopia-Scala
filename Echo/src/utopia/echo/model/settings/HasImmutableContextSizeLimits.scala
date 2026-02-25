package utopia.echo.model.settings

import utopia.flow.util.Mutate

/**
 * Common trait for immutable, yet copyable interfaces that specify context size limits
 * @author Mikko Hilpinen
 * @since 24.02.2026, v1.5
 */
trait HasImmutableContextSizeLimits[+Repr] extends HasContextSizeLimits
{
	// ABSTRACT ------------------------
	
	/**
	 * @param limits New context size limits to apply
	 * @return A copy of this instance with the specified limits
	 */
	def withContextSizeLimits(limits: ContextSizeLimits): Repr
	
	
	// OTHER    ------------------------
	
	def withMaxContextSize(max: Int) = mapContextSizeLimits { _.withMax(max) }
	def withMinContextSize(min: Int) = mapContextSizeLimits { _.withMin(min) }
	def withMinContextSizeWhenThinking(min: Int) = mapContextSizeLimits { _.withMinWhenThinking(min) }
	def withAdditionalContextSize(additional: Int) = mapContextSizeLimits { _.withAdditional(additional) }
	
	def mapContextSizeLimits(f: Mutate[ContextSizeLimits]) = withContextSizeLimits(contextSizeLimits)
	
	def mapMaxContextSize(f: Mutate[Int]) = mapContextSizeLimits { _.mapMax(f) }
	def mapMinContextSize(f: Mutate[Int]) = mapContextSizeLimits { _.mapMin(f) }
	def mapMinContextSizeWhenThinking(f: Mutate[Int]) = mapContextSizeLimits { _.mapMinWhenThinking(f) }
	def mapAdditionalContextSize(f: Mutate[Int]) = mapContextSizeLimits { _.mapAdditional(f) }
}
