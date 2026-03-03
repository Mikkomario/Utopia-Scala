package utopia.echo.model.request.vastai

import utopia.annex.controller.ApiClient
import utopia.annex.model.request.GetRequest
import utopia.annex.model.response.RequestResult
import utopia.echo.model.vastai.instance.VastAiInstance
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.model.immutable.Model

import scala.concurrent.Future

/**
 * A request used for showing all reserved instances for the user
 * @author Mikko Hilpinen
 * @since 25.02.2026, v1.5
 */
object ShowInstances extends GetRequest[Seq[VastAiInstance]]
{
	// ATTRIBUTES   ------------------------
	
	override val path: String = "instances"
	override val pathParams: Model = Model.empty
	override val deprecated: Boolean = false
	
	
	// IMPLEMENTED  -----------------------
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[Seq[VastAiInstance]]] =
		prepared.parseValue { body =>
			body.getModelOrVector match {
				case Left(body) => body.tryGet("instances") { _.tryParseModelsWith(VastAiInstance) }
				case Right(values) => values.tryMapAll { _.tryModel.flatMap(VastAiInstance.apply) }
			}
		}
}
