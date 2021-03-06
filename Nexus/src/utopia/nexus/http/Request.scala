package utopia.nexus.http

import utopia.flow.datastructure.immutable.Model
import utopia.flow.datastructure.immutable.Constant
import utopia.flow.datastructure.immutable.Value
import utopia.access.http.Method
import utopia.access.http.Cookie
import utopia.access.http.Headers
import utopia.flow.time.Now

import java.time.Instant

/**
 * A request represents an http request made from client side to server side. A request targets 
 * a single resource with an operation and may contain parameters, body and headers
 * @author Mikko Hilpinen
 * @since 3.9.2017
  * @param method http method used in request
  * @param targetUrl Uri targeted through this request
  * @param path Path parsed from the targeted uri, if available (default = None)
  * @param parameters The parameters provided with the request (query or post)
  * @param headers Headers provided with this request (default = empty)
  * @param body The body elements provided with this request (streamed)
  * @param rawCookies Cookies provided with this request (default = empty)
 * @param created Creation time of this request (default = Now)
 */
class Request(val method: Method, val targetUrl: String, val path: Option[Path] = None,
              val parameters: Model[Constant] = Model(Vector()), val headers: Headers = Headers.empty,
              val body: Seq[StreamedBody] = Vector(), rawCookies: Iterable[Cookie] = Vector(),
              val created: Instant = Now)
{
    // ATTRIBUTES    ---------------------------
    
    /**
     * The cookies provided with the request. All keys are cookie names in lower case letters
     */
    val cookies = rawCookies.map { cookie => (cookie.name.toLowerCase(), cookie) }.toMap
    
    
    // COMPUTED --------------------------------
    
    // def jsonBody = body.headOption.map { b => Source.fromInputStream(b.) }
    
    
    // IMPLEMENTED  ----------------------------
    
    override def toString =
    {
        val sb = new StringBuilder
        sb ++= s"$method $targetUrl"
        path.foreach { p => sb ++= s" ($p)" }
        if (parameters.nonEmpty)
            sb ++= s", parameters=$parameters"
        if (headers.nonEmpty)
            sb ++= s", headers=$headers"
        if (cookies.nonEmpty)
            sb ++= s", cookies: ${ Model.fromMap(cookies.view.mapValues { _.toModel }.toMap) }"
        if (body.nonEmpty)
        {
            sb ++= " with a body"
            if (body.size > 1)
                sb ++= s" (${body.size} parts)"
        }
        
        sb.result()
    }
    
    
    // OPERATORS    ----------------------------
    
    /**
     * @return This request with added parameters
     */
    def ++(params: Model[Constant]) = withAddedParameters(params)
    
    
    // OTHER METHODS    ------------------------
    
    /**
     * @param cookieName Name of the targeted cookie
     * @return Value of that cookie. Empty value if no such cookie existed.
     */
    def cookieValue(cookieName: String) =
        cookies.get(cookieName.toLowerCase).map { _.value }.getOrElse(Value.empty)
    
    /**
     * Creates a new request with some parameters added
     */
    def withAddedParameters(params: Model[Constant]) = new Request(method, targetUrl, path, 
            parameters ++ params, headers, body, cookies.values)
}