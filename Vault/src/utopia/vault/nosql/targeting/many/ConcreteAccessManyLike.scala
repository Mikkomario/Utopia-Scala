package utopia.vault.nosql.targeting.many

import utopia.flow.operator.enumeration.End
import utopia.flow.operator.enumeration.End.{First, Last}
import utopia.vault.nosql.targeting.grouped.ConcreteAccessGroupedLike
import utopia.vault.nosql.targeting.one.TargetingOne
import utopia.vault.sql.{Condition, OrderBy}

/**
  * Common trait for concrete AccessManyLike implementations (i.e. non-wrappers)
  * @author Mikko Hilpinen
  * @since 24.06.2025, v1.21.1
  */
trait ConcreteAccessManyLike[+A, +Repr <: TargetingManyLike[A, Repr, _]]
	extends ConcreteAccessGroupedLike[Seq[A], Repr] with AccessManyLike[A, Repr]
{
	// ABSTRACT --------------------------
	
	/**
	  * @return Copy of this access point that's limited to one row / item, if applicable.
	  *         Access points which don't support limits should yield themselves.
	  */
	protected def limitedToOne: Repr
	
	
	// IMPLEMENTED  ----------------------
	
	override def apply(end: End, ordering: Option[OrderBy], filter: Option[Condition]) = {
		// Applies the correct ordering
		val access = {
			if (ordering.isEmpty && (end == First || this.ordering.isEmpty))
				limitedToOne
			else
				ordering match {
					case Some(ordering) =>
						limitedToOne.withOrdering(end match {
							case First => ordering
							case Last => -ordering
						})
					case None =>
						end match {
							case First => limitedToOne
							case Last =>
								ordering match {
									case Some(ordering) => limitedToOne.withOrdering(-ordering)
									case None => limitedToOne
								}
						}
				}
		}
		// Creates a view to the first element in this access point
		TargetingOne.headOf[Repr, A](access.filter(filter))
	}
}
