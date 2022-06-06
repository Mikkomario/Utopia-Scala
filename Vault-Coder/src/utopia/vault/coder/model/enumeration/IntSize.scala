package utopia.vault.coder.model.enumeration

/**
  * An enumeration for different available integer sizes
  * @author Mikko Hilpinen
  * @since 9.2.2022, v1.5
  */
sealed trait IntSize
{
	// ABSTRACT ----------------------------
	
	/**
	  * @return The largest allowed value for this number size
	  */
	def maxValue: Int
	
	/**
	  * @return SQL version of this data type
	  */
	def toSql: String
	
	
	// COMPUTED ----------------------------
	
	/**
	  * @return Largest allowed length for this number size
	  */
	def maxLength = maxValue.toString.length
}

object IntSize
{
	// ATTRIBUTES   -----------------------
	
	/**
	  * All available integer size options
	  */
	val values = Vector(Tiny, Small, Medium, Default)
	
	
	// NESTED   ---------------------------
	
	case object Tiny extends IntSize
	{
		override def maxValue = 127
		
		override def toSql = "TINYINT"
	}
	case object Small extends IntSize
	{
		override def maxValue = 32767
		
		override def toSql = "SMALLINT"
	}
	case object Medium extends IntSize
	{
		override def maxValue = 8388607
		
		override def toSql = "MEDIUMINT"
	}
	case object Default extends IntSize
	{
		override def maxValue = Int.MaxValue
		
		override def toSql = "INT"
	}
}
