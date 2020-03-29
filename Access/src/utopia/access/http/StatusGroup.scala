package utopia.access.http

object StatusGroup
{
    // OTHER    -----------------------
    
    /**
     * Finds the best suited status group for the provided status code
     * @param statusCode The status code [100, 600[
     */
    def forCode(statusCode: Int) = 
    {
        if (statusCode < 200)
            Information
        else if (statusCode < 300)
            Success
        else if (statusCode < 400)
            Redirect
        else if (statusCode < 500)
            ClientError
        else
            ServerError
    }
    
    
    // VALUES   -----------------------
    
    /**
      * This class of status code indicates a provisional response, consisting only of the Status-Line
      * and optional headers, and is terminated by an empty line. There are no required headers for this
      * class of status code. Since HTTP/1.0 did not define any 1xx status codes, servers MUST NOT send
      * a 1xx response to an HTTP/1.0 client except under experimental conditions.
      */
    case object Information extends StatusGroup
    
    /**
      * This class of status code indicates that the client's request was successfully received,
      * understood, and accepted.
      */
    case object Success extends StatusGroup
    
    /**
      * This class of status code indicates that further action needs to be taken by the user agent in
      * order to fulfill the request. The action required MAY be carried out by the user agent without
      * interaction with the user if and only if the method used in the second request is GET or HEAD.
      * A client SHOULD detect infinite redirection loops, since such loops generate network traffic for
      * each redirection.
      */
    case object Redirect extends StatusGroup
    
    /**
      * The 4xx class of status code is intended for cases in which the client seems to have erred.
      * Except when responding to a HEAD request, the server SHOULD include an entity containing an
      * explanation of the error situation, and whether it is a temporary or permanent condition.
      * These status codes are applicable to any request method. User agents SHOULD display any
      * included entity to the user.
      */
    case object ClientError extends StatusGroup
    
    /**
      * Response status codes beginning with the digit "5" indicate cases in which the server is
      * aware that it has erred or is incapable of performing the request. Except when responding to a
      * HEAD request, the server SHOULD include an entity containing an explanation of the error
      * situation, and whether it is a temporary or permanent condition. User agents SHOULD display
      * any included entity to the user. These response codes are applicable to any request method.
      */
    case object ServerError extends StatusGroup
}

/**
 * StatusGroups describe a larger type of html statuses which includes multiple more specific 
 * statuses.
 * @author Mikko Hilpinen
 * @since 20.8.2017
 */
sealed trait StatusGroup