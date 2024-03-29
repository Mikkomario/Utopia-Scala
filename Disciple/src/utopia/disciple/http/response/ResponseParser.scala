package utopia.disciple.http.response

import utopia.access.http.{Headers, Status, StatusGroup}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.error.DataTypeException
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.generic.model.mutable.DataType.ModelType
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.parse.AutoClose._
import utopia.flow.parse.json.{JsonParser, JsonReader}
import utopia.flow.parse.xml.XmlReader
import utopia.flow.util.logging.Logger

import java.io.InputStream
import scala.io.{Codec, Source}
import scala.util.{Failure, Success, Try}

object ResponseParser
{
	// COMPUTED	-----------------------------
	
	/**
	  * @param defaultEncoding Encoding assumed when no encoding information is present in the response headers (implicit)
	  * @param logger An implicit logger used for recording parsing failures
	  * @return A response parser that produces string content (empty string on read failures and empty responses)
	  */
	def string(implicit defaultEncoding: Codec, logger: Logger) =
		parseOrDefault("") { (stream, headers, _) =>
			Try { Source.fromInputStream(stream)(headers.codec.getOrElse(defaultEncoding)).consume { _.getLines().mkString } }
		}
	/**
	  * @param defaultEncoding Encoding assumed when no encoding information is present in the response headers (implicit)
	  * @return A response parser that produces string content (empty string on empty responses, Try[string] otherwise)
	  */
	def tryString(implicit defaultEncoding: Codec) = defaultOnEmpty("") { (stream, headers, _) =>
		Try { Source.fromInputStream(stream)(headers.codec.getOrElse(defaultEncoding)).consume { _.getLines().mkString } }
	}
	
	/**
	  * @param defaultEncoding Encoding assumed when no encoding information is present in the response headers (implicit)
	  * @return A response parser that produces an xml element. Produces a failure on empty and non-parseable responses.
	  */
	def xml(implicit defaultEncoding: Codec) = failOnEmpty { (stream, headers, _) =>
		XmlReader.parseStream(stream, headers.codec.getOrElse(defaultEncoding).charSet) }
	
	/**
	  * @param parser         Json parser
	  * @param defaultEncoding Encoding assumed when no encoding information is present in the response headers (implicit)
	  * @param logger          An implicit logger used for recording parsing failures
	  * @return A response parser that produces a value (empty value on empty responses and parsing failures)
	  */
	def value(implicit parser: JsonParser, defaultEncoding: Codec, logger: Logger) =
		valueWith(Some(parser))
	/**
	  * @param parser         A json parser
	  * @param defaultEncoding Encoding assumed when no encoding information is present in the response headers (implicit)
	  * @return A response parser that produces a value (empty value on empty responses)
	  */
	def tryValue(implicit parser: JsonParser, defaultEncoding: Codec) =
		tryValueWith(Some(parser))
	/**
	  * @param parser         A json parser
	  * @param defaultEncoding Encoding assumed when no encoding information is present in the response headers (implicit)
	  * @param logger          An implicit logger used for recording parsing failures
	  * @return A response parser that produces a model (empty model on empty responses and parsing failures)
	  */
	def model(implicit parser: JsonParser, defaultEncoding: Codec, logger: Logger) =
		modelWith(Some(parser))
	/**
	  * @param parser         A json parser
	  * @param defaultEncoding Encoding assumed when no encoding information is present in the response headers (implicit)
	  * @return A response parser that produces models. Fails on conversion and parsing failures.
	  */
	def tryModel(implicit parser: JsonParser, defaultEncoding: Codec) =
		tryModelWith(Some(parser))
	/**
	  * @param parser         A json parser
	  * @param defaultEncoding Encoding assumed when no encoding information is present in the response headers (implicit)
	  * @param logger          An implicit logger used for recording parsing failures
	  * @return A response parser that produces a value vector (empty vector on empty responses).
	  *         If the response contained a non-vector value, it is wrapped in a vector.
	  */
	def values(implicit parser: JsonParser, defaultEncoding: Codec, logger: Logger) =
		valuesWith(Some(parser))
	/**
	  * @param parser         A json parser
	  * @param defaultEncoding Encoding assumed when no encoding information is present in the response headers (implicit)
	  * @return A response parser that produces a value vector (empty vector on empty responses).
	  *         If the response contained a non-vector value, it is wrapped in a vector.
	  *         Contains a failure on parsing failures.
	  */
	def tryValues(implicit parser: JsonParser, defaultEncoding: Codec) =
		tryValuesWith(Some(parser))
	/**
	  * @param parser         A json parser
	  * @param defaultEncoding Encoding assumed when no encoding information is present in the response headers (implicit)
	  * @param logger          An implicit logger used for recording parsing failures
	  * @return A response parser that produces a model vector (empty vector on empty responses and parsing failures).
	  *         If the response contained a singular model, it is wrapped in a vector.
	  */
	def models(implicit parser: JsonParser, defaultEncoding: Codec, logger: Logger) =
		modelsWith(Some(parser))
	/**
	  * @param parser         A json parser
	  * @param defaultEncoding Encoding assumed when no encoding information is present in the response headers (implicit)
	  * @return A response parser that produces a model vector (empty vector on empty responses).
	  *         If the response contained a singular model, it is wrapped in a vector.
	  *         Contains a failure if json parsing or any model parsing failed.
	  */
	def tryModels(implicit parser: JsonParser, defaultEncoding: Codec) =
		tryModelsWith(Some(parser))
	
	
	// OTHER	-----------------------------
	
	/**
	  * @param parsers Available json parsers
	  * @param defaultEncoding Encoding assumed when no encoding information is present in the response headers (implicit)
	  * @param logger An implicit logger used for recording parsing failures
	  * @return A response parser that produces a value (empty value on empty responses and parsing failures)
	  */
	def valueWith(parsers: Iterable[JsonParser])(implicit defaultEncoding: Codec, logger: Logger) =
		parseOrDefault(Value.empty) { (stream, headers, _) => parseValue(stream, headers, parsers) }
	/**
	  * @param parsers Available json parsers
	  * @param defaultEncoding Encoding assumed when no encoding information is present in the response headers (implicit)
	  * @return A response parser that produces a value (empty value on empty responses)
	  */
	def tryValueWith(parsers: Iterable[JsonParser])(implicit defaultEncoding: Codec) =
		defaultOnEmpty(Value.empty) { (stream, headers, _) => parseValue(stream, headers, parsers) }
	/**
	  * @param parsers Available json parsers
	  * @param defaultEncoding Encoding assumed when no encoding information is present in the response headers (implicit)
	  * @param logger An implicit logger used for recording parsing failures
	  * @return A response parser that produces a model (empty model on empty responses and parsing failures)
	  */
	def modelWith(parsers: Iterable[JsonParser])(implicit defaultEncoding: Codec, logger: Logger) =
		parseOrDefault(Model.empty) { (stream, headers, _) => parseValue(stream, headers, parsers).map { _.getModel } }
	/**
	  * @param parsers Available json parsers
	  * @param defaultEncoding Encoding assumed when no encoding information is present in the response headers (implicit)
	  * @return A response parser that produces models. Fails on conversion and parsing failures.
	  */
	def tryModelWith(parsers: Iterable[JsonParser])(implicit defaultEncoding: Codec) =
		defaultOnEmpty(Model.empty) { (stream, headers, _) =>
			parseValue(stream, headers, parsers).flatMap { v =>
				v.castTo(ModelType).toTry { DataTypeException(s"Can't cast ${ v.description } to a model") }
					.map { _.getModel }
			}
		}
	
	/**
	  * @param parsers Available json parsers
	  * @param defaultEncoding Encoding assumed when no encoding information is present in the response headers (implicit)
	  * @param logger An implicit logger used for recording parsing failures
	  * @return A response parser that produces a value vector (empty vector on empty responses).
	  *         If the response contained a non-vector value, it is wrapped in a vector.
	  */
	def valuesWith(parsers: Iterable[JsonParser])(implicit defaultEncoding: Codec, logger: Logger) =
		parseOrDefault(Vector[Value]()) { (stream, headers, _) => parseValue(stream, headers, parsers).map { value =>
			if (value.isEmpty)
				Vector()
			else
				value.vectorOr(Vector(value))
		} }
	/**
	  * @param parsers         Available json parsers
	  * @param defaultEncoding Encoding assumed when no encoding information is present in the response headers (implicit)
	  * @return A response parser that produces a value vector (empty vector on empty responses).
	  *         If the response contained a non-vector value, it is wrapped in a vector.
	  *         Contains a failure on parsing failures.
	  */
	def tryValuesWith(parsers: Iterable[JsonParser])(implicit defaultEncoding: Codec) =
		defaultOnEmpty(Vector[Value]()) { (stream, headers, _) => parseValue(stream, headers, parsers).map { v =>
			if (v.isEmpty) Vector() else v.vectorOr(Vector(v))
		} }
	/**
	  * @param parsers Available json parsers
	  * @param defaultEncoding Encoding assumed when no encoding information is present in the response headers (implicit)
	  * @param logger An implicit logger used for recording parsing failures
	  * @return A response parser that produces a model vector (empty vector on empty responses and parsing failures).
	  *         If the response contained a singular model, it is wrapped in a vector.
	  */
	def modelsWith(parsers: Iterable[JsonParser])(implicit defaultEncoding: Codec, logger: Logger) =
		parseOrDefault(Vector[Model]()) { (stream, headers, _) => parseValue(stream, headers, parsers).map { value =>
			if (value.isEmpty)
				Vector()
			else
				value.vectorOr { Vector(value) }.flatMap { _.model }
		} }
	/**
	  * @param parsers         Available json parsers
	  * @param defaultEncoding Encoding assumed when no encoding information is present in the response headers (implicit)
	  * @return A response parser that produces a model vector (empty vector on empty responses).
	  *         If the response contained a singular model, it is wrapped in a vector.
	  *         Contains a failure if json parsing or any model parsing failed.
	  */
	def tryModelsWith(parsers: Iterable[JsonParser])(implicit defaultEncoding: Codec) =
		defaultOnEmpty(Vector[Model]()) { (stream, headers, _) => parseValue(stream, headers, parsers).flatMap { v =>
			v.getVector.tryMap { _.tryModel }
		} }
	
	/**
	  * Creates a bew response parser based on the two specified functions
	  * @param parseBody Function for parsing non-empty responses
	  * @param processEmpty Function for processing empty responses
	  * @tparam A Type of function result
	  * @return A new parser
	  */
	def apply[A](parseBody: (InputStream, Headers, Status) => A)(processEmpty: (Headers, Status) => A) =
		new ResponseParser[A](parseBody, processEmpty)
	/**
	  * Creates a new response parser that provides the specified default value for empty responses
	  * @param default Default value (call by name)
	  * @param parseBody Function for parsing response body (may fail)
	  * @tparam A Type of successful parse result
	  * @return A new parser
	  */
	def defaultOnEmpty[A](default: => A)(parseBody: (InputStream, Headers, Status) => Try[A]) =
		new ResponseParser[Try[A]](parseBody, (_, _) => Success(default))
	/**
	  * Creates a new response parser that uses the specified default value in case of both parsing failures and
	  * empty responses
	  * @param default Default result (call by name)
	  * @param parseBody Function for parsing response body (may fail)
	  * @param logger An implicit logger used for recording parsing failures
	  * @tparam A Type of successful parse result
	  * @return A new parser
	  */
	def parseOrDefault[A](default: => A)(parseBody: (InputStream, Headers, Status) => Try[A])(implicit logger: Logger) =
		new ResponseParser[A]((stream, headers, status) =>
			parseBody(stream, headers, status).getOrMap { error => logger(error); default }, (_, _) => default)
	/**
	  * Creates a parser that simply fails on empty responses
	  * @param parseBody A function for processing non-empty responses (may fail)
	  * @tparam A Type of parse result
	  * @return A new response parser
	  */
	def failOnEmpty[A](parseBody: (InputStream, Headers, Status) => Try[A]) = new ResponseParser[Try[A]](
		parseBody, (_, _) => Failure(new NoContentException("Response doesn't contain a body")))
	/**
	  * Creates a new response parser based on two functions, one for successful statuses (1XX - 3XX) and another for
	  * failure statuses (4XX - 5XX)
	  * @param parseSuccess Parser used on success status codes (stream parameter is None on empty responses)
	  * @param parseFailure Parser used on failure status codes (stream parameter is None on empty responses)
	  * @tparam A Type of parse result
	  * @return A new parser
	  */
	def successOrFailure[A](parseSuccess: (Option[InputStream], Headers) => A)
						   (parseFailure: (Option[InputStream], Headers) => A) =
		apply { (stream, headers, status) =>
			if (StatusGroup.failure.contains(status.group))
				parseFailure(Some(stream), headers)
			else
				parseSuccess(Some(stream), headers)
		} { (headers, status) =>
			if (StatusGroup.failure.contains(status.group))
				parseFailure(None, headers)
			else
				parseSuccess(None, headers)
		}
	/**
	  * Creates a new response parser based on two functions, one for successful statuses (1XX - 3XX) and another for
	  * failure statuses (4XX - 5XX)
	  * @param parseSuccess Parser used on success status codes (stream parameter is None on empty responses)
	  * @param parseFailure Parser used on failure status codes (stream parameter is None on empty responses)
	  * @tparam F Failure parse result type
	  * @tparam S Success parse result type
	  * @return A new parser that returns either success or failure
	  */
	def eitherSuccessOrFailure[F, S](parseSuccess: (Option[InputStream], Headers) => S)
									(parseFailure: (Option[InputStream], Headers) => F) =
		successOrFailure[Either[F, S]] { (stream, headers) => Right(parseSuccess(stream, headers)) } {
			(stream, headers) => Left(parseFailure(stream, headers)) }
	/**
	  * Combines two response parsers, using one in success statuses and other for failure statuses
	  * @param successParser Success response parser
	  * @param failureParser Failure response parser
	  * @tparam F Failure response type
	  * @tparam S Success response type
	  * @return A response parser that contains either success or failure result, based on response status
	  */
	def eitherSuccessOrFailure[F, S](successParser: ResponseParser[S], failureParser: ResponseParser[F]) =
	{
		apply { (stream, headers, status) =>
			if (StatusGroup.failure.contains(status.group))
				Left(failureParser(stream, headers, status))
			else
				Right(successParser(stream, headers, status))
		} { (headers, status) =>
			if (StatusGroup.failure.contains(status.group))
				Left(failureParser(headers, status))
			else
				Right(successParser(headers, status))
		}
	}
	
	private def parseValue(stream: InputStream, headers: Headers, parsers: Iterable[JsonParser])
						  (implicit defaultEncoding: Codec): Try[Value] =
	{
		val encoding = headers.codec.getOrElse(defaultEncoding)
		// On non-json content types, produces a string value (except for xml content type, which is converted to
		// a model)
		if (headers.contentType.forall { _.subType ~== "json" }) {
			// Checks whether a custom parser can be used
			parsers.find { _.defaultEncoding == encoding } match {
				case Some(parser) => parser(stream)
				case None => JsonReader(stream, encoding)
			}
		}
		else if (headers.contentType.exists { _.subType ~== "xml" })
			XmlReader.parseStream(stream, encoding.charSet).map { _.toModel }
		else
			Try { Source.fromInputStream(stream)(encoding).consume { _.getLines().mkString } }
	}
}

/**
  * These parsers are used for processing streamed response content
  * @author Mikko Hilpinen
  * @since 15.5.2020, v1.3
  * @param parseBody Function for parsing response stream body
  * @param parseEmpty Function for processing an empty response
  */
class ResponseParser[+A](parseBody: (InputStream, Headers, Status) => A, parseEmpty: (Headers, Status) => A)
{
	/**
	  * Parses response stream
	  * @param stream Stream to process
	  * @param headers Response headers
	  * @param status Response status
	  * @return Parsed response contents
	  */
	def apply(stream: InputStream, headers: Headers, status: Status) = parseBody(stream, headers, status)
	
	/**
	  * Processes an empty response
	  * @param headers Response headers
	  * @param status Response status
	  * @return Process result
	  */
	def apply(headers: Headers, status: Status) = parseEmpty(headers, status)
}
