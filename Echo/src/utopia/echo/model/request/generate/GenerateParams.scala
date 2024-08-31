package utopia.echo.model.request.generate

import utopia.echo.model.LlmDesignator
import utopia.echo.model.enumeration.ModelParameter
import utopia.flow.generic.model.immutable.Value
import utopia.flow.util.Mutate
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysFalse

/**
  * A set of parameters used to construct a request to the generate endpoint.
  * @param query The query to send to the LLM
  * @param options Behavioral parameters included in this request. Default = empty.
  * @param conversationContext 'context' property returned by the last LLM response,
  *                             if conversation context should be kept.
  *                             Default = empty = new conversation.
  * @param deprecationView A function which yields true if this request gets deprecated and should be retracted
  *                        (if not yet sent out).
  *                        Default = always false.
  * @param llm Name of the targeted LLM
  * @author Mikko Hilpinen
  * @since 31.08.2024, v1.1
  */
case class GenerateParams(query: Query, options: Map[ModelParameter, Value] = Map(),
                          conversationContext: Value = Value.empty, deprecationView: View[Boolean] = AlwaysFalse)
                         (implicit val llm: LlmDesignator)
{
	// COMPUTED -------------------------
	
	/**
	  * @return A request based on these parameters.
	  *         Streaming option still needs to be specified before this request may be sent.
	  */
	def toRequest = Generate(this)
	
	/**
	  * @return Copy of these parameters with no options / parameters assigned
	  */
	def withoutOptions = copy(options = Map())
	/**
	  * @return Copy of these parameters without conversation context included
	  */
	def withoutContext = withContext(Value.empty)
	/**
	  * @return Copy of these parameters with no deprecation condition
	  */
	def neverDeprecating = withDeprecationView(AlwaysFalse)
	
	
	// OTHER    -------------------------
	
	/**
	  * @param context New conversation context to assign
	  * @return Copy of these parameters with the specified context
	  */
	def withContext(context: Value) = copy(conversationContext = context)
	/**
	  * @param condition A view which contains true once or if this request deprecates.
	  *                  Deprecations are only applied until this request is sent.
	  * @return Copy of these parameters with the specified deprecation condition
	  */
	def withDeprecationView(condition: View[Boolean]) = copy(deprecationView = condition)
	/**
	  * @param condition A call-by-name function used for testing whether this request is deprecated.
	  *                  Deprecations are only applied until this request is sent.
	  * @return Copy of these parameters with the specified deprecation condition
	  */
	def withDeprecationCondition(condition: => Boolean) = withDeprecationView(View(condition))
	/**
	  * @param llm Targeted LLM
	  * @return Copy of these parameters with the specified LLM assigned as the target
	  */
	def toLlm(llm: LlmDesignator) = copy()(llm = llm)
	
	/**
	  * @param f A mapping function for the wrapped query
	  * @return Copy of these parameters with a mapped query
	  */
	def mapQuery(f: Mutate[Query]) = copy(query = f(query))
	/**
	  * @param f A mapping function for the applied parameters / options
	  * @return Copy of these parameters with mapped parameters
	  */
	def mapOption(f: Mutate[Map[ModelParameter, Value]]) = copy(options = f(options))
	
	/**
	  * @param parameter Parameter to assign
	  * @param value Value assigned to the specified parameter
	  * @return Copy of these parameters with the specified value assigned
	  */
	def withOption(parameter: ModelParameter, value: Value) = mapOption { _ + (parameter -> value) }
	/**
	  * @param parameter A parameter value pair to assign
	  * @return Copy of these parameters with the specified parameter assigned
	  */
	def +(parameter: (ModelParameter, Value)) = mapOption { _ + parameter }
}