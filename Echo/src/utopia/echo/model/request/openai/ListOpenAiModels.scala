package utopia.echo.model.request.openai

import utopia.annex.controller.ApiClient
import utopia.annex.model.request.GetRequest
import utopia.annex.model.response.RequestResult
import utopia.echo.model.response.openai.OpenAiModelInfo
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysFalse

import scala.concurrent.Future
import scala.language.implicitConversions

object ListOpenAiModels
{
	// ATTRIBUTES   ---------------------
	
	private lazy val default = new ListOpenAiModels()
	
	
	// IMPLICIT -------------------------
	
	implicit def objectAsRequest(o: ListOpenAiModels.type): ListOpenAiModels = o.default
	
	
	// OTHER    --------------------------
	
	/**
	 * @param deprecationView A view that contains true if this request should be retracted
	 * @return A new request for listing Open AI models
	 */
	def withDeprecationView(deprecationView: View[Boolean]) = new ListOpenAiModels(deprecationView)
}

/**
 * A request for listing (Open AI) models
 * @author Mikko Hilpinen
 * @since 26.02.2026, v1.5
 */
class ListOpenAiModels(deprecationView: View[Boolean] = AlwaysFalse) extends GetRequest[Seq[OpenAiModelInfo]]
{
	// ATTRIBUTES   ----------------------
	
	override val path: String = "models"
	override val pathParams: Model = Model.empty
	
	
	// IMPLEMENTED  ---------------------
	
	override def deprecated: Boolean = deprecationView.value
	
	// Expects the response to be either:
	//      - A JSON object containing "data": [...]
	//      - A JSON object array
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[Seq[OpenAiModelInfo]]] =
		prepared.parseValue { body =>
			body.getModelOrVector match {
				case Left(model) => model.tryGet("data") { _.tryParseModelsWith(OpenAiModelInfo) }
				case Right(values) => values.tryMapAll { _.tryModel.flatMap(OpenAiModelInfo.apply) }
			}
		}
}
