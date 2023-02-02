package utopia.flow.operator

/**
  * A common trait for binary enumerations, such as -/+, min/max, first/last and so on.
  * @author Mikko Hilpinen
  * @since 1.2.2023, v2.0
  */
trait Binary[Repr] extends SelfComparable[Repr] with Reversible[Repr]
{
	// COMPUTED -------------------------
	
	/**
	  * @return The opposite of this item. Alias for -this.
	  */
	def opposite = -this
}
