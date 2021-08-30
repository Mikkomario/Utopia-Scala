package utopia.citadel.coder.model.enumeration

import utopia.flow.util.StringExtensions._

/**
  * An enumeration for different types a class property may have
  * @author Mikko Hilpinen
  * @since 29.8.2021, v0.1
  */
sealed trait PropertyType
{
	/**
	  * @return Converts this property type into SQL
	  */
	def toSql: String
	/**
	  * @return Converts this property type into Scala syntax
	  */
	def toScala: String
	/**
	  * @return Whether this property type allows NULL values
	  */
	def isNullable: Boolean
}

/**
  * Basic property types are linked with simple data types and they are not nullable
  */
sealed trait BasicPropertyType extends PropertyType
{
	/**
	  * @return Converts this property type into SQL without any nullable statement
	  */
	def toSqlBase: String
	
	override def toSql = toSqlBase + " NOT NULL"
	
	override def isNullable = false
}

object BasicPropertyType
{
	// OTHER    -----------------------
	
	/**
	  * @param typeName A property type name / string
	  * @param length Associated property length, if specified (optional)
	  * @return Basic property type matching that specification. None if no match was found.
	  */
	def interpret(typeName: String, length: Option[Int] = None) =
		typeName.toLowerCase match
		{
			case "text" => Some(Text(length.getOrElse(255)))
			case "int" => Some(Integer)
			case "long" => Some(BigInt)
			case "datetime" => Some(DateTime)
			case _ => None
		}
	
	
	// NESTED   -----------------------------
	
	/**
	  * Standard integer property type
	  */
	case object Integer extends BasicPropertyType
	{
		override def toSqlBase = "INT"
		override def toScala = "Int"
	}
	
	/**
	  * Long / Bigint property type
	  */
	case object BigInt extends BasicPropertyType
	{
		override def toSqlBase = "BIGINT"
		override def toScala = "Long"
	}
	
	/**
	  * Date + Time (UTC) property type.
	  */
	case object DateTime extends BasicPropertyType
	{
		override def toSqlBase = "DATETIME"
		override def toScala = "Instant"
	}
	
	/**
	  * String / text property type with a certain length
	  * @param length Content max length (default = 255)
	  */
	case class Text(length: Int = 255) extends BasicPropertyType {
		override def toSqlBase = s"VARCHAR($length)"
		override def toScala = "String"
	}
}

object PropertyType
{
	// OTHER    -------------------------------
	
	/**
	  * @param typeName A property type name / string (case-insensitive)
	  * @param length Associated property length (optional)
	  * @return A property type matching that specification. None if no match was found.
	  */
	def interpret(typeName: String, length: Option[Int] = None) =
		typeName.toLowerCase match
		{
			case "creation" => Some(CreationTime)
			case other =>
				if (other.contains("option"))
					BasicPropertyType.interpret(other.afterFirst("[").untilFirst("]"), length)
				else
					None
		}
	
	
	// NESTED   -------------------------------
	
	/**
	  * Property that always sets to the instance creation time
	  */
	case object CreationTime extends PropertyType
	{
		override def toSql = "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP"
		override def toScala = "Instant"
		override def isNullable = false
	}
	
	/**
	  * Property that allows NULL / None values
	  * @param baseType The type of the content when it is defined
	  */
	case class Optional(baseType: BasicPropertyType) extends PropertyType
	{
		override def toSql = baseType.toSqlBase
		override def toScala = s"Option[${baseType.toScala}]"
		override def isNullable = true
	}
}
