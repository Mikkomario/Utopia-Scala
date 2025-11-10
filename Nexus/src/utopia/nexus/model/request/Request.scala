package utopia.nexus.model.request

import utopia.access.model.enumeration.ContentCategory.Text
import utopia.access.model.enumeration.Method
import utopia.access.model.{Cookie, Headered, Headers}
import utopia.flow.collection.immutable.Empty
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Constant, Model, Value}
import utopia.flow.operator.MaybeEmpty
import utopia.flow.parse.json.JsonParser
import utopia.flow.time.Now
import utopia.flow.util.Mutate
import utopia.flow.util.StringExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.View

import java.time.Instant
import scala.util.{Failure, Success, Try}

object Request
{
	// TYPES    -----------------------
	
	/**
	 * A [[Request]] that still contains a streamed body (if applicable)
	 */
	type StreamedRequest = Request[StreamOrReader]
}

/**
 * Represents an http request made from a client side to a server.
 * Targets a single resource with some operation, and may contain parameters a possible body, and headers
 * @tparam A Type of this request's body contents
 * @author Mikko Hilpinen
 * @since 3.9.2017
  * @param method http method used in request
  * @param body The body of this request.
 * @param url (absolute) URL targeted by this request
  * @param path Path to the targeted resource. Default = empty.
  * @param parameters Query (or post) parameters specified in this request. Default = empty.
  * @param headers Headers provided with this request (default = empty)
  * @param cookies Cookies provided with this request (default = empty)
 * @param created Creation time of this request (default = Now)
 */
case class Request[+A](method: Method, body: A, url: String, path: Seq[String] = Empty,
                       parameters: Model = Model.empty, headers: Headers = Headers.empty,
                       cookies: Iterable[Cookie] = Empty, created: Instant = Now)
	extends Headered[Request[A]] with View[A] with MaybeEmpty[Request[A]]
{
    // ATTRIBUTES    ---------------------------
	
	/**
	 * A model based on the specified cookies
	 */
	lazy val cookieModel = Model.withConstants(cookies.map { c => Constant(c.name, c.value) })
    /**
     * The cookies provided with the request. All keys are cookie names in lower case letters
     */
    lazy val cookieMap = cookies.iterator.map { cookie => (cookie.name.toLowerCase(), cookie) }.toMap
    
    
    // COMPUTED --------------------------------
    
    /**
      * @return A string representation of this request's path
      */
    def pathString = path.mkString("/")
	
	@deprecated("Renamed to url", "v2.0")
	def targetUrl = url
    
    
    // IMPLEMENTED  ----------------------------
	
	override def self: Request[A] = this
	
	override def value: A = body
	override def isEmpty: Boolean = headers.contentLength.contains(0)
	
	override def toString =
	    s"$method $url${
		    parameters.propertiesIterator.map { p =>
			   val valueStr = p.value.notEmpty match {
				   case Some(value) => s": $value"
				   case None => ""
			   }
			    s"${ p.name }$valueStr"
		    }.mkString("&").prependIfNotEmpty("?") }"
	
	override def withHeaders(headers: Headers, overwrite: Boolean): Request[A] =
		copy(headers = if (overwrite) headers else this.headers ++ headers)
	
	override def mapValue[B](f: A => B) = map(f)
	
	
	// OPERATORS    ----------------------------
    
    /**
     * @return This request with added parameters
     */
    def ++(params: Model) = withParameters(params, append = true)
    
    
    // OTHER METHODS    ------------------------
    
	def withMethod(method: Method) = copy(method = method)
	def mapMethod(f: Mutate[Method]) = withMethod(f(method))
	
	/**
	 * @param body A new body to assign to this request
	 * @tparam B The type of the body's value
	 * @return A copy of this request with the specified body
	 */
	def withBody[B](body: B) = copy(body = body)
	/**
	 * Alias for [[withBody]]
	 */
	def withValue[B](value: B) = withBody(value)
	/**
	 * @param f A mapping function applied to this request's body
	 * @tparam B Type of the mapped body value
	 * @return Copy of this request with a mapped body
	 */
	def mapBody[B](f: A => B) = withBody(f(body))
	/**
	 * @param f A mapping function applied to this request's body. May yield a failure.
	 * @tparam B Type of the mapped body value, if successful
	 * @return Copy of this request with a mapped body. Failure if 'f' yielded a failure.
	 */
	def tryMapBody[B](f: A => Try[B]) = f(body).map(withBody)
	
	/**
	 * Alias for [[mapBody]]
	 * @param f A mapping function applied to this request's body
	 * @tparam B Type of the mapped body
	 * @return A copy of this request with the specified body
	 */
	def map[B](f: A => B) = mapBody(f)
	/**
	 * Alias for [[tryMapBody]]
	 * @param f A mapping function applied to this request's body. May yield a failure.
	 * @tparam B Type of the mapped body
	 * @return A copy of this request with the specified body. Failure if 'f' yielded a failure.
	 */
	def tryMap[B](f: A => Try[B]) = tryMapBody(f)
	
	def withUrl(url: String) = copy(url = url)
	def mapUrl(f: Mutate[String]) = withUrl(f(url))
	
	def withPath(path: Seq[String]) = copy(path = path)
	def mapPath(f: Mutate[Seq[String]]) = withPath(f(path))
	
	/**
	 * @param params Parameters to assign to this request
	 * @param append Whether to append these parameters to the existing parameters
	 *               (default = false = overwrites existing parameters)
	 * @return A copy of this request with the specified parameters
	 */
	def withParameters(params: Model, append: Boolean = false) =
		if (append) mapParameters { _ ++ params } else copy(parameters = params)
	def mapParameters(f: Mutate[Model]) = copy(parameters = f(parameters))
    def withAddedParameters(params: Model) = withParameters(params, append = true)
	
	/**
	 * Buffers this request's body into a String
	 * @param log Implicit logging implementation used for recording failures during stream-closing
	 * @return This request with its body buffered into a string. Failure if buffering failed.
	 */
	def bufferText(implicit log: Logger, ev: A <:< StreamOrReader) =
		if (isEmpty) Success(withValue("")) else tryMap { _.bufferToString }
	/**
	 * Buffers this request's body into an XML element
	 * @param log Implicit logging implementation used for recording failures during stream-closing
	 * @return This request with its body buffered into XML. Failure if buffering failed.
	 */
	def bufferXml(implicit log: Logger, ev: A <:< StreamOrReader) = tryMap { _.bufferToXml }
	/**
	 * Buffers this requests body into a [[Value]].
	 *
	 * This operation is supported for the following content types:
	 *      - `*`/json => Contents will be parsed as JSON
	 *      - `*`/xml => Contents will be parsed into an [[utopia.flow.parse.xml.XmlElement]],
	 *                   and then converted into a [[Model]], then into a Value.
	 *      - text/`*` => Contents will be parsed into a String and wrapped into a Value
	 *      - Unspecified => Assumes that the contents are JSON
	 *
	 * @param jsonParser JSON parser used for interpreting JSON content, if applicable
	 * @param log Implicit logging implementation used for recording failures during stream-closing
	 * @return This request its body buffered into a Value.
	 *         Failure if buffering failed, or if the content type was not supported.
	 */
	def buffer(implicit jsonParser: JsonParser, log: Logger, ev: A <:< StreamOrReader) =
		bufferedValue.map(withBody)
	/**
	 * Buffers this request's body into a [[Value]], assuming that the body, if present, is written in JSON
	 * @param jsonParser JSON parser to use
	 * @param log Implicit logging implementation used for recording failures during stream-closing
	 * @return This request with its body buffered into a Value. Failure if buffering failed.
	 * @see [[buffer]]
	 */
	def bufferJson(implicit jsonParser: JsonParser, log: Logger, ev: A <:< StreamOrReader) =
		bufferedJsonValue.map(withBody)
	
	/**
	 * Buffers this requests body into a [[Value]].
	 *
	 * This operation is supported for the following content types:
	 *      - `*`/json => Contents will be parsed as JSON
	 *      - `*`/xml => Contents will be parsed into an [[utopia.flow.parse.xml.XmlElement]],
	 *                   and then converted into a [[Model]], then into a Value.
	 *      - text/`*` => Contents will be parsed into a String and wrapped into a Value
	 *      - Unspecified => Assumes that the contents are JSON
	 *
	 * @param jsonParser JSON parser used for interpreting JSON content, if applicable
	 * @param log Implicit logging implementation used for recording failures during stream-closing
	 * @return This request's body buffered into a Value.
	 *         Failure if buffering failed, or if the content type was not supported.
	 */
	def bufferedValue(implicit jsonParser: JsonParser, log: Logger, ev: A <:< StreamOrReader) = {
		if (isEmpty || value.isEmpty.isCertainlyTrue)
			Success(Value.empty)
		else
			headers.contentType match {
				case Some(contentType) =>
					contentType.subType.toLowerCase match {
						case "json" => body.bufferAsJson
						case "xml" => body.bufferToXml.map { _.toSimpleModel.toValue }
						case _ =>
							if (contentType.category == Text)
								body.bufferToString.map { s => s: Value }
							else
								Failure(new UnsupportedOperationException(s"Can't buffer $contentType into a Value"))
					}
				case None =>  body.bufferAsJson
			}
	}
	/**
	 * Buffers this request's body into a [[Value]], assuming that the body, if present, is written in JSON
	 * @param jsonParser JSON parser to use
	 * @param log Implicit logging implementation used for recording failures during stream-closing
	 * @return This request's body buffered into a Value. Failure if buffering failed.
	 * @see [[bufferedValue]]
	 */
	def bufferedJsonValue(implicit jsonParser: JsonParser, log: Logger, ev: A <:< StreamOrReader) =
		if (isEmpty || value.isEmpty.isCertainlyTrue) Success(Value.empty) else body.bufferAsJson
	
	/**
	 * @param cookieName Name of the targeted cookie
	 * @return Value of that cookie. Empty value if no such cookie existed.
	 */
	@deprecated("Please use cookieModel instead", "v2.0")
	def cookieValue(cookieName: String) = cookieModel(cookieName)
}