package utopia.exodus.rest.resource

import utopia.nexus.http.Path
import utopia.nexus.rest.ResourceSearchResult.Error
import utopia.nexus.rest.{Context, Resource}

/**
  * A common trait for resource rest node implementations which don't have any child nodes
  * @author Mikko Hilpinen
  * @since 8.12.2020, v1
  */
trait ResourceWithoutChildren[-C <: Context] extends Resource[C]
{
	override def follow(path: Path)(implicit context: C) = Error(
		message = Some(s"$name doesn't have any child nodes"))
}
