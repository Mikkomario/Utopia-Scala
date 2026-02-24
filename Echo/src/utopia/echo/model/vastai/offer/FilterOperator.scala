package utopia.echo.model.vastai.offer

import utopia.flow.operator.sign.Sign
import utopia.flow.operator.sign.Sign.{Negative, Positive}

import scala.language.implicitConversions

/**
 * An enumeration for different filter operators supported by Vast AI
 * @author Mikko Hilpinen
 * @since 24.02.2026, v1.4.1
 */
sealed trait FilterOperator
{
	/**
	 * @return Name of this operator, as it appears in the JSON body
	 */
	def name: String
	
	/**
	 * @return A negated version of this filter
	 */
	def unary_! : FilterOperator
}

object FilterOperator
{
	// VALUES   --------------------------
	
	/**
	 * An operator that accepts values equal to some other value
	 */
	case object EqualTo extends FilterOperator
	{
		override val name: String = "eq"
		
		override def unary_! : FilterOperator = NotEqualTo
	}
	/**
	 * An operator that accepts values not equal with some other value
	 */
	case object NotEqualTo extends FilterOperator
	{
		override val name: String = "neg"
		
		override def unary_! : FilterOperator = EqualTo
	}
	
	/**
	 * An operator that accepts values less or greater than some value
	 * @param acceptedDirection Whether to accept smaller (Negative) or larger (Positive) values
	 * @param isInclusive Whether to include / accept the compared value
	 */
	sealed case class LinearComparison(acceptedDirection: Sign, isInclusive: Boolean = false) extends FilterOperator
	{
		// COMPUTED ----------------------------
		
		/**
		 * @return Copy of this comparison, also accepting equal values
		 */
		def orEqual = if (isInclusive) this else copy(isInclusive = true)
		
		
		// IMPLEMENTED  ------------------------
		
		override def name: String = {
			val initial = acceptedDirection match {
				case Positive => 'g'
				case Negative => 'l'
			}
			val suffix = if (isInclusive) "e" else ""
			s"${initial}t$suffix"
		}
		
		override def unary_! : FilterOperator = LinearComparison(-acceptedDirection, !isInclusive)
	}
	/**
	 * An operator that accepts values greater than some other value
	 */
	object GreaterThan extends LinearComparison(Positive)
	/**
	 * An operator that accepts values smaller than some other value
	 */
	object LessThan extends LinearComparison(Negative)
	
	/**
	 * An operator that accepts values that match one of the specified options
	 */
	case object In extends FilterOperator
	{
		override val name: String = "in"
		
		override def unary_! : FilterOperator = NotIn
	}
	/**
	 * An operator that accepts values that match none of the specified options
	 */
	case object NotIn extends FilterOperator
	{
		override val name: String = "notin"
		
		override def unary_! : FilterOperator = In
	}
}