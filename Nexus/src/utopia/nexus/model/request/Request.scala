package utopia.nexus.model.request

import utopia.access.model.enumeration.Method
import utopia.access.model.{Cookie, Headered, Headers}
import utopia.flow.collection.immutable.Empty
import utopia.flow.generic.model.immutable.{Constant, Model}
import utopia.flow.time.Now
import utopia.flow.util.Mutate
import utopia.flow.util.StringExtensions._
import utopia.flow.view.immutable.View

import java.time.Instant
import scala.util.Try

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
case class Request[+A](method: Method, body: RequestBody[A], url: String, path: Seq[String] = Empty,
                       parameters: Model = Model.empty, headers: Headers = Headers.empty,
                       cookies: Iterable[Cookie] = Empty, created: Instant = Now)
	extends Headered[Request[A]] with View[A]
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
	
	override def value: A = body.value
	
    override def toString =
	    s"$method $url${
		    parameters.propertiesIterator.map { p => s"${ p.name }:${ p.value }" }.mkString("&").prependIfNotEmpty("?") }"
	
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
	def withBody[B](body: RequestBody[B]) = copy(body = body)
	/**
	 * @param f A mapping function applied to this request's body
	 * @tparam B Type of the mapped body value
	 * @return Copy of this request with a mapped body
	 */
	def mapBody[B](f: RequestBody[A] => RequestBody[B]) = withBody(f(body))
	/**
	 * @param f A mapping function applied to this request's body. May yield a failure.
	 * @tparam B Type of the mapped body value, if successful
	 * @return Copy of this request with a mapped body. Failure if 'f' yielded a failure.
	 */
	def tryMapBody[B](f: RequestBody[A] => Try[RequestBody[B]]) = f(body).map(withBody)
	
	/**
	 * @param f A mapping function applied to this request's body value
	 * @tparam B Type of the mapped body value
	 * @return A copy of this request with the specified body value
	 */
	def map[B](f: A => B) = mapBody { _.map(f) }
	/**
	 * @param f A mapping function applied to this request's body value. May yield a failure.
	 * @tparam B Type of the mapped body value
	 * @return A copy of this request with the specified body value. Failure if 'f' yielded a failure.
	 */
	def tryMap[B](f: A => Try[B]) = tryMapBody { _.tryMap(f) }
	
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
	 * @param cookieName Name of the targeted cookie
	 * @return Value of that cookie. Empty value if no such cookie existed.
	 */
	@deprecated("Please use cookieModel instead", "v2.0")
	def cookieValue(cookieName: String) = cookieModel(cookieName)
}