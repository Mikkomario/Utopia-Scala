package utopia.nexus.test

import utopia.access.model.enumeration.Status.{NoContent, OK}
import utopia.access.model.enumeration.{Method, Status}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.nexus.controller.api.node.LeafNode
import utopia.nexus.model.response.RequestResult

/**
  * A restful resource which replies with request information
  * @author Mikko Hilpinen
  * @since 17.07.2024, v1.0
  */
class EchoNode extends LeafNode[NexusTestContext]
{
	// ATTRIBUTES   -----------------------
	
	override val name: String = "echo"
	
	
	// IMPLEMENTED  -----------------------
	
	override def allowedMethods = Method.values
	
	override def apply(method: Method, remainingPath: Seq[String])(implicit context: NexusTestContext): RequestResult =
		context.handlePossibleValuePost { body =>
			val req = context.request
			val status = body("status").int.flatMap { code => Status.values.find {_.code == code} }.getOrElse(OK)
			if (status == NoContent)
				RequestResult.Empty
			else {
				val responseBody = Model.from(
					"apiVersion" -> context.apiVersion.toString,
					"method" -> method.toString,
					"path" -> Model.from(
						"full" -> req.path,
						"remaining" -> remainingPath
					),
					"headers" -> req.headers,
					"parameters" -> req.parameters,
					"body" -> body
				)
				RequestResult(responseBody, status)
			}
		}
}
