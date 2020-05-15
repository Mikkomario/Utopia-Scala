package utopia.disciple.http.request

import utopia.access.http.{Headers, Method}
import utopia.access.http.Method._
import utopia.flow.datastructure.immutable.{Constant, Model, Value}
import utopia.flow.datastructure.template

import scala.collection.immutable.VectorBuilder
import scala.io.Codec

// See https://hc.apache.org/httpcomponents-client-ga/

/**
 * Requests are used for requesting certain data and for performing certain actions on server side 
 * over a http connection
 * @author Mikko Hilpinen
 * @since 26.11.2017
  * @param requestUri Targeted uri / url
  * @param method Http method used (default = GET)
  * @param params Parameters included in request (model format, default = empty)
  * @param headers Headers sent with the request (default = current date headers)
  * @param body Body included in request (default = None)
  * @param timeout Request timeout (default = no timeout specified)
  * @param parameterEncoding Encoding option used for query (uri) parameters. None if no encoding should be used (default)
  * @param supportsBodyParameters Whether parameters could be moved to request body when body is omitted (default = true).
  *                               Use false if you wish to force parameters to uri parameters)
 */
case class Request(requestUri: String, method: Method = Get, params: Model[Constant] = Model.empty,
                   headers: Headers = Headers.currentDateHeaders, body: Option[Body] = None,
                   timeout: Timeout = Timeout.empty, parameterEncoding: Option[Codec] = None,
                   supportsBodyParameters: Boolean = true)
{
    // IMPLEMENTED  --------------------
    
    override def toString =
    {
        val result = new StringBuilder
        result ++= s"$method $requestUri"
        val extra = new VectorBuilder[String]
        if (params.nonEmpty)
            extra += s"Parameters: $params"
        if (body.nonEmpty)
            extra += s"sBody: $body"
        if (headers.fields.nonEmpty)
            extra += s"Headers: $headers"
        if (timeout.nonEmpty)
            extra += s"Timeout: $timeout"
        
        val endStrings = extra.result()
        if (endStrings.nonEmpty)
            result ++= s"(${endStrings.mkString(", ")})"
        
        result.result()
    }
    
    
    // OPERATORS    --------------------
    
    /**
     * Adds a new parameter to this request
     */
    def +(parameter: Constant) = copy(params = params + parameter)
    
    /**
     * Adds a new parameter to this request
     */
    def +(parameter: (String, Value)): Request = this + Constant(parameter._1, parameter._2)
    
    /**
     * Adds multiple new parameters to this request
     */
    def ++(params: template.Model[Constant]) = copy(params = this.params ++ params)
    
    
    // OTHER METHODS    ----------------
    
    /**
     * Modifies the headers of this request
     */
    def withModifiedHeaders(mod: Headers => Headers) = copy(headers = mod(headers))
    
    /**
      * @param timeout New request timeout
      * @return A copy of this request with specified timeout
      */
    def withTimeout(timeout: Timeout) = copy(timeout = timeout)
    
    /**
      * @param f A mapping function for timeout
      * @return A copy of this request with specified timeout
      */
    def mapTimeout(f: Timeout => Timeout) = withTimeout(f(timeout))
}