package utopia.citadel.coder.model.enumeration

import utopia.citadel.coder.model.scala.{Reference, ScalaType}
import utopia.flow.util.StringExtensions._

/**
  * An enumeration for different types a class property may have
  * @author Mikko Hilpinen
  * @since 29.8.2021, v0.1
  */
sealed trait PropertyType
{
	/**
	  * @return A scala type matching this property type
	  */
	def toScala: ScalaType
	/**
	  * @return Converts this property type into SQL
	  */
	def toSql: String
	/**
	  * @return Whether this property type allows NULL values
	  */
	def isNullable: Boolean
	/**
	  * @return Default value assigned for this type by default. Empty string if no specific default is used.
	  */
	def baseDefault: String
	/**
	  * @return Whether a database index should be created based on this property type
	  */
	def createsIndex: Boolean
	
	/**
	  * @return A non-nullable copy of this data type
	  */
	def notNull: PropertyType
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
	
	override def createsIndex = false
	
	override def notNull = this
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
			case "long" => Some(BigInteger)
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
		override def toScala = ScalaType.int
		override def baseDefault = ""
	}
	
	/**
	  * Long / Bigint property type
	  */
	case object BigInteger extends BasicPropertyType
	{
		override def toSqlBase = "BIGINT"
		override def toScala = ScalaType.long
		override def baseDefault = ""
	}
	
	/**
	  * Date + Time (UTC) property type.
	  */
	case object DateTime extends BasicPropertyType
	{
		override def toSqlBase = "DATETIME"
		override def toScala = Reference.instant
		override def baseDefault = "Instant.now()"
	}
	
	/**
	  * String / text property type with a certain length
	  * @param length Content max length (default = 255)
	  */
	case class Text(length: Int = 255) extends BasicPropertyType {
		override def toSqlBase = s"VARCHAR($length)"
		override def toScala = ScalaType.string
		override def baseDefault = ""
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
		override def toScala = Reference.instant
		override def isNullable = false
		override def baseDefault = "Instant.now()"
		override def createsIndex = true
		
		override def notNull = this
	}
	
	/**
	  * Property that allows NULL / None values
	  * @param baseType The type of the content when it is defined
	  */
	case class Optional(baseType: BasicPropertyType) extends PropertyType
	{
		override def toSql = baseType.toSqlBase
		override def toScala = ScalaType.option(baseType.toScala)
		override def isNullable = true
		override def baseDefault = "None"
		override def createsIndex = baseType.createsIndex
		
		override def notNull = baseType
	}
	
	/**
	  * Property that refers another class / table
	  * @param referencedTableName Name of the referenced table
	  * @param dataType Data type used in the reference
	  * @param isNullable Whether property values should be optional
	  */
	case class ClassReference(referencedTableName: String, dataType: BasicPropertyType = BasicPropertyType.Integer,
	                          isNullable: Boolean = false)
		extends PropertyType
	{
		override def toScala = if (isNullable) ScalaType.option(dataType.toScala) else dataType.toScala
		
		override def toSql = if (isNullable) dataType.toSqlBase else dataType.toSql
		
		override def baseDefault = if (isNullable) "None" else ""
		
		// Index is created when foreign key is generated
		override def createsIndex = false
		
		override def notNull = if (isNullable) copy(isNullable = false) else this
	}
}
