package utopia.nexus.rest

import utopia.flow.util.StringExtensions._
import utopia.nexus.http.Path
import utopia.nexus.rest.ResourceSearchResult.{Error, Follow}

/**
  * A common utility trait for Rest resources that have specified child nodes under them
  * @author Mikko Hilpinen
  * @since 8.3.2021, v1.5.1
  */
trait ResourceWithChildren[-C <: Context] extends Resource[C]
{
	// ABSTRACT	-------------------------
	
	/**
	  * @return Child nodes available under this node
	  */
	def children: Iterable[Resource[C]]
	
	
	// IMPLEMENTED	---------------------
	
	override def follow(path: Path)(implicit context: C) = {
		val c = children
		c.find { _.name ~== path.head } match {
			case Some(next) => Follow(next, path.tail)
			case None =>
				Error(message = Some(s"${path.head} is not a child node of $name. Available options: [${
					c.map { _.name }.mkString(", ")}]"))
		}
	}
}
