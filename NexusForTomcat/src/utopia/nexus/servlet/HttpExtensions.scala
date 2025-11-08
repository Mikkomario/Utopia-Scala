package utopia.nexus.servlet

import utopia.access.model.enumeration.ContentCategory._
import utopia.access.model.enumeration.Method
import utopia.access.model.{ContentType, Cookie, Headers}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.TryFuture
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.parse.AutoClose._
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.TryExtensions._
import utopia.flow.util.logging.Logger
import utopia.nexus.http.{Path, Request, ServerSettings, StreamedBody}
import utopia.nexus.model.response.Response

import java.io.{BufferedReader, InputStreamReader}
import java.net.URLDecoder
import java.nio.charset.Charset
import java.util
import javax.servlet.http.{HttpServletRequest, HttpServletResponse, Part}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

/**
 * This object contains extensions that can be used with HttpServletRequest and HttpServletResponse 
 * classes on server side.
 * @author Mikko Hilpinen
 * @since 9.9.2017
 */
object HttpExtensions
{
	implicit class TomcatResponse2(val r: Response) extends AnyVal
	{
		/**
		 * Updates an http servlet-response to match this response
		 * @param response The response to update to match this one
		 * @param exc Implicit execution context (used for asynchronous stream-closing)
		 * @param log Implicit logging implementation. Used for dealing with non-critical failures.
		 * @return A future that resolves once the response has been fully written.
		 *         May yield a failure.
		 */
		def update(response: HttpServletResponse)(implicit exc: ExecutionContext, log: Logger) =
			Try[Try[Future[Try[Unit]]]] {
				// Updates the status, headers & cookies
				response.setStatus(r.status.code)
				// TODO: See whether content type and content length must be specified separately
				r.headers.fields.foreach { case (header, value) => response.addHeader(header, value) }
				r.newCookies.foreach { cookie =>
					val javaCookie = new javax.servlet.http.Cookie(cookie.name, cookie.value.toJson)
					cookie.lifeLimitSeconds.foreach(javaCookie.setMaxAge)
					javaCookie.setSecure(cookie.isSecure)
					response.addCookie(javaCookie)
				}
				
				// Case: No body to write => Success
				if (r.body.isEmpty)
					Success(TryFuture.successCompletion)
				else {
					// Starts writing the response body
					val stream = response.getOutputStream
					Try { r.body.writeTo(stream) } match {
						// Case: Writing successfully initiated => Checks whether it has already completed
						case Success(writeCompletionFuture) =>
							writeCompletionFuture.current match {
								// Case: Writing already completed => Flushes & closes the stream
								case Some(immediateResult) =>
									val flushResult = Try { stream.flush() }
									stream.closeQuietly()
										.logWithMessage("Failure while attempting to close the response stream")
									immediateResult.orElse[Unit](flushResult).map { _ => TryFuture.successCompletion }
								
								// Case: Writing not completed yet => Closes the stream once writing finishes
								case None =>
									writeCompletionFuture.onComplete { _ =>
										Try { stream.flush() }
											.logWithMessage("Failure while flushing the response stream")
										stream.closeQuietly()
											.logWithMessage("Failure while attempting to close the response stream")
									}
									Success(writeCompletionFuture)
							}
						// Case: Failed to start writing => Closes the stream & yields a failure
						case Failure(error) =>
							stream.closeQuietly().logWithMessage("Failure while closing the response stream")
							Failure(error)
					}
				}
			}.flatten.flattenToFuture
	}
	
	implicit class ConvertibleRequest(val r: HttpServletRequest) extends AnyVal
	{
		/**
		 * Converts a httpServletRequest into a http Request
		 */
		def toRequest(implicit settings: ServerSettings, jsonParser: JsonParser) =
		{
			Option(r.getMethod).map(Method.apply).map { method =>
				val path = Option(r.getRequestURI).flatMap(Path.parse)
				
				val paramValues = r.getParameterNames.asScala.map { pName =>
					(pName, parseQueryParam(r.getParameter(pName)))
				}.flatMap { case (name, value) =>
					value.toOption.map {name -> _}
				}
				val parameters = Model(paramValues.toVector)
				
				val headers = Headers(r.getHeaderNames.asScala.map { hName => (hName, r.getHeader(hName)) }.toMap)
				
				val javaCookies = Option(r.getCookies).map {_.toVector}.getOrElse(Vector())
				val cookies = javaCookies.map { javaCookie =>
					Cookie(javaCookie.getName,
						Option(javaCookie.getValue).map {jsonParser.valueOf}.getOrElse(Value.empty),
						if (javaCookie.getMaxAge < 0) None else Some(javaCookie.getMaxAge),
						javaCookie.getSecure)
				}
				
				val body = bodyFromRequest(r, headers).filter {!_.isEmpty}
				/*
				val uploads = Try(r.getParts).toOption.map { _.asScala.flatMap {part =>
						part.getContentType.toOption.flatMap(ContentType.parse).map {
						new FileUpload(part.getName, part.getSize, _, part.getSubmittedFileName,
						part.getInputStream, part.write) }}}*/
				
				new Request(method, r.getRequestURL.toString, path, parameters, headers, body, cookies)
			}
		}
		
		private def parseQueryParam(paramValue: String)(implicit settings: ServerSettings, jsonParser: JsonParser) =
		{
			val decoded = settings.expectedParameterEncoding match {
				case Some(encoding) => Try {URLDecoder.decode(paramValue, encoding.charSet.name())}
				case None => Success(paramValue)
			}
			decoded.map(jsonParser.valueOf)
		}
		
		private def bodyFromRequest(request: HttpServletRequest, headers: Headers) =
		{
			val contentType = headers.contentType
			
			if (contentType.isEmpty)
				Vector()
			else if (contentType.get.category == MultiPart)
				Try(request.getParts).toOption.map {_.asScala.map(partToBody)}.toVector.flatten
			else
				Vector(new StreamedBody(request.getReader, contentType.get,
					Some(request.getContentLengthLong).filter {_ >= 0}, headers))
		}
		
		private def partToBody(part: Part) =
		{
			val headers = parseHeaders(part.getHeaderNames, part.getHeader)
			val contentType = Option(part.getContentType).flatMap(ContentType.parse).getOrElse(Text.plain)
			val charset = headers.charset.getOrElse(Charset.defaultCharset())
			
			new StreamedBody(new BufferedReader(new InputStreamReader(part.getInputStream, charset)),
				contentType, Some(part.getSize), headers,
				Option(part.getSubmittedFileName).getOrElse {Option(part.getName).getOrElse("")})
		}
		
		private def parseHeaders(headerNames: util.Collection[String], getValue: String => String) =
			Headers(headerNames.asScala.map(hName => (hName, getValue(hName))).toMap)
	}
}