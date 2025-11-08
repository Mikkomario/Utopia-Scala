package utopia.nexus.http

import utopia.access.model.enumeration.ContentCategory._
import utopia.access.model.{ContentType, Headers}
import utopia.nexus.model.request.RequestBody

/**
* This type of body holds the whole data in memory
* @author Mikko Hilpinen
* @since 12.5.2018
**/
@deprecated("Replaced with RequestBody", "v2.0")
case class BufferedBody[+A](contents: A, contentType: ContentType = Text.plain, contentLength: Option[Long] = None,
                            headers: Headers = Headers.currentDateHeaders, name: String = "")
	extends Body with RequestBody[A]
{
	override def value: A = contents
	
	override def isEmpty: Boolean = super[Body].isEmpty
	
	/**
	  * Maps the contents of this body
	  * @param f A function for mapping content
	  * @tparam B Type of result content
	  * @return A copy of this body with mapped content
	  */
	override def map[B](f: A => B) = copy(contents = f(contents))
}