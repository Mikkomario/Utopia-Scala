package utopia.nexus.servlet

import utopia.access.http.Status
import utopia.access.http.StatusGroup.ServerError
import utopia.flow.collection.immutable.Empty
import utopia.flow.parse.json.JsonParser
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.Logger
import utopia.nexus.http.{Request, ServerSettings}
import utopia.nexus.interceptor.{RequestInterceptor, ResponseInterceptor}
import utopia.nexus.rest.RequestHandler

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

/**
  * A logic component for an API, based around RequestHandler
  * @author Mikko Hilpinen
  * @since 18.8.2022, v1.2.4
  * @param requestHandler A request handler that will receive and process the requests
  * @param interceptors Interceptors for possibly modifying the incoming requests before they're
  *                     passed to the request handler (default = empty)
  * @param postProcessors Interceptors for possibly modifying generated responses before they are sent
  *                       to the client (default = empty)
  * @param jsonParser Implicit json parser implementation
  * @param settings Implicit server settings
  * @param logger Implicit logging implementation
  */
class ApiLogic(requestHandler: RequestHandler[_], interceptors: Seq[RequestInterceptor] = Empty,
               postProcessors: Seq[ResponseInterceptor] = Empty)
              (implicit override val jsonParser: JsonParser, override val settings: ServerSettings, logger: Logger)
	extends ServletLogic
{
	// INITIAL CODE ------------------------
	
	Status.setup()
	
	
	// IMPLEMENTED  ------------------------
	
	override def apply(request: Request) = {
		val requestTime = Now.toInstant
		// Intercepts the request, generates the response
		val response = requestHandler(interceptors.foldLeft(request) { (req, i) => i.intercept(req) })
		// Logs server-side errors
		if (response.status.group == ServerError) {
			val pathString = request.path match {
				case Some(p) => p.toString
				case None => ""
			}
			logger(s"${request.method} $pathString yielded ${response.status}")
		}
		// Adds a date-header, if not present. Also post-processes the response
		val dateModifiedResponse = response.mapHeaders { h => h.withDate(h.date match {
			case Some(date) => requestTime min date
			case None => requestTime
		}) }
		postProcessors.foldLeft(dateModifiedResponse) { (res, p) => p.intercept(res, request) }
	}
	
	override def processConversionFailure(request: HttpServletRequest, response: HttpServletResponse) =
		logger(s"WARNING: Failed to process incoming request ${request.getMethod} ${request.getRequestURI}")
	
	
	// OTHER    --------------------------
	
	/**
	  * @param interceptor A new request interceptor
	  * @return A new api logic incorporating that interceptor
	  */
	def withInterceptor(interceptor: RequestInterceptor) =
		new ApiLogic(requestHandler, interceptors :+ interceptor, postProcessors)
	/**
	  * @param processor A new post-processor
	  * @return A new api logic incorporating that post-processor
	  */
	def withPostProcessor(processor: ResponseInterceptor) =
		new ApiLogic(requestHandler, interceptors, postProcessors :+ processor)
}
