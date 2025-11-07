package utopia.nexus.result

import utopia.nexus.http.Response
import utopia.nexus.http.Request

@deprecated("Replaced with ContentWriter", "v2.0")
object ResultParser
{
	// IMPLICIT ---------------------
	
	/**
	 * @param f A function that handles result-parsing
	 * @return A result parser wrapping that function
	 */
	implicit def apply(f: (Result, Request) => Response): ResultParser = new _ResultParser(f)
	
	
	// OTHER    ---------------------
	
    /**
     * Wraps a function into a result parser instance
     */
    @deprecated("Renamed to .apply(...)", "v2.0")
    def forFunction(f: (Result, Request) => Response) = apply(f)
	
	
	// NESTED   ---------------------
	
	private class _ResultParser(f: (Result, Request) => Response) extends ResultParser
	{
		override def apply(result: Result, request: Request): Response = f(result, request)
	}
}

/**
* These tools are used for parsing result data into an http response
* @author Mikko Hilpinen
* @since 24.5.2018
**/
@deprecated("Replaced with ContentWriter", "v2.0")
trait ResultParser
{
    /**
     * Parses a result into a response
     */
	def apply(result: Result, request: Request): Response
}