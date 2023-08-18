package utopia.flow.operator

import utopia.flow.operator.ComparisonOperator.Equality
import utopia.flow.operator.Sign.{Negative, Positive}
import utopia.flow.operator.SignOrZero.Neutral

/**
  * An enumeration for different types of comparison operators: ==, <, >, <= and >=
  * @author Mikko Hilpinen
  * @since 18.8.2023, v2.2
  */
sealed trait ComparisonOperator extends Reversible[ComparisonOperator]
{
	// ABSTRACT ------------------------
	
	/**
	  * @return Negated copy of this condition
	  */
	def unary_! : ComparisonOperator
	
	/**
	  * @param a The first value
	  * @param b The second value
	  * @param ord Implicit ordering
	  * @tparam A Type of the two values
	  * @return Whether these two values (in this order) satisfy this function / condition
	  */
	def apply[A](a: A, b: A)(implicit ord: Ordering[A]): Boolean
	
	/**
	  * @param other Another comparison operator
	  * @return An operator that returns true if either of these conditions is satisfied
	  */
	def ||(other: ComparisonOperator): ComparisonOperator
	/**
	  * @param other Another comparison operator
	  * @return An operator that only returns true if both of these conditions are satisfied
	  */
	def &&(other: ComparisonOperator): ComparisonOperator
	
	
	// COMPUTED -------------------------
	
	/**
	  * @return Copy of this condition that also returns true in case of equal items
	  */
	def orEqual = this || Equality
	
	
	// IMPLEMENTED  --------------------
	
	override def self: ComparisonOperator = this
	
	
	// OTHER    ------------------------
	
	/**
	  * @param other Another condition
	  * @return Copy of this condition which also requires that the specified condition must NOT be met
	  */
	def butNot(other: ComparisonOperator) = &&(!other)
}

object ComparisonOperator
{
	// ATTRIBUTE ------------------------
	
	/**
	  * An operator that returns true in cases where the first item is smaller than the second item
	  */
	lazy val smallerThan = DirectionalComparison(Negative)
	/**
	  * An operator that returns true in cases where the first item is larger than the second item
	  */
	lazy val largerThan = DirectionalComparison(Positive)
	/**
	  * An operator that returns true in cases where the first item is smaller than or equal to the second item
	  */
	lazy val smallerOrEqual = smallerThan.orEqual
	/**
	  * An operator that returns true in cases where the first item is larger than or equal to the second item
	  */
	lazy val largerOrEqual = largerThan.orEqual
	
	
	// NESTED   ------------------------
	
	/**
	  * An operator that returns true for equal items only
	  */
	case object Equality extends ComparisonOperator
	{
		override def unary_! : ComparisonOperator = Inequality
		override def unary_- : ComparisonOperator = this
		
		override def apply[A](a: A, b: A)(implicit ord: Ordering[A]): Boolean = ord.equiv(a, b)
		
		override def ||(other: ComparisonOperator): ComparisonOperator = other match {
			case Equality | Never => this
			case Always | Inequality => Always
			case d: DirectionalComparison => if (d.includesEqual) d else d.copy(includesEqual = true)
		}
		override def &&(other: ComparisonOperator): ComparisonOperator = other match {
			case Equality | Always => this
			case Inequality | Never => Never
			case d: DirectionalComparison => if (d.includesEqual) this else Never
		}
	}
	/**
	  * An operator that returns true for unequal items only
	  */
	case object Inequality extends ComparisonOperator
	{
		override def unary_! : ComparisonOperator = Equality
		override def unary_- : ComparisonOperator = this
		
		override def apply[A](a: A, b: A)(implicit ord: Ordering[A]): Boolean = !ord.equiv(a, b)
		
		override def ||(other: ComparisonOperator): ComparisonOperator = other match {
			case Inequality | Never => this
			case Always | Equality => Always
			case d: DirectionalComparison => if (d.includesEqual) Always else this
		}
		override def &&(other: ComparisonOperator): ComparisonOperator = other match {
			case Inequality | Always => this
			case Equality | Never => Never
			case d: DirectionalComparison => if (d.includesEqual) d.copy(includesEqual = false) else d
		}
	}
	/**
	  * An operator that represents smaller than, larger than, or some other variant of these
	  * @param requiredDirection Allowed direction of increase (Positive) or decrease (Negative)
	  * @param includesEqual Whether equal values should be included
	  */
	case class DirectionalComparison(requiredDirection: Sign, includesEqual: Boolean = false) extends ComparisonOperator
	{
		// IMPLEMENTED  --------------------
		
		override def unary_! : ComparisonOperator = DirectionalComparison(-requiredDirection, !includesEqual)
		override def unary_- : ComparisonOperator = copy(requiredDirection = -requiredDirection)
		
		override def apply[A](a: A, b: A)(implicit ord: Ordering[A]): Boolean = Sign.of(ord.compare(a, b)) match {
			case Neutral => includesEqual
			case s: Sign => s == requiredDirection
		}
		
		override def ||(other: ComparisonOperator): ComparisonOperator = other match {
			case Always => Always
			case Never => this
			case Equality => if (includesEqual) this else copy(includesEqual = true)
			case Inequality => if (includesEqual) Always else Inequality
			case DirectionalComparison(dir, eq) =>
				if (dir == requiredDirection)
					copy(includesEqual = includesEqual || eq)
				else if (includesEqual || eq)
					Always
				else
					Inequality
		}
		override def &&(other: ComparisonOperator): ComparisonOperator = other match {
			case Always => this
			case Never => Never
			case Equality => if (includesEqual) Equality else Never
			case Inequality => if (includesEqual) copy(includesEqual = false) else this
			case DirectionalComparison(dir, eq) =>
				if (dir == requiredDirection)
					copy(includesEqual = includesEqual && eq)
				else if (includesEqual && eq)
					Equality
				else
					Never
		}
	}
	/**
	  * An operator that always returns true, regardless of input
	  */
	case object Always extends ComparisonOperator
	{
		override def unary_! : ComparisonOperator = Never
		override def unary_- : ComparisonOperator = this
		
		override def apply[A](a: A, b: A)(implicit ord: Ordering[A]): Boolean = true
		
		override def ||(other: ComparisonOperator): ComparisonOperator = this
		override def &&(other: ComparisonOperator): ComparisonOperator = other
	}
	/**
	  * An operator that always returns false, regardless of input
	  */
	case object Never extends ComparisonOperator
	{
		override def unary_! : ComparisonOperator = Always
		override def unary_- : ComparisonOperator = this
		
		override def apply[A](a: A, b: A)(implicit ord: Ordering[A]): Boolean = false
		
		override def ||(other: ComparisonOperator): ComparisonOperator = other
		override def &&(other: ComparisonOperator): ComparisonOperator = this
	}
}