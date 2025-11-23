package utopia.nexus.controller.servlet

import utopia.access.model.enumeration.Method
import utopia.flow.async.AsyncExtensions._
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.logging.Logger
import utopia.nexus.model.servlet.ParameterEncoding
import HttpExtensions._

import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import scala.concurrent.ExecutionContext

object LogicWrappingServlet
{
	// OTHER    -----------------------------
	
	/**
	  * @param logic Logic to wrap
	  * @return A servlet using the specified logic
	  */
	def apply(logic: ServletLogic)(implicit exc: ExecutionContext): LogicWrappingServlet =
		new _LogicWrappingServlet(logic)
	
	
	// NESTED   -----------------------------
	
	private class _LogicWrappingServlet(override val logic: ServletLogic)(implicit val exc: ExecutionContext)
		extends LogicWrappingServlet
	{
		override implicit def logger: Logger = logic.logger
		override implicit def jsonParser: JsonParser = logic.jsonParser
		override implicit def expectedParameterEncoding: ParameterEncoding = logic.expectedParameterEncoding
	}
}

/**
  * This servlet implementation wraps a logic component, handling standard conversions individually
  * @author Mikko Hilpinen
  * @since 18.8.2022, v1.2.4
  */
abstract class LogicWrappingServlet extends HttpServlet
{
	// ABSTRACT -------------------------------
	
	/**
	 * @return Logging implementation used (implicit)
	 */
	implicit def logger: Logger
	/**
	 * @return JSON parser used (implicit)
	 */
	implicit def jsonParser: JsonParser
	/**
	 * @return Implicit execution context used in asynchronous processing
	 */
	implicit def exc: ExecutionContext
	/**
	 * @return Expected parameter encoding (implicit)
	 */
	implicit def expectedParameterEncoding: ParameterEncoding
	
	/**
	  * @return Servlet logic implementation
	  */
	def logic: ServletLogic
	
	
	// COMPUTED -------------------------------
	
	// FIXME: These easily form a cyclic loop within the application, at least when combined with ApiRoot
	
	
	
	// IMPLEMENTED  ---------------------------
	
	override def service(req: HttpServletRequest, resp: HttpServletResponse) = {
		// Default implementation doesn't support PATCH, so skips some validations from the parent class, if possible
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
		// Processes the request
		val response = logic(req.toNexusRequest)
		// Writes the acquired response
		val writeCompletionFuture = response.update(res)
		
		// Case: Response-writing will be completed asynchronously => Starts async mode
		if (writeCompletionFuture.isEmpty) {
			val asyncContext = req.startAsync()
			// Completes the async mode once the writing finishes
			writeCompletionFuture.onComplete { _ => asyncContext.complete() }
		}
	}
}
