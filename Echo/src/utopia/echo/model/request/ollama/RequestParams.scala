package utopia.echo.model.request.ollama

import utopia.echo.model.llm.LlmDesignator
import utopia.echo.model.settings.HasImmutableModelSettings
import utopia.flow.util.UncertainBoolean
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysFalse

/**
  * Common trait for sets of parameters used to construct a request to an Ollama chat or generate endpoint.
  * @author Mikko Hilpinen
  * @since 31.08.2024, v1.1
  */
trait RequestParams[+Repr] extends HasImmutableModelSettings[Repr]
{
	// ABSTRACT -------------------------
	
	/**
	  * @return Name of the targeted LLM
	  */
	def llm: LlmDesignator
	/**
	  * @return A view which contains true once/if this request gets deprecated and should be retracted
	  *         (if not yet sent out)
	  */
	def deprecationView: View[Boolean]
	/**
	 * @return Whether to enable thinking / reflection features for LLMs that support it.
	 *              - true if thinking should be enabled => Yields a separate thinking and text output
	 *              - false if thinking should be disabled => The LLM should not enter thinking mode
	 *              - Uncertain if no adjustment should be made =>
	 *                The LLM may enter thinking mode. Output will be generated as normal text.
	 */
	def think: UncertainBoolean
	
	/**
	  * @param llm Targeted LLM
	  * @return Copy of these parameters with the specified LLM assigned as the target
	  */
	def toLlm(llm: LlmDesignator): Repr
	/**
	  * Replaces the deprecation view listed in these parameters
	  * @param condition A view which contains true once or if this request deprecates.
	  *                  Deprecations are only applied until this request is sent.
	  * @return Copy of these parameters with the specified deprecation condition
	  */
	def withDeprecationView(condition: View[Boolean]): Repr
	/**
	 * @param think Whether to allow, disallow, or not affect LLM thinking
	 * @return A copy of these parameters with the specified think setting
	 */
	def withThink(think: UncertainBoolean): Repr
	
	
	// COMPUTED -------------------------
	
	/**
	  * @return Copy of these parameters with no deprecation condition
	  */
	def neverDeprecating = withDeprecationView(AlwaysFalse)
	
	/**
	 * @return A copy of these parameters with thinking explicitly enabled
	 */
	def thinking = withThink(true)
	/**
	 * @return A copy of these parameters with thinking disabled
	 */
	def notThinking = withThink(false)
	
	
	// OTHER    -------------------------
	
	/**
	  * @param condition A call-by-name function used for testing whether this request is deprecated.
	  *                  Deprecations are only applied until this request is sent.
	  * @return Copy of these parameters with the specified deprecation condition
	  */
	def withDeprecationCondition(condition: => Boolean) = withDeprecationView(View(condition))
}