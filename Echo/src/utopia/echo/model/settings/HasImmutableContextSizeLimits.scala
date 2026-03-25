package utopia.echo.model.settings

import utopia.echo.model.tokenization.TokenCount
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
	
	def withMaxContextSize(max: TokenCount) = mapContextSizeLimits { _.withMax(max) }
	def withMinContextSize(min: TokenCount, sameWithThink: Boolean = false) =
		mapContextSizeLimits { _.withMin(min, sameWithThink) }
	def withMinContextSizeWhenThinking(min: TokenCount) = mapContextSizeLimits { _.withMinWhenThinking(min) }
	def withAdditionalContextSize(additional: TokenCount) = mapContextSizeLimits { _.withAdditional(additional) }
	
	def mapContextSizeLimits(f: Mutate[ContextSizeLimits]) = withContextSizeLimits(f(contextSizeLimits))
	
	def mapMaxContextSize(f: Mutate[TokenCount]) = mapContextSizeLimits { _.mapMax(f) }
	def mapMinContextSize(f: Mutate[TokenCount]) = mapContextSizeLimits { _.mapMin(f) }
	def mapMinContextSizeWhenThinking(f: Mutate[TokenCount]) = mapContextSizeLimits { _.mapMinWhenThinking(f) }
	def mapAdditionalContextSize(f: Mutate[TokenCount]) = mapContextSizeLimits { _.mapAdditional(f) }
}
