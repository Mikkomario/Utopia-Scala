package utopia.nexus.result

import utopia.nexus.http.Request
import utopia.access.http.Status
import utopia.flow.generic.model.immutable.Value
import utopia.nexus.http.Response

import java.nio.charset.StandardCharsets

/**
* Raw result parsers try to minimise the amount of metadata in a response
* @author Mikko Hilpinen
* @since 24.5.2018
**/
trait RawResultParser extends ResultParser
{
	// ABSTRACT    ----------------------
    
    /**
     * Parses the data portion of a response
     */
    def parseDataResponse(data: Value, status: Status, request: Request): Response
    
    
    // IMPLEMENTED METHODS    ----------
    
    def apply(result: Result, request: Request) = 
    {
        val response = 
        {
            if (result.data.isEmpty)
                result.description.map(Response.plainText(_, result.status, 
                        request.headers.preferredCharset getOrElse StandardCharsets.UTF_8)) getOrElse 
                        Response.empty(result.status)
            else
                parseDataResponse(result.data, result.status, request)
        }
        response.withModifiedHeaders(_ ++ result.headers)
    }
}