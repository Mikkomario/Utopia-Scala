package utopia.access.http

import utopia.flow.util.Equatable

object Status
{
    // VALUES   ----------------------------
    
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
      * If the client has performed a conditional GET request and access is allowed, but the document
      * has not been modified, the server SHOULD respond with this status code. The 304 response
      * MUST NOT contain a message-body, and thus is always terminated by the first empty line after the
      * header fields.
      */
    case object NotModified extends Status("Not Modified", 304)
    
    /**
      * The request could not be understood by the server due to malformed syntax. The client SHOULD NOT
      * repeat the request without modifications.
      */
    case object BadRequest extends Status("Bad Request", 400)
    
    case object Unauthorized extends Status("Unauthorized", 401)
    
    /**
      * The server understood the request, but is refusing to fulfill it. Authorization will not help
      * and the request SHOULD NOT be repeated. If the request method was not HEAD and the server wishes
      * to make public why the request has not been fulfilled, it SHOULD describe the reason for the
      * refusal in the entity. If the server does not wish to make this information available to the
      * client, the status code 404 (Not Found) can be used instead.
      */
    case object Forbidden extends Status("Forbidden", 403)
    
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
    case object MethodNotAllowed extends Status("Method Not Allowed", 405)
    
    /**
      * The server encountered an unexpected condition which prevented it from fulfilling the request.
      */
    case object InternalServerError extends Status("Internal Server Error", 500)
    
    /**
      * The server does not support the functionality required to fulfill the request. This is the
      * appropriate response when the server does not recognize the request method and is not
      * capable of supporting it for any resource.
      */
    case object NotImplemented extends Status("Not Implemented", 501)
    
    /**
      * The server is currently unable to handle the request due to a temporary overloading or
      * maintenance of the server. The implication is that this is a temporary condition which will be
      * alleviated after some delay. If known, the length of the delay MAY be indicated in a
      * Retry-After header. If no Retry-After is given, the client SHOULD handle the response as it
      * would for a 500 response.
      */
    case object ServiceUnavailable extends Status("Service Unavailable", 503)
}

/**
 * Different statuses are used for signaling different response roles<br>
 * See also: https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html [referenced: 25.11.2017]
 * @author Mikko Hilpinen
 * @since 20.8.2017
 */
class Status(val name: String, val code: Int) extends Equatable
{
    // ATTRIBUTES    -----------------------
    
    /**
     * The status group this particular status belongs to
     */
    val group = StatusGroup.forCode(code)
    
    
    // IMPLEMENTED    ----------------------
    
    override def properties = Vector(code)
    
    override def toString = s"$name ($code)"
}

