package utopia.flow.operator

/**
  * A common trait for instances which can be reversed (support the unary - -operator)
  * @author Mikko Hilpinen
  * @since 20.9.2021, v1.12
  */
trait Reversible[+Repr] extends Any
{
	/**
	  * @return A reversed copy of this item
	  */
	def unary_- : Repr
}
