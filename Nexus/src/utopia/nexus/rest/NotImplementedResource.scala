package utopia.nexus.rest

import utopia.access.http.Method
import utopia.access.http.Status.NotImplemented
import utopia.flow.collection.immutable.Empty
import utopia.nexus.http.Path
import utopia.nexus.result.Result

/**
  * A common trait for rest nodes which don't contain any request handling implementation (except for child nodes)
  * @author Mikko Hilpinen
  * @since 18.2.2022, v1.7
  */
trait NotImplementedResource[-C <: Context] extends Resource[C]
{
	override def allowedMethods: Seq[Method] = Empty
	
	override def toResponse(remainingPath: Option[Path])(implicit context: C) =
		Result.Failure(NotImplemented, s"No implementation for $name exists at this time").toResponse
}
