package utopia.nexus.servlet

import utopia.flow.parse.JsonParser
import utopia.nexus.http.{Request, Response, ServerSettings}

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

/**
  * Common trait for logical servlet implementations
  * @author Mikko Hilpinen
  * @since 18.8.2022, v1.2.4
  */
trait ServletLogic
{
	/**
	  * @return Json parser used (implicit)
	  */
	implicit def jsonParser: JsonParser
	/**
	  * @return Server settings used (implicit)
	  */
	implicit def settings: ServerSettings
	
	/**
	  * Receives a request and produces a response
	  * @param request Request to receive
	  * @return A response to return back to the client
	  */
	def apply(request: Request): Response
	
	/**
	  * This method is called when a request can't be converted into a Nexus request for some reason
	  * @param request A request that couldn't be converted
	  * @param response A response that may be updated to answer this
	  */
	def processConversionFailure(request: HttpServletRequest, response: HttpServletResponse): Unit
}
