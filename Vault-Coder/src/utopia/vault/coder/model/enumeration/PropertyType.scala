package utopia.vault.coder.model.enumeration

import utopia.flow.generic.ValueConversions._
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.{Class, Enum, Name}
import utopia.vault.coder.model.enumeration.BasicPropertyType.{DateTime, IntNumber}
import utopia.vault.coder.model.enumeration.IntSize.Default
import utopia.vault.coder.model.enumeration.NamingConvention.CamelCase
import utopia.vault.coder.model.enumeration.PropertyType.TimeDuration.{fromValueReferences, toValueReferences}
import utopia.vault.coder.model.scala.code.CodePiece
import utopia.vault.coder.model.scala.datatype.{Reference, ScalaType}
import utopia.vault.coder.model.scala.template.{ScalaTypeConvertible, ValueTypeConvertible}

import java.util.concurrent.TimeUnit

/**
  * A common trait for property types which support both nullable (optional) and non-nullable (concrete) variants
  * @author Mikko Hilpinen
  * @since 29.8.2021, v0.1
  */
trait PropertyType extends ScalaTypeConvertible with ValueTypeConvertible with SqlTypeConvertible
{
	// ABSTRACT -------------------------
	
	/**
	  * @return The basic property type information used by this type
	  */
	def baseType: BasePropertyType
	
	/**
	  * @return Property name to use for this type by default (when no name is specified elsewhere)
	  */
	def defaultPropertyName: Name
	
	/**
	  * @return Whether this property type allows NULL (SQL) and None (Scala) values
	  */
	def isNullable: Boolean
	
	/**
	  * @return A nullable (optional) copy of this property type
	  */
	def nullable: PropertyType
	/**
	  * @return A non-nullable copy of this data type
	  */
	def notNull: PropertyType
	
	/**
	  * Writes a default documentation / description for a property
	  * @param className Name of the described class
	  * @param propName Name of the described property
	  * @return A default documentation for that property. Empty if no documentation can / should be generated.
	  */
	def writeDefaultDescription(className: Name, propName: Name): String
	
	
	// COMPUTED ----------------------
	
	/**
	  * @return Converts this property type into SQL
	  */
	def toSql: String = {
		val base = baseType.toBaseSql
		if (isNullable) base else s"$base NOT NULL"
	}
}

/**
  * A common trait for property type implementations that are concrete (ie. not null) and can be wrapped in a
  * Scala.Option
  */
trait ConcretePropertyType extends PropertyType with BasePropertyType
{
	// ABSTRACT ------------------------
	
	def optionFromValueCode(valueCode: String): CodePiece
	def optionToValueCode(optionCode: String): CodePiece
	
	
	// IMPLEMENTED  --------------------
	
	override def baseType = this
	
	override def isNullable = false
	
	override def nullable = OptionWrapped
	override def notNull = this
	
	override def fromValuesCode(valuesCode: String) =
		fromValueCode("v").mapText { fromValue => s"$valuesCode.map { v => $fromValue }" }
	
	
	// NESTED   -----------------------
	
	/**
	  * An option-wrapped version of the parent type
	  */
	object OptionWrapped extends PropertyType
	{
		override def baseType = ConcretePropertyType.this
		
		override def isNullable = true
		
		override def toScala = ScalaType.option(baseType.toScala)
		
		override def defaultPropertyName = ConcretePropertyType.this.defaultPropertyName
		
		override def columnNameSuffix = ConcretePropertyType.this.columnNameSuffix
		
		override def baseSqlDefault = ""
		
		override def createsIndexByDefault = ConcretePropertyType.this.createsIndexByDefault
		
		override def nullable = baseType
		override def notNull = this
		
		override def defaultValue = CodePiece("None")
		
		override def writeDefaultDescription(className: Name, propName: Name) =
			ConcretePropertyType.this.writeDefaultDescription(className, propName)
		
		override def fromValueCode(valueCode: String) = optionFromValueCode(valueCode)
		override def fromValuesCode(valuesCode: String) =
			fromValueCode("v").mapText { fromValue => s"$valuesCode.flatMap { v => $fromValue }" }
		override def toValueCode(instanceCode: String) = optionToValueCode(instanceCode)
	}
}

/**
  * Basic property types are linked with simple data types and they are not nullable
  */
sealed trait BasicPropertyType extends ConcretePropertyType
{
	// ABSTRACT ------------------------------
	
	/**
	  * @return Name of the Value's property that converts to this data type (optional version, e.g. "int")
	  */
	def fromValuePropName: String
	
	
	// IMPLEMENTED  --------------------------
	
	override def columnNameSuffix = None
	
	override def createsIndexByDefault = false
	
	override def fromValueCode(valueCode: String) = CodePiece(s"$valueCode.get${fromValuePropName.capitalize}")
	override def toValueCode(instanceCode: String) = CodePiece(instanceCode, Set(Reference.valueConversions))
	
	override def optionFromValueCode(valueCode: String) = s"$valueCode.$fromValuePropName"
	override def optionToValueCode(optionCode: String) = CodePiece(optionCode, Set(Reference.valueConversions))
	
	override def writeDefaultDescription(className: Name, propName: Name) = ""
}

object BasicPropertyType
{
	// COMPUTED -----------------------
	
	private def objectValues = Vector(LongNumber, DoubleNumber, Bool, DateTime, Date, Time)
	
	
	// OTHER    -----------------------
	
	/**
	  * @param typeName A property type name / string
	  * @param specifiedLength Associated property length, if specified (optional)
	  * @param propertyName Name specified for the property (optional)
	  * @return Basic property type matching that specification. None if no match was found.
	  */
	def interpret(typeName: String, specifiedLength: Option[Int] = None, propertyName: Option[String] = None) =
	{
		val lowerName = typeName
		def _findWith(searches: Iterable[BasicPropertyType => Boolean]) =
			searches.findMap { search => objectValues.find(search) }
		
		if (lowerName.startsWith("text") || lowerName.startsWith("string") || lowerName.startsWith("varchar")) {
			// Text length may be specified within parentheses after the type (E.g. "String(3)")
			val length = lowerName.afterFirst("(").untilFirst(")").int.orElse(specifiedLength).getOrElse(255)
			Some(Text(length))
		}
		else if (lowerName.contains("int")) {
			// Int size may be specified in parentheses after the type name
			// E.g. "Int(Tiny)" => TINYINT or "INT(320)" => SMALLINT
			val lengthPart = lowerName.afterFirst("(").untilFirst(")").notEmpty
			val (size, maxValue) = lengthPart match {
				case Some(s) =>
					IntSize.values.find { size => (size.toString ~== s) || (size.toSql ~== s) } match {
						// Case: Size is specified by name
						case Some(size) => size -> None
						case None =>
							s.int match {
								// Case: Size is specified by maximum value
								case Some(maxValue) =>
									IntSize.values.find { _.maxValue >= maxValue } match {
										case Some(size) => size -> Some(maxValue)
										case None => Default -> None
									}
								// Case: Size can't be parsed
								case None => Default -> None
							}
					}
				case None =>
					// If parentheses are not used, checks the "length" property as well,
					// comparing it to integer maximum length (characters-wise)
					specifiedLength match {
						case Some(maxLength) =>
							IntSize.values.find { _.maxLength >= maxLength } match {
								// Case: Max length is specified and fits into an integer
								case Some(size) =>
									size -> Vector.fill(maxLength)('9').mkString.int.filter { _ < size.maxValue }
								// Case: Max length is specified but is too large
								case None => Default -> None
							}
						// Case: No size or length is specified
						case None => Default -> None
					}
			}
			Some(IntNumber(size, maxValue))
		}
		else
			_findWith(Vector(
				v => v.fromValuePropName.toLowerCase == lowerName,
				v => v.toScala.toString.toLowerCase == lowerName,
				v => v.toBaseSql.toLowerCase == lowerName,
				v => v.defaultPropertyName.variants.exists { _.toLowerCase == lowerName }
			)).orElse {
				// Attempts to find with property name also
				propertyName.map { _.toLowerCase }.flatMap { lowerName =>
					if (lowerName.startsWith("is") || lowerName.startsWith("was"))
						Some(Bool)
					else if (lowerName.contains("name"))
						Some(Text(lowerName.afterFirst("(").untilFirst(")").int
							.orElse(specifiedLength).getOrElse(255)))
					else
						objectValues.filter { _.defaultPropertyName.variants.exists(_.contains(lowerName)) }
							.maxByOption { _.defaultPropertyName.singular.length }
				}
			}
	}
	
	
	// NESTED   -----------------------------
	
	/**
	  * Long / Bigint property type
	  */
	case object LongNumber extends BasicPropertyType
	{
		override def toBaseSql = "BIGINT"
		override def toScala = ScalaType.long
		override def defaultValue = CodePiece.empty
		override def baseSqlDefault = ""
		override def fromValuePropName = "long"
		override def defaultPropertyName = "number"
	}
	
	/**
	  * Double property type
	  */
	case object DoubleNumber extends BasicPropertyType
	{
		override def toBaseSql = "DOUBLE"
		override def toScala = ScalaType.double
		override def defaultValue = CodePiece.empty
		override def baseSqlDefault = ""
		override def fromValuePropName = "double"
		override def defaultPropertyName = "amount"
	}
	
	/**
	  * Boolean property type
	  */
	case object Bool extends BasicPropertyType
	{
		override def toBaseSql = "BOOLEAN"
		override def toScala = ScalaType.boolean
		override def defaultValue = "false"
		override def baseSqlDefault = "FALSE"
		override def fromValuePropName = "boolean"
		override def defaultPropertyName = "flag"
	}
	
	/**
	  * Date + Time (UTC) / Instant / Datetime property type.
	  */
	case object DateTime extends BasicPropertyType
	{
		override def toBaseSql = "DATETIME"
		override def toScala = Reference.instant
		override def defaultValue = Reference.now.targetCode
		override def baseSqlDefault = ""
		override def fromValuePropName = "instant"
		override def defaultPropertyName = "timestamp"
	}
	
	/**
	  * Date / LocalDate type
	  */
	case object Date extends BasicPropertyType
	{
		override def toBaseSql = "DATE"
		override def toScala = Reference.localDate
		override def defaultValue = Reference.today.targetCode
		override def baseSqlDefault = ""
		override def fromValuePropName = "localDate"
		override def defaultPropertyName = "date"
	}
	
	/**
	  * Time / LocalTime type
	  */
	case object Time extends BasicPropertyType
	{
		override def toBaseSql = "TIME"
		override def toScala = Reference.localTime
		override def defaultValue = Reference.now.targetCode
		override def baseSqlDefault = ""
		override def fromValuePropName = "localTime"
		override def defaultPropertyName = "time"
	}
	
	/**
	  * String / text property type with a certain length
	  * @param length Content max length (default = 255)
	  */
	case class Text(length: Int = 255) extends BasicPropertyType {
		override def toBaseSql = s"VARCHAR($length)"
		override def toScala = ScalaType.string
		override def defaultValue = CodePiece.empty
		override def baseSqlDefault = ""
		override def fromValuePropName = "string"
		override def defaultPropertyName = if (length < 100) "name" else "text"
	}
	
	/**
	  * Standard integer property type
	  */
	case class IntNumber(size: IntSize = Default, maxValue: Option[Int] = None) extends BasicPropertyType
	{
		override def toBaseSql = maxValue match {
			case Some(max) => s"${size.toSql}(${max.toString.length})"
			case None => size.toSql
		}
		override def toScala = ScalaType.int
		override def defaultValue = CodePiece.empty
		override def baseSqlDefault = ""
		override def fromValuePropName = "int"
		override def defaultPropertyName = Name("index", "indices", CamelCase.lower)
	}
}

/**
  * A common type for property types which are based on another type
  */
trait PropertyTypeWrapper extends PropertyType
{
	// ABSTRACT --------------------------
	
	/**
	  * @return The wrapped type
	  */
	protected def wrapped: PropertyType
	
	
	// IMPLEMENTED  ----------------------
	
	override def baseType = wrapped.baseType
	override def isNullable = wrapped.isNullable
	
	override def toScala = wrapped.toScala
	override def defaultValue = wrapped.defaultValue
	
	override def columnNameSuffix = wrapped.columnNameSuffix
	override def baseSqlDefault = wrapped.baseSqlDefault
	override def createsIndexByDefault = wrapped.createsIndexByDefault
	
	override def fromValueCode(valueCode: String) = wrapped.fromValueCode(valueCode)
	override def fromValuesCode(valuesCode: String) = wrapped.fromValuesCode(valuesCode)
	override def toValueCode(instanceCode: String) = wrapped.toValueCode(instanceCode)
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
		typeName.toLowerCase match {
			case "creation" | "created" => Some(CreationTime)
			case "updated" | "modification" | "update" => Some(UpdateTime)
			case "deprecation" | "deprecated" => Some(Deprecation)
			case "expiration" | "expired" => Some(Expiration)
			case "value" => Some(GenericValue(length.getOrElse(255)))
			case other =>
				if (other.contains("option"))
					_interpret(other.afterFirst("[").untilLast("]"), length, propertyName).map { _.nullable }
				else
					_interpret(other, length, propertyName)
		}
	
	// Returns a concrete type
	private def _interpret(typeName: String, length: Option[Int], propertyName: Option[String]) =
		typeName match {
			case "days" => Some(DayCount)
			case other =>
				if (typeName.contains("duration")) {
					Some(typeName.afterFirst("[").untilLast("]") match {
						case "s" | "second" | "seconds" => TimeDuration.seconds
						case "m" | "min" | "minute" | "minutes" => TimeDuration.minutes
						case "h" | "hour" | "hours" => TimeDuration.hours
						case _ => TimeDuration.millis
					})
				}
				else
					BasicPropertyType.interpret(other, length, propertyName)
						.orElse {
							// If nothing else works, attempts to find the match with the specified property name
							propertyName.map { _.toLowerCase }.flatMap { lowerName =>
								val options = Vector(CreationTime, Deprecation, Expiration,
									GenericValue(length.getOrElse(255)), DayCount) ++ TimeDuration.values
								options.filter { _.defaultPropertyName.variants.exists { _.contains(lowerName) } }
									.maxByOption { _.defaultPropertyName.singular.length }
							}
						}
		}
	
	
	// NESTED   -------------------------------
	
	/**
	  * Property that always sets to the instance creation time
	  */
	case object CreationTime extends PropertyType with BasePropertyType
	{
		override def baseType = this
		
		override def toBaseSql = "TIMESTAMP"
		override def toScala = Reference.instant
		
		override def defaultValue = Reference.now.targetCode
		override def baseSqlDefault = "CURRENT_TIMESTAMP"
		
		override def defaultPropertyName = Name("created", "creationTimes", CamelCase.lower)
		override def columnNameSuffix = None
		
		override def createsIndexByDefault = true
		
		override def isNullable = false
		
		override def notNull = this
		// A nullable variant of creation time isn't considered a standard creation time anymore
		override def nullable = DateTime.OptionWrapped
		
		override def fromValueCode(valueCode: String) = CodePiece(s"$valueCode.getInstant")
		override def toValueCode(instanceCode: String) = CodePiece(instanceCode, Set(Reference.valueConversions))
		override def fromValuesCode(valuesCode: String) = CodePiece(s"$valuesCode.map { _.getInstant }")
		
		override def writeDefaultDescription(className: Name, propName: Name) =
			s"Time when this $className was added to the database"
	}
	
	/**
	  * Property that always sets to the instance creation time
	  */
	case object UpdateTime extends PropertyTypeWrapper
	{
		override protected def wrapped = CreationTime
		
		override def baseSqlDefault = "CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
		
		override def defaultPropertyName = Name("lastUpdated", "lastUpdateTimes", CamelCase.lower)
		
		override def notNull = this
		// A nullable variant of creation time isn't considered a standard update time anymore
		override def nullable = DateTime.OptionWrapped
		
		override def writeDefaultDescription(className: Name, propName: Name) =
			s"Time when this $className was last updated"
	}
	
	/**
	  * Property that is null until the instance is deprecated, after which the property contains a timestamp of that
	  * deprecation event
	  */
	case object Deprecation extends PropertyTypeWrapper
	{
		protected def wrapped = DateTime.OptionWrapped
		
		override def defaultPropertyName = Name("deprecatedAfter", "deprecationTimes", CamelCase.lower)
		override def createsIndexByDefault = true
		
		override def nullable = this
		override def notNull = Expiration
		
		override def writeDefaultDescription(className: Name, propName: Name) =
			s"Time when this $className became deprecated. None while this $className is still valid."
	}
	
	/**
	  * Contains a time threshold for instance deprecation
	  */
	case object Expiration extends PropertyTypeWrapper
	{
		protected def wrapped = DateTime
		
		override def defaultPropertyName = Name("expires", "expirationTimes", CamelCase.lower)
		override def createsIndexByDefault = true
		
		override def nullable = Deprecation
		override def notNull = this
		
		override def writeDefaultDescription(className: Name, propName: Name) =
			s"Time when this $className expires / becomes invalid"
	}
	
	/**
	  * Represents a number of days
	  */
	case object DayCount extends ConcretePropertyType
	{
		override def toScala = Reference.days
		override def toBaseSql = "INT"
		
		override def defaultPropertyName = "duration"
		override def columnNameSuffix = Some("days")
		override def defaultValue = CodePiece("Days.zero", Set(Reference.days))
		override def baseSqlDefault = "0"
		
		override def createsIndexByDefault = false
		
		override def fromValueCode(valueCode: String) = CodePiece(s"Days($valueCode.getInt)", Set(Reference.days))
		override def toValueCode(instanceCode: String) =
			CodePiece(s"$instanceCode.length", Set(Reference.valueConversions))
		
		override def optionFromValueCode(valueCode: String) =
			CodePiece(s"$valueCode.int.map { Days(_) }", Set(Reference.days))
		override def optionToValueCode(optionCode: String) =
			CodePiece(s"$optionCode.map { _.length }", Set(Reference.valueConversions))
		
		override def writeDefaultDescription(className: Name, propName: Name) = ""
	}
	
	case class GenericValue(length: Int = 255) extends PropertyType with BasePropertyType
	{
		override def baseType = this
		
		override def toScala = Reference.value
		override def toBaseSql = s"VARCHAR($length)"
		
		// Values are at the same time nullable (have an empty value) and not nullable (aren't wrapped in option)
		override def isNullable = true
		
		override def defaultPropertyName = "value"
		override def defaultValue = CodePiece("Value.empty", Set(Reference.value))
		override def columnNameSuffix = None
		override def baseSqlDefault = ""
		override def createsIndexByDefault = false
		
		override def nullable = this
		override def notNull = this
		
		override def fromValueCode(valueCode: String) = valueCode
		override def fromValuesCode(valuesCode: String) = valuesCode
		override def toValueCode(instanceCode: String) = instanceCode
		
		override def writeDefaultDescription(className: Name, propName: Name) = s"Generic $propName of this $className"
	}
	
	object TimeDuration
	{
		private val fromValueReferences = Set(Reference.timeUnit, Reference.finiteDuration)
		private val toValueReferences = Set(Reference.valueConversions, Reference.timeUnit)
		
		val millis = apply(TimeUnit.MILLISECONDS)
		val seconds = apply(TimeUnit.SECONDS)
		val minutes = apply(TimeUnit.MINUTES)
		val hours = apply(TimeUnit.HOURS)
		
		def values = Vector(millis, seconds, minutes, hours)
	}
	/**
	  * Represents a duration of time
	  * @param unit Unit used when storing this duration to the database
	  */
	case class TimeDuration(unit: TimeUnit) extends ConcretePropertyType
	{
		private def unitConversionCode = s".toUnit(TimeUnit.${unit.name})"
		
		override def toScala = Reference.finiteDuration
		override def toBaseSql = unit match {
			case TimeUnit.NANOSECONDS | TimeUnit.MICROSECONDS | TimeUnit.MILLISECONDS => "BIGINT"
			case _ => "INT"
		}
		
		override def defaultPropertyName = "duration"
		override def columnNameSuffix = Some(unit match {
			case TimeUnit.NANOSECONDS => "nanos"
			case TimeUnit.MICROSECONDS => "microseconds"
			case TimeUnit.MILLISECONDS => "millis"
			case TimeUnit.SECONDS => "seconds"
			case TimeUnit.MINUTES => "minutes"
			case TimeUnit.HOURS => "hours"
			case TimeUnit.DAYS => "days"
			case _ => unit.toString.toLowerCase
		})
		override def defaultValue = CodePiece("Duration.Zero", Set(Reference.duration))
		override def baseSqlDefault = "0"
		
		override def createsIndexByDefault = false
		
		override def fromValueCode(valueCode: String) =
			CodePiece(s"FiniteDuration($valueCode.getLong, TimeUnit.${unit.name})", fromValueReferences)
		override def toValueCode(instanceCode: String) =
			CodePiece(instanceCode + unitConversionCode, toValueReferences)
		override def optionFromValueCode(valueCode: String) =
			CodePiece(s"$valueCode.long.map { FiniteDuration(_, TimeUnit.${unit.name}) }", fromValueReferences)
		override def optionToValueCode(optionCode: String) =
			CodePiece(s"$optionCode.map { _$unitConversionCode }", toValueReferences)
		
		override def writeDefaultDescription(className: Name, propName: Name) = s"Duration of this $className"
	}
	
	/**
	  * Property that refers another class / table
	  * @param referencedTableName Name of the referenced table
	  * @param referencedColumnName Name of the column being referred to (default = id)
	  * @param referencedType The type of the referenced column (default = standard integer)
	  */
	case class ClassReference(referencedTableName: Name, referencedColumnName: Name = Class.defaultIdName,
	                          referencedType: PropertyType = IntNumber())
		extends PropertyTypeWrapper
	{
		override protected def wrapped = referencedType
		
		override def defaultPropertyName: Name = referencedTableName + "id"
		// Index is created when foreign key is generated
		override def createsIndexByDefault = false
		
		override def notNull = if (isNullable) copy(referencedType = referencedType.notNull) else this
		override def nullable = if (isNullable) this else copy(referencedType = referencedType.nullable)
		
		override def writeDefaultDescription(className: Name, propName: Name) =
			s"${referencedColumnName.toText.singular.capitalize} of the $referencedTableName linked with this $className"
	}
	
	/**
	  * Property type that accepts enumeration values
	  * @param enumeration Enumeration from which the values are picked
	  */
	case class EnumValue(enumeration: Enum) extends ConcretePropertyType
	{
		// IMPLEMENTED  ---------------------------
		
		override def toScala = enumeration.reference
		// Since there usually aren't a huge number of enumeration values, TINYINT is used
		override def toBaseSql = "TINYINT"
		override def defaultValue = CodePiece.empty
		override def baseSqlDefault = ""
		override def defaultPropertyName = enumeration.name.uncapitalize
		override def columnNameSuffix = Some("id")
		override def createsIndexByDefault = false
		
		// Returns a Try (bug?)
		override def fromValueCode(valueCode: String) =
			CodePiece(s"${enumeration.name}.forId($valueCode.getInt)", Set(enumeration.reference))
		override def fromValuesCode(valuesCode: String) =
			CodePiece(s"$valuesCode.flatMap { _.int }.flatMap(${enumeration.name}.findForId)",
				Set(enumeration.reference))
		override def toValueCode(instanceCode: String) = CodePiece(s"$instanceCode.id", Set(Reference.valueConversions))
		override def optionFromValueCode(valueCode: String) =
			CodePiece(s"$valueCode.int.flatMap(${enumeration.name}.findForId)", Set(enumeration.reference))
		override def optionToValueCode(optionCode: String) =
			CodePiece(s"$optionCode.map { _.id }", Set(Reference.valueConversions))
		
		override def writeDefaultDescription(className: Name, propName: Name) =
			s"${enumeration.name} of this $className"
	}
}
