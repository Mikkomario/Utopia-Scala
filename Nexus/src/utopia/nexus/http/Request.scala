package utopia.nexus.http

import utopia.access.model.enumeration.Method
import utopia.access.model.{Cookie, Headers}
import utopia.flow.collection.immutable.Empty
import utopia.flow.generic.model.immutable.Model
import utopia.flow.time.Now
import utopia.nexus.model
import utopia.nexus.model.request.RequestBody.EmptyStreamedRequestBody
import utopia.nexus.model.request.StreamOrReader

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
  * @param bodyParts The body elements provided with this request (streamed)
  * @param rawCookies Cookies provided with this request (default = empty)
 * @param created Creation time of this request (default = Now)
 */
@deprecated("Replaced with a new version in package model.request", "v2.0")
class Request(method: Method, targetUrl: String, path: Option[Path] = None,
              parameters: Model = Model.empty, headers: Headers = Headers.empty,
              val bodyParts: Seq[StreamedBody] = Empty, rawCookies: Iterable[Cookie] = Empty,
              created: Instant = Now)
	extends model.request.Request[StreamOrReader](method, bodyParts.headOption.getOrElse(EmptyStreamedRequestBody),
		targetUrl, path match {
			case Some(path) => path.parts
			case None => Empty
		},
		parameters, headers, rawCookies, created)
{
    // OPERATORS    ----------------------------
    
    /**
     * @return This request with added parameters
     */
    override def ++(params: Model) = withAddedParameters(params)
    
    
    // OTHER METHODS    ------------------------
    
    /**
     * Creates a new request with some parameters added
     */
    override def withAddedParameters(params: Model) =
	    new Request(method, targetUrl, path, parameters ++ params, headers, bodyParts, rawCookies, created)
}