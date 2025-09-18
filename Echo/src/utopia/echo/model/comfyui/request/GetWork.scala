package utopia.echo.model.comfyui.request

import utopia.annex.controller.ApiClient
import utopia.annex.model.request.GetRequest
import utopia.annex.model.response.RequestResult
import utopia.flow.generic.model.immutable.Model
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysFalse

import scala.concurrent.Future

/**
 * Requests historical information about a previously requested work
 *
 * @author Mikko Hilpinen
 * @since 05.08.2025, v1.4
 */
class GetWork(promptId: String, deprecationView: View[Boolean] = AlwaysFalse) extends GetRequest[Model]
{
	// ATTRIBUTES   ---------------------
	
	override lazy val path: String = s"history/$promptId"
	override val pathParams: Model = Model.empty
	
	
	// IMPLEMENTED  --------------------
	
	override def deprecated: Boolean = deprecationView.value
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[Model]] = prepared.getModel
}
