package utopia.nexus.rest

import utopia.nexus.http.ServerSettings
import utopia.nexus.result.ResultParser
import utopia.nexus.http.Request
import utopia.nexus.model.request.{RequestContext, StreamOrReader}

/**
* Contexts are used for storing and sharing data during a request handling
* @author Mikko Hilpinen
* @since 22.5.2018
**/
@deprecated("Replaced with RequestContext", "v2.0")
trait Context extends AutoCloseable with RequestContext[StreamOrReader]
{
    // ABSTRACT    ------------------------
    
    /**
     * The request in this context
     */
    override def request: Request
    
    /**
     * The settings associated with this context
     */
	def settings: ServerSettings
	
	/**
	 * The parser used for parsing result data
	 */
	def resultParser: ResultParser
}