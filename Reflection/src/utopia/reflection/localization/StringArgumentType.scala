package utopia.reflection.localization

import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.ValueConvertible

object StringArgumentType
{
	private case object StringType extends StringArgumentType
	{
		val identifier = 's'
		def parse(arg: Any) = arg.toString
	}
	private case object UpperCaseStringType extends StringArgumentType
	{
		val identifier = 'S'
		def parse(arg: Any) = arg.toString.toUpperCase()
	}
	private case object IntegerType extends StringArgumentType
	{
		val identifier = 'i'
		def parse(arg: Any) = arg match
		{
			case i: Int => i.toString
			case d: Double => d.toInt.toString
			case l: Long => l.toString
			case i: Integer => i.toString
			case v: Value => valueToString(v)
			case v: ValueConvertible => valueToString(v.toValue)
			case a => valueToString(a.toString.toValue)
		}
		
		private def valueToString(v: Value) = v.int.map { _.toString } getOrElse ""
	}
	private case object DecimalType extends StringArgumentType
	{
		val identifier = 'd'
		def parse(arg: Any) = arg match
		{
			case d: Double => doubleToString(d)
			case i: Int => doubleToString(i.toDouble)
			case l: Long => doubleToString(l.toDouble)
			case v: Value => valueToString(v)
			case v: ValueConvertible => valueToString(v.toValue)
			case a => valueToString(a.toString.toValue)
		}
		
		private def valueToString(v: Value) = v.double.map(doubleToString) getOrElse ""
		
		private def doubleToString(d: Double) =
		{
			// Cuts the unnecessary decimals
			val roundLong = (d * 100).round
			(roundLong / 100.0).toString
		}
	}
	
	/**
	  * All possible values of this argument type
	  */
	val values: Vector[StringArgumentType] = Vector(StringType, UpperCaseStringType, IntegerType, DecimalType)
	
	/**
	  * Finds an argument type matching provided identifier
	  * @param identifier An identifier
	  * @return An argument type for the specified identifier
	  */
	def apply(identifier: Char) = values.find { _.identifier == identifier }
	
	/**
	  * Parses an argument to string
	  * @param arg The argument
	  * @param identifier The argument type identifier
	  * @return A parsed string from the argument
	  */
	def parse(arg: Any, identifier: Char) = values.find { _.identifier == identifier }.map { _.parse(arg) } getOrElse ""
}

/**
  * This trait represents a an argument type for string interpolation
  * @author Mikko Hilpinen
  * @since 22.4.2019, v1+
  */
sealed trait StringArgumentType
{
	/**
	  * @return An identifier for this argument type
	  */
	def identifier: Char
	
	/**
	  * Parses a string from an argument
	  * @param arg An argument
	  * @return A string representation of the argument in for this type
	  */
	def parse(arg: Any): String
}
