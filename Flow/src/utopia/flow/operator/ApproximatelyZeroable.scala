package utopia.flow.operator

/**
  * A common trait for items which may be zero and which support approximate comparisons
  * @author Mikko Hilpinen
  * @since 8.8.2022, v1.16
  * @tparam A Type of item to which this item may be compared
  * @tparam Repr actual representation type of this item
  */
trait ApproximatelyZeroable[-A, +Repr] extends Any with ApproximatelyEquatable[A] with Zeroable[Repr]
{
	/**
	  * @return Whether this item is very close to zero
	  */
	def isAboutZero: Boolean
	/**
	  * @return Whether this item is not very close to zero
	  */
	def isNotAboutZero = !isAboutZero
}
