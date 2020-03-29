package utopia.vault.model.enumeration

/**
  * Operators used when comparing columns and values with each other
  * @author Mikko Hilpinen
  * @since 1.2.2020, v1.4
  */
sealed trait ComparisonOperator
{
	/**
	  * @return An Sql representation of this operator
	  */
	def toSql: String
}

object ComparisonOperator
{
	/**
	  * Returns true when both items are equal. Null safe.
	  */
	case object Equal extends ComparisonOperator
	{
		override def toSql = "<=>"
	}
	/**
	  * Returns true when both items are unequal. Null safe.
	  */
	case object NotEqual extends ComparisonOperator
	{
		override def toSql = "<>"
	}
	/**
	  * Returns true when the first item is larger than the second item
	  */
	case object Larger extends ComparisonOperator
	{
		override def toSql = ">"
	}
	/**
	  * Returns true when the first item is larger than the second item or the items are equal
	  */
	case object LargerOrEqual extends ComparisonOperator
	{
		override def toSql = ">="
	}
	/**
	  * Returns true when the first item is smaller than the second item
	  */
	case object Smaller extends ComparisonOperator
	{
		override def toSql = "<"
	}
	/**
	  * Returns true when the first item is smaller than the second item or the items are equal
	  */
	case object SmallerOrEqual extends ComparisonOperator
	{
		override def toSql = "<="
	}
	/**
	  * Returns true when the first item matches the pattern in the second item
	  */
	case object Like extends ComparisonOperator
	{
		override def toSql = "LIKE"
	}
	/**
	  * Returns true when the first item doesn't match the pattern in the second item
	  */
	case object NotLike extends ComparisonOperator
	{
		override def toSql = "NOT LIKE"
	}
}
