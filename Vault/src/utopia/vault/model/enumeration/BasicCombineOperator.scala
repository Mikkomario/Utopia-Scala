package utopia.vault.model.enumeration

/**
  * Represents an operator that can combine two logical conditions together
  * @author Mikko Hilpinen
  * @since 1.2.2020, v1.4
  */
sealed trait BasicCombineOperator
{
	/**
	  * @return An sql representation of this operator
	  */
	def toSql: String
}

object BasicCombineOperator
{
	/**
	  * Returns true only when all conditions return true
	  */
	case object And extends BasicCombineOperator
	{
		override def toSql = "AND"
	}
	/**
	  * Returns true when any condition returns true
	  */
	case object Or extends BasicCombineOperator
	{
		override def toSql = "OR"
	}
}
