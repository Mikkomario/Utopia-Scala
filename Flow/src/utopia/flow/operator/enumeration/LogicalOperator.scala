package utopia.flow.operator.enumeration

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.Reversible

/**
  * Represents a symmetric logical operator that combines two (boolean) values
  * @author Mikko Hilpinen
  * @since 14.11.2023, v2.3
  */
// NB: Incomplete implementation
sealed trait LogicalOperator extends Reversible[LogicalOperator]
{
	// ABSTRACT -----------------------
	
	/**
	  * @return The logical opposite of this operator
	  */
	def unary_! : LogicalOperator
	
	/**
	  * Applies this operator to two boolean values
	  * @param a The first value
	  * @param b The second value (call-by-name, only called if logically necessary)
	  * @return Logical result of the two values
	  */
	def apply(a: Boolean, b: => Boolean): Boolean
	
	
	// IMPLEMENTED  ------------------
	
	override def self: LogicalOperator = this
}

object LogicalOperator
{
	// ATTRIBUTES   --------------------
	
	val values = PositiveLogicalOperator.values ++ NegativeLogicalOperator.values
	
	
	// NESTED   ------------------------
	
	object PositiveLogicalOperator
	{
		val values = Pair(And, Or)
	}
	sealed trait PositiveLogicalOperator extends LogicalOperator
	{
		override def unary_! : NegativeLogicalOperator
		
		override def unary_- = !this
	}
	object NegativeLogicalOperator
	{
		val values = Pair(NotBoth, Nor)
	}
	sealed trait NegativeLogicalOperator extends LogicalOperator
	{
		override def unary_! : PositiveLogicalOperator
		
		override def unary_- : PositiveLogicalOperator = !this
	}
	/*
	sealed trait LogicalEqualityOperator extends LogicalOperator
	{
		override def unary_! : LogicalEqualityOperator
		
		override def unary_- = !this
	}*/
	
	
	// VALUES   -----------------------
	
	case object And extends PositiveLogicalOperator
	{
		// IMPLEMENTED  -------------------
		
		override def unary_! = NotBoth
		
		override def apply(a: Boolean, b: => Boolean) = a && b
		
		
		// OTHER    ----------------------
		
		def similar[A](l: Option[A], r: => Option[A]) = l.flatMap { l => r.map { Pair(l, _) } }
		def different[L, R](l: Option[L], r: => Option[R]) = l.flatMap { l => r.map { (l, _) } }
	}
	case object Or extends PositiveLogicalOperator
	{
		// IMPLEMENTED  -----------------
		
		override def unary_! = Nor
		
		override def apply(a: Boolean, b: => Boolean) = a || b
		
		
		// OTHER    --------------------
		
		def similar[A](l: Option[A], r: => Option[A]) = l.orElse(r)
		def different[L, R](l: Option[L], r: => Option[R]) = l match {
			case Some(l) => Some(Left(l))
			case None => r.map { Right(_) }
		}
	}
	case object NotBoth extends NegativeLogicalOperator
	{
		// IMPLEMENTED  -----------------
		
		override def unary_! = And
		
		override def apply(a: Boolean, b: => Boolean) = !a || !b
		
		
		// OTHER    --------------------
		
		def similar[A](l: Option[A], r: Option[A]) = l match {
			case Some(l) => if (r.isEmpty) Some(l) else None
			case None => r
		}
		def different[L, R](l: Option[L], r: => Option[R]) = l match {
			case Some(l) => if (r.isEmpty) Some(Left(l)) else None
			case None => r.map { Right(_) }
		}
	}
	case object Nor extends NegativeLogicalOperator
	{
		// IMPLEMENTED  -----------------
		
		override def unary_! = Or
		
		override def apply(a: Boolean, b: => Boolean) = !a && !b
	}
	/*
	case object DifferentFrom extends LogicalEqualityOperator
	{
		override def unary_! = SameAs
		
		override def apply(a: Boolean, b: => Boolean) = a != b
	}
	case object SameAs extends LogicalEqualityOperator
	{
		override def unary_! = DifferentFrom
		
		override def apply(a: Boolean, b: => Boolean) = a == b
	}
	 */
}