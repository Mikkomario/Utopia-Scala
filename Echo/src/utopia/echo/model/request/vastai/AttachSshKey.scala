package utopia.echo.model.request.vastai

import utopia.access.model.enumeration.Method
import utopia.access.model.enumeration.Method.Post
import utopia.annex.controller.ApiClient
import utopia.annex.model.request.ApiRequest
import utopia.annex.model.response.RequestResult
import utopia.disciple.model.error.RequestFailedException
import utopia.disciple.model.request.Body
import utopia.echo.model.request.vastai.AttachSshKey.AttachSshKeyResponseParser
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysFalse

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object AttachSshKey
{
	// NESTED   ------------------------
	
	private object AttachSshKeyResponseParser extends FromModelFactory[String]
	{
		override def apply(model: HasProperties): Try[String] = {
			if (model("success").booleanOr(true))
				Success(model("msg").getString)
			else
				Failure(new RequestFailedException(s"Server responded with: $model"))
		}
	}
}

/**
 * Attaches an SSH key to a rented instance
 * @author Mikko Hilpinen
 * @since 01.03.2026, v1.5
 */
case class AttachSshKey(instanceId: Int, sshKey: String, deprecationView: View[Boolean] = AlwaysFalse)
	extends ApiRequest[String]
{
	// ATTRIBUTES   ----------------------
	
	override val method: Method = Post
	override val path: String = s"instances/$instanceId/ssh"
	override val pathParams: Model = Model.empty
	override val body: Either[Value, Body] = Left(Model.from("ssh_key" -> sshKey))
	
	
	// IMPLEMENTED  ----------------------
	
	override def deprecated: Boolean = deprecationView.value
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[String]] =
		prepared.getOne(AttachSshKeyResponseParser)
}
