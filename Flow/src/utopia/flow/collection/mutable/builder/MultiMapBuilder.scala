package utopia.flow.collection.mutable.builder

import scala.collection.immutable.VectorBuilder
import scala.collection.mutable

/**
  * Used for building maps which contain multiple values per key. Mutable.
  * @author Mikko Hilpinen
  * @since 2.11.2021, v1.14
  */
class MultiMapBuilder[K, V] extends mutable.Builder[(K, V), Map[K, Vector[V]]]
{
	// ATTRIBUTES   -------------------------------
	
	private val builders = mutable.HashMap[K, VectorBuilder[V]]()
	
	
	// IMPLEMENTED  -------------------------------
	
	override def addOne(elem: (K, V)) =
	{
		builders.getOrElseUpdate(elem._1, new VectorBuilder[V]()) += elem._2
		this
	}
	
	override def clear() = builders.clear()
	
	override def result() = builders.view.mapValues { _.result() }.toMap
}
