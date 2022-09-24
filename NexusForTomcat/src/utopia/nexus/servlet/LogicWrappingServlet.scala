package utopia.nexus.servlet

import utopia.access.http.Method
import utopia.access.http.Status.BadRequest
import utopia.flow.operator.EqualsExtensions._
import HttpExtensions._
import utopia.flow.parse.json.JsonParser
import utopia.nexus.http.ServerSettings

import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

/**
  * This servlet implementation wraps a logic component, handling standard conversions individually
  * @author Mikko Hilpinen
  * @since 18.8.2022, v1.2.4
  */
abstract class LogicWrappingServlet extends HttpServlet
{
	// ABSTRACT -------------------------------
	
	/**
	  * @return Servlet logic implementation
	  */
	def logic: ServletLogic
	
	
	// IMPLEMENTED  ---------------------------
	
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
		implicit val jsonParser: JsonParser = logic.jsonParser
		implicit val settings: ServerSettings = logic.settings
		
		// Request conversion may fail
		req.toRequest match {
			case Some(request) =>
				// Generates the response
				val response = logic(request)
				// Returns the response
				response.update(res)
			case None =>
				res.setStatus(BadRequest.code)
				logic.processConversionFailure(req, res)
		}
	}
}
