package utopia.access.http

import utopia.flow.error.EnvironmentNotSetupException
import utopia.flow.operator.EqualsBy

object Status
{
    // ATTRIBUTES   -----------------------------
    
    private var _values = Vector[Status]()
    private var setupDone = false
    
    
    // COMPUTED ---------------------------------
    
    /**
      * All Values listed in this project
      * @throws EnvironmentNotSetupException If Status.setup() hasn't been called yet
      */
    @throws[EnvironmentNotSetupException]("Status.setup() must be called first")
    def values =  if (setupDone) _values else throw EnvironmentNotSetupException(
        "Status.values should only be called after Status.setup() has been called")
    
    
    // OTHER    ---------------------------------
    
    /**
      * Sets up the initial status codes. This must be called before using other methods in this object.
      */
    def setup() =
    {
        if (!setupDone)
        {
            setupDone = true
            _values = Vector(OK, Created, Accepted,
                NoContent, MovedPermanently, Found, NotModified,
                BadRequest, Unauthorized, Forbidden, NotFound, MethodNotAllowed,
                InternalServerError, NotImplemented, ServiceUnavailable)
        }
    }
    
    /**
      * Introduces a new known status
      * @param status New status to register
      * @throws EnvironmentNotSetupException If Status.setup() hasn't been called yet
      */
    @throws[EnvironmentNotSetupException]("Status.setup() must be called first")
    def introduce(status: Status) =
    {
        if (setupDone)
        {
            if (_values.forall { _.code != status.code })
                _values :+= status
        }
        else
            throw EnvironmentNotSetupException(
                "Status.introduce(Status) should only be called after Status.setup() has been called")
    }
    
    /**
      * Introduces new known statuses
      * @param statuses Statuses to register
      * @throws EnvironmentNotSetupException If Status.setup() hasn't been called yet
      */
    @throws[EnvironmentNotSetupException]("Status.setup() must be called first")
    def introduce(statuses: IterableOnce[Status]) =
    {
        if (setupDone)
            _values ++= statuses.iterator.filterNot { s => _values.exists { _.code == s.code } }
        else
            throw EnvironmentNotSetupException(
                "Status.introduce(IterableOnce[Status]) should only be called after Status.setup() has been called")
    }
    
    
    // NESTED   ---------------------------------
    
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
    case object MovedPermanently extends Status("Moved Permanently", 301)
    
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
    case object NotModified extends Status("Not Modified", 304, isTemporary = true)
    
    /**
      * The request could not be understood by the server due to malformed syntax. The client SHOULD NOT
      * repeat the request without modifications.
      */
    case object BadRequest extends Status("Bad Request", 400, doNotRepeat = true)
    
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
    case object Unauthorized extends Status("Unauthorized", 401, doNotRepeat = true)
    
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
    case object MethodNotAllowed extends Status("Method Not Allowed", 405, doNotRepeat = true)
    
    /**
      * The server encountered an unexpected condition which prevented it from fulfilling the request.
      */
    case object InternalServerError extends Status("Internal Server Error", 500)
    
    /**
      * The server does not support the functionality required to fulfill the request. This is the
      * appropriate response when the server does not recognize the request method and is not
      * capable of supporting it for any resource.
      */
    case object NotImplemented extends Status("Not Implemented", 501, doNotRepeat = true)
    
    /**
      * The server is currently unable to handle the request due to a temporary overloading or
      * maintenance of the server. The implication is that this is a temporary condition which will be
      * alleviated after some delay. If known, the length of the delay MAY be indicated in a
      * Retry-After header. If no Retry-After is given, the client SHOULD handle the response as it
      * would for a 500 response.
      */
    case object ServiceUnavailable extends Status("Service Unavailable", 503, isTemporary = true)
}

/**
 * Different statuses are used for signaling different response roles<br>
 * See also: https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html [referenced: 25.11.2017]
 * @author Mikko Hilpinen
 * @since 20.8.2017
  * @param name Status name (for human readers)
  * @param code Status code
  * @param isTemporary Whether it is known that this status may change in future requests after a delay. False if
  *                    status is permanent or it's not known whether it could be temporary (see 'doNotRepeat' for
  *                    possible details)
  * @param doNotRepeat Whether it is known that the current version of the server is very likely to never accept
  *                    the request as it is currently presented and therefore the client shouldn't try to re-send the
  *                    request. False if it is unknown whether the server status may change in time or if this current
  *                    status is only temporary (see 'isTemporary' for more details)
 */
// TODO: Use UncertainBoolean here
class Status(val name: String, val code: Int, val isTemporary: Boolean = false, val doNotRepeat: Boolean = false)
    extends EqualsBy
{
    // ATTRIBUTES    -----------------------
    
    /**
     * The status group this particular status belongs to
     */
    val group = StatusGroup.forCode(code)
    
    
    // COMPUTED -------------------------
    
    /**
      * @return Whether this status represents a success (code range 200-399)
      */
    def isSuccess = code < 400
    /**
      * @return Whether this status represents a failure (code range 400-)
      */
    def isFailure = code >= 400
    
    
    // IMPLEMENTED    ----------------------
    
    protected override def equalsProperties = Vector(code)
    
    override def toString = s"$name ($code)"
}

