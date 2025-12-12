package utopia.flow.collection.mutable.builder

import utopia.flow.collection.immutable.Empty

import scala.collection.mutable

object BuildNothing
{
	// ATTRIBUTES   ----------------------
	
	/**
	 * Builds nothing. Yields Unit.
	 */
	lazy val unit = new BuildNothing[Unit](())
	/**
	 * Builds nothing. Yields an empty collection.
	 */
	lazy val empty = new BuildNothing[Empty.type](Empty)
	
	
	// COMPUTED -------------------------
	
	/**
	 * Creates a builder that only checks whether input was empty or not
	 * @tparam A Type of the accepted input
	 * @return A builder that yields true if input was non-empty, and false otherwise
	 */
	def nonEmptyFlag[A]: mutable.Builder[A, Boolean] = new BuildNonEmptyFlag()
	
	
	// NESTED   -------------------------
	
	private class BuildNonEmptyFlag extends mutable.Builder[Any, Boolean]
	{
		// ATTRIBUTES   -----------------
		
		private var nonEmpty = false
		
		
		// IMPLEMENTED  -----------------
		
		override def clear(): Unit = nonEmpty = false
		override def result(): Boolean = nonEmpty
		
		override def addOne(elem: Any): BuildNonEmptyFlag.this.type = {
			nonEmpty = true
			this
		}
		override def addAll(elems: IterableOnce[Any]): BuildNonEmptyFlag.this.type = {
			if (!nonEmpty && elems.iterator.hasNext)
				nonEmpty = true
			this
		}
	}
}

/**
 * A builder that ignores the input values. May be used as a placeholder.
 * @author Mikko Hilpinen
 * @since 25.11.2025, v2.8
 */
class BuildNothing[+A](fixedResult: A) extends mutable.Builder[Any, A]
{
	override def clear() = ()
	override def result() = fixedResult
	
	override def addOne(elem: Any) = this
	override def addAll(elems: IterableOnce[Any]): BuildNothing.this.type = this
}
