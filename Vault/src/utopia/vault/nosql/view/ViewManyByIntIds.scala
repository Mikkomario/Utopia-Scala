package utopia.vault.nosql.view

import utopia.flow.collection.immutable.IntSet
import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.template.Indexed

/**
  * Common trait for view factories that provide views to multiple items at once
  * and support integer index -based targeting
  * @tparam V Type of the views constructed
  * @author Mikko Hilpinen
  * @since 30.07.2024, v1.20
  */
trait ViewManyByIntIds[+V] extends ViewFactory[V] with Indexed
{
	/**
	  * @param ids Ids of the targeted items
	  * @return Access to those items
	  */
	def apply(ids: IterableOnce[Int]): V = {
		val condition = ids match {
			case s: IntSet => index.in(s)
			case i: Iterable[Int] =>
				val kn = ids.knownSize
				// Case: Short collection => Won't bother converting to an int set
				if (kn >= 0 && kn <= 6)
					index.in(i)
				// Case: Longer collection => Converts targeted ids to an int set
				else
					index.in(IntSet.from(i))
			case i => index.in(IntSet.from(i))
		}
		apply(condition)
	}
}
