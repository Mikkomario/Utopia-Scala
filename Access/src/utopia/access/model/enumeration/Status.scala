package utopia.access.model.enumeration

import utopia.flow.collection.immutable.Single
import utopia.flow.operator.equality.EqualsBy
import utopia.flow.util.{OpenEnumeration, OpenEnumerationValue, UncertainBoolean}

object Status extends OpenEnumeration[Status, Int]
{
	// INITIAL CODE -----------------------------
	
	introduce(OK, Created, Accepted,
		NoContent, MovedPermanently, Found, NotModified, TemporaryRedirect,
		BadRequest, Unauthorized, Forbidden, NotFound,
		MethodNotAllowed, NotAcceptable, Teapot, Locked, TooEarly, TooManyRequests, NoResponse,
		InternalServerError, NotImplemented, ServiceUnavailable)
	
	
	// OTHER    ---------------------------------
	
	/**
	 * @param code A status code
	 * @return Status matching that code
	 */
	def apply(code: Int) = values.find { _.code == code }.getOrElse { new Status("Other", code) }
	
	@deprecated("This function doesn't do anything anymore.", "v1.6")
	def setup() = ()
	
	
	// VALUES   ---------------------------------
	
	/**
	 * The request has succeeded.
	 */
	case object OK extends Status("OK", 200)
	
	/**
	 * The request has been fulfilled and resulted in a new resource being created.
	 * The newly created resource can be referenced by the URI(s) returned in the entity of the
	 * response, with the most specific URI for the resource given by a Location header field.
	 * The response SHOULD include an entity containing a list of resource characteristics and
	 * location(s) from which the user or user agent can choose the one most appropriate.
	 * The entity format is specified by the media type given in the Content-Type header field.
	 * The origin server MUST create the resource before returning the 201 status code.
	 * If the action cannot be carried out immediately, the server SHOULD respond with 202
	 * (Accepted) response instead.
	 */
	case object Created extends Status("Created", 201)
	
	/**
	 * The request has been accepted for processing, but the processing has not been completed.
	 * The request might or might not eventually be acted upon, as it might be disallowed when
	 * processing actually takes place. There is no facility for re-sending a status code from an
	 * asynchronous operation such as this.<p>
	 *
	 * The 202 response is intentionally non-committal. Its purpose is to allow a server to accept a
	 * request for some other process (perhaps a batch-oriented process that is only run once per day)
	 * without requiring that the user agent's connection to the server persist until the process is
	 * completed. The entity returned with this response SHOULD include an indication of the request's
	 * current status and either a pointer to a status monitor or some estimate of when the user can
	 * expect the request to be fulfilled.
	 */
	case object Accepted extends Status("Accepted", 202)
	
	/**
	 * The server has fulfilled the request but does not need to return an entity-body, and might
	 * want to return updated metainformation. The response MAY include new or updated metainformation
	 * in the form of entity-headers, which if present SHOULD be associated with the requested variant.<p>
	 *
	 * If the client is a user agent, it SHOULD NOT change its document view from that which caused the
	 * request to be sent. This response is primarily intended to allow input for actions to take place
	 * without causing a change to the user agent's active document view, although any new or updated
	 * metainformation SHOULD be applied to the document currently in the user agent's active view.<p>
	 *
	 * The 204 response MUST NOT include a message-body, and thus is always terminated by the first
	 * empty line after the header fields.
	 */
	case object NoContent extends Status("No Content", 204)
	
	/**
	 * The requested resource has been assigned a new permanent URI and any future references to
	 * this resource SHOULD use one of the returned URIs.
	 * Clients with link editing capabilities ought to automatically re-link references to the Request-URI
	 * to one or more of the new references returned by the server, where possible.
	 * This response is cacheable unless indicated otherwise.
	 *
	 * The new permanent URI SHOULD be given by the Location field in the response.
	 * Unless the request method was HEAD, the entity of the response SHOULD contain a short hypertext
	 * note with a hyperlink to the new URI(s).
	 *
	 * If the 301 status code is received in response to a request other than GET or HEAD,
	 * the user agent MUST NOT automatically redirect the request unless it can be confirmed by the user,
	 * since this might change the conditions under which the request was issued.
	 */
	case object MovedPermanently extends Status("Moved Permanently", 301,
		isTemporary = false, doNotRepeat = true)
	
	/**
	 * The requested resource resides temporarily under a different URI.
	 * Since the redirection might be altered on occasion, the client SHOULD continue to use the Request-URI
	 * for future requests. This response is only cacheable if indicated by a Cache-Control or
	 * Expires header field.
	 *
	 * The temporary URI SHOULD be given by the Location field in the response.
	 * Unless the request method was HEAD, the entity of the response SHOULD contain a short hypertext note
	 * with a hyperlink to the new URI(s).
	 *
	 * If the 302 status code is received in response to a request other than GET or HEAD,
	 * the user agent MUST NOT automatically redirect the request unless it can be confirmed by the user,
	 * since this might change the conditions under which the request was issued.
	 */
	case object Found extends Status("Found", 302, isTemporary = true)
	
	/**
	 * If the client has performed a conditional GET request and access is allowed, but the document
	 * has not been modified, the server SHOULD respond with this status code. The 304 response
	 * MUST NOT contain a message-body, and thus is always terminated by the first empty line after the
	 * header fields.
	 */
	case object NotModified extends Status("Not Modified", 304, isTemporary = true, doNotRepeat = false)
	
	/**
	 * The HTTP 307 Temporary Redirect redirection response status code indicates that the resource requested has
	 * been temporarily moved to the URL in the Location header.
	 *
	 * A browser receiving this status will automatically request the resource at the URL in the Location header,
	 * redirecting the user to the new page. Search engines receiving this response will not attribute links to the
	 * original URL to the new resource, meaning no SEO value is transferred to the new URL.
	 *
	 * The method and the body of the original request are reused to perform the redirected request.
	 * In the cases where you want the request method to be changed to GET, use 303 See Other instead.
	 * This is useful when you want to give an answer to a successful PUT request that is not the uploaded resource,
	 * but a status monitor or confirmation message like "You have successfully uploaded XYZ".
	 *
	 * The difference between 307 and 302 is that 307 guarantees that the client will not change the request method
	 * and body when the redirected request is made.
	 * With 302, older clients incorrectly changed the method to GET. 307 and 302 responses are identical when the
	 * request method is GET.
	 */
	case object TemporaryRedirect extends Status("Temporary Redirect", 307, isTemporary = true, doNotRepeat = false)
	
	/**
	 * The request could not be understood by the server due to malformed syntax. The client SHOULD NOT
	 * repeat the request without modifications.
	 */
	case object BadRequest extends Status("Bad Request", 400, isTemporary = false, doNotRepeat = true)
	
	/**
	 * The request requires user authentication. The response MUST include a WWW-Authenticate header field
	 * (section 14.47) containing a challenge applicable to the requested resource. The client MAY repeat the
	 * request with a suitable Authorization header field (section 14.8). If the request already included
	 * Authorization credentials, then the 401 response indicates that authorization has been refused for
	 * those credentials. If the 401 response contains the same challenge as the prior response, and the user
	 * agent has already attempted authentication at least once, then the user SHOULD be presented the entity that
	 * was given in the response, since that entity might include relevant diagnostic information.
	 * HTTP access authentication is explained in "HTTP Authentication: Basic and Digest Access Authentication"
	 */
	case object Unauthorized extends Status("Unauthorized", 401, isTemporary = false, doNotRepeat = true)
	
	/**
	 * The server understood the request, but is refusing to fulfill it. Authorization will not help
	 * and the request SHOULD NOT be repeated. If the request method was not HEAD and the server wishes
	 * to make public why the request has not been fulfilled, it SHOULD describe the reason for the
	 * refusal in the entity. If the server does not wish to make this information available to the
	 * client, the status code 404 (Not Found) can be used instead.
	 */
	case object Forbidden extends Status("Forbidden", 403, doNotRepeat = true)
	
	/**
	 * The server has not found anything matching the Request-URI. No indication is given of whether
	 * the condition is temporary or permanent. The 410 (Gone) status code SHOULD be used if the server
	 * knows, through some internally configurable mechanism, that an old resource is permanently
	 * unavailable and has no forwarding address. This status code is commonly used when the server
	 * does not wish to reveal exactly why the request has been refused, or when no other response is
	 * applicable.
	 */
	case object NotFound extends Status("Not Found", 404)
	
	/**
	 * The method specified in the Request-Line is not allowed for the resource identified by the
	 * Request-URI. The response MUST include an Allow header containing a list of valid methods for
	 * the requested resource.
	 */
	case object MethodNotAllowed extends Status("Method Not Allowed", 405, isTemporary = false, doNotRepeat = true)
	
	/**
	 * The HTTP 406 Not Acceptable client error response status code indicates that the server could not produce a
	 * response matching the list of acceptable values defined in the request's proactive content negotiation headers
	 * and that the server was unwilling to supply a default representation.
	 *
	 * Proactive content negotiation headers include:
	 *      - Accept
	 *      - Accept-Encoding
	 *      - Accept-Language
	 *
	 * A server may return responses that differ from the request's accept headers.
	 * In such cases, a 200 response with a default resource that doesn't match the client's list of acceptable
	 * content negotiation values may be preferable to sending a 406 response.
	 *
	 * If a server returns a 406, the body of the message should contain the list of available representations
	 * for the resource, allowing the user to choose, although no standard way for this is defined.
	 */
	case object NotAcceptable extends Status("Not Acceptable", 406, isTemporary = false, doNotRepeat = true)
	
	/**
	 * The HTTP 418 I'm a teapot status response code indicates that the server refuses to brew coffee
	 * because it is, permanently, a teapot.
	 * A combined coffee/tea pot that is temporarily out of coffee should instead return 503.
	 * This error is a reference to Hyper Text Coffee Pot Control Protocol defined in April Fools'
	 * jokes in 1998 and 2014.
	 *
	 * While originally defined in RFC 2324 as an April Fools' joke,
	 * this status code was formally reserved in RFC 9110 due to its wide deployment as a joke,
	 * so it cannot be assigned any non-joke semantics for the foreseeable future.
	 *
	 * Some websites use this response for requests they do not wish to handle, such as automated queries.
	 */
	case object Teapot extends Status("I'm a Teapot", 418, isTemporary = false, doNotRepeat = true)
	/**
	 * The HTTP 423 Locked client error response status code indicates that a resource is locked,
	 * meaning it can't be accessed.
	 */
	case object Locked extends Status("Locked", 423, isTemporary = false, doNotRepeat = true)
	/**
	 * The HTTP 425 Too Early client error response status code indicates that the server was unwilling to risk
	 * processing a request that might be replayed to avoid potential replay attacks.
	 *
	 * If a client has interacted with a server recently, early data (also known as zero round-trip time (0-RTT) data)
	 * allows the client to send data to a server in the first round trip of a connection,
	 * without waiting for the TLS handshake to complete. A client that sends a request in early data does not
	 * need to include the Early-Data header.
	 *
	 * See https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Early-Data for more information.
	 */
	case object TooEarly extends Status("Too Early", 425)
	/**
	 * The HTTP 429 Too Many Requests client error response status code indicates the client
	 * has sent too many requests in a given amount of time.
	 * This mechanism of asking the client to slow down the rate of requests is commonly called "rate limiting".
	 *
	 * A Retry-After header may be included to this response to indicate how long a client should wait
	 * before making the request again.
	 *
	 * Implementations of rate limiting vary; restrictions may be server-wide or per resource. Typically,
	 * rate-limiting restrictions are based on a client's IP but can be specific to users or authorized
	 * applications if requests are authenticated or contain a cookie.
	 */
	case object TooManyRequests extends Status("Too Many Requests", 429, isTemporary = true, doNotRepeat = false)
	/**
	 * HTTP response status code 444 No Response is an unofficial client error specific to nginx.
	 * The server closes the HTTP Connection without sending any data back to the client,
	 * including this HTTP status code itself.
	 */
	case object NoResponse extends Status("No Response", 444, isTemporary = false, doNotRepeat = true)
	
	/**
	 * The server encountered an unexpected condition which prevented it from fulfilling the request.
	 */
	case object InternalServerError extends Status("Internal Server Error", 500)
	
	/**
	 * The server does not support the functionality required to fulfill the request. This is the
	 * appropriate response when the server does not recognize the request method and is not
	 * capable of supporting it for any resource.
	 */
	case object NotImplemented extends Status("Not Implemented", 501, isTemporary = false, doNotRepeat = true)
	
	/**
	 * The server is currently unable to handle the request due to a temporary overloading or
	 * maintenance of the server. The implication is that this is a temporary condition which will be
	 * alleviated after some delay. If known, the length of the delay MAY be indicated in a
	 * Retry-After header. If no Retry-After is given, the client SHOULD handle the response as it
	 * would for a 500 response.
	 */
	case object ServiceUnavailable extends Status("Service Unavailable", 503,
		isTemporary = true, doNotRepeat = false)
}

/**
 * Different statuses are used for signaling different response roles.
 * @author Mikko Hilpinen
 * @since 20.8.2017
 * @param name Status name (for human readers)
 * @param code Status code
 * @param isTemporary Whether this status may change in future requests after some delay.
 *                    False (certain) if status is known to be permanent.
 * @param doNotRepeat Whether the current version of the server is very likely to never accept
 *                    the request as it is currently presented and therefore the client shouldn't try to re-send the
 *                    request.
 *                    UncertainBoolean if it is unknown whether the server status may change in time or if this current
 *                    status is only temporary
 * @see [[https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html]] [referenced: 25.11.2017]
 */
class Status(val name: String, val code: Int, val isTemporary: UncertainBoolean = UncertainBoolean,
             val doNotRepeat: UncertainBoolean = UncertainBoolean)
	extends EqualsBy with OpenEnumerationValue[Int]
{
	// ATTRIBUTES    -----------------------
	
	/**
	 * The status group this particular status belongs to
	 */
	lazy val group = StatusGroup.forCode(code)
	
	
	// COMPUTED -------------------------
	
	/**
	 * @return Whether this status represents a success (code range 200-399)
	 */
	def isSuccess = code < 400
	/**
	 * @return Whether this status represents a failure (code range 400+)
	 */
	def isFailure = code >= 400
	
	
	// IMPLEMENTED    ----------------------
	
	override def identifier: Int = code
	
	protected override def equalsProperties = Single(code)
	
	override def toString = s"$name ($code)"
}

