package utopia.nexus.http

import utopia.access.http.ContentCategory._
import utopia.access.http.ContentType
import utopia.access.http.Headers

/**
* This type of body holds the whole data in memory
* @author Mikko Hilpinen
* @since 12.5.2018
**/
case class BufferedBody[+A](contents: A, contentType: ContentType = Text.plain,
        contentLength: Option[Long] = None, headers: Headers = Headers.currentDateHeaders,
        name: Option[String] = None) extends Body
{
	/**
	  * Maps the contents of this body
	  * @param f A function for mapping content
	  * @tparam B Type of result content
	  * @return A copy of this body with mapped content
	  */
	def map[B](f: A => B) = copy(contents = f(contents))
}