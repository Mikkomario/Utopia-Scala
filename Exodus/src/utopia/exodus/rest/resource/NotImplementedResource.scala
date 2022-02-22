package utopia.exodus.rest.resource

import utopia.access.http.Status.NotImplemented
import utopia.nexus.http.Path
import utopia.nexus.rest.{Context, Resource}
import utopia.nexus.result.Result

/**
  * A common trait for rest nodes which don't contain any request handling implementation
  * (except perhaps for child nodes)
  * @author Mikko Hilpinen
  * @since 8.12.2020, v1
  */
@deprecated("Please use the copy in Nexus instead", "v4.0")
trait NotImplementedResource[-C <: Context] extends Resource[C]
{
	override def allowedMethods = Vector()
	
	override def toResponse(remainingPath: Option[Path])(implicit context: C) = Result.Failure(NotImplemented,
		s"No implementation for $name exists at this time").toResponse
}
