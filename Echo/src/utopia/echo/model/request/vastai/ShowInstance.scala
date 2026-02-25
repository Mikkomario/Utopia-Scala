package utopia.echo.model.request.vastai

import utopia.annex.controller.ApiClient
import utopia.annex.model.request.GetRequest
import utopia.annex.model.response.RequestResult
import utopia.echo.model.request.vastai.ShowInstance.ShowInstanceResponseParser
import utopia.echo.model.vastai.instance.VastAiInstance
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysFalse

import scala.concurrent.Future
import scala.util.Try

object ShowInstance
{
	// NESTED   ------------------------
	
	private object ShowInstanceResponseParser extends FromModelFactory[VastAiInstance]
	{
		override def apply(model: HasProperties): Try[VastAiInstance] = {
			val readModel = model("instances", "instance").model.getOrElse(model)
			VastAiInstance(readModel)
		}
	}
}

/**
 * Shows information concerning a rented instance / machine / active contract
 * @param instanceId ID of the targeted instance
 * @param deprecationView A view that contains true if this request should be retracted. Default = always false.
 * @author Mikko Hilpinen
 * @since 24.02.2026, v1.4.1
 */
case class ShowInstance(instanceId: Int, deprecationView: View[Boolean] = AlwaysFalse)
	extends GetRequest[VastAiInstance]
{
	// ATTRIBUTES   ----------------------
	
	override val path: String = s"instances/$instanceId"
	override val pathParams: Model = Model.empty
	
	
	// IMPLEMENTED  ---------------------
	
	override def deprecated: Boolean = deprecationView.value
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[VastAiInstance]] =
		prepared.getOne(ShowInstanceResponseParser)
}
