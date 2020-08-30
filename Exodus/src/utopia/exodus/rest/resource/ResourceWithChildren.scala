package utopia.exodus.rest.resource

import utopia.flow.util.StringExtensions._
import utopia.nexus.http.Path
import utopia.nexus.rest.ResourceSearchResult.{Error, Follow}
import utopia.nexus.rest.{Context, Resource}

/**
  * A common trait for simple resources which have a static list of children under them (no id parsing etc.)
  * @author Mikko Hilpinen
  * @since 6.5.2020, v1
  */
trait ResourceWithChildren[C <: Context] extends Resource[C]
{
	// ABSTRACT	--------------------------
	
	/**
	  * @return Child resources under this resource
	  */
	def children: Vector[Resource[C]]
	
	
	// IMPLEMENTED	-----------------------
	
	override def follow(path: Path)(implicit context: C) =
		children.find { _.name ~== path.head }.map { Follow(_, path.tail) }.getOrElse(
			Error(message = Some(s"$name only contains following child nodes: [${children.map { _.name }.mkString(", ")}]")))
}
