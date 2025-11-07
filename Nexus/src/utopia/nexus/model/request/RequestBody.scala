package utopia.nexus.model.request

import utopia.access.model.{Headered, Headers}
import utopia.access.model.enumeration.ContentCategory.Text
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.operator.MaybeEmpty
import utopia.flow.parse.json.JsonParser
import utopia.flow.view.immutable.View

import scala.util.{Failure, Success, Try}

object RequestBody
{
	// TYPES    ----------------------
	
	/**
	 * A type alias for a RequestBody in a streamed form
	 */
	type StreamedRequestBody = RequestBody[StreamOrReader]
	
	
	// COMPUTED ----------------------
	
	/**
	 * @return An empty request body
	 */
	def empty: RequestBody[Value] = EmptyRequestBody
	/**
	 * @return An empty streamed request body
	 */
	def emptyStream: StreamedRequestBody = EmptyStreamedRequestBody
	
	
	// OTHER    ----------------------
	
	/**
	 * @param value The wrapped body value / content
	 * @param headers The headers associated with this request or request body part
	 * @param name The name of this part, if applicable
	 * @tparam A Type of the wrapped value
	 * @return A new request body
	 */
	def apply[A](value: A, headers: Headers, name: String = ""): RequestBody[A] =
		_RequestBody(value, headers, name)
	
	
	// NESTED   ----------------------
	
	case object EmptyRequestBody extends EmptyRequestBody[Value](Value.empty)
	class EmptyRequestBody[+A](override val value: A) extends RequestBody[A]
	{
		override val isEmpty: Boolean = true
		override val headers: Headers = Headers.empty.withContentLength(0)
		override val name: String = ""
		
		override def withValue[B](newValue: B): RequestBody[B] = new EmptyRequestBody[B](newValue)
	}
	
	case object EmptyStreamedRequestBody extends EmptyRequestBody[StreamOrReader](StreamOrReader.empty)
	
	private case class _RequestBody[+A](value: A, headers: Headers, name: String) extends RequestBody[A]
}

/**
* This trait represents a request body or a part of a request body (in case of multipart requests)
* @author Mikko Hilpinen
* @since 05.11.2025, v2.0 - Based on an earlier version written 12.5.2018
**/
trait RequestBody[+A] extends View[A] with Headered[RequestBody[A]] with MaybeEmpty[RequestBody[A]]
{
    // ABSTRACT    -------------------
    
	/**
	 * The name of this part, if applicable (empty otherwise)
	 */
	def name: String
	
	
	// COMPUTED PROPERTIES    ---------
	
	/**
	 * Whether this body is empty
	 */
	def isEmpty = headers.contentLength.contains(0) && !headers.isChunked
	
	/**
	 * @return This request body with its contents buffered into a string.
	 *         Failure if buffering failed.
	 */
	def bufferedToString(implicit ev: A <:< StreamOrReader) =
		if (isEmpty) Success(withValue("")) else bufferWith { _.bufferToString }
	/**
	 * @return This request body with its contents buffered into XML.
	 *         Failure if buffering failed.
	 */
	def bufferedToXml(implicit ev: A <:< StreamOrReader) = bufferWith { _.bufferToXml }
	
	/**
	 * Buffers this requests contents into a [[Value]].
	 *
	 * This operation is supported for the following content types:
	 *      - `*`/json => Contents will be parsed as JSON
	 *      - `*`/xml => Contents will be parsed into an [[utopia.flow.parse.xml.XmlElement]],
	 *                   and then converted into a Value.
	 *      - text/`*` => Contents will be parsed into a String and wrapped into a Value
	 *      - Unspecified => Assumes that the contents are JSON
	 *
	 * @param jsonParser JSON parser used for interpreting JSON content, if applicable
	 * @return This request body with its contents buffered into a Value.
	 *         Failure if buffering failed, or if the content type was not supported.
	 */
	def buffered(implicit jsonParser: JsonParser, ev: A <:< StreamOrReader) = {
		if (isEmpty || value.isEmpty.isCertainlyTrue)
			Success(withValue(Value.empty))
		else
			headers.contentType match {
				case Some(contentType) =>
					contentType.subType.toLowerCase match {
						case "json" => bufferWith { _.bufferAsJson }
						case "xml" => bufferWith { _.bufferToXml.map { _.toValue } }
						case _ =>
							if (contentType.category == Text)
								bufferWith { _.bufferToString.map { s => s: Value } }
							else
								Failure(new UnsupportedOperationException(s"Can't buffer $contentType into a Value"))
					}
				case None =>  bufferWith { _.bufferAsJson }
			}
	}
	
	/**
	 * Buffers this requests contents into a [[Value]], assuming JSON content
	 * @param jsonParser JSON parser to use
	 * @return This request body with its contents buffered into a Value. Failure if buffering failed.
	 * @see [[buffered]]
	 */
	def bufferedAsJson(implicit jsonParser: JsonParser, ev: A <:< StreamOrReader) =
		if (isEmpty || value.isEmpty.isCertainlyTrue) Success(withValue(Value.empty)) else bufferWith { _.bufferAsJson }
	
	
	// IMPLEMENTED  --------------------
	
	override def self: RequestBody[A] = this
	
	override def withHeaders(headers: Headers, overwrite: Boolean): RequestBody[A] =
		RequestBody(value, if (overwrite) headers else this.headers ++ headers, name)
	
	override def mapValue[B](f: A => B) = map(f)
	
	
	// OTHER    ------------------------
	
	/**
	 * @param newValue A new value to assign to this request body
	 * @tparam B Type of the new value
	 * @return A copy of this body with the specified value
	 */
	def withValue[B](newValue: B) = RequestBody(newValue, headers, name)
	/**
	 * @param f A mapping function to apply to this body's value
	 * @tparam B Type of the mapped value
	 * @return A mapped copy of this body
	 */
	def map[B](f: A => B) = withValue(f(value))
	/**
	 * @param f A mapping function to apply to this body's value. May yield a failure.
	 * @tparam B Type of successful mapping results
	 * @return A mapped copy of this. A failure if 'f' yielded a failure.
	 */
	def tryMap[B](f: A => Try[B]) = f(value).map(withValue)
	
	/**
	 * Buffers this body's contents using the specified function
	 * @param f A function that buffers the streamed request contents
	 * @tparam B Type of the buffered results, when successful
	 * @return A buffered copy of this request body.
	 *         Failure if parsing / buffering failed.
	 */
	def bufferWith[B](f: StreamOrReader => Try[B])(implicit ev: A <:< StreamOrReader) =
		f(value).map(withValue)
}