package utopia.vault.coder.model.enumeration

import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.{Class, Enum, Name}
import utopia.vault.coder.model.enumeration.BasicPropertyType.DateTime
import utopia.vault.coder.model.enumeration.NamingConvention.CamelCase
import utopia.vault.coder.model.enumeration.PropertyType.Optional
import utopia.vault.coder.model.scala.code.CodePiece
import utopia.vault.coder.model.scala.{Reference, ScalaType}

import java.util.concurrent.TimeUnit

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
	  * @return Property name to use for this type by default (when no name is specified elsewhere)
	  */
	def defaultPropertyName: Name
	/**
	  * @return Default value assigned for this type by default. Empty if no specific default is used.
	  */
	def baseDefault: CodePiece
	/**
	  * @return Default value for this type by default in the SQL document
	  */
	def baseSqlDefault: String
	/**
	  * @return Whether a database index should be created based on this property type
	  */
	def createsIndexByDefault: Boolean
	
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
	def fromValueCode(valueCode: String): CodePiece
	/**
	  * Writes a code that reads a collection of these properties from a vector of values
	  * @param valuesCode Code that returns a vector of values
	  * @return Code for accessing the specified values and converting them to a collection of this type in Scala
	  */
	def fromValuesCode(valuesCode: String): CodePiece
	/**
	  * Writes a code that converts this property to a value.
	  * @param instanceCode Code for referring to 'this' instance
	  * @return A code that returns a value based on this instance
	  */
	def toValueCode(instanceCode: String): CodePiece
	
	/**
	  * Writes a default documentation / description for a property
	  * @param className Name of the described class
	  * @param propName Name of the described property
	  * @return A default documentation for that property. Empty if no documentation can / should be generated.
	  */
	def writeDefaultDescription(className: Name, propName: Name): String
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
	override def createsIndexByDefault = false
	
	override def notNull = this
	override def nullable = Optional(this)
	
	override def fromValueCode(valueCode: String) = CodePiece(s"$valueCode.get${fromValuePropName.capitalize}")
	override def fromValuesCode(valuesCode: String) =
		CodePiece(s"$valuesCode.map { v => ${fromValueCode("v")} }")
	override def toValueCode(instanceCode: String) = CodePiece(instanceCode, Set(Reference.valueConversions))
	
	override def writeDefaultDescription(className: Name, propName: Name) = ""
}

object BasicPropertyType
{
	// COMPUTED -----------------------
	
	private def objectValues = Vector(IntNumber, LongNumber, DoubleNumber, Bool, DateTime, Date, Time)
	
	
	// OTHER    -----------------------
	
	/**
	  * @param typeName A property type name / string
	  * @param length Associated property length, if specified (optional)
	  * @param propertyName Name specified for the property (optional)
	  * @return Basic property type matching that specification. None if no match was found.
	  */
	def interpret(typeName: String, length: Option[Int] = None, propertyName: Option[String] = None) =
	{
		val lowerName = typeName
		def _findWith(searches: Iterable[BasicPropertyType => Boolean]) =
			searches.findMap { search => objectValues.find(search) }
		
		if (lowerName == "text" || lowerName == "string" || lowerName == "varchar")
			Some(Text(length.getOrElse(255)))
		else
			_findWith(Vector(
				v => v.fromValuePropName.toLowerCase == lowerName,
				v => v.toScala.toString.toLowerCase == lowerName,
				v => v.toSqlBase.toLowerCase == lowerName,
				v => v.defaultPropertyName.variants.exists { _.toLowerCase == lowerName }
			)).orElse {
				// Attempts to find with property name also
				propertyName.map { _.toLowerCase }.flatMap { lowerName =>
					if (lowerName.startsWith("is") || lowerName.startsWith("was"))
						Some(Bool)
					else if (lowerName.contains("name"))
						Some(Text(length.getOrElse(255)))
					else
						objectValues.filter { _.defaultPropertyName.variants.exists(_.contains(lowerName)) }
							.maxByOption { _.defaultPropertyName.singular.length }
				}
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
		override def baseDefault = CodePiece.empty
		override def baseSqlDefault = ""
		override def fromValuePropName = "int"
		override def defaultPropertyName = Name("index", "indices", CamelCase.lower)
	}
	
	/**
	  * Long / Bigint property type
	  */
	case object LongNumber extends BasicPropertyType
	{
		override def toSqlBase = "BIGINT"
		override def toScala = ScalaType.long
		override def baseDefault = CodePiece.empty
		override def baseSqlDefault = ""
		override def fromValuePropName = "long"
		override def defaultPropertyName = "number"
	}
	
	/**
	  * Double property type
	  */
	case object DoubleNumber extends BasicPropertyType
	{
		override def toSqlBase = "DOUBLE"
		override def toScala = ScalaType.double
		override def baseDefault = CodePiece.empty
		override def baseSqlDefault = ""
		override def fromValuePropName = "double"
		override def defaultPropertyName = "amount"
	}
	
	/**
	  * Boolean property type
	  */
	case object Bool extends BasicPropertyType
	{
		override def toSqlBase = "BOOLEAN"
		override def toScala = ScalaType.boolean
		override def baseDefault = "false"
		override def baseSqlDefault = "FALSE"
		override def fromValuePropName = "boolean"
		override def defaultPropertyName = "flag"
	}
	
	/**
	  * Date + Time (UTC) / Instant / Datetime property type.
	  */
	case object DateTime extends BasicPropertyType
	{
		override def toSqlBase = "DATETIME"
		override def toScala = Reference.instant
		override def baseDefault = Reference.now.targetCode
		override def baseSqlDefault = ""
		override def fromValuePropName = "instant"
		override def defaultPropertyName = "timestamp"
	}
	
	/**
	  * Date / LocalDate type
	  */
	case object Date extends BasicPropertyType
	{
		override def toSqlBase = "DATE"
		override def toScala = Reference.localDate
		override def baseDefault = Reference.today.targetCode
		override def baseSqlDefault = ""
		override def fromValuePropName = "localDate"
		override def defaultPropertyName = "date"
	}
	
	/**
	  * Time / LocalTime type
	  */
	case object Time extends BasicPropertyType
	{
		override def toSqlBase = "TIME"
		override def toScala = Reference.localTime
		override def baseDefault = Reference.now.targetCode
		override def baseSqlDefault = ""
		override def fromValuePropName = "localTime"
		override def defaultPropertyName = "time"
	}
	
	/**
	  * String / text property type with a certain length
	  * @param length Content max length (default = 255)
	  */
	case class Text(length: Int = 255) extends BasicPropertyType {
		override def toSqlBase = s"VARCHAR($length)"
		override def toScala = ScalaType.string
		override def baseDefault = CodePiece.empty
		override def baseSqlDefault = ""
		override def fromValuePropName = "string"
		override def defaultPropertyName = if (length < 100) "name" else "text"
	}
}

object PropertyType
{
	// OTHER    -------------------------------
	
	/**
	  * @param typeName A property type name / string (case-insensitive)
	  * @param length Associated property length (optional)
	  * @param propertyName Name specified for the property in question (optional)
	  * @return A property type matching that specification. None if no match was found.
	  */
	def interpret(typeName: String, length: Option[Int] = None, propertyName: Option[String] = None) =
		typeName.toLowerCase match
		{
			case "creation" | "created" => Some(CreationTime)
			case "deprecation" | "deprecated" => Some(Deprecation)
			case "expiration" | "expired" => Some(Expiration)
			case "value" => Some(GenericValue(length.getOrElse(255)))
			case other =>
				if (other.contains("option"))
					_interpret(other.afterFirst("[").untilLast("]"), length, propertyName, isNullable = true)
				else
					_interpret(other, length, propertyName, isNullable = false)
		}
	
	private def _interpret(typeName: String, length: Option[Int], propertyName: Option[String],
	                       isNullable: Boolean): Option[PropertyType] =
		typeName match
		{
			case "days" => Some(if (isNullable) OptionalDayCount else DayCount)
			case other =>
				if (typeName.contains("duration"))
				{
					val notNull = typeName.afterFirst("[").untilLast("]") match
					{
						case "s" | "second" | "seconds" => TimeDuration.seconds
						case "m" | "min" | "minute" | "minutes" => TimeDuration.minutes
						case "h" | "hour" | "hours" => TimeDuration.hours
						case _ => TimeDuration.millis
					}
					Some(if (isNullable) notNull.nullable else notNull)
				}
				else
					BasicPropertyType.interpret(other, length, propertyName)
						.map { base => if (isNullable) Optional(base) else base }
						.orElse {
							// If nothing else works, attempts to find the match with the specified property name
							propertyName.map { _.toLowerCase }.flatMap { lowerName =>
								val options = Vector(CreationTime, Deprecation, Expiration,
									GenericValue(length.getOrElse(255)), OptionalDayCount, DayCount) ++ TimeDuration.values
								options.filter { _.defaultPropertyName.variants.exists { _.contains(lowerName) } }
									.bestMatch { Vector(_.isNullable == isNullable) }
									.maxByOption { _.defaultPropertyName.singular.length }
									.map { dataType => if (isNullable) dataType.nullable else dataType }
							}
						}
		}
	
	
	// NESTED   -------------------------------
	
	/**
	  * Property that always sets to the instance creation time
	  */
	case object CreationTime extends PropertyType
	{
		override def toSql = "TIMESTAMP NOT NULL"
		override def toScala = Reference.instant
		override def isNullable = false
		override def baseDefault = Reference.now.targetCode
		override def baseSqlDefault = "CURRENT_TIMESTAMP"
		override def defaultPropertyName = Name("created", "creationTimes", CamelCase.lower)
		override def createsIndexByDefault = true
		
		override def notNull = this
		override def nullable = Optional(DateTime)
		
		override def fromValueCode(valueCode: String) = CodePiece(s"$valueCode.getInstant")
		override def toValueCode(instanceCode: String) =
			CodePiece(instanceCode, Set(Reference.valueConversions))
		override def fromValuesCode(valuesCode: String) = CodePiece(s"$valuesCode.map { _.getInstant }")
		
		override def writeDefaultDescription(className: Name, propName: Name) =
			s"Time when this $className was first created"
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
		override def baseSqlDefault = ""
		override def defaultPropertyName = Name("deprecatedAfter", "deprecationTimes", CamelCase.lower)
		override def createsIndexByDefault = true
		
		override def nullable = this
		override def notNull = Expiration
		
		override def fromValueCode(valueCode: String) = CodePiece(s"$valueCode.instant")
		override def fromValuesCode(valuesCode: String) = CodePiece(s"$valuesCode.flatMap { _.instant }")
		override def toValueCode(instanceCode: String) =
			CodePiece(instanceCode, Set(Reference.valueConversions))
		
		override def writeDefaultDescription(className: Name, propName: Name) =
			s"Time when this $className became deprecated. None while this $className is still valid."
	}
	
	/**
	  * Contains a time threshold for instance deprecation
	  */
	case object Expiration extends PropertyType
	{
		override def toScala = Reference.instant
		override def toSql = "DATETIME NOT NULL"
		
		override def isNullable = false
		override def baseDefault = CodePiece.empty
		override def baseSqlDefault = ""
		override def defaultPropertyName = Name("expires", "expirationTimes", CamelCase.lower)
		override def createsIndexByDefault = true
		
		override def nullable = Deprecation
		override def notNull = this
		
		override def fromValueCode(valueCode: String) = s"$valueCode.getInstant"
		override def fromValuesCode(valuesCode: String) = s"$valuesCode.map { _.getInstant }"
		override def toValueCode(instanceCode: String) =
			CodePiece(instanceCode, Set(Reference.valueConversions))
		
		override def writeDefaultDescription(className: Name, propName: Name) =
			s"Time when this $className expires / becomes invalid"
	}
	
	/**
	  * Represents a number of days
	  */
	case object DayCount extends PropertyType
	{
		override def isNullable = false
		
		override def toScala = Reference.days
		override def toSql = "INT NOT NULL"
		
		override def defaultPropertyName = "duration"
		
		override def baseDefault = CodePiece("Days.zero", Set(Reference.days))
		override def baseSqlDefault = "0"
		
		override def createsIndexByDefault = false
		
		override def nullable = OptionalDayCount
		override def notNull = this
		
		override def fromValueCode(valueCode: String) = CodePiece(s"Days($valueCode.getInt)", Set(Reference.days))
		override def fromValuesCode(valuesCode: String) =
			CodePiece(s"$valuesCode.map { v => Days(v.getInt) }", Set(Reference.days))
		override def toValueCode(instanceCode: String) =
			CodePiece(s"$instanceCode.length", Set(Reference.valueConversions))
		
		override def writeDefaultDescription(className: Name, propName: Name) = ""
	}
	
	/**
	  * Represents a number of days, may be 0
	  */
	case object OptionalDayCount extends PropertyType
	{
		override def toScala = ScalaType.option(Reference.days)
		override def toSql = "INT"
		
		override def isNullable = true
		override def createsIndexByDefault = false
		
		override def defaultPropertyName = "duration"
		override def baseDefault = "None"
		override def baseSqlDefault = ""
		
		override def nullable = this
		override def notNull = DayCount
		
		override def fromValueCode(valueCode: String) =
			CodePiece(s"$valueCode.int.map { Days(_) }", Set(Reference.days))
		override def fromValuesCode(valuesCode: String) =
			CodePiece(s"$valuesCode.flatMap { _.int }.map(Days.apply)", Set(Reference.days))
		override def toValueCode(instanceCode: String) =
			CodePiece(s"$instanceCode.map { _.length }", Set(Reference.valueConversions))
		
		override def writeDefaultDescription(className: Name, propName: Name) = ""
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
		override def baseSqlDefault = ""
		override def defaultPropertyName = baseType.defaultPropertyName
		override def createsIndexByDefault = baseType.createsIndexByDefault
		
		override def notNull = baseType
		override def nullable = this
		
		override def fromValueCode(valueCode: String) = s"$valueCode.${baseType.fromValuePropName}"
		override def fromValuesCode(valuesCode: String) =
			CodePiece(s"$valuesCode.flatMap { _.${baseType.fromValuePropName} }")
		override def toValueCode(instanceCode: String) = CodePiece(instanceCode, Set(Reference.valueConversions))
		
		override def writeDefaultDescription(className: Name, propName: Name) = ""
	}
	
	case class GenericValue(length: Int = 255) extends PropertyType
	{
		override def toScala = Reference.value
		override def toSql = s"VARCHAR($length)"
		
		// Values are at the same time nullable (have an empty value) and not nullable (aren't wrapped in option)
		override def isNullable = true
		
		override def defaultPropertyName = "value"
		override def baseDefault = CodePiece("Value.empty", Set(Reference.value))
		override def baseSqlDefault = ""
		override def createsIndexByDefault = false
		
		override def nullable = this
		override def notNull = this
		
		override def fromValueCode(valueCode: String) = valueCode
		override def fromValuesCode(valuesCode: String) = valuesCode
		override def toValueCode(instanceCode: String) =
			CodePiece(instanceCode, Set(Reference.valueConversions))
		
		override def writeDefaultDescription(className: Name, propName: Name) = s"Generic $propName of this $className"
	}
	
	object TimeDuration
	{
		val millis = apply(TimeUnit.MILLISECONDS)
		val seconds = apply(TimeUnit.SECONDS)
		val minutes = apply(TimeUnit.MINUTES)
		val hours = apply(TimeUnit.HOURS)
		
		def values = Vector(millis, seconds, minutes, hours)
	}
	/**
	  * Represents a duration of time
	  * @param unit Unit used when storing this duration to the database
	  * @param isNullable Whether None is accepted as a value
	  */
	case class TimeDuration(unit: TimeUnit, isNullable: Boolean = false) extends PropertyType
	{
		override def toScala = if (isNullable) ScalaType.option(Reference.finiteDuration) else Reference.finiteDuration
		override def toSql =
		{
			val sqlType = unit match
			{
				case TimeUnit.NANOSECONDS | TimeUnit.MICROSECONDS | TimeUnit.MILLISECONDS => "BIGINT"
				case _ => "INT"
			}
			if (isNullable) sqlType else sqlType + " NOT NULL"
		}
		
		override def defaultPropertyName = "duration"
		override def baseDefault =
			if (isNullable) "None" else CodePiece("Duration.Zero", Set(Reference.duration))
		override def baseSqlDefault = if (isNullable) "" else "0"
		
		override def createsIndexByDefault = false
		
		override def nullable = copy(isNullable = true)
		override def notNull = copy(isNullable = false)
		
		override def fromValueCode(valueCode: String) =
		{
			val text =
			{
				if (isNullable)
					s"$valueCode.long.map { FiniteDuration(_, TimeUnit.${unit.name}) }"
				else
					s"FiniteDuration($valueCode.getLong, TimeUnit.${unit.name})"
			}
			CodePiece(text, Set(Reference.timeUnit, Reference.finiteDuration))
		}
		override def fromValuesCode(valuesCode: String) =
		{
			val text = {
				if (isNullable)
					s"$valuesCode.flatMap { _.long }.map { FiniteDuration(_, TimeUnit.${unit.name}) }"
				else
					s"$valuesCode.map { v => FiniteDuration(v.getLong, TimeUnit.${unit.name}) }"
			}
			CodePiece(text, Set(Reference.timeUnit, Reference.finiteDuration))
		}
		override def toValueCode(instanceCode: String) =
		{
			val conversion = s".toUnit(TimeUnit.${unit.name})"
			val end = if (isNullable) s".map { _$conversion }" else conversion
			CodePiece(instanceCode + end, Set(Reference.valueConversions, Reference.timeUnit))
		}
		
		override def writeDefaultDescription(className: Name, propName: Name) = s"Duration of this $className"
	}
	
	/**
	  * Property that refers another class / table
	  * @param referencedTableName Name of the referenced table
	  * @param referencedColumnName Name of the column being referred to (default = id)
	  * @param dataType Data type used in the reference
	  * @param isNullable Whether property values should be optional
	  */
	case class ClassReference(referencedTableName: Name, referencedColumnName: Name = Class.defaultIdName,
	                          dataType: BasicPropertyType = BasicPropertyType.IntNumber,
	                          isNullable: Boolean = false)
		extends PropertyType
	{
		override def toScala = if (isNullable) ScalaType.option(dataType.toScala) else dataType.toScala
		override def toSql = if (isNullable) dataType.toSqlBase else dataType.toSql
		
		override def baseDefault = if (isNullable) "None" else CodePiece.empty
		override def baseSqlDefault = ""
		override def defaultPropertyName: Name = referencedTableName + "id"
		// Index is created when foreign key is generated
		override def createsIndexByDefault = false
		
		override def notNull = if (isNullable) copy(isNullable = false) else this
		override def nullable = if (isNullable) this else copy(isNullable = true)
		
		override def fromValueCode(valueCode: String) =
		{
			if (isNullable)
				s"$valueCode.${dataType.fromValuePropName}"
			else
				dataType.fromValueCode(valueCode)
		}
		override def fromValuesCode(valuesCode: String) =
		{
			if (isNullable)
				CodePiece(s"$valuesCode.flatMap { _.${dataType.fromValuePropName} }")
			else
			{
				val raw = dataType.fromValueCode("v")
				CodePiece(s"$valuesCode.map { v => $raw }", raw.references)
			}
		}
		override def toValueCode(instanceCode: String) = CodePiece(instanceCode, Set(Reference.valueConversions))
		
		override def writeDefaultDescription(className: Name, propName: Name) =
			s"Id of the $referencedTableName linked with this $className"
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
		override def baseDefault = if (isNullable) CodePiece("None") else CodePiece.empty
		override def baseSqlDefault = ""
		override def defaultPropertyName = enumeration.name.uncapitalize
		override def createsIndexByDefault = false
		override def notNull = if (isNullable) copy(isNullable = false) else this
		override def nullable = if (isNullable) this else copy(isNullable = true)
		
		override def fromValueCode(valueCode: String) =
		{
			val text =
			{
				if (isNullable)
					s"$valueCode.int.flatMap(${enumeration.name}.findForId)"
				else
					s"${enumeration.name}.forId($valueCode.getInt)"
			}
			CodePiece(text, Set(enumeration.reference))
		}
		override def fromValuesCode(valuesCode: String) =
			CodePiece(s"$valuesCode.flatMap { _.int }.flatMap(${enumeration.name}.findForId)",
				Set(enumeration.reference))
		override def toValueCode(instanceCode: String) =
			CodePiece(if (isNullable) s"$instanceCode.map { _.id }" else s"$instanceCode.id",
				Set(Reference.valueConversions))
		
		override def writeDefaultDescription(className: Name, propName: Name) =
			s"${enumeration.name} of this $className"
	}
}
