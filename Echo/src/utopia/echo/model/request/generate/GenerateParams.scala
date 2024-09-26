package utopia.echo.model.request.generate

import utopia.echo.model.llm.{LlmDesignator, ModelSettings}
import utopia.echo.model.request.RequestParams
import utopia.flow.generic.model.immutable.Value
import utopia.flow.util.Mutate
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysFalse

/**
  * A set of parameters used to construct a request to the generate endpoint.
  * @param query The query to send to the LLM
  * @param settings Behavioral parameters included in this request. Default = empty.
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
case class GenerateParams(query: Query, settings: ModelSettings = ModelSettings.empty,
                          conversationContext: Value = Value.empty, deprecationView: View[Boolean] = AlwaysFalse)
                         (implicit override val llm: LlmDesignator)
	extends RequestParams[GenerateParams]
{
	// COMPUTED -------------------------
	
	/**
	  * @return A request based on these parameters.
	  *         Streaming option still needs to be specified before this request may be sent.
	  */
	def toRequest = Generate(this)
	
	/**
	  * @return Copy of these parameters without conversation context included
	  */
	def withoutContext = withContext(Value.empty)
	
	
	// IMPLEMENTED  ---------------------
	
	override def toLlm(llm: LlmDesignator) = copy()(llm = llm)
	override def withSettings(settings: ModelSettings): GenerateParams = copy(settings = settings)
	override def withDeprecationView(condition: View[Boolean]) = copy(deprecationView = condition)
	
	
	// OTHER    -------------------------
	
	/**
	  * @param context New conversation context to assign
	  * @return Copy of these parameters with the specified context
	  */
	def withContext(context: Value) = copy(conversationContext = context)
	
	/**
	  * @param f A mapping function for the wrapped query
	  * @return Copy of these parameters with a mapped query
	  */
	def mapQuery(f: Mutate[Query]) = copy(query = f(query))
}