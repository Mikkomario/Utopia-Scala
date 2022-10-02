package utopia.nexus.result

import utopia.nexus.http.Request
import utopia.access.http.Status
import utopia.flow.generic.model.immutable.Value
import utopia.nexus.http.Response

/**
* This parser outputs the data in JSON format as "raw" (http response-like) as possible
* @author Mikko Hilpinen
* @since 24.5.2018
**/
object UseRawJSON extends RawResultParser
{
    def parseDataResponse(data: Value, status: Status, request: Request) = Response.fromValue(data, status)
}