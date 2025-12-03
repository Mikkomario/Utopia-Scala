package utopia.nexus.result

import utopia.access.model.enumeration.Status.{BadRequest, OK}
import utopia.access.model.enumeration.{Status, StatusGroup}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.nexus.controller.write.JsonContentWriter.JsonEnveloper
import utopia.nexus.controller.write.JsonContentWriter.JsonEnveloper.JsonEnvelopeNames
import utopia.nexus.http.{Request, Response}

/**
* This result parser wraps the result in an envelope
* @author Mikko Hilpinen
* @since 24.5.2018
**/
@deprecated("Replaced with JsonEnveloper", "v2.0")
case class UseJsonEnvelope(getDataName: Status => String = s => if (s.group == StatusGroup.Success) "data" else "error",
                           descriptionName: String = "description", statusName: String = "status")
	extends JsonEnveloper()(JsonEnvelopeNames(value = getDataName(OK), valueOnFailure = getDataName(BadRequest),
		description = descriptionName))
	with ResultParser
{
	def apply(result: Result, request: Request) = {
	    val buffer = Vector.newBuilder[(String, Value)]
	    
	    buffer += statusName -> result.status.code
	    result.description.notEmpty.foreach { buffer += descriptionName -> _ }
	    if (!result.data.isEmpty)
	        buffer += getDataName(result.status) -> result.data
	    
	    Response.fromModel(Model(buffer.result()))
	}
}