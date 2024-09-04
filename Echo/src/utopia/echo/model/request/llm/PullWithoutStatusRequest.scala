package utopia.echo.model.request.llm

import utopia.access.http.Method
import utopia.access.http.Method.Post
import utopia.annex.controller.ApiClient
import utopia.annex.model.request.ApiRequest
import utopia.annex.model.response.RequestResult
import utopia.disciple.http.request.Body
import utopia.echo.model.LlmDesignator
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysFalse

import scala.concurrent.Future

object PullWithoutStatusRequest
{
	/**
	  * Creates a new condition for pulling an LLM
	  * @param deprecationView A view which contains true if this request becomes deprecated.
	  *                        Deprecated requests are not sent.
	  *                        If deprecation becomes true after this request has been sent, it is ignored.
	  *                        Default = never.
	  * @param llm Targeted LLM (implicit)
	  * @return A new request for pulling LLM data
	  */
	def apply(deprecationView: View[Boolean] = AlwaysFalse)(implicit llm: LlmDesignator) =
		new PullWithoutStatusRequest(deprecationView)
	
	/**
	  * Creates a new condition for pulling an LLM
	  * @param deprecationCondition A call-by-name function which returns true if this request becomes deprecated.
	  *                        Deprecated requests are not sent.
	  *                        If deprecation becomes true after this request has been sent, it is ignored.
	  *                        Default = never.
	  * @param llm Targeted LLM (implicit)
	  * @return A new request for pulling LLM data
	  */
	def deprecatingIf(deprecationCondition: => Boolean)(implicit llm: LlmDesignator) =
		apply(View(deprecationCondition))
}

/**
  * This request prompts the Ollama server to pull an LLM from a remote server.
  * This variant of pull request doesn't track the pull / download status at all,
  * and only yields a response once the whole process has completed, which may take a long time.
  *
  * For status-tracking requests, which are better in for use-cases with a UI, see [[PullStreamingRequest]].
  *
  * @param deprecationView A view which contains true if this request becomes deprecated
  * @param llm Targeted LLM's designator
  * @author Mikko Hilpinen
  * @since 03.09.2024, v1.1
  */
class PullWithoutStatusRequest(deprecationView: View[Boolean] = AlwaysFalse)(implicit llm: LlmDesignator)
	extends ApiRequest[String]
{
	// IMPLEMENTED  -------------------------
	
	override def method: Method = Post
	override def path: String = "pull"
	override def body: Either[Value, Body] = Left(Model.from("name" -> llm.name, "stream" -> false))
	
	override def deprecated: Boolean = deprecationView.value
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[String]] =
		prepared.getOne { model =>
			model("status").string.toTry {
				new NoSuchElementException(
					s"Response model didn't contain property \"status\". Available properties were: [${
						model.nonEmptyPropertiesIterator.map { _.name }.mkString(", ") }]")
			}
		}
}
