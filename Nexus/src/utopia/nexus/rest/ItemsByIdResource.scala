package utopia.nexus.rest

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.nexus.http.Path
import utopia.nexus.rest.ResourceSearchResult.{Error, Follow}

/**
  * A common trait for resources that provide access to other resources based on an item id.
  * For example, a resource 'items' would provide access to resources 'items/1', 'items/2' etc,
  * depending on the type of id in question (in this example, integer)
  * @author Mikko Hilpinen
  * @since 13.10.2022, v1.9
  */
trait ItemsByIdResource[-C <: Context] extends Resource[C]
{
	// ABSTRACT -------------------------
	
	/**
	  * @param id A resource id in value format. Parsed from a path parameter.
	  * @return A resource accessible with that id. None if the id was invalid (i.e. not of correct type or format).
	  */
	protected def resourceForId(id: Value): Option[Resource[C]]
	
	
	// IMPLEMENTED  ---------------------
	
	override def follow(path: Path)(implicit context: C) =
		resourceForId(path.head) match {
			case Some(next) => Follow(next, path.tail)
			case None => Error(message = s"${ path.head } is not a valid id")
		}
}
