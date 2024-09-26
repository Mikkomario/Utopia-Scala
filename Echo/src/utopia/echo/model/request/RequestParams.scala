package utopia.echo.model.request

import utopia.echo.model.llm.{HasImmutableModelSettings, LlmDesignator}
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
	
	
	// COMPUTED -------------------------
	
	/**
	  * @return Copy of these parameters with no deprecation condition
	  */
	def neverDeprecating = withDeprecationView(AlwaysFalse)
	
	
	// OTHER    -------------------------
	
	/**
	  * @param condition A call-by-name function used for testing whether this request is deprecated.
	  *                  Deprecations are only applied until this request is sent.
	  * @return Copy of these parameters with the specified deprecation condition
	  */
	def withDeprecationCondition(condition: => Boolean) = withDeprecationView(View(condition))
}