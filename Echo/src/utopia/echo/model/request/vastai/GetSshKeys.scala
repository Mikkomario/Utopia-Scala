package utopia.echo.model.request.vastai

import utopia.annex.controller.ApiClient
import utopia.annex.model.request.GetRequest
import utopia.annex.model.response.RequestResult
import utopia.disciple.model.error.RequestFailedException
import utopia.echo.model.request.vastai.GetSshKeys.GetSshKeysResponseParser
import utopia.echo.model.vastai.instance.SshKey
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysFalse

import scala.concurrent.Future
import scala.util.{Failure, Try}

object GetSshKeys
{
	// NESTED   --------------------------
	
	private object GetSshKeysResponseParser extends FromModelFactory[Seq[SshKey]]
	{
		override def apply(model: HasProperties): Try[Seq[SshKey]] = {
			if (model("success").booleanOr(true))
				model.tryGet("ssh_keys") { _.tryVectorWith { _.tryModel.flatMap(SshKey.apply) } }
			else
				Failure(new RequestFailedException(s"Server responded with: $model"))
		}
	}
}

/**
 * Requests a list of SSH keys attached to a rented instance
 * @author Mikko Hilpinen
 * @since 01.03.2026, v1.5
 */
case class GetSshKeys(instanceId: Int, deprecationView: View[Boolean] = AlwaysFalse) extends GetRequest[Seq[SshKey]]
{
	// ATTRIBUTES   ------------------------
	
	override val path: String = s"instances/$instanceId/ssh"
	override val pathParams: Model = Model.empty
	
	
	// IMPLEMENTED  ------------------------
	
	override def deprecated: Boolean = deprecationView.value
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[Seq[SshKey]]] =
		prepared.getOne(GetSshKeysResponseParser)
}