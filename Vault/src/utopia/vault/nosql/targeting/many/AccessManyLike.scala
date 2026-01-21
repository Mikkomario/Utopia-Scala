package utopia.vault.nosql.targeting.many

import utopia.flow.collection.immutable.Empty
import utopia.vault.nosql.targeting.grouped.AccessGroupedLike
import utopia.vault.nosql.targeting.one.TargetingOne

/**
  * An interface used for accessing multiple items with each search query
  * @author Mikko Hilpinen
  * @since 16.05.2025, v1.21
  */
trait AccessManyLike[+A, +Repr]
	extends AccessGroupedLike[Seq[A], Repr] with TargetingManyLike[A, Repr, TargetingOne[Option[A]]]
{
	// IMPLEMENTED  ------------------
	
	override protected def emptyResult: Seq[A] = Empty
}
