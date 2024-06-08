package utopia.access.http.error

import utopia.flow.collection.CollectionExtensions._
import utopia.access.http.ContentType
import utopia.flow.collection.immutable.Empty

object ContentTypeException
{
	/**
	  * Creates a new exception
	  * @param proposed Proposed content type
	  * @param allowed Content types allowed (default = empty)
	  * @return A new exception
	  */
	def notAccepted(proposed: ContentType, allowed: Seq[ContentType] = Empty) = {
		val allowedString = {
			if (allowed.isEmpty)
				""
			else if (allowed hasSize 1)
				s", please use ${allowed.head}"
			else
				s", accepted types: [${allowed.mkString(", ")}]"
		}
		new ContentTypeException(s"Content type $proposed is not accepted$allowedString")
	}
}

/**
  * Exceptions thrown when there are problems regarding request or response content types
  * @author Mikko Hilpinen
  * @since 1.12.2020, v1.2.2
  */
class ContentTypeException(message: String) extends Exception(message)
