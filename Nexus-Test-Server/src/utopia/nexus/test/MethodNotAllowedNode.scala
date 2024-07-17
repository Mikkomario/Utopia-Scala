package utopia.nexus.test

import utopia.access.http.Status.NoContent
import utopia.flow.collection.immutable.Empty
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
	override def allowedMethods = Empty
	
	override def toResponse(remainingPath: Option[Path])(implicit context: Context): Response = Response.empty(NoContent)
}
