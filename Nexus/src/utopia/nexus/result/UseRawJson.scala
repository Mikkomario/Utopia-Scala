package utopia.nexus.result

import utopia.access.model.enumeration.Status
import utopia.flow.generic.model.immutable.Value
import utopia.nexus.controller.write.JsonContentWriter.PlainJsonContentWriter
import utopia.nexus.http.{Request, Response}

/**
* This parser outputs the data in JSON format as "raw" (http response-like) as possible
* @author Mikko Hilpinen
* @since 24.5.2018
**/
@deprecated("Replaced with PlainJsonContentWriter", "v2.0")
object UseRawJson extends PlainJsonContentWriter("", writeDescriptionAsPlainText = true) with RawResultParser
{
    def parseDataResponse(data: Value, status: Status, request: Request) = Response.fromValue(data, status)
}