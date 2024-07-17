package utopia.annex.test

import utopia.access.http.Method.Post
import utopia.access.http.{Method, Status}
import utopia.access.http.Status.OK
import utopia.annex.controller.ApiClient
import utopia.annex.model.request.ApiRequest
import utopia.annex.model.response.RequestResult
import utopia.disciple.http.request.Body
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.generic.casting.ValueConversions._

import scala.concurrent.Future

/**
  * An API request for receiving an echo of this request
  * @author Mikko Hilpinen
  * @since 17.07.2024, v1.8
  */
case class GetEcho(content: Value = Value.empty, override val method: Method = Post, requestedStatus: Status = OK)
	extends ApiRequest[Model]
{
	override def path: String = "echo"
	override def body: Either[Value, Body] = Left(Model.from("status" -> requestedStatus.code, "content" -> content))
	
	override def deprecated: Boolean = false
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[Model]] = prepared.getModel
}
