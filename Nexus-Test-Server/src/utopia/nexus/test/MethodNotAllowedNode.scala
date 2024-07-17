package utopia.nexus.test

import utopia.access.http.Method.Post
import utopia.access.http.Status.NoContent
import utopia.flow.collection.immutable.Single
import utopia.nexus.http.{Path, Response}
import utopia.nexus.rest.{Context, LeafResource}

/**
  * A simple test resource that doesn't allow any methods
  * @author Mikko Hilpinen
  * @since 17.07.2024, v1.0
  */
object MethodNotAllowedNode extends LeafResource[Context]
{
	override def name: String = "unallowed"
	override def allowedMethods = Single(Post)
	
	override def toResponse(remainingPath: Option[Path])(implicit context: Context): Response =
		Response.empty(NoContent)
}
