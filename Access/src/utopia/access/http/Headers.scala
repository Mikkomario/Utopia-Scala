package utopia.access.http

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template
import utopia.flow.generic.model.template.{ModelConvertible, Property}
import utopia.flow.operator.MaybeEmpty
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.time.Now
import utopia.flow.util.{Mutate, NotEmpty}
import utopia.flow.util.StringExtensions._

import java.nio.charset.{Charset, StandardCharsets}
import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneOffset, ZonedDateTime}
import java.util.Base64
import scala.io.Codec
import scala.language.implicitConversions
import scala.math.Ordering.Double.TotalOrdering
import scala.util.{Success, Try}

object Headers extends FromModelFactory[Headers]
{   
    // ATTRIBUTES    ---------------------
    
    /**
     * An empty set of headers
     */
    val empty = Headers()
    
    
    // IMPLICIT --------------------------
    
    // Treats the headers object as an empty headers instance
    implicit def headersAsInstance(headers: Headers.type): Headers = headers.empty
    
    
    // COMPUTED PROPERTIES    ------------
    
    /**
     * A set of headers containing a date specification
     */
    def currentDateHeaders = empty.withCurrentDate
    
    
    // IMPLEMENTED    ----------------------
    
    override def apply(model: template.ModelLike[Property]) = {
        val fields = model.nonEmptyProperties
            .flatMap { property => property.value.string.map { property.name.toLowerCase -> _ } }.toMap
        Success(new Headers(fields))
    }
    
    
    // OTHER    ----------------------------
    
    /**
      * Creates a new set of headers from the specified fields
      * @param fields Header fields
      * @return A new set of headers
      */
    def apply(fields: Map[String, String] = Map()) = new Headers(fields.map { case (k, v) => k.toLowerCase -> v })
}

/**
 * Headers represent headers used in html responses and requests
 * @author Mikko Hilpinen
 * @since 22.8.2017
 */
case class Headers private(fields: Map[String, String]) extends ModelConvertible with MaybeEmpty[Headers]
{
    // ATTRIBUTES   -----------------
    
    /**
      * The methods allowed for the server resource
      */
    lazy val allowedMethods = commaSeparatedValues("Allow").flatMap { Method.parse }
    
    /**
      * The content types accepted by the client
      */
    lazy val acceptedTypes = commaSeparatedValues("Accept").flatMap { ContentType.parse }
    /**
      * The charsets accepted by the client. Each charset is matched with a weight modifier. Higher
      * weight charsets should be preferred by the server.
      */
    lazy val acceptedCharsets = weightedValues("Accept-Charset").flatMap { case (charset, weight) =>
        Try(Charset.forName(charset)).toOption.map { _ -> weight }
    }
    
    /**
      * The type of the associated content. None if not defined.
      */
    lazy val contentType = semicolonSeparatedValues("Content-Type").headOption.flatMap { ContentType.parse }
    
    /**
      * The Date general-header field represents the date and time at which the message was
      * originated, having the same semantics as orig-date in RFC 822. The field value is an
      * HTTP-date, as described in section 3.3.1; it MUST be sent in RFC 1123 [8]-date format.
      */
    lazy val date = timeHeader("Date")
    
    
    // IMPLEMENTED    ---------------
    
    override def toModel = Model(fields.toVector.map { case (key, value) => key -> value.toValue })
    
    override def toString = toModel.toString
    
    
    // COMPUTED   ------------------
    
    /**
     * The charset preferred by the client. None if no charset is specified.
     */
    def preferredCharset = acceptedCharsets.maxByOption { _._2 }.map { _._1 }
    /**
     * The charset preferred by the client. UTF 8 if the client doesn't specify any other charset
     */
    def preferredCharsetOrUTF8 = preferredCharset getOrElse StandardCharsets.UTF_8
    /**
     * @return Languages that are accepted by the client, with weight modifiers. A larger weight means that the language
     *         is preferred by the client
     */
    def acceptedLanguages = weightedValues("Accept-Language")
    /**
     * @return The language most preferred by the client. None if not specified.
     */
    def preferredLanguage = preferredValue("Accept-Language")
    
    /**
      * The character set used in the associated content. None if not defined or unrecognised
      */
    def charset = contentType.flatMap { _.charset }
    /**
     * The name of the character set used in the associated content. None if not defined
     */
    @deprecated("Deprecated for removal. Likely buggy as well. Please use .charset instead.", "v1.5.1")
    def charsetString = semicolonSeparatedValues("Content-Type").getOption(1)
    /**
     * @return Encoding specified in the content type header (in codec format)
     */
    def codec = charset.map { Codec(_) }
    
    /**
     * 	The length of the response body in octets (8-bit bytes)
     */
    def contentLength = apply("Content-Length").getInt
    /**
      * @return Whether content length information has been provided
      */
    def isContentLengthProvided = isDefined("Content-Length")
    
    /**
     * @return Whether the date header is defined
     */
    def hasDate = isDefined("Date")
    /**
      * Creates a new set of headers with the updated message date / time
      */
    def withCurrentDate = withDate(Now)
    
    /**
     * The location of the generated or searched resource. (Usually) contains the whole url.
     */
    def location = apply("Location")
    
    /**
     * The time when the resource was last modified
     */
    def lastModified = timeHeader("Last-Modified")
    
    /**
      * @return The 'if modified since' -header which is used when the client wants to only update its cached data,
      *         in case there are changes.
      */
    def ifModifiedSince = timeHeader("If-Modified-Since")
    
    /**
     * Whether the data is chunked and the content length omitted
     */
    def isChunked = apply("Transfer-Encoding").contains("chunked")
    
    /**
      * @return Whether an authorization header has been specified
      */
    def containsAuthorization = isDefined("Authorization")
    /**
      * @return The provided authorization. Eg. "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==".
      *         An empty string if no auth header is provided.
      */
    def authorization = apply("Authorization")
    /**
     * @return Decrypted Username and password from a basic authorization header. None if the header was missing, not
     *         a basic authorization or not properly encoded
     */
    def basicAuthorization = NotEmpty(authorization).flatMap { auth =>
        val (authType, encodedValue) = auth.splitAtFirst(" ").toTuple
        if (authType ~== "Basic")
            Try { Base64.getDecoder.decode(encodedValue) }.toOption.map {
                new String(_, Codec.UTF8.charSet).splitAtFirst(":").toTuple }
        else
            None
    }
    /**
      * @return Non-decrypted (expected to have no encoding) token registered with the "bearer" authorization header.
      *         An empty string if there was no authorization header or if it was not of type "bearer".
      */
    def bearerAuthorization = authorization.mapIfNotEmpty { auth =>
        val (authType, token) = auth.splitAtFirst(" ").toTuple
        if (authType ~== "Bearer") token else ""
    }
    /**
      * @return Whether a bearer (token) authorization has been specified in this request
      *         (Authorization header starts with 'bearer ')
      */
    def containsBearerAuthorization = authorization.startsWithIgnoreCase("Bearer ")
    
    
    // IMPLEMENTED  ---------------
    
    override def self = this
    
    /**
      * @return Whether these headers are empty
      */
    override def isEmpty = fields.isEmpty
    
    
    // OPERATORS    ---------------
    
    /**
      * @param headerName A header
      * @return Whether these headers contain the specified header
      */
    def contains(headerName: String) = fields.contains(headerName.toLowerCase)
    
    /**
     * Finds the value associated with the specified header name. The value may contain multiple 
     * parts, depending from the header format. Returns None if the header has no value.
     */
    def apply(headerName: String) = get(headerName).getOrElse("")
    /**
      * @param headerName Name of the targeted header
      * @return String value of the targeted header. None if the header was not specified.
      */
    def get(headerName: String) = fields.get(headerName.toLowerCase)
    
    /**
     * Adds new values to a header. Will not overwrite any existing values.
     */
    def +(headerName: String, values: Seq[String], separator: String): Headers =
        withHeaderAdded(headerName, values, separator)
    /**
     * Adds a new value to a header. Will not overwrite any existing values.
     */
    def +(headerName: String, value: String, separator: String = ",") =
        withHeaderAdded(headerName, value, separator)
    /**
      * @param header A header key value pair
      * @return A copy of these headers with specified header appended (';' is used to separate multiple header values)
      */
    def +(header: Pair[String]) = withHeaderAdded(header.first, header.second)
    /**
      * @param header A header key value pair
      * @return A copy of these headers with specified header appended (';' is used to separate multiple header values)
      */
    // Kept for backwards compatibility (v1.4.1)
    def +(header: (String, String)) = withHeaderAdded(header._1, header._2)
    
    /**
     * Combines two headers with each other. If the headers have same keys, uses the keys from the 
     * rightmost headers
     */
    def ++(headers: Headers) = headers.notEmpty match {
        case Some(other) => Headers(fields ++ other.fields)
        case None => this
    }
    
    
    // OTHER METHODS    -----------
    
    /**
      * Checks whether speficied header field has been defined
      * @param headerName Header name (case-insensitive)
      * @return Whether such a header has been provided
      */
    def isDefined(headerName: String) = fields.contains(headerName.toLowerCase)
    
    /**
     * Returns multiple values where the original value is split into multiple parts. Returns an 
     * empty vector if there were no values for the header name
     */
    def splitValues(headerName: String, separator: String) =
        apply(headerName).split(separator).view.filter { _.nonEmpty }.toVector
    /**
     * Returns multiple values where the original value is separated with a comma (,). Returns an 
     * empty vector if there were no values for the header name
     */
    def commaSeparatedValues(headerName: String) = splitValues(headerName, ",")
    /**
     * Returns multiple values where the original value is separated with a semicolon (;). Returns an 
     * empty vector if there were no values for the header name
     */
    def semicolonSeparatedValues(headerName: String) = splitValues(headerName, ";")
    
    /**
     * @param headerName Name of target header
     * @return All values for the header with their relative weights where a larger value is more preferred
     */
    def weightedValues(headerName: String) = commaSeparatedValues(headerName).map {
        _.split(";") }.map {  set =>
            val weight = (if (set.length > 1) set(1).double else None) getOrElse 1.0
            set.head -> weight
    }.toMap
    
    /**
     * @param headerName Name of target header
     * @return The most preferred value for this header (based on value weights)
     */
    def preferredValue(headerName: String) = weightedValues(headerName).maxByOption { _._2 }.map { _._1 }
    
    /**
     * Returns a copy of these headers with a new header. Overwrites any previous values on the 
     * targeted header.
     */
    def withHeader(headerName: String, values: Seq[String], separator: String = ","): Headers = 
        withHeader(headerName, values.mkString(separator))
    /**
     * Returns a copy of these headers with a new header. Overwrites any previous values on the 
     * targeted header.
     */
    def withHeader(headerName: String, value: String) = new Headers(fields + (headerName.toLowerCase -> value))
    /**
      * Adds new values to a header. Will not overwrite any existing values.
      */
    def withHeaderAdded(headerName: String, values: Seq[String], separator: String): Headers = {
        if (values.nonEmpty)
            withHeaderAdded(headerName, values.reduce { (a, b) => s"$a$separator$b" }, separator)
        else
            this
    }
    /**
      * Adds a new value to a header. Will not overwrite any existing values.
      */
    def withHeaderAdded(headerName: String, value: String, separator: String = ",") = {
        get(headerName) match {
            // Case: Header already exists => Appends a value
            case Some(existingValue) =>
                val newValue = s"$existingValue$separator$value"
                Headers(fields + (headerName.toLowerCase -> newValue))
            // Case: New header => adds it
            case None => withHeader(headerName.toLowerCase, value)
        }
    }
    
    /**
     * Parses a header field into a time instant
     */
    def timeHeader(headerName: String) = get(headerName).flatMap { dateStr =>
        Try { Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(dateStr)) }.toOption
    }
    /**
     * Parses an instant into correct format and adds it as a header value. Overwrites a previous 
     * version of that header, if there is one.
     */
    def withTimeHeader(headerName: String, value: Instant) = withHeader(headerName, 
            DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.ofInstant(value, ZoneOffset.UTC)))
    
    /**
     * @param headerName Name of target header
     * @param values Header values, each with their own weight
     * @return A copy of these headers with added weighted headers
     */
    def withWeightedHeader(headerName: String, values: Map[String, Double]) =
        withHeader(headerName, values.map{ case (v, w) => s"$v;q=$w" }.toSeq)
    
    /**
     * Checks whether a method is allowed for the server side resource
     */
    def allows(method: Method) = allowedMethods.contains(method)
    /**
     * Overwrites the list of methods allowed to be used on the targeted resource
     */
    def withAllowedMethods(methods: Seq[Method]) = withHeader("Allow", methods.mkString(","))
    /**
     * Adds a new method to the methods allowed for the targeted resource
     */
    def withMethodAllowed(method: Method) = withHeaderAdded("Allow", method.toString)
    
    /**
     * Checks whether the client accepts the provided content type
     */
    def accepts(contentType: ContentType) = acceptedTypes.contains(contentType)
    /**
     * Finds the first accepted type from the provided options
     */
    def getAcceptedType(options: Seq[ContentType]) = {
        val accepted = acceptedTypes
        options.find(accepted.contains)
    }
    /**
     * Finds the most preferred accepted charset from the provided options
     */
    def getAcceptedCharset(options: Seq[Charset]) = {
        val accepted = acceptedCharsets.view.filterKeys(options.contains)
        if (accepted.isEmpty)
            None
        else
            Some(accepted.maxBy(_._2)._1)
    }
    /**
     * @param options Available language options
     * @return The most preferred language from the specified options. None if none of them is accepted.
     */
    def getAcceptedLanguage(options: Seq[String]) =
        acceptedLanguages.view.filterKeys { lang => options.exists { _ ~== lang } }.maxByOption { _._2 }.map { _._1 }
    
    /**
     * Overwrites the set of accepted content types
     */
    def withAcceptedTypes(types: Seq[ContentType]) = withHeader("Accept", types.mkString(","))
    /**
      * @param f A mapping function for modifying the accepted types
      * @return Copy of these headers with modified accepted types
      */
    def mapAcceptedTypes(f: Mutate[Seq[ContentType]]) = withAcceptedTypes(f(acceptedTypes))
    
    /**
      * @param contentType The only acceptable content type
      * @return A copy of these headers that only accepts that content type
      */
    def withAcceptedType(contentType: ContentType) = withHeader("Accept", contentType.toString)
    /**
     * Adds a new content type to the list of content types accepted by the client
     */
    def withTypeAccepted(contentType: ContentType) = withHeaderAdded("Accept", contentType.toString)
    
    /**
     * Overwrites the set of accepted charsets
     */
    def withAcceptedCharsets(charsets: Map[Charset, Double]) =
        withWeightedHeader("Accept-Charset", charsets.map { case (charset, wt) => charset.name() -> wt })
    /**
     * Overwrites the set of accepted charsets
     */
    def withAcceptedCharsets(charsets: Seq[Charset]) =
        withHeader("Accept-Charset", charsets.map { _.name }.mkString(","))
    /**
     * Adds a new charset to the list of accepted charsets
     */
    def withCharsetAccepted(charset: Charset, weight: Double = 1) =
        withHeaderAdded("Accept-Charset", s"${charset.name()};q=$weight")
    
    /**
     * @param languages Accepted languages with their "weights"
     * @return A copy of these headers with specified accepted languages
     */
    def withAcceptedLanguages(languages: Map[String, Double]) = withWeightedHeader("Accept-Language", languages)
    /**
     * @param languages A list of accepted languages
     * @return A copy of these headers with specified accepted languages
     */
    def withAcceptedLanguages(languages: Seq[String]) = withHeader("Accept-Language", languages)
    /**
     * @param language Accepted language
     * @param weight Priority of specified language (default = 1.0)
     * @return A copy of these headers with specified accepted language added
     */
    def withLanguageAccepted(language: String, weight: Double = 1) =
        withHeaderAdded("Accept-Language", s"$language;q=$weight")
    
    /**
     * Creates a new headers with the content type (and character set) specified
     * @param contentType the type of the content
     * @param charset then encoding that was used used when the content was written to the response
     */
    def withContentType(contentType: ContentType, charset: Option[Charset] = None) = {
        val charsetPart = charset match {
            case Some(charset) => s";${charset.name()}"
            case None => ""
        }
        withHeader("Content-Type", s"$contentType$charsetPart")
    }
    
    /**
     * Creates a new header with the time when the message associated with this header was originated. 
     * If the message was just created, you may wish to use #withCurrentDate
     */
    def withDate(time: Instant) = withTimeHeader("Date", time)
    
    /**
     * Creates a new header with the time when the resource was last modified
     */
    def withLastModified(time: Instant) = withTimeHeader("Last-Modified", time)
    
    /**
      * @param timeThreshold A time threshold for the 'if-modified-since' -header
      * @return A copy of these headers with specified if-modified-since threshold
      */
    def withIfModifiedSince(timeThreshold: Instant) = withTimeHeader("If-Modified-Since", timeThreshold)
    
    /**
     * Creates a new header with a specified location information
     */
    def withLocation(location: String) = withHeader("Location", location)
    
    /**
      * Creates a copy of these headers that contains authorization
      * @param authString The authorization string. Eg. "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ=="
      * @return A copy of these headers with authorization
      */
    def withAuthorization(authString: String) = withHeader("Authorization", authString)
    /**
      * Creates a copy of these headers that uses basic authentication
      * @param userName Username
      * @param password Password
      * @return A copy of these headers with basic authentication header included
      */
    def withBasicAuthorization(userName: String, password: String) = {
        // Encodes the username + password with base64
        val encoded = Base64.getEncoder.encodeToString(s"$userName:$password".getBytes(Codec.UTF8.charSet))
        withAuthorization(s"Basic $encoded")
    }
    /**
      * @param token Authorization/access token
      * @return A copy of these headers with an authorization header containing specified token
      */
    def withBearerAuthorization(token: String) = withAuthorization(s"Bearer $token")
    
    // TODO: Implement support for following predefined headers:
    // https://en.wikipedia.org/wiki/List_of_HTTP_header_fields
    /*
     * - Content-Encoding
     * - Content-Language
     * - Expires (?)
     */
}