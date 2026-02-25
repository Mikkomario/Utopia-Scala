package utopia.echo.model.request.vastai

import utopia.access.model.enumeration.Method
import utopia.access.model.enumeration.Method.Delete
import utopia.annex.controller.ApiClient
import utopia.annex.model.request.ApiRequest
import utopia.annex.model.response.RequestResult
import utopia.disciple.model.error.RequestFailedException
import utopia.disciple.model.request.Body
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysFalse

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
 * Ends the rent by deleting the instance and all associated data. Irreversible.
 * @author Mikko Hilpinen
 * @since 25.02.2026, v1.5
 */
case class DestroyInstance(instanceId: Int, deprecationView: View[Boolean] = AlwaysFalse) extends ApiRequest[String]
{
	// ATTRIBUTES   -------------------------
	
	override val method: Method = Delete
	override val path: String = s"instances/$instanceId"
	override val pathParams: Model = Model.empty
	override val body: Either[Value, Body] = Left(Value.empty)
	
	
	// IMPLEMENTED  -------------------------
	
	override def deprecated: Boolean = deprecationView.value
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[String]] =
		prepared.parseValue { body =>
			body.model match {
				case Some(body) =>
					if (body("success").booleanOr(true))
						Success(body("msg").getString)
					else
						Failure(new RequestFailedException(
							body("msg").stringOr("Instance-deletion failed (no message included)")))
				case None => Success("")
			}
		}
}