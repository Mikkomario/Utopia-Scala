package utopia.vault.coder.model.enumeration

import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.Enum
import utopia.vault.coder.model.enumeration.BasicPropertyType.DateTime
import utopia.vault.coder.model.enumeration.PropertyType.Optional
import utopia.vault.coder.model.scala.{Reference, ScalaType}

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
	  * @return A nullable (optional) copy of this property type
	  */
	def nullable: PropertyType
	/**
	  * @return A non-nullable copy of this data type
	  */
	def notNull: PropertyType
	
	/**
	  * Writes a code that reads this property type from a value.
	  * @param valueCode Code for accessing a value
	  * @return Code for accessing a value and converting it to this type (in scala)
	  */
	def fromValueCode(valueCode: String): String
	/**
	  * Writes a code that converts this property to a value. May assume that ValueConversions have been imported.
	  * @param instanceCode Code for referring to 'this' instance
	  * @return A code that returns a value based on this instance
	  */
	def toValueCode(instanceCode: String): String
}

/**
  * Basic property types are linked with simple data types and they are not nullable
  */
sealed trait BasicPropertyType extends PropertyType
{
	// ABSTRACT ------------------------------
	
	/**
	  * @return Converts this property type into SQL without any nullable statement
	  */
	def toSqlBase: String
	
	/**
	  * @return Name of the Value's property that converts to this data type (optional version, E.g. "int")
	  */
	def fromValuePropName: String
	
	
	// IMPLEMENTED  --------------------------
	
	override def toSql = toSqlBase + " NOT NULL"
	
	override def isNullable = false
	
	override def createsIndex = false
	
	override def notNull = this
	
	override def nullable = Optional(this)
	
	override def fromValueCode(valueCode: String) = s"$valueCode.get${fromValuePropName.capitalize}"
	
	override def toValueCode(instanceCode: String) = instanceCode
}

object BasicPropertyType
{
	// COMPUTED -----------------------
	
	private def objectValues = Vector(IntNumber, LongNumber, DoubleNumber, Bool, DateTime, Date, Time)
	
	
	// OTHER    -----------------------
	
	/**
	  * @param typeName A property type name / string
	  * @param length Associated property length, if specified (optional)
	  * @return Basic property type matching that specification. None if no match was found.
	  */
	def interpret(typeName: String, length: Option[Int] = None) =
	{
		val lowerName = typeName
		objectValues.find { v =>
			lowerName == v.toScala.toScala.toLowerCase ||
				lowerName == v.toSql.toLowerCase ||
				lowerName == v.toString.toLowerCase
		}.orElse {
			if (lowerName == "text" || lowerName == "string" || lowerName == "varchar")
				Some(Text(length.getOrElse(255)))
			else
				None
		}
	}
	
	
	// NESTED   -----------------------------
	
	/**
	  * Standard integer property type
	  */
	case object IntNumber extends BasicPropertyType
	{
		override def toSqlBase = "INT"
		override def toScala = ScalaType.int
		override def baseDefault = ""
		override def fromValuePropName = "int"
	}
	
	/**
	  * Long / Bigint property type
	  */
	case object LongNumber extends BasicPropertyType
	{
		override def toSqlBase = "BIGINT"
		override def toScala = ScalaType.long
		override def baseDefault = ""
		override def fromValuePropName = "long"
	}
	
	/**
	  * Double property type
	  */
	case object DoubleNumber extends BasicPropertyType
	{
		override def toSqlBase = "DOUBLE"
		override def toScala = ScalaType.double
		override def baseDefault = ""
		override def fromValuePropName = "double"
	}
	
	/**
	  * Boolean property type
	  */
	case object Bool extends BasicPropertyType
	{
		override def toSqlBase = "BOOLEAN"
		override def toScala = ScalaType.boolean
		override def baseDefault = "false"
		override def fromValuePropName = "boolean"
	}
	
	/**
	  * Date + Time (UTC) / Instant / Datetime property type.
	  */
	case object DateTime extends BasicPropertyType
	{
		override def toSqlBase = "DATETIME"
		override def toScala = Reference.instant
		override def baseDefault = "Instant.now()"
		override def fromValuePropName = "instant"
	}
	
	/**
	  * Date / LocalDate type
	  */
	case object Date extends BasicPropertyType
	{
		override def toSqlBase = "DATE"
		override def toScala = Reference.localDate
		override def baseDefault = "LocalDate.now()"
		override def fromValuePropName = "localDate"
	}
	
	/**
	  * Time / LocalTime type
	  */
	case object Time extends BasicPropertyType
	{
		override def toSqlBase = "TIME"
		override def toScala = Reference.localTime
		override def baseDefault = "LocalTime.now()"
		override def fromValuePropName = "localTime"
	}
	
	/**
	  * String / text property type with a certain length
	  * @param length Content max length (default = 255)
	  */
	case class Text(length: Int = 255) extends BasicPropertyType {
		override def toSqlBase = s"VARCHAR($length)"
		override def toScala = ScalaType.string
		override def baseDefault = ""
		override def fromValuePropName = "string"
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
			case "deprecation" => Some(Deprecation)
			case "expiration" => Some(Expiration)
			case other =>
				if (other.contains("option"))
					BasicPropertyType.interpret(other.afterFirst("[").untilFirst("]"), length)
				else
					BasicPropertyType.interpret(other, length)
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
		override def nullable = Optional(DateTime)
		
		override def fromValueCode(valueCode: String) = s"$valueCode.getInstant"
		override def toValueCode(instanceCode: String) = instanceCode
	}
	
	/**
	  * Property that is null until the instance is deprecated, after which the property contains a timestamp of that
	  * deprecation event
	  */
	case object Deprecation extends PropertyType
	{
		override def toScala = ScalaType.option(Reference.instant)
		override def toSql = "DATETIME"
		
		override def isNullable = true
		override def baseDefault = "None"
		override def createsIndex = true
		
		override def nullable = this
		override def notNull = Expiration
		
		override def fromValueCode(valueCode: String) = s"$valueCode.instant"
		override def toValueCode(instanceCode: String) = instanceCode
	}
	
	/**
	  * Contains a time threshold for instance deprecation
	  */
	case object Expiration extends PropertyType
	{
		override def toScala = Reference.instant
		override def toSql = "DATETIME NOT NULL"
		
		override def isNullable = false
		override def baseDefault = ""
		override def createsIndex = true
		
		override def nullable = Deprecation
		override def notNull = this
		
		override def fromValueCode(valueCode: String) = s"$valueCode.getInstant"
		override def toValueCode(instanceCode: String) = instanceCode
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
		override def nullable = this
		
		override def fromValueCode(valueCode: String) = s"$valueCode.${baseType.fromValuePropName}"
		override def toValueCode(instanceCode: String) = instanceCode
	}
	
	/**
	  * Property that refers another class / table
	  * @param referencedTableName Name of the referenced table
	  * @param dataType Data type used in the reference
	  * @param isNullable Whether property values should be optional
	  */
	case class ClassReference(referencedTableName: String, dataType: BasicPropertyType = BasicPropertyType.IntNumber,
	                          isNullable: Boolean = false)
		extends PropertyType
	{
		override def toScala = if (isNullable) ScalaType.option(dataType.toScala) else dataType.toScala
		
		override def toSql = if (isNullable) dataType.toSqlBase else dataType.toSql
		
		override def baseDefault = if (isNullable) "None" else ""
		
		// Index is created when foreign key is generated
		override def createsIndex = false
		
		override def notNull = if (isNullable) copy(isNullable = false) else this
		override def nullable = if (isNullable) this else copy(isNullable = true)
		
		override def fromValueCode(valueCode: String) =
		{
			if (isNullable)
				s"$valueCode.${dataType.fromValuePropName}"
			else
				dataType.fromValueCode(valueCode)
		}
		override def toValueCode(instanceCode: String) = instanceCode
	}
	
	/**
	  * Property type that accepts enumeration values
	  * @param enumeration Enumeration from which the values are picked
	  * @param isNullable Whether this type accepts null / empty
	  */
	case class EnumValue(enumeration: Enum, isNullable: Boolean = false) extends PropertyType
	{
		// IMPLEMENTED  ---------------------------
		
		override def toScala =
			if (isNullable) ScalaType.option(enumeration.reference) else enumeration.reference
		override def toSql = if (isNullable) "INT" else "INT NOT NULL"
		override def baseDefault = ""
		override def createsIndex = false
		override def notNull = if (isNullable) copy(isNullable = false) else this
		override def nullable = if (isNullable) this else copy(isNullable = true)
		
		// TODO: Should add references too (?)
		override def fromValueCode(valueCode: String) =
		{
			if (isNullable)
				s"$valueCode.int.flatMap(${enumeration.name}.findForId)"
			else
				s"${enumeration.name}.forId($valueCode.getInt)"
		}
		override def toValueCode(instanceCode: String) =
			if (isNullable) s"$instanceCode.map { _.id }" else s"$instanceCode.id"
	}
}
