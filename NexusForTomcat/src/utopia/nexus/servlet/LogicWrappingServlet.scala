package utopia.nexus.servlet

import utopia.access.model.enumeration.Status.BadRequest
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.nexus.controller.servlet.HttpExtensions._
import utopia.access.model.enumeration.Method
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.logging.Logger
import utopia.nexus.http.ServerSettings

import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import scala.concurrent.ExecutionContext

@deprecated("Replaced with a new version in controller.servlet", "v2.0")
object LogicWrappingServlet
{
	// OTHER    -----------------------------
	
	/**
	  * @param logic Logic to wrap
	  * @return A servlet using the specified logic
	  */
	def apply(logic: ServletLogic)(implicit exc: ExecutionContext, log: Logger): LogicWrappingServlet =
		new _LogicWrappingServlet(logic)
	
	
	// NESTED   -----------------------------
	
	private class _LogicWrappingServlet(override val logic: ServletLogic)
	                                   (implicit val exc: ExecutionContext, val logger: Logger)
		extends LogicWrappingServlet
}

/**
  * This servlet implementation wraps a logic component, handling standard conversions individually
  * @author Mikko Hilpinen
  * @since 18.8.2022, v1.2.4
  */
@deprecated("Replaced with a new version in controller.servlet", "v2.0")
abstract class LogicWrappingServlet extends HttpServlet
{
	// ABSTRACT -------------------------------
	
	implicit def exc: ExecutionContext
	implicit def logger: Logger
	
	/**
	  * @return Servlet logic implementation
	  */
	def logic: ServletLogic
	
	
	// IMPLEMENTED  ---------------------------
	
	override def service(req: HttpServletRequest, resp: HttpServletResponse) = {
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
