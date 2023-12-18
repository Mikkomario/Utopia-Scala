package utopia.flow.collection.immutable

import scala.collection.AbstractIndexedSeqView

/**
  * An empty indexed seq view implementation
  * @author Mikko Hilpinen
  * @since 18.12.2023, v2.3
  */
class EmptyIndexedSeqView[+A] extends AbstractIndexedSeqView[A]
{
	override def length: Int = 0
	override def apply(i: Int): A = throw new IndexOutOfBoundsException(s"Index $i is out of bounds (empty view)")
}
