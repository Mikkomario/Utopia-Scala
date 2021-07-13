package utopia.disciple.apache

import org.apache.http.{Consts, HttpEntity}
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.{CloseableHttpResponse, HttpDelete, HttpGet, HttpPatch, HttpPost, HttpPut}
import org.apache.http.client.utils.URIBuilder
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.message.{BasicHeader, BasicNameValuePair}
import utopia.access.http.Method._
import utopia.access.http.{Headers, Method, Status}
import utopia.disciple.http.request.TimeoutType.{ConnectionTimeout, ManagerTimeout, ReadTimeout}
import utopia.disciple.http.request.{Body, Request, Timeout}
import utopia.disciple.http.response.{ResponseParser, StreamedResponse}
import utopia.flow.datastructure.immutable.{Constant, Model, Value}
import utopia.flow.parse.{JSONReader, JsonParser}
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.AutoClose._

import java.io.OutputStream
import java.net.URLEncoder
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Codec
import scala.jdk.CollectionConverters._
import scala.language.{implicitConversions, postfixOps}
import scala.util.{Failure, Success, Try}


/**
* Gateways are used for making http requests. Each instance may have its own settings and uses its own apache http
  * connection manager & client.
* @author Mikko Hilpinen
* @since 22.2.2018
  * @param jsonParsers Json parsers to use when content encoding matches parser default.
  *                    Specify your own parser/parsers to override default functionality (JSONReader).
  *                    It is highly recommended to set this parameter to something else than the default.
  *                    A recommended option (when the server is using UTF-8 encoding) is to use JsonBunny from
  *                    the Utopia BunnyMunch module.
  * @param maxConnectionsPerRoute The maximum number of simultaneous connections to a single route (default = 2)
  * @param maxConnectionsTotal The maximum number of simultaneous connections in total (default = 10)
  * @param maximumTimeout Maximum timeouts for a single request (default = 5 minutes connection, 5 minutes read,
  *                       infinite queuing timeout).
  * @param parameterEncoding Encoding option used for query (uri) parameters.
  *                          None if no encoding should be used (default).
  * @param defaultResponseEncoding Default character encoding used when parsing response data
  *                                (used when no character encoding is specified in response headers) (default = UTF-8)
  * @param allowBodyParameters Whether parameters could be moved to request body when body is omitted (default = true).
  *                            Use false if you wish to force parameters to uri parameters.
  * @param allowJsonInUriParameters Whether uri parameters should be allowed to be converted to json values before
  *                                 applying them. False if you want the parameters to be added "as is"
  *                                 (using .toString). This mostly affects string values, whether they should be
  *                                 wrapped in quotation marks or not. (default = true = use json value format)
**/
class Gateway(jsonParsers: Vector[JsonParser] = Vector(JSONReader), maxConnectionsPerRoute: Int = 2,
              maxConnectionsTotal: Int = 10,
              maximumTimeout: Timeout = Timeout(connection = 5.minutes, read = 5.minutes),
              parameterEncoding: Option[Codec] = None,
              defaultResponseEncoding: Codec = Codec.UTF8, allowBodyParameters: Boolean = true,
              allowJsonInUriParameters: Boolean = true)
{
    // ATTRIBUTES    -------------------------
	
	/**
	  * Default character encoding used when parsing response data (used when no character encoding is specified in
	  * response headers)
	  */
	private implicit val _defaultResponseEncoding: Codec = defaultResponseEncoding
	
    private val connectionManager = new PoolingHttpClientConnectionManager()
	connectionManager.setDefaultMaxPerRoute(maxConnectionsPerRoute)
	connectionManager.setMaxTotal(maxConnectionsTotal)
	
	// TODO: Add customizable timeouts (see https://www.baeldung.com/httpclient-timeout)
    private val client = HttpClients.custom().setConnectionManager(connectionManager)
	    .setConnectionManagerShared(true).build()
    
    
    // OTHER METHODS    ----------------------
    
    // TODO: Add support for multipart body:
    // https://stackoverflow.com/questions/2304663/apache-httpclient-making-multipart-form-post
	
    /**
     * Performs a synchronous request over a HTTP connection, calling the provided function 
     * when a response is received. <b>Please note that this function blocks during the request</b>
     * @param request the request that is sent to the server
     * @param consumeResponse the function that handles the server response (or the lack of it)
	  * @tparam R Type of consume function result
	  * @return Consume function result
     */
    def makeBlockingRequest[R](request: Request)(consumeResponse: Try[StreamedResponse] => R) =
    {
        Try
        {
            // Makes the base request (uri + params + body)
            val base = makeRequestBase(request.method, request.requestUri, request.params, request.body)
            
            // Adds the headers
            request.headers.fields.foreach { case (key, value) => base.addHeader(key, value) }
            
			// Sets the timeout
			val config =
			{
				val builder = RequestConfig.custom()
				(request.timeout min maximumTimeout).thresholds.view.mapValues { _.toMillis.toInt }.foreach { case (timeoutType, millis) =>
					timeoutType match
					{
						case ConnectionTimeout => builder.setConnectTimeout(millis)
						case ReadTimeout => builder.setSocketTimeout(millis)
						case ManagerTimeout => builder.setConnectionRequestTimeout(millis)
					}
				}
				builder.build()
			}
			base.setConfig(config)
			
            // Performs the request and consumes any response
			client.execute(base).consume { response => consumeResponse(Success(wrapResponse(response))) }
        } match {
			case Success(result) => result
            case Failure(error) => consumeResponse(Failure(error))
        }
    }
    
    /**
     * Performs an asynchronous request over a http(s) connection, calling the provided function
     * when a response is received
     * @param request the request that is sent to the server
     * @param consumeResponse the function that handles the server response (or the lack of it)
	  * @tparam R Consume function result type
	  * @return Future with eventual consume function results
     */
    def makeRequest[R](request: Request)(consumeResponse: Try[StreamedResponse] => R)
					  (implicit context: ExecutionContext) = Future { makeBlockingRequest(request)(consumeResponse) }
	
	/**
	  * Performs a request and buffers / parses it to the program memory
	  * @param request the request that is sent to the server
	  * @param parser the function that parses the response stream contents
	  * @return A future that holds the request results. Contains a failure if no data was received.
	  */
	def responseFor[A](request: Request)(parser: ResponseParser[A])
					  (implicit context: ExecutionContext) =
		makeRequest(request) { result => result.map { _.buffered(parser) } }
	
	/**
	  * Performs an asynchronous request and parses the response body into a string (empty string on empty
	  * responses and read failures)
	  * @param request Request to send
	  * @param exc Implicit execution context
	  * @return Future with the buffered response
	  */
	def stringResponseFor(request: Request)(implicit exc: ExecutionContext) = responseFor(request)(ResponseParser.string)
	
	/**
	  * Performs an asynchronous request and parses response body into a value (empty value on empty responses and
	  * read failures). Supports json and xml content types. Other content types are read as raw strings.
	  * @param request Request to send
	  * @param exc Implicit execution context
	  * @return Future with the buffered response
	  */
	def valueResponseFor(request: Request)(implicit exc: ExecutionContext) = responseFor(request)(
		ResponseParser.valueWith(jsonParsers))
	
	/**
	  * Performs an asynchronous request and parses response body into a model (empty model on empty responses and
	  * read/parse failures). Supports json and xml content types.
	  * @param request Request to send
	  * @param exc Implicit execution context
	  * @return Future with the buffered response
	  */
	def modelResponseFor(request: Request)(implicit exc: ExecutionContext) = responseFor(request)(
		ResponseParser.modelWith(jsonParsers))
	
	/**
	  * Performs an asynchronous request and parses response body into a value vector (empty vector on empty responses and
	  * read failures). Supports json and xml content types. Other content types are interpreted as strings and
	  * converted to values, then wrapped in a vector.
	  * @param request Request to send
	  * @param exc Implicit execution context
	  * @return Future with the buffered response
	  */
	def valueVectorResponseFor(request: Request)(implicit exc: ExecutionContext) =
		responseFor(request)(ResponseParser.valuesWith(jsonParsers))
	
	/**
	  * Performs an asynchronous request and parses response body into a model vector (empty vector on empty responses and
	  * read/parse failures). Supports json and xml content types. Responses with only a single model have their
	  * contents wrapped in a vector.
	  * @param request Request to send
	  * @param exc Implicit execution context
	  * @return Future with the buffered response
	  */
	def modelVectorResponseFor(request: Request)(implicit exc: ExecutionContext) =
		responseFor(request)(ResponseParser.modelsWith(jsonParsers))
	
	/**
	  * Performs an asynchronous request and parses response body into an xml element (failure on empty responses and
	  * read/parse failures).
	  * @param request Request to send
	  * @param exc Implicit execution context
	  * @return Future with the buffered response
	  */
	def xmlResponseFor(request: Request)(implicit exc: ExecutionContext) =
		responseFor(request)(ResponseParser.xml)
    
    // Adds parameters and body to the request base. No headers are added at this point
	private def makeRequestBase(method: Method, baseUri: String, params: Model[Constant] = Model.empty,
	                            body: Option[HttpEntity]) =
	{
	    if (method == Get || method == Delete)
	    {
	        // Adds the parameters to uri, no body is supported
	        val uri = makeUriWithParams(baseUri, params)
	        if (method == Get) new HttpGet(uri) else new HttpDelete(uri)
	    }
	    else if (body.isEmpty && allowBodyParameters)
	    {
	        // If there is no body, adds the parameters as a body entity instead
	        val base =
			{
				if (method == Post) new HttpPost(baseUri)
				else if (method == Put) new HttpPut(baseUri)
				else new HttpPatch(baseUri)
			}
	        makeParametersEntity(params).foreach(base.setEntity)
	        base
	    }
	    else
	    {
	        // If both a body and parameters were provided, adds params to uri
	        val uri = makeUriWithParams(baseUri, params)
	        val base = if (method == Post) new HttpPost(uri) else new HttpPut(uri)
	        base.setEntity(body.get)
	        base
	    }
	}
	
	// Adds parameter values in JSON format to request uri, returns combined uri
	private def makeUriWithParams(baseUri: String, params: Model[Constant]) =
	{
	    val builder = new URIBuilder(baseUri)
		// May encode parameter values
	    params.attributes.foreach { a => builder.addParameter(a.name, paramValue(a.value)) }
	    builder.build()
	}
	
	private def paramValue(originalValue: Value) =
	{
		// Uses JSON if possible
		val valueString = if (allowJsonInUriParameters) originalValue.toJson else originalValue.getString
		parameterEncoding match
		{
			case Some(codec) => URLEncoder.encode(valueString, codec.charSet.name())
			case None => valueString
		}
	}
	
	private def makeParametersEntity(params: Model[Constant]) = 
	{
	    if (params.isEmpty)
	        None
	    else
	    {
	        val paramsList = params.attributes.map { c => new BasicNameValuePair(c.name, c.value.getString) }
	        Some(new UrlEncodedFormEntity(paramsList.asJava, Consts.UTF_8))
	    }
	}
	
	private def wrapResponse(response: CloseableHttpResponse) = 
	{
	    val status = statusForCode(response.getStatusLine.getStatusCode)
	    val headers = Headers(response.getAllHeaders.map(h => (h.getName, h.getValue)).toMap)
	    
	    new StreamedResponse(status, headers)({ Option(response.getEntity).map { _.getContent } })
	}
	
	private def statusForCode(code: Int) = Status.values.find { _.code == code }.getOrElse(new Status("Other", code))
	
	
	// IMPLICIT CASTS    ------------------------
	
	private implicit def convertOption[T](option: Option[T])
	        (implicit f: T => HttpEntity): Option[HttpEntity] = option.map(f)
	
	//noinspection JavaAccessorMethodOverriddenAsEmptyParen
	private implicit class EntityBody(val b: Body) extends HttpEntity
	{
		// Inspection is suppressed because this is an implemented function from another library
		
		//noinspection ScalaDeprecation
		override def consumeContent() =
	    {
	        b.stream.foreach { input =>
    	        val bytes = new Array[Byte](1024) //1024 bytes - Buffer size
                Iterator.continually (input.read(bytes)).takeWhile (-1 !=).foreach { _ => }
	        }
	    }
	    
        override def getContent() = b.stream.getOrElse(null)
        
        override def getContentEncoding() = b.contentEncoding.map(new BasicHeader("Content-Encoding", _)).orNull
		
		override def getContentLength() = b.contentLength.getOrElse(-1)
		
		// See https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Type
		// Content-Type: text/html; charset=UTF-8
		override def getContentType() =
		{
			// TODO: Charset should be put to content type parameters and not separately
			val charsetPart = b.charset match
			{
				case Some(charset) => s"; charset=${charset.name()}"
				case None => ""
			}
			new BasicHeader("Content-Type", s"${b.contentType}$charsetPart")
		}
		
		override def isChunked() = b.chunked
		
		override def isRepeatable() = b.repeatable
		
		override def isStreaming() = !b.repeatable
		
		override def writeTo(output: OutputStream) = b.writeTo(output).get
	}
}