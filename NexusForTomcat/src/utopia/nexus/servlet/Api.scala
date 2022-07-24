package utopia.nexus.servlet

import utopia.access.http.{Method, Status}
import utopia.access.http.Status.BadRequest
import utopia.flow.generic.DataType
import utopia.flow.util.StringExtensions._
import utopia.nexus.http.ServerSettings
import utopia.nexus.rest.RequestHandler
import HttpExtensions._
import utopia.access.http.StatusGroup.ServerError
import utopia.flow.parse.JsonParser
import utopia.flow.util.logging.Logger

import javax.servlet.annotation.MultipartConfig
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

/**
  * A common Api implementation class
  * @author Mikko Hilpinen
  * @since 24.7.2022, v1.2.4
  * @param requestHandler A handler that deals with incoming requests
  * @param serverSettings Implicit server settings to use
  * @param jsonParser Implicit json parser to use
  * @param logger A logging implementation to handle request-related warnings
  */
@MultipartConfig(
	fileSizeThreshold   = 1048576,  // 1 MB
	maxFileSize         = 10485760, // 10 MB
	maxRequestSize      = 20971520, // 20 MB
)
class Api(requestHandler: RequestHandler[_])
         (implicit serverSettings: ServerSettings, jsonParser: JsonParser, logger: Logger)
	extends HttpServlet
{
	// ATTRIBUTES	--------------------------
	
	// Sets up common settings
	DataType.setup()
	Status.setup()
	
	
	// IMPLEMENTED METHODS    ----------------
	
	override def service(req: HttpServletRequest, resp: HttpServletResponse) =
	{
		// Default implementation doesn't support PATCH, so skips some validations from parent if possible
		if (Method.values.exists { _.name ~== req.getMethod })
			handleRequest(req, resp)
		else
			super.service(req, resp)
	}
	
	override def doGet(req: HttpServletRequest, resp: HttpServletResponse) = handleRequest(req, resp)
	override def doPost(req: HttpServletRequest, resp: HttpServletResponse) = handleRequest(req, resp)
	override def doPut(req: HttpServletRequest, resp: HttpServletResponse) = handleRequest(req, resp)
	override def doDelete(req: HttpServletRequest, resp: HttpServletResponse) = handleRequest(req, resp)
	
	
	// OTHER	------------------------------
	
	private def handleRequest(req: HttpServletRequest, res: HttpServletResponse) = {
		// Request conversion may fail
		req.toRequest match {
			case Some(request) =>
				// Generates the response
				val response = requestHandler(request)
				// Logs server-side errors
				if (response.status.group == ServerError) {
					val pathString = request.path match {
						case Some(p) => p.toString
						case None => ""
					}
					logger(s"${request.method} $pathString yielded ${response.status}")
				}
				// Returns the response
				response.update(res)
			case None =>
				logger(s"WARNING: Failed to process incoming request ${req.getMethod} ${req.getRequestURI}")
				res.setStatus(BadRequest.code)
		}
	}
}
