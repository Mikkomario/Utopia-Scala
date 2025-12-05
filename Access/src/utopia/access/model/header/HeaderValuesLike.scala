package utopia.access.model.header

import utopia.flow.util.Mutate

/**
 * Common trait for collection-based header values
 * @author Mikko Hilpinen
 * @since 04.12.2025, v1.7
 */
trait HeaderValuesLike[A, +Repr] extends HeaderValue[Seq[A]]
{
	// ABSTRACT --------------------------
	
	def filter(f: A => Boolean): Repr
	
	def appended(item: A): Repr
	def appendedAll(items: IterableOnce[A]): Repr
	
	def mapEach(f: Mutate[A]): Repr
	
	
	// OTHER    --------------------------
	
	def filterNot(f: A => Boolean) = filter { !f(_) }
}
