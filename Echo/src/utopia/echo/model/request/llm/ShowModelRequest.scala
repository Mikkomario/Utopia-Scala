package utopia.echo.model.request.llm

import utopia.access.http.Method
import utopia.access.http.Method.Post
import utopia.annex.controller.ApiClient
import utopia.annex.model.request.ApiRequest
import utopia.annex.model.response.RequestResult
import utopia.disciple.http.request.Body
import utopia.echo.model.llm.LlmDesignator
import utopia.echo.model.response.llm.ModelShowInfo
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysFalse

import scala.concurrent.Future

object ShowModelRequest
{
	// OTHER    ------------------------
	
	/**
	  * Creates a new request
	  * @param deprecationView A view that contains true when/if this request is deprecated and should be retracted
	  *                        (unless already sent).
	  *                        Default = always false.
	  * @param llm Targeted LLM
	  * @return A new request for retrieving the model's information
	  */
	def apply(deprecationView: View[Boolean] = AlwaysFalse)(implicit llm: LlmDesignator) =
		new ShowModelRequest(deprecationView)
	
	/**
	  * Creates a new request
	  * @param condition A function that returns true if this request has been deprecated and should be retracted
	  *                  (unless already sent).
	  * @param llm Targeted LLM
	  * @return A new request for retrieving the model's information
	  */
	def deprecatingIf(condition: => Boolean)(implicit llm: LlmDesignator) = apply(View(condition))
}

/**
  * A request used for prompting Ollama to provide model information, such as the contents of the model's model-file
  * @author Mikko Hilpinen
  * @since 19.09.2024, v1.1
  */
class ShowModelRequest(deprecationView: View[Boolean] = AlwaysFalse)(implicit llm: LlmDesignator)
	extends ApiRequest[ModelShowInfo]
{
	override def method: Method = Post
	override def path: String = "show"
	
	override def body: Either[Value, Body] = Left(Model.from("name" -> llm.llmName))
	override def deprecated: Boolean = deprecationView.value
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[ModelShowInfo]] =
		prepared.getOne(ModelShowInfo)
}