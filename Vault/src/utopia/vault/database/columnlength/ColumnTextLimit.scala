package utopia.vault.database.columnlength

import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._

/**
  * Used for limiting column text length
  * @author Mikko Hilpinen
  * @since 21.11.2021, v1.12
  */
trait ColumnTextLimit extends ColumnLengthLimit
{
	/**
	  * @return Maximum text length allowed by this limit / type
	  */
	def maxLength: Int
	
	override def maxValue: Long = maxLength
	
	override def test(input: Value, allowCrop: Boolean = false) =
		input.string match {
			case Some(string) =>
				val length = string.length
				if (length <= maxLength)
					Right(input)
				else if (allowCrop)
					Right(string.take(maxLength))
				else
					Left(ColumnTextLimit.fitting(length))
			case None =>
				if (input.isEmpty)
					Right(input)
				else
					Left(None)
		}
}

object ColumnTextLimit
{
	/**
	  * @param numberOfCharacters Number of characters to fit
	  * @return A column text limit that allows that number of characters.
	  *         None if there wasn't a limit large enough found.
	  */
	def fitting(numberOfCharacters: Int) =
	{
		if (numberOfCharacters < VarcharLimit.maxLength)
			Some(VarcharLimit(numberOfCharacters))
		else
			Vector(TextLimit, MediumTextLimit, LongTextLimit).find { _.maxLength >= numberOfCharacters }
	}
	
	/**
	  * A variable character length limit
	  */
	object VarcharLimit {
		// See: https://stackoverflow.com/questions/332798/equivalent-of-varcharmax-in-mysql
		/**
		  * @return Absolute maximum Varchar length limit (when using utf8 character set)
		  */
		def maxLength = 21844
	}
	case class VarcharLimit(maxLength: Int = 255) extends ColumnTextLimit
	{
		override def sqlType = s"VARCHAR($maxLength)"
	}
	
	// See: https://stackoverflow.com/questions/23646511/what-is-the-max-length-for-char-type-in-mysql
	/**
	  * A limit that allows 0-255 characters
	  */
	case object TinyTextLimit extends ColumnTextLimit
	{
		override def maxLength = 255
		override def sqlType = "TINYTEXT"
	}
	/**
	  * A limit that allows up to 65 535 characters
	  */
	case object TextLimit extends ColumnTextLimit
	{
		override def maxLength = 65535
		override def sqlType = "TEXT"
	}
	/**
	  * A limit that allows up to 16 777 215 characters
	  */
	case object MediumTextLimit extends ColumnTextLimit
	{
		override def maxLength = 16777215
		override def sqlType = "MEDIUMTEXT"
	}
	/**
	  * A limit that allows up to 4 294 967 295 characters
	  */
	case object LongTextLimit extends ColumnTextLimit
	{
		// Long text allows larger strings, but current testing method only supports integers
		override def maxLength = Int.MaxValue
		override def maxValue = 4294967295L
		override def sqlType = "LONGTEXT"
	}
}
