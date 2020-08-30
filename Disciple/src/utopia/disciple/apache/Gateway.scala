package utopia.disciple.apache

import scala.jdk.CollectionConverters._
import utopia.access.http.Method._

import scala.language.implicitConversions
import scala.language.postfixOps
import utopia.access.http.Method
import org.apache.http.client.methods.{CloseableHttpResponse, HttpDelete, HttpGet, HttpPatch, HttpPost, HttpPut}
import org.apache.http.impl.client.HttpClients
import utopia.access.http.Status
import utopia.access.http.Headers

import scala.util.Try
import scala.util.Success
import scala.util.Failure
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import java.io.InputStream

import utopia.flow.util.AutoClose._
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.TimeExtensions._
import utopia.flow.datastructure.immutable.{Constant, Model, Value}
import org.apache.http.message.BasicNameValuePair
import org.apache.http.Consts
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.HttpEntity
import org.apache.http.client.utils.URIBuilder
import org.apache.http.message.BasicHeader
import java.io.OutputStream
import java.net.URLEncoder
import java.nio.charset.Charset

import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.impl.client.CloseableHttpClient
import utopia.disciple.http.request.TimeoutType.{ConnectionTimeout, ManagerTimeout, ReadTimeout}
import utopia.disciple.http.request.{Body, Request, Timeout}
import utopia.disciple.http.response.{ResponseParser, StreamedResponse}
import utopia.flow.generic.ModelType
import utopia.flow.parse.{JSONReader, JsonParser, XmlReader}

import scala.io.{Codec, Source}


/**
* Gateway is the singular instance, through which simple http requests can be made
* @author Mikko Hilpinen
* @since 22.2.2018
**/
object Gateway
{
    // ATTRIBUTES    -------------------------
    
    private var _introducedStatuses = Status.values
    
    private val connectionManager = new PoolingHttpClientConnectionManager()
    
	// TODO: Add customizable timeouts (see https://www.baeldung.com/httpclient-timeout)
    private var _client: Option[CloseableHttpClient] = None
    private def client = _client.getOrElse(HttpClients.custom().setConnectionManager(
                connectionManager).setConnectionManagerShared(true).build())
	
	/**
	 * Default character encoding used when parsing response data (used when no character encoding is specified in
	 * response headers)
	 */
	implicit var defaultResponseEncoding: Codec = Codec.UTF8
	
	/**
	  * Set of custom json parsers to use when content encoding matches parser default. Specify your own parser/parsers
	  * to override default functionality (JSONReader)
	  */
	var jsonParsers = Vector[JsonParser]()
	
	/**
	  * Maximum timeouts for a single request. Changing this value will affect all future requests.
	  */
	var maximumTimeout = Timeout(connection = 5.minutes, read = 5.minutes)
	
    
    // COMPUTED PROPERTIES    ----------------
    
    /**
     * The maximum number of simultaneous connections to a single route
     */
    def maxConnectionsPerRoute = connectionManager.getDefaultMaxPerRoute
    def maxConnectionsPerRoute_=(max: Int) = 
    {
        connectionManager.setDefaultMaxPerRoute(max)
        invalidateClient()
    }
    /**
     * The maximum number of simultaneous connections in total
     */
    def maxConnectionsTotal = connectionManager.getMaxTotal
    def maxConnectionsTotal_=(max: Int) = 
    {
        connectionManager.setMaxTotal(max)
        invalidateClient()
    }
    
    
    // OTHER METHODS    ----------------------
    
    // TODO: Add support for multipart body:
    // https://stackoverflow.com/questions/2304663/apache-httpclient-making-multipart-form-post
	
	/**
	  * Introduces the specified status as one of the recognized statuses in this interface. The new status
	  * will replace a possible existing status with the same code.
	  * @param status Status to recognize in future responses
	  */
	def introduceStatus(status: Status) =
		_introducedStatuses = _introducedStatuses.filterNot { _.code == status.code } :+ status
	
	/**
	  * Introduces a number of new statuses so that they are recognized by this interface. New status versions will
	  * replace possible already existing statuses with same codes.
	  * @param statuses Statues to recognize in future responses.
	  */
	def introduceStatuses(statuses: Iterable[Status]) =
	{
		val newStatuses = statuses.distinctBy { _.code }
		_introducedStatuses = _introducedStatuses.filterNot { s =>
			newStatuses.exists { _.code == s.code } } ++ newStatuses
	}
	
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
            val base = makeRequestBase(request.method, request.requestUri, request.params, request.body,
				request.supportsBodyParameters, request.parameterEncoding)
            
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
     * Performs a request and buffers / parses it to the program memory
     * @param request the request that is sent to the server
     * @param parseResponse the function that parses the response stream contents
     * @return A future that holds the request results. Please note that the Future is a failure if no data was received.
     */
	@deprecated("replaced with responseFor(...)", "v1.3")
    def getResponse[A](request: Request)(parseResponse: (InputStream, Headers) => Try[A])(implicit context: ExecutionContext) =
		responseFor(request)(ResponseParser.failOnEmpty { (stream, headers, _) => parseResponse(stream, headers) })
	
	/**
	  * Performs a request and buffers / parses it to the program memory
	  * @param request the request that is sent to the server
	  * @param contentOnEmptyResponse The content that replaces empty result contents
	  * @param parseResponse the function that parses the response stream contents
	  * @return A future that holds the request results. Please note that the Future is a failure if no data was received.
	  */
	@deprecated("Replaced with responseFor(...)", "v1.3")
	def getResponse[A](request: Request, contentOnEmptyResponse: => A)(parseResponse: (InputStream, Headers) => Try[A])
					  (implicit context: ExecutionContext) =
	{
		responseFor(request)(ResponseParser.defaultOnEmpty(contentOnEmptyResponse) { (stream, headers, _) =>
			parseResponse(stream, headers) })
	}
	
	/**
	  * Performs an asynchronous request and parses the response body into a string (empty string on empty
	  * responses and read failures)
	  * @param request Request to send
	  * @param exc Implicit execution context
	  * @return Future with the buffered response
	  */
	def stringResponseFor(request: Request)(implicit exc: ExecutionContext) = responseFor(request)(ResponseParser.string)
	
	/**
	  * Performs an asynchronous request and parses the response to program memory as string
	  * @param request The request sent to the server
	  * @param context An implicit execution context
	  * @return A future for the parsed response
	  */
	@deprecated("Please use stringResponseFor(...) instead", "v1.3")
	def getStringResponse(request: Request)(implicit context: ExecutionContext) =
		responseFor(request)(ResponseParser.tryString)
	
	/**
	 * A parsing function that reads response contents as a string
	 * @param stream Response body stream
	 * @param headers Response body headers
	 * @return Parsed string. May contain failure.
	 */
	@deprecated("Replaced with ResponseParser.tryString", "v1.3")
	def stringFromResponse(stream: InputStream, headers: Headers) = Try {
		Source.fromInputStream(stream)(headers.codec.getOrElse(defaultResponseEncoding)).consume { _.getLines.mkString } }
	
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
	  * Performs an asynchronous request and parses the response from JSON
	  * @param request Request sent to the server
	  * @param context Implicit execution context
	  * @return A future for the parsed response
	  */
	@deprecated("Please use valueResponseFor(...) instead", "v1.3")
	def getJSONResponse(request: Request)(implicit context: ExecutionContext) =
		getResponse(request, Value.empty)(jsonFromResponse)
	
	/**
	 * A parsing function that reads response into a json value
	 * @param stream Response body stream
	 * @param headers Response body headers
	 * @return Parsed json value
	 */
	@deprecated("Replaced with ResponseParser.value(...)", "v1.3")
	def jsonFromResponse(stream: InputStream, headers: Headers) =
	{
		// Checks whether a custom parser can be used
		val encoding = headers.codec.getOrElse(defaultResponseEncoding)
		jsonParsers.find { _.defaultEncoding == encoding } match
		{
			case Some(parser) => parser(stream)
			case None => JSONReader(stream, encoding)
		}
	}
	
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
	 * Performs an asynchronous request and parses the response from JSON to a model. Expects response contents to
	 * be a json object.
	 * @param request Request set to server
	 * @param context Implicit execution context
	 * @return A future for the parsed response
	 */
	@deprecated("Replaced with modelResponseFor(...)", "v1.3")
	def getJSONModelResponse(request: Request)(implicit context: ExecutionContext) =
		getResponse(request, Model.empty)(jsonModelFromResponse)
	
	/**
	 * A parsing function tha reads response body into a json model
	 * @param stream Response body stream
	 * @param headers Response body headers
	 * @return Parsed json model
	 */
	@deprecated("Replaced with modelResponseFor(...)", "v1.3")
	def jsonModelFromResponse(stream: InputStream, headers: Headers) = jsonFromResponse(stream, headers).map { _.getModel }
	
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
	 * Performs an asynchronous request and parses the response from JSON to a vector. Expects response contents
	 * to be a json array.
	 * @param request Request set to server
	 * @param context Implicit execution context
	 * @return A future for the parsed response
	 */
	@deprecated("Replaced with valueVectorResponseFor(...)", "v1.3")
	def getJSONVectorResponse(request: Request)(implicit context: ExecutionContext) =
		getResponse(request, Vector[Value]())(jsonVectorFromResponse)
	
	/**
	 * A parsing function that reads response body into a list of json values
	 * @param stream Response body stream
	 * @param headers Response body headers
	 * @return Parsed values
	 */
	@deprecated("Replaced with valueVectorResponseFor(...)", "v1.3")
	def jsonVectorFromResponse(stream: InputStream, headers: Headers) = jsonFromResponse(stream, headers).map { _.getVector }
	
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
	 * Performs an asynchronous request and parses the response from JSON to a number of models. Expects response contents
	 * to be a json array filled with json objects.
	 * @param request Request set to server
	 * @param context Implicit execution context
	 * @return A future for the parsed response
	 */
	@deprecated("Replaced with modelVectorResponseFor(...)", "v1.3")
	def getJSONMultiModelResponse(request: Request)(implicit context: ExecutionContext) =
		getResponse(request, Vector[Model[Constant]]())(multipleJsonModelsFromResponse)
	
	/**
	 * A parsing function that reads one or more models from response (expects json model or json array format)
	 * @param stream Response body stream
	 * @param headers Response body headers
	 * @return Parsed model(s)
	 */
	@deprecated("Replaced with modelVectorResponseFor(...)", "v1.3")
	def multipleJsonModelsFromResponse(stream: InputStream, headers: Headers) = jsonFromResponse(stream, headers).map { v =>
		if (v.isOfType(ModelType))
			Vector(v.getModel)
		else
			v.getVector.flatMap { _.model }
	}
	
	/**
	  * Performs an asynchronous request and parses response body into an xml element (failure on empty responses and
	  * read/parse failures).
	  * @param request Request to send
	  * @param exc Implicit execution context
	  * @return Future with the buffered response
	  */
	def xmlResponseFor(request: Request)(implicit exc: ExecutionContext) = responseFor(request)(ResponseParser.xml)
	
	/**
	  * Performs an asynchronous request and parses the response to Xml
	  * @param request Request sent to the server
	  * @param context Implicit execution context
	  * @return A future for the parsed response
	  */
	@deprecated("Replaced with xmlResponseFor(...)", "v1.3")
	def getXmlResponse(request: Request)(implicit context: ExecutionContext) =
		getResponse(request)(xmlFromResponse)
	
	/**
	 * A parsing function that reads response body into an xml element
	 * @param stream Response body stream
	 * @param headers Response body headers
	 * @return Parsed xml element
	 */
	@deprecated("Replaced with xmlResponseFor(...)", "v1.3")
	def xmlFromResponse(stream: InputStream, headers: Headers) = XmlReader.parseStream(stream,
		headers.charset.getOrElse(Charset.forName("UTF-8")))
	
    private def invalidateClient() = 
    {
        _client.foreach(_.close())
        _client = None
    }
    
    // Adds parameters and body to the request base. No headers are added at this point
	private def makeRequestBase(method: Method, baseUri: String, params: Model[Constant] = Model.empty,
	        body: Option[HttpEntity], supportBodyParameters: Boolean, parameterEncoding: Option[Codec]) =
	{
	    if (method == Get || method == Delete)
	    {
	        // Adds the parameters to uri, no body is supported
	        val uri = makeUriWithParams(baseUri, params, parameterEncoding)
	        if (method == Get) new HttpGet(uri) else new HttpDelete(uri)
	    }
	    else if (body.isEmpty && supportBodyParameters)
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
	        val uri = makeUriWithParams(baseUri, params, parameterEncoding)
	        val base = if (method == Post) new HttpPost(uri) else new HttpPut(uri)
	        base.setEntity(body.get)
	        base
	    }
	}
	
	// Adds parameter values in JSON format to request uri, returns combined uri
	private def makeUriWithParams(baseUri: String, params: Model[Constant], encoding: Option[Codec]) =
	{
	    val builder = new URIBuilder(baseUri)
		// May encode parameter values
	    params.attributes.foreach { a => builder.addParameter(a.name, paramValue(a.value, encoding)) }
	    builder.build()
	}
	
	private def paramValue(originalValue: Value, encoding: Option[Codec]) =
	{
		val valueString = originalValue.toJson
		encoding match
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
	        val paramsList = params.attributes.map(c => new BasicNameValuePair(c.name, c.value.stringOr()))
	        Some(new UrlEncodedFormEntity(paramsList.asJava, Consts.UTF_8))
	    }
	}
	
	private def wrapResponse(response: CloseableHttpResponse) = 
	{
	    val status = statusForCode(response.getStatusLine.getStatusCode)
	    val headers = new Headers(response.getAllHeaders.map(h => (h.getName, h.getValue)).toMap)
	    
	    new StreamedResponse(status, headers)({ Option(response.getEntity).map { _.getContent } })
	}
	
	private def statusForCode(code: Int) = _introducedStatuses.find(
	        _.code == code).getOrElse(new Status("Other", code))
	
	
	// IMPLICIT CASTS    ------------------------
	
	private implicit def convertOption[T](option: Option[T])
	        (implicit f: T => HttpEntity): Option[HttpEntity] = option.map(f)
	
	//noinspection JavaAccessorMethodOverriddenAsEmptyParen
	private implicit class EntityBody(val b: Body) extends HttpEntity
	{
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
		
		override def getContentType() = new BasicHeader("Content-Type", b.contentType.toString() + b.charset.map(_.name()).getOrElse(""))
		
		override def isChunked() = b.chunked
		
		override def isRepeatable() = b.repeatable
		
		override def isStreaming() = !b.repeatable
		
		override def writeTo(output: OutputStream) = b.writeTo(output).get
	}
}