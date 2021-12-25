package utopia.flow.datastructure.immutable

import utopia.flow.datastructure.template.MapLike

/**
  * A map where keys are paths which may contain one or more items
  * @author Mikko Hilpinen
  * @since 25.12.2021, v1.14.1
  */
case class DeepMap[K, V](private val wrapped: Map[K, Either[DeepMap[K, V], V]]) extends MapLike[Iterable[K], V]
{
	// IMPLEMENTED  ---------------------------
	
	override def apply(path: Iterable[K]) =
		get(path).getOrElse { throw new NoSuchElementException(s"No value for [${path.mkString(", ")}]") }
	
	
	// OTHER    -------------------------------
	
	/**
	  * @param path A path to look up
	  * @return Value found with that path
	  */
	def get(path: Iterable[K]) = _apply(path.iterator)
	
	/* TODO: Continue
	def +(pair: (Iterable[K], V)) = {
	
	}*/
	
	private def _apply(iter: Iterator[K]): Option[V] = {
		iter.nextOption().flatMap(wrapped.get).flatMap {
			case Right(v) => Some(v)
			// Delegates the search to the nested map, if necessary
			case Left(map) => map._apply(iter)
		}
	}
}
