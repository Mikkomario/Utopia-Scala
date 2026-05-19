package utopia.disciple.model.request

import org.apache.hc.core5.http.HttpEntity

object HttpEntityConvertible
{
	// NESTED   ---------------------------
	
	implicit class HttpEntityIsHttpEntity(val entity: HttpEntity) extends AnyVal with HttpEntityConvertible
	{
		override def toHttpEntity: HttpEntity = entity
		
		override def toString: String = entity.toString
	}
}

/**
 * Common trait for classes that be converted to HTTP entities that may be passed as request bodies.
 * @author Mikko Hilpinen
 * @since 19.05.2026, v1.9.3
 */
trait HttpEntityConvertible extends Any
{
	/**
	 * @return An HTTP Entity representation of this instance
	 */
	def toHttpEntity: HttpEntity
}
