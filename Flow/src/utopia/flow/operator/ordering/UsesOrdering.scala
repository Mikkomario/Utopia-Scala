package utopia.flow.operator.ordering

/**
  * Common trait for items which are compared to each other using separate ordering instances.
  * This is useful in cases where there are multiple ways to order / compare the items.
  * In cases where there is only one way to compare the items, please use [[SelfComparable]] instead
  *
  * @tparam A Type of items compared against this item
  *
  * @author Mikko Hilpinen
  * @since 17.4.2023, v2.1
  */
trait UsesOrdering[A]
{
	// ABSTRACT ------------------------
	
	/**
	  * @param other Another item
	  * @param ord Ordering to use
	  * @return Whether this item is smaller than the other item, when the specified ordering is used
	  */
	def <(other: A)(implicit ord: Ordering[A]): Boolean
	/**
	  * @param other Another item
	  * @param ord   Ordering to use
	  * @return Whether this item is larger than the other item, when the specified ordering is used
	  */
	def >(other: A)(implicit ord: Ordering[A]): Boolean
	
	
	// OTHER    ------------------------
	
	/**
	  * @param other Another item
	  * @param ord   Ordering to use
	  * @return Whether this item is smaller than or equal to the other item, when the specified ordering is used
	  */
	def <=(other: A)(implicit ord: Ordering[A]) = !(this > other)
	/**
	  * @param other Another item
	  * @param ord   Ordering to use
	  * @return Whether this item is larger than or equal to the other item, when the specified ordering is used
	  */
	def >=(other: A)(implicit ord: Ordering[A]) = !(this < other)
}

/**
  * Common trait for items that compare against their own kind, using various implicit orderings
  * @tparam A Type of this item
  */
trait UsesSelfOrdering[A] extends UsesOrdering[A]
{
	// ABSTRACT --------------------
	
	/**
	  * @return This instance
	  */
	def self: A
	
	
	// IMPLEMENTED  ----------------
	
	override def <(other: A)(implicit ord: Ordering[A]): Boolean = ord.lt(self, other)
	override def >(other: A)(implicit ord: Ordering[A]): Boolean = ord.gt(self, other)
	
	
	// OTHER    -------------------
	
	/**
	  * @param other Another item
	  * @param ord Implicit ordering to use
	  * @return The smaller of these two items, according to the specified ordering
	  */
	def min(other: A)(implicit ord: Ordering[A]) = if (ord.gt(self, other)) other else self
	/**
	  * @param other Another item
	  * @param ord   Implicit ordering to use
	  * @return The larger of these two items, according to the specified ordering
	  */
	def max(other: A)(implicit ord: Ordering[A]) = if (ord.lt(self, other)) other else self
}