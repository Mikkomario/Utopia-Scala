package utopia.nexus.rest
import utopia.nexus.http.Path
import utopia.nexus.rest.ResourceSearchResult.Error

/**
  * A common utility trait for Rest nodes that don't have any child nodes
  * @author Mikko Hilpinen
  * @since 8.3.2021, v1.5.1
  */
trait LeafResource[-C <: Context] extends Resource[C]
{
	override def follow(path: Path)(implicit context: C) =
		Error(message = Some(s"$name doesn't contain any child nodes"))
}
