package utopia.disciple.model.request

import org.apache.hc.core5.function.Supplier
import org.apache.hc.core5.http.{Header, HttpEntity}
import utopia.access.model.ContentType
import utopia.disciple.model.request.RequestBody.BodyAsEntity

import java.io.{InputStream, OutputStream}
import java.util
import scala.util.Try

object RequestBody
{
	// NESTED   -------------------------
	
	private class BodyAsEntity(body: RequestBody) extends HttpEntity
	{
		override def isRepeatable: Boolean = body.repeatable
		override def isStreaming: Boolean = body.streaming
		override def isChunked: Boolean = body.chunked
		
		override def getContentLength: Long = body.contentLength.getOrElse(-1L)
		override def getContentType: String = body.contentType.toString
		override def getContentEncoding: String = body.contentEncoding.orNull
		
		override def getContent: InputStream = throw new UnsupportedOperationException("This entity is write only")
		
		override def getTrailerNames: util.Set[String] = new java.util.HashSet[String]()
		override def getTrailers: Supplier[util.List[_ <: Header]] = () => new java.util.ArrayList[Header]()
		
		override def writeTo(outputStream: OutputStream): Unit = body.writeTo(outputStream).get
		
		override def close(): Unit = body.close()
	}
}

/**
* Common trait for request body implementations
* @author Mikko Hilpinen
* @since 19.5.2026, based on Body implementation written in 1.5.2018
**/
trait RequestBody extends HttpEntityConvertible with AutoCloseable
{
    // ABSTRACT    -------------------------
    
    /**
     * Whether this body can repeat its contents multiple times.
     * I.e. whether [[writeTo]] may be called multiple times.
     */
	def repeatable: Boolean
	/**
	 * @return Whether this body has not been 100% buffered into memory,
	 *         and instead depends on an input stream of some kind.
	 */
	def streaming: Boolean
	
	/**
	 * The content type of this body
	 */
	def contentType: ContentType
	/**
	 * The content encoding used, if applicable
	 */
	def contentEncoding: Option[String]
	/**
	 * The length of the body content in bytes, if applicable.
	 * None if this body should be "chunked".
	 */
	def contentLength: Option[Long]
	
	/**
	 * Writes the contents of this body into an output stream
	 * @param output An output stream
	 * @return A success or a failure
	 */
	def writeTo(output: OutputStream): Try[Unit]
	
	
	// COMPUTED -----------------------------
	
	/**
	 * Whether this body has unspecified content length, and is therefore delivered in "chunks"
	 */
	def chunked: Boolean = contentLength.isEmpty
	
	
	// IMPLEMENTED  -------------------------
	
	override def toHttpEntity: HttpEntity = new BodyAsEntity(this)
}
