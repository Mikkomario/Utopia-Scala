package utopia.disciple.apache

import org.apache.hc.client5.http.classic.methods.{HttpDelete, HttpGet, HttpPatch, HttpPost, HttpPut}
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity
import org.apache.hc.client5.http.impl.classic.{CloseableHttpResponse, HttpClientBuilder, HttpClients}
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager
import org.apache.hc.client5.http.socket.{ConnectionSocketFactory, PlainConnectionSocketFactory}
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory
import org.apache.hc.core5.http.config.RegistryBuilder
import org.apache.hc.core5.http.message.BasicNameValuePair
import org.apache.hc.core5.http.{Header, HttpEntity}
import org.apache.hc.core5.net.URIBuilder
import org.apache.hc.core5.ssl.SSLContexts
import utopia.access.http.Method._
import utopia.access.http.{Headers, Method, Status}
import utopia.disciple.controller.{RequestInterceptor, ResponseInterceptor}
import utopia.disciple.http.request.TimeoutType.{ConnectionTimeout, ManagerTimeout, ReadTimeout}
import utopia.disciple.http.request.{Body, Request, Timeout}
import utopia.disciple.http.response.{ResponseParser, StreamedResponse}
import utopia.flow.collection.immutable.{Empty, Single}
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.operator.Identity
import utopia.flow.parse.AutoClose._
import utopia.flow.parse.json.{JsonParser, JsonReader}
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.Logger

import java.io.OutputStream
import java.net.{URI, URLEncoder}
import java.nio.charset.StandardCharsets
import java.util
import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Codec
import scala.jdk.CollectionConverters._
import scala.language.{implicitConversions, postfixOps}
import scala.util.{Failure, Success, Try}

object Gateway
{
	/**
	  * Creates a new gateway instance
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
	  * @param requestInterceptors  Interceptors that access and potentially modify all outgoing requests (default = empty)
	  * @param responseInterceptors Interceptors that access and potentially modify all incoming responses (default = empty)
	  * @param allowBodyParameters Whether parameters could be moved to request body when body is omitted (default = true).
	  *                            Use false if you wish to force parameters to uri parameters.
	  * @param allowJsonInUriParameters Whether uri parameters should be allowed to be converted to json values before
	  *                                 applying them. False if you want the parameters to be added "as is"
	  *                                 (using .toString). This mostly affects string values, whether they should be
	  *                                 wrapped in quotation marks or not. (default = true = use json value format)
	  * @param disableTrustStoreVerification Whether the SSL trust store verification process should be disabled entirely.
	  *                                      Setting this to true may compromise data security, but will work around
	  *                                      Java's "trustAnchors parameter must be non-empty" -error.
	  *                                      Default = false = verify outgoing requests using the SSL trust store.
	  *                                      Use with discretion.
	  *
	  *                                      If you wish to fix these errors properly instead, please check:
	  *                                      https://stackoverflow.com/questions/6784463/error-trustanchors-parameter-must-be-non-empty
	  * @return A new gateway instance
	  */
	def apply(jsonParsers: Seq[JsonParser] = Single(JsonReader), maxConnectionsPerRoute: Int = 2,
	          maxConnectionsTotal: Int = 10,
	          maximumTimeout: Timeout = Timeout(connection = 5.minutes, read = 5.minutes),
	          parameterEncoding: Option[Codec] = None, defaultResponseEncoding: Codec = Codec.UTF8,
	          requestInterceptors: Seq[RequestInterceptor] = Empty,
	          responseInterceptors: Seq[ResponseInterceptor] = Empty,
	          allowBodyParameters: Boolean = true, allowJsonInUriParameters: Boolean = true,
	          disableTrustStoreVerification: Boolean = false) =
		new Gateway(jsonParsers, maxConnectionsPerRoute, maxConnectionsTotal, maximumTimeout, parameterEncoding,
			defaultResponseEncoding, requestInterceptors, responseInterceptors, Identity, allowBodyParameters,
			allowJsonInUriParameters, disableTrustStoreVerification)
	
	/**
	  * Creates a new gateway instance
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
	  * @param requestInterceptors  Interceptors that access and potentially modify all outgoing requests (default = empty)
	  * @param responseInterceptors Interceptors that access and potentially modify all incoming responses (default = empty)
	  * @param allowBodyParameters Whether parameters could be moved to request body when body is omitted (default = true).
	  *                            Use false if you wish to force parameters to uri parameters.
	  * @param allowJsonInUriParameters Whether uri parameters should be allowed to be converted to json values before
	  *                                 applying them. False if you want the parameters to be added "as is"
	  *                                 (using .toString). This mostly affects string values, whether they should be
	  *                                 wrapped in quotation marks or not. (default = true = use json value format)
	  * @param disableTrustStoreVerification Whether the SSL trust store verification process should be disabled entirely.
	  *                                      Setting this to true may compromise data security, but will work around
	  *                                      Java's "trustAnchors parameter must be non-empty" -error.
	  *                                      Default = false = verify outgoing requests using the SSL trust store.
	  *                                      Use with discretion.
	  *
	  *                                      If you wish to fix these errors properly instead, please check:
	  *                                      https://stackoverflow.com/questions/6784463/error-trustanchors-parameter-must-be-non-empty
	  * @param customizeClient A function for customizing the http client when it is first created
	  * @return A new gateway instance
	  */
	def custom(jsonParsers: Seq[JsonParser] = Single(JsonReader), maxConnectionsPerRoute: Int = 2,
	           maxConnectionsTotal: Int = 10,
	           maximumTimeout: Timeout = Timeout(connection = 5.minutes, read = 5.minutes),
	           parameterEncoding: Option[Codec] = None, defaultResponseEncoding: Codec = Codec.UTF8,
	           requestInterceptors: Seq[RequestInterceptor] = Empty,
	           responseInterceptors: Seq[ResponseInterceptor] = Empty,
	           allowBodyParameters: Boolean = true, allowJsonInUriParameters: Boolean = true,
	           disableTrustStoreVerification: Boolean = false)
	          (customizeClient: HttpClientBuilder => HttpClientBuilder) =
		new Gateway(jsonParsers, maxConnectionsPerRoute, maxConnectionsTotal, maximumTimeout, parameterEncoding,
			defaultResponseEncoding, requestInterceptors, responseInterceptors, customizeClient, allowBodyParameters,
			allowJsonInUriParameters, disableTrustStoreVerification)
}

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
  * @param requestInterceptors Interceptors that access and potentially modify all outgoing requests (default = empty)
  * @param responseInterceptors Interceptors that access and potentially modify all incoming responses (default = empty)
  * @param customizeClient A function for customizing the http client when it is first created (default = identity)
  * @param allowBodyParameters Whether parameters could be moved to request body when body is omitted (default = true).
  *                            Use false if you wish to force parameters to uri parameters.
  * @param allowJsonInUriParameters Whether uri parameters should be allowed to be converted to json values before
  *                                 applying them. False if you want the parameters to be added "as is"
  *                                 (using .toString). This mostly affects string values, whether they should be
  *                                 wrapped in quotation marks or not. (default = true = use json value format)
  * @param disableTrustStoreVerification Whether the SSL trust store verification process should be disabled entirely.
  *                                      Setting this to true may compromise data security, but will work around
  *                                      Java's "trustAnchors parameter must be non-empty" -error.
  *                                      Default = false = verify outgoing requests using the SSL trust store.
  *                                      Use with discretion.
  *
  *                                      If you wish to fix these errors properly instead, please check:
  *                                      https://stackoverflow.com/questions/6784463/error-trustanchors-parameter-must-be-non-empty
**/
class Gateway(jsonParsers: Seq[JsonParser] = Single(JsonReader), maxConnectionsPerRoute: Int = 2,
              maxConnectionsTotal: Int = 10,
              maximumTimeout: Timeout = Timeout(connection = 5.minutes, read = 5.minutes),
              parameterEncoding: Option[Codec] = None, defaultResponseEncoding: Codec = Codec.UTF8,
              requestInterceptors: Seq[RequestInterceptor] = Empty,
              responseInterceptors: Seq[ResponseInterceptor] = Empty,
              customizeClient: HttpClientBuilder => HttpClientBuilder = Identity,
              allowBodyParameters: Boolean = true, allowJsonInUriParameters: Boolean = true,
              disableTrustStoreVerification: Boolean = false)
{
    // ATTRIBUTES    -------------------------
	
	/**
	  * Default character encoding used when parsing response data (used when no character encoding is specified in
	  * response headers)
	  */
	private implicit val _defaultResponseEncoding: Codec = defaultResponseEncoding
	
    private lazy val connectionManager = {
	    val manager = {
		    if (disableTrustStoreVerification) {
			    // From https://www.baeldung.com/httpclient-ssl
			    // The tutorial used one function which didn't exist in HttpClient v5
			    val sslSocketFactory = new SSLConnectionSocketFactory(
				    SSLContexts.custom().loadTrustMaterial(null, (_: Any, _: Any) => true).build())
			    val socketFactoryRegistry = RegistryBuilder.create[ConnectionSocketFactory]()
				    .register("https", sslSocketFactory)
				    .register("http", new PlainConnectionSocketFactory())
				    .build()
			    new PoolingHttpClientConnectionManager(socketFactoryRegistry)
		    }
		    else
			    new PoolingHttpClientConnectionManager()
	    }
	    manager.setDefaultMaxPerRoute(maxConnectionsPerRoute)
	    manager.setMaxTotal(maxConnectionsTotal)
	    manager
    }
	
	// TODO: Add customizable timeouts (see https://www.baeldung.com/httpclient-timeout)
    private lazy val client = customizeClient(
	    HttpClients.custom().setConnectionManager(connectionManager).setConnectionManagerShared(true)).build()
    
	
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
    def makeBlockingRequest[R](request: Request)(consumeResponse: Try[StreamedResponse] => R) = {
	    // Intercepts the request, if appropriate
	    val req = requestInterceptors.foldLeft(request) { (r, i) => i.intercept(r) }
        Try {
            // Makes the base request (uri + params + body)
            val base = makeRequestBase(req.method, req.requestUri, req.params, req.body)
            // Adds the headers
	        req.headers.fields.foreach { case (key, value) => base.addHeader(key, value) }
			// Sets the timeout
			val config = {
				val builder = RequestConfig.custom()
				(req.timeout min maximumTimeout).thresholds.view.mapValues { _.toMillis.toInt }
					.foreach { case (timeoutType, millis) =>
						timeoutType match {
							case ConnectionTimeout => builder.setConnectTimeout(millis, TimeUnit.MILLISECONDS)
							case ReadTimeout => builder.setResponseTimeout(millis, TimeUnit.MILLISECONDS)
							case ManagerTimeout => builder.setConnectionRequestTimeout(millis, TimeUnit.MILLISECONDS)
						}
					}
				builder.build()
			}
			base.setConfig(config)
			
            // Performs the request and consumes any response
			client.execute(base).consume { rawResponse =>
				// Intercepts the response, if appropriate
				val response = responseInterceptors
					.foldLeft(Success(wrapResponse(rawResponse)): Try[StreamedResponse]) { (r, i) => i.intercept(r, req) }
				consumeResponse(response)
			}
        } match {
			case Success(result) => result
            case Failure(error) =>
	            // Intercepts request failures, also
	            consumeResponse(responseInterceptors
		            .foldLeft(Failure(error): Try[StreamedResponse]) { (r, i) => i.intercept(r, req) })
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
	  * @param logger An implicit logging implementation for handling possible response body parsing failures
	  * @return Future with the buffered response
	  */
	def stringResponseFor(request: Request)(implicit exc: ExecutionContext, logger: Logger) =
		responseFor(request)(ResponseParser.string)
	/**
	  * Performs an asynchronous request and parses the response body into a string
	  * @param request Request to send
	  * @param exc     Implicit execution context
	  * @return Future with the buffered response
	  */
	def tryStringResponseFor(request: Request)(implicit exc: ExecutionContext) =
		responseFor(request)(ResponseParser.tryString)
	
	/**
	  * Performs an asynchronous request and parses response body into a value (empty value on empty responses and
	  * read failures). Supports json and xml content types. Other content types are read as raw strings.
	  * @param request Request to send
	  * @param exc Implicit execution context
	  * @param logger An implicit logging implementation for handling possible response body parsing failures
	  * @return Future with the buffered response
	  */
	def valueResponseFor(request: Request)(implicit exc: ExecutionContext, logger: Logger) =
		responseFor(request)(ResponseParser.valueWith(jsonParsers))
	/**
	  * Performs an asynchronous request and parses response body into a value.
	  * Supports json and xml content types. Other content types are read as raw strings.
	  * @param request Request to send
	  * @param exc     Implicit execution context
	  * @return Future with the buffered response
	  */
	def tryValueResponseFor(request: Request)(implicit exc: ExecutionContext) =
		responseFor(request)(ResponseParser.tryValueWith(jsonParsers))
	
	/**
	  * Performs an asynchronous request and parses response body into a model (empty model on empty responses and
	  * read/parse failures). Supports json and xml content types.
	  * @param request Request to send
	  * @param exc Implicit execution context
	  * @param logger An implicit logging implementation for handling possible response body parsing failures
	  * @return Future with the buffered response
	  */
	def modelResponseFor(request: Request)(implicit exc: ExecutionContext, logger: Logger) =
		responseFor(request)(ResponseParser.modelWith(jsonParsers))
	/**
	  * Performs an asynchronous request and parses response body into a model.
	  * Supports json and xml content types.
	  * @param request Request to send
	  * @param exc     Implicit execution context
	  * @return Future with the buffered response
	  */
	def tryModelResponseFor(request: Request)(implicit exc: ExecutionContext) =
		responseFor(request)(ResponseParser.tryModelWith(jsonParsers))
	
	/**
	  * Performs an asynchronous request and parses response body into a value vector (empty vector on empty responses and
	  * read failures). Supports json and xml content types. Other content types are interpreted as strings and
	  * converted to values, then wrapped in a vector.
	  * @param request Request to send
	  * @param exc Implicit execution context
	  * @param logger An implicit logging implementation for handling possible response body parsing failures
	  * @return Future with the buffered response
	  */
	def valueVectorResponseFor(request: Request)(implicit exc: ExecutionContext, logger: Logger) =
		responseFor(request)(ResponseParser.valuesWith(jsonParsers))
	/**
	  * Performs an asynchronous request and parses response body into a value vector.
	  * Supports json and xml content types. Other content types are interpreted as strings and
	  * converted to values, then wrapped in a vector.
	  * @param request Request to send
	  * @param exc     Implicit execution context
	  * @return Future with the buffered response
	  */
	def tryValueVectorResponseFor(request: Request)(implicit exc: ExecutionContext) =
		responseFor(request)(ResponseParser.tryValuesWith(jsonParsers))
	/**
	  * Performs an asynchronous request and parses response body into a model vector (empty vector on empty responses and
	  * read/parse failures). Supports json and xml content types. Responses with only a single model have their
	  * contents wrapped in a vector.
	  * @param request Request to send
	  * @param exc Implicit execution context
	  * @param logger An implicit logging implementation for handling possible response body parsing failures
	  * @return Future with the buffered response
	  */
	def modelVectorResponseFor(request: Request)(implicit exc: ExecutionContext, logger: Logger) =
		responseFor(request)(ResponseParser.modelsWith(jsonParsers))
	/**
	  * Performs an asynchronous request and parses response body into a model vector.
	  * Supports json and xml content types. Responses with only a single model have their
	  * contents wrapped in a vector.
	  * @param request Request to send
	  * @param exc     Implicit execution context
	  * @return Future with the buffered response
	  */
	def tryModelVectorResponseFor(request: Request)(implicit exc: ExecutionContext) =
		responseFor(request)(ResponseParser.tryModelsWith(jsonParsers))
	
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
	private def makeRequestBase(method: Method, baseUri: String, params: Model = Model.empty,
	                            body: Option[HttpEntity]) =
	{
		def baseWith(uri: URI) = method match {
			case Get => new HttpGet(uri)
			case Delete => new HttpDelete(uri)
			case Post => new HttpPost(uri)
			case Put => new HttpPut(uri)
			case Patch => new HttpPatch(uri)
		}
		
	    if (method == Get || method == Delete) {
	        // Adds the parameters to uri, no body is supported
	        val uri = makeUriWithParams(baseUri, params)
		    baseWith(uri)
	    }
	    else if (body.isEmpty && allowBodyParameters) {
	        // If there is no body, adds the parameters as a body entity instead
	        val base = baseWith(new URI(baseUri))
	        makeParametersEntity(params).foreach(base.setEntity)
	        base
	    }
	    else {
	        // If both a body and parameters were provided, adds params to uri
	        val uri = makeUriWithParams(baseUri, params)
	        val base = baseWith(uri)
	        base.setEntity(body.get)
	        base
	    }
	}
	
	// Adds parameter values in JSON format to request uri, returns combined uri
	private def makeUriWithParams(baseUri: String, params: Model) =
	{
	    val builder = new URIBuilder(baseUri)
		// May encode parameter values
	    params.properties.foreach { a => builder.addParameter(a.name, paramValue(a.value)) }
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
	
	private def makeParametersEntity(params: Model) =
	{
	    if (params.isEmpty)
	        None
	    else
	    {
	        val paramsList = params.properties.map { c => new BasicNameValuePair(c.name, c.value.getString) }
	        Some(new UrlEncodedFormEntity(paramsList.asJava, StandardCharsets.UTF_8))
	    }
	}
	
	private def wrapResponse(response: CloseableHttpResponse) = 
	{
	    val status = statusForCode(response.getCode)
	    val headers = Headers(response.getHeaders.map(h => (h.getName, h.getValue)).toMap)
		
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
		
		/*
		override def consumeContent() =
	    {
	        b.stream.foreach { input =>
    	        val bytes = new Array[Byte](1024) //1024 bytes - Buffer size
                Iterator.continually (input.read(bytes)).takeWhile (-1 !=).foreach { _ => }
	        }
	    }*/
		
		
		override def getTrailers = () => new util.ArrayList[Header]()
		
		override def close() = {
			// Consumes and closes the input stream
			b.stream.foreach { input =>
				val bytes = new Array[Byte](1024) //1024 bytes - Buffer size
				Iterator.continually (input.read(bytes)).takeWhile (-1 !=).foreach { _ => }
				input.close()
			}
		}
		
		override def getTrailerNames = new util.HashSet[String]()
		
		override def getContent() = b.stream.getOrElse(null)
        
        override def getContentEncoding() = b.contentEncoding.orNull
		
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
			s"${b.contentType}$charsetPart"
		}
		
		override def isChunked() = b.chunked
		
		override def isRepeatable() = b.repeatable
		
		override def isStreaming() = !b.repeatable
		
		override def writeTo(output: OutputStream) = b.writeTo(output).get
	}
}