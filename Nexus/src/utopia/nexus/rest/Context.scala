package utopia.nexus.rest

import utopia.nexus.http.ServerSettings
import utopia.nexus.result.ResultParser
import utopia.nexus.http.Request

/**
* Contexts are used for storing and sharing data during a request handling
* @author Mikko Hilpinen
* @since 22.5.2018
**/
trait Context extends AutoCloseable
{
    // ABSTRACT    ------------------------
    
    /**
     * The request in this context
     */
    def request: Request
    
    /**
     * The settings associated with this context
     */
	def settings: ServerSettings
	
	/**
	 * The parser used for parsing result data
	 */
	def resultParser: ResultParser
	
	
	// IMPLEMENTED    ---------------------
	
	/**
	 * Closes / finalises the context before it is discarded
	 */
	override def close()
}