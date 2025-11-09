package utopia.nexus.servlet

import utopia.access.model.enumeration.ContentCategory._
import utopia.access.model.enumeration.Method
import utopia.access.model.enumeration.Method.Get
import utopia.access.model.{ContentType, Cookie, Headers}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.TryFuture
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Constant, Model, Value}
import utopia.flow.parse.AutoClose._
import utopia.flow.parse.json.JsonParser
import utopia.flow.parse.string.Regex
import utopia.flow.util.StringExtensions._
import utopia.flow.util.TryExtensions._
import utopia.flow.util.logging.Logger
import utopia.nexus
import utopia.nexus.http.{Path, ServerSettings, StreamedBody}
import utopia.nexus.model.request.{Request, StreamOrReader}
import utopia.nexus.model.response.Response
import utopia.nexus.model.servlet.ParameterEncoding

import java.io.{BufferedReader, InputStreamReader}
import java.net.URLDecoder
import java.nio.charset.{Charset, StandardCharsets}
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
	// ATTRIBUTES   ------------------------
	
	private val unwrittenHeaders = Set("content-type", "content-length")
	
	
	// EXTENSIONS   ------------------------
	
	// TODO: Make sure the output stream is always flushed
	implicit class TomcatResponse(val r: Response) extends AnyVal
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
				r.headers.contentType.foreach { cType => response.setContentType(cType.toString) }
				r.headers.contentLength.foreach(response.setContentLengthLong)
				r.headers.fields.foreach { case (header, value) =>
					if (!unwrittenHeaders.contains(header.toLowerCase))
						response.addHeader(header, value)
				}
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
		def toNexusRequest(implicit jsonParser: JsonParser, expectedParameterEncoding: ParameterEncoding, log: Logger) = {
			// Parses the method (defaulting to GET)
			val method = Option(r.getMethod) match {
				case Some(method) => Method(method)
				case None => Get
			}
			// Parses the URL and the targeted path
			val url = r.getRequestURL.toString
			val path = r.getRequestURI.splitIterator(Regex.forwardSlash).filter { _.nonEmpty }.toOptimizedSeq
			// Parses the request parameters (which may be specified as query parameters or form values)
			val params = Model.withConstants(r.getParameterNames.asScala
				.flatMap { paramName =>
					Option(r.getParameterValues(paramName)).filter { _.nonEmpty }.map { paramValues =>
						// Decodes the parameter values, if appropriate
						val decodedValuesIter = expectedParameterEncoding.charset match {
							case Some(encoding) =>
								val encodingName = encoding.name()
								paramValues.iterator.flatMap { value =>
									Try { URLDecoder.decode(value, encodingName) }
										.logWithMessage(s"Failed to parse the value of request parameter \"$paramName\"")
								}
							case None => paramValues.iterator
						}
						// Interprets the value
						val value: Value = decodedValuesIter.toOptimizedSeq.emptyOneOrMany match {
							// Case: Decoding failed => Empty
							case None => Value.empty
							// Case: Single value => Parses it as a JSON value
							case Some(Left(value)) => jsonParser.valueOf(value)
							// Case: Multiple values => Parses each as a JSON value and combines them into a Seq/Vector
							case Some(Right(values)) => values.map(jsonParser.valueOf)
						}
						Constant(paramName, value)
					}
				}
				.toOptimizedSeq)
			// Parses the headers & cookies
			val headers = Headers(r.getHeaderNames.asScala.map { header => header -> r.getHeader(header) }.toMap)
			val cookies = Option(r.getCookies) match {
				case Some(cookies) =>
					cookies.iterator
						.map { cookie =>
							val value: Value = Option(cookie.getValue) match {
								case Some(value) => jsonParser.valueOf(value)
								case None => Value.empty
							}
							Cookie(cookie.getName, value, Some(cookie.getMaxAge).filter { _ >= 0 },
								isSecure = cookie.getSecure)
						}
						.toOptimizedSeq
				case None => Empty
			}
			// Parses the request body
			val body = {
				// Case: Content-length specified as 0 => Expects there to be no body
				if (headers.contentLength.contains(0))
					StreamOrReader.empty
				// Case: There may be a body => Constructs a StreamOrReader to read the body contents, when needed
				else
					StreamOrReader(charset = headers.charset.getOrElse {
						Option(r.getCharacterEncoding)
							.flatMap { encoding => Try { Charset.forName(encoding) }.toOption }
							.getOrElse(StandardCharsets.UTF_8)
					}) { r.getInputStream } { r.getReader }
			}
			Request(method, body, url, path, params, headers, cookies)
		}
		
		/**
		 * Converts a httpServletRequest into a http Request
		 */
		@deprecated("Deprecated for removal. Please use .toNexusRequest instead", "v2.0")
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
				
				new nexus.http.Request(method, r.getRequestURL.toString, path, parameters, headers, body, cookies)
			}
		}
		@deprecated("Deprecated for removal", "v2.0")
		private def parseQueryParam(paramValue: String)(implicit settings: ServerSettings, jsonParser: JsonParser) =
		{
			val decoded = settings.expectedParameterEncoding match {
				case Some(encoding) => Try {URLDecoder.decode(paramValue, encoding.charSet.name())}
				case None => Success(paramValue)
			}
			decoded.map(jsonParser.valueOf)
		}
		@deprecated("Deprecated for removal", "v2.0")
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
		@deprecated("Deprecated for removal", "v2.0")
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