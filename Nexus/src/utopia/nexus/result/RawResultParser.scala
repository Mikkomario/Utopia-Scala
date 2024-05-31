package utopia.nexus.result

import utopia.access.http.Status
import utopia.flow.generic.model.immutable.Value
import utopia.flow.util.NotEmpty
import utopia.nexus.http.{Request, Response}

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
    
    def apply(result: Result, request: Request) = {
        val response = {
            if (result.data.isEmpty)
                NotEmpty(result.description) match {
                    case Some(description) =>
                        Response.plainText(description, result.status,
                            request.headers.preferredCharset getOrElse StandardCharsets.UTF_8)
                    case None => Response.empty(result.status)
                }
            else
                parseDataResponse(result.data, result.status, request)
        }
        response.withModifiedHeaders(_ ++ result.headers)
    }
}