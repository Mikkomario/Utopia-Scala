package utopia.nexus.test

import utopia.access.http.Status.{NoContent, OK}
import utopia.access.http.{Method, Status}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.util.Version
import utopia.nexus.http.{Path, Response}
import utopia.nexus.rest.{LeafResource, PostContext}
import utopia.nexus.result.Result

/**
  * A restful resource which replies with request information
  * @author Mikko Hilpinen
  * @since 17.07.2024, v1.0
  */
class EchoNode(implicit apiVersion: Version) extends LeafResource[PostContext]
{
	// ATTRIBUTES   -----------------------
	
	override val name: String = "echo"
	
	
	// IMPLEMENTED  -----------------------
	
	override def allowedMethods = Method.values
	
	override def toResponse(remainingPath: Option[Path])(implicit context: PostContext): Response = {
		context.handlePossibleValuePost { body =>
			val req = context.request
			val status = body("status").int.flatMap { code => Status.values.find { _.code == code } }.getOrElse(OK)
			if (status == NoContent)
				Result.Empty
			else {
				val responseBody = Model.from(
					"apiVersion" -> apiVersion.toString,
					"method" -> req.method.toString,
					"path" -> Model.from(
						"full" -> req.path.map { _.toString },
						"remaining" -> remainingPath.map { _.toString }
					),
					"headers" -> req.headers,
					"parameters" -> req.parameters,
					"body" -> body
				)
				if (status.isFailure)
					Result.Failure(status, data = responseBody)
				else
					Result.Success(responseBody, status)
			}
		}.toResponse
	}
}
