package utopia.nexus.result

import utopia.nexus.http.Response
import utopia.nexus.http.Request

object ResultParser
{
    /**
     * Wraps a function into a result parser instance
     */
    def forFunction(f: (Result, Request) => Response) = new ResultParser
    {
        def apply(result: Result, request: Request) = f(result, request)
    }
}

/**
* These tools are used for parsing result data into an http response
* @author Mikko Hilpinen
* @since 24.5.2018
**/
trait ResultParser
{
    /**
     * Parses a result into a response
     */
	def apply(result: Result, request: Request): Response
}