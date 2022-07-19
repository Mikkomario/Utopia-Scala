package utopia.vault.coder.model.datatype

import utopia.flow.generic.ValueConversions._
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.{Class, Enum, Name}
import utopia.vault.coder.model.datatype.BasicPropertyType.{DateTime, IntNumber}
import utopia.vault.coder.model.datatype.PropertyType.TimeDuration.{fromValueReferences, toValueReferences}
import utopia.vault.coder.model.enumeration.IntSize
import utopia.vault.coder.model.enumeration.IntSize.Default
import utopia.vault.coder.model.enumeration.NamingConvention.CamelCase
import utopia.vault.coder.model.scala.code.CodePiece
import utopia.vault.coder.model.scala.datatype.{Reference, ScalaType}
import utopia.vault.coder.model.scala.template.{ScalaTypeConvertible, ValueConvertibleType}

import java.util.concurrent.TimeUnit

/**
  * A common trait for property types which support both nullable (optional) and non-nullable (concrete) variants
  * @author Mikko Hilpinen
  * @since 29.8.2021, v0.1
  */
// TODO: Separate this file to multiple sub-files
trait PropertyType extends ScalaTypeConvertible with ValueConvertibleType
{
	// ABSTRACT -------------------------
	
	/**
	  * @return The SQL / database representations of this type. Many if this property spans multiple columns.
	  */
	def sqlConversions: Vector[SqlTypeConversion]
	
	/**
	  * @return A non-empty default value for (construction) parameters of this type.
	  *         Empty code if no such value is available.
	  */
	def nonEmptyDefaultValue: CodePiece
	
	/**
	  * @return Property name to use for this type by default (when no name is specified elsewhere)
	  */
	def defaultPropertyName: Name
	
	/**
	  * @return Whether the conversion from a Value may fail, meaning that fromValueCode
	  *         yields instances of Try instead of instances of this type.
	  */
	def yieldsTryFromValue: Boolean
	
	/**
	  * @return An optional copy of this property type (one that accepts None or other such empty value)
	  */
	def optional: PropertyType
	/**
	  * @return A non-optional (ie. concrete) version of this data type
	  */
	def concrete: PropertyType
	
	/**
	  * Writes a code that reads this an instance of this type from a value.
	  * @param valueCode Code for accessing a value
	  * @return Code for accessing a value and converting it to this type (in scala)
	  */
	def fromValueCode(valueCode: String): CodePiece
	/**
	  * Writes a code that reads a vector of instances of this type from a vector of values
	  * @param valuesCode Code that returns a vector of values
	  * @return Code for accessing the specified values and converting them to a vector of this type's instances in Scala
	  */
	def fromValuesCode(valuesCode: String): CodePiece
	
	/**
	  * Writes a default documentation / description for a property
	  * @param className Name of the described class
	  * @param propName Name of the described property
	  * @return A default documentation for that property. Empty if no documentation can / should be generated.
	  */
	def writeDefaultDescription(className: Name, propName: Name): String
	
	
	// COMPUTED ------------------------
	
	/**
	  * @return Whether this type matches a single database-column only
	  */
	def isSingleColumn = sqlConversions.size == 1
	/**
	  * @return Whether this type matches multiple database-columns
	  */
	def isMultiColumn = sqlConversions.size > 1
	
	/**
	  * @return Whether this property type accepts empty values (is optional)
	  */
	def isOptional = emptyValue.nonEmpty
	/**
	  * @return Whether this property type doesn't have an "empty" value (ie. must always be specified)
	  */
	def isConcrete = emptyValue.isEmpty
	
	/**
	  * @return Code that returns a default value that may be used with this property type
	  */
	def defaultValue: CodePiece = nonEmptyDefaultValue.notEmpty.getOrElse(emptyValue)
	
	
	// IMPLEMENTED  --------------------
	
	override def toScala = scalaType
}

trait SingleColumnPropertyType extends PropertyType
{
	// ABSTRACT ------------------------
	
	/**
	  * @return The SQL data type that matches this property type
	  */
	def sqlConversion: SqlTypeConversion
	
	
	// IMPLEMENTED  --------------------
	
	override def sqlConversions = Vector(sqlConversion)
}

trait DirectlySqlConvertiblePropertyType extends SingleColumnPropertyType
{
	// ABSTRACT -----------------------
	
	/**
	  * @return The sql type this type converts to
	  */
	def sqlType: SqlPropertyType
	
	
	// IMPLEMENTED  -------------------
	
	override def sqlConversion = DirectSqlTypeConversion(this, sqlType)
	
	override def optional = this
}

/**
  * A common trait for property type implementations that are concrete (ie. not null) and can be wrapped in a
  * Scala.Option
  */
trait ConcreteSingleColumnPropertyType extends SingleColumnPropertyType
{
	// ABSTRACT ------------------------
	
	def sqlType: SqlPropertyType
	
	/**
	  * Writes code that takes a Value and outputs an instance of this type within an Option
	  * @param valueCode Reference to the value to convert
	  * @return Code that converts that value into an Option
	  */
	def optionFromValueCode(valueCode: String): CodePiece
	/**
	  * Writes code that takes an Option (which may contain an instance of this type) and yields a Value
	  * @param optionCode Reference to the option to convert
	  * @return Code that converts an Option to a Value
	  */
	def optionToValueCode(optionCode: String): CodePiece
	
	
	// IMPLEMENTED  --------------------
	
	override def sqlConversion = OptionWrappingSqlConversion
	
	override def optional = OptionWrapped
	override def concrete = this
	
	override def fromValuesCode(valuesCode: String) = {
		val instanceCode = fromValueCode("v")
		if (instanceCode.isEmpty)
			instanceCode.copy(text = valuesCode)
		else
			instanceCode.mapText { fromValue => s"$valuesCode.map { v => $fromValue }" }
	}
	
	
	// NESTED   -----------------------
	
	/**
	  * An option-wrapped version of the parent type
	  */
	object OptionWrapped extends DirectlySqlConvertiblePropertyType
	{
		override def scalaType = ScalaType.option(ConcreteSingleColumnPropertyType.this.scalaType)
		override def sqlType = ConcreteSingleColumnPropertyType.this.sqlType.copy(defaultValue = "", isNullable = true)
		
		override def yieldsTryFromValue = false
		
		override def nonEmptyDefaultValue = CodePiece.empty
		override def emptyValue = CodePiece.none
		
		override def defaultPropertyName = ConcreteSingleColumnPropertyType.this.defaultPropertyName
		
		override def concrete = ConcreteSingleColumnPropertyType.this
		
		override def writeDefaultDescription(className: Name, propName: Name) =
			ConcreteSingleColumnPropertyType.this.writeDefaultDescription(className, propName)
		
		override def fromValueCode(valueCode: String) = optionFromValueCode(valueCode)
		override def fromValuesCode(valuesCode: String) =
			fromValueCode("v").mapText { fromValue => s"$valuesCode.flatMap { v => $fromValue }" }
		
		override def toValueCode(instanceCode: String) = optionToValueCode(instanceCode)
	}
	
	object OptionWrappingSqlConversion extends SqlTypeConversion
	{
		override def origin = scalaType
		override def intermediate = OptionWrapped
		override def target = sqlType
		
		override def midConversion(originCode: String) = s"Some($originCode)"
	}
}

/**
  * Basic property types are linked with simple data types and they are not nullable
  */
sealed trait BasicPropertyType extends ConcreteSingleColumnPropertyType
{
	// ABSTRACT ------------------------------
	
	/**
	  * @return Name of the Value's property that converts to this data type (optional version, e.g. "int")
	  */
	def fromValuePropName: String
	
	
	// IMPLEMENTED  --------------------------
	
	override def yieldsTryFromValue = false
	
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
				v => v.sqlType.baseTypeSql.toLowerCase == lowerName,
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
		override val sqlType = SqlPropertyType("BIGINT")
		
		override def scalaType = ScalaType.long
		
		override def nonEmptyDefaultValue = CodePiece.empty
		override def emptyValue = CodePiece.empty
		
		override def fromValuePropName = "long"
		override def defaultPropertyName = "number"
	}
	
	/**
	  * Double property type
	  */
	case object DoubleNumber extends BasicPropertyType
	{
		override val sqlType = SqlPropertyType("DOUBLE")
		
		override def scalaType = ScalaType.double
		
		override def nonEmptyDefaultValue = CodePiece.empty
		override def emptyValue = CodePiece.empty
		
		override def fromValuePropName = "double"
		override def defaultPropertyName = "amount"
	}
	
	/**
	  * Boolean property type
	  */
	case object Bool extends BasicPropertyType
	{
		override val sqlType = SqlPropertyType("BOOLEAN", "FALSE")
		
		override def scalaType = ScalaType.boolean
		
		override def nonEmptyDefaultValue = "false"
		override def emptyValue = CodePiece.empty
		
		override def fromValuePropName = "boolean"
		override def defaultPropertyName = "flag"
	}
	
	/**
	  * Date + Time (UTC) / Instant / Datetime property type.
	  */
	case object DateTime extends BasicPropertyType
	{
		override val sqlType = SqlPropertyType("DATETIME")
		
		override def scalaType = Reference.instant
		
		override def nonEmptyDefaultValue = Reference.now.targetCode
		override def emptyValue = CodePiece.empty
		
		override def fromValuePropName = "instant"
		override def defaultPropertyName = "timestamp"
	}
	
	/**
	  * Date / LocalDate type
	  */
	case object Date extends BasicPropertyType
	{
		override val sqlType = SqlPropertyType("DATE")
		
		override def scalaType = Reference.localDate
		
		override def nonEmptyDefaultValue = Reference.today.targetCode
		override def emptyValue = CodePiece.empty
		
		override def fromValuePropName = "localDate"
		override def defaultPropertyName = "date"
	}
	
	/**
	  * Time / LocalTime type
	  */
	case object Time extends BasicPropertyType
	{
		override val sqlType = SqlPropertyType("TIME")
		
		override def scalaType = Reference.localTime
		
		override def nonEmptyDefaultValue = Reference.now.targetCode
		override def emptyValue = CodePiece.empty
		
		override def fromValuePropName = "localTime"
		override def defaultPropertyName = "time"
	}
	
	/**
	  * String / text property type with a certain length
	  * @param length Content max length (default = 255)
	  */
	case class Text(length: Int = 255) extends BasicPropertyType {
		
		override val sqlType = SqlPropertyType(s"VARCHAR($length)")
		override val emptyValue = "\"\""
		
		override def scalaType = ScalaType.string
		
		override def nonEmptyDefaultValue = CodePiece.empty
		
		override def fromValuePropName = "string"
		override def defaultPropertyName = if (length < 100) "name" else "text"
	}
	
	/**
	  * Standard integer property type
	  */
	case class IntNumber(size: IntSize = Default, maxValue: Option[Int] = None) extends BasicPropertyType
	{
		override lazy val sqlType = SqlPropertyType(maxValue match {
			case Some(max) => s"${size.toSql}(${max.toString.length})"
			case None => size.toSql
		})
		
		override def scalaType = ScalaType.int
		
		override def nonEmptyDefaultValue = CodePiece.empty
		override def emptyValue = CodePiece.empty
		
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
	
	override def scalaType = wrapped.scalaType
	override def sqlConversions = wrapped.sqlConversions
	
	override def yieldsTryFromValue = wrapped.yieldsTryFromValue
	
	override def emptyValue = wrapped.emptyValue
	override def nonEmptyDefaultValue = wrapped.nonEmptyDefaultValue
	
	override def fromValueCode(valueCode: String) = wrapped.fromValueCode(valueCode)
	override def fromValuesCode(valuesCode: String) = wrapped.fromValuesCode(valuesCode)
	override def toValueCode(instanceCode: String) = wrapped.toValueCode(instanceCode)
}

trait SingleColumnPropertyTypeWrapper extends PropertyTypeWrapper with SingleColumnPropertyType
{
	// ABSTRACT --------------------------
	
	override protected def wrapped: SingleColumnPropertyType
	
	
	// IMPLEMENTED  ----------------------
	
	override def sqlConversion = wrapped.sqlConversion
	
	override def sqlConversions = super[SingleColumnPropertyType].sqlConversions
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
					_interpret(other.afterFirst("[").untilLast("]"), length, propertyName).map { _.optional }
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
	case object CreationTime extends ConcreteSingleColumnPropertyType
	{
		override lazy val sqlType = SqlPropertyType("TIMESTAMP", "CURRENT_TIMESTAMP", indexByDefault = true)
		override lazy val defaultPropertyName = Name("created", "creationTimes", CamelCase.lower)
		
		override def scalaType = Reference.instant
		
		override def yieldsTryFromValue = false
		
		override def emptyValue = CodePiece.empty
		override def nonEmptyDefaultValue = Reference.now.targetCode
		
		override def fromValueCode(valueCode: String) = CodePiece(s"$valueCode.getInstant")
		override def toValueCode(instanceCode: String) = CodePiece(instanceCode, Set(Reference.valueConversions))
		override def optionFromValueCode(valueCode: String) = s"$valueCode.instant"
		override def optionToValueCode(optionCode: String) =
			CodePiece(optionCode, Set(Reference.valueConversions))
		override def writeDefaultDescription(className: Name, propName: Name) =
			s"Time when this $className was added to the database"
	}
	
	/**
	  * Property that always sets to the instance creation time
	  */
	case object UpdateTime extends SingleColumnPropertyTypeWrapper
	{
		override lazy val sqlConversion =
			wrapped.sqlConversion.modifyTarget(defaultValue = "CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
		override lazy val defaultPropertyName = Name("lastUpdated", "lastUpdateTimes", CamelCase.lower)
		
		override protected def wrapped = CreationTime
		
		override def optional = DateTime.optional
		override def concrete = this
		
		override def writeDefaultDescription(className: Name, propName: Name) =
			s"Time when this $className was last updated"
	}
	
	/**
	  * Property that is null until the instance is deprecated, after which the property contains a timestamp of that
	  * deprecation event
	  */
	case object Deprecation extends SingleColumnPropertyTypeWrapper
	{
		override lazy val sqlConversion = wrapped.sqlConversion.modifyTarget(indexByDefault = true)
		override lazy val defaultPropertyName = Name("deprecatedAfter", "deprecationTimes", CamelCase.lower)
		
		protected def wrapped = DateTime.OptionWrapped
		
		override def optional = this
		override def concrete = Expiration
		
		override def writeDefaultDescription(className: Name, propName: Name) =
			s"Time when this $className became deprecated. None while this $className is still valid."
	}
	
	/**
	  * Contains a time threshold for instance deprecation
	  */
	case object Expiration extends SingleColumnPropertyTypeWrapper
	{
		override lazy val sqlConversion = wrapped.sqlConversion.modifyTarget(indexByDefault = true)
		override lazy val defaultPropertyName = Name("expires", "expirationTimes", CamelCase.lower)
		
		protected def wrapped = DateTime
		
		override def optional = Deprecation
		override def concrete = this
		
		override def writeDefaultDescription(className: Name, propName: Name) =
			s"Time when this $className expires / becomes invalid"
	}
	
	/**
	  * Represents a number of days
	  */
	case object DayCount extends ConcreteSingleColumnPropertyType
	{
		override val sqlType = SqlPropertyType("INT", "0", "days")
		override lazy val nonEmptyDefaultValue = CodePiece("Days.zero", Set(Reference.days))
		
		override def yieldsTryFromValue = false
		
		override def scalaType = Reference.days
		
		override def emptyValue = CodePiece.empty
		
		override def defaultPropertyName = "duration"
		
		override def fromValueCode(valueCode: String) = CodePiece(s"Days($valueCode.getInt)", Set(Reference.days))
		override def toValueCode(instanceCode: String) =
			CodePiece(s"$instanceCode.length", Set(Reference.valueConversions))
		
		override def optionFromValueCode(valueCode: String) =
			CodePiece(s"$valueCode.int.map { Days(_) }", Set(Reference.days))
		override def optionToValueCode(optionCode: String) =
			CodePiece(s"$optionCode.map { _.length }", Set(Reference.valueConversions))
		
		override def writeDefaultDescription(className: Name, propName: Name) = ""
	}
	
	case class GenericValue(length: Int = 255) extends DirectlySqlConvertiblePropertyType
	{
		override lazy val sqlType = SqlPropertyType(s"VARCHAR($length)")
		
		override def scalaType = Reference.value
		
		override def yieldsTryFromValue = false
		
		override def nonEmptyDefaultValue = CodePiece.empty
		override def emptyValue = CodePiece("Value.empty", Set(Reference.value))
		
		override def concrete = this
		
		override def defaultPropertyName = "value"
		
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
	case class TimeDuration(unit: TimeUnit) extends ConcreteSingleColumnPropertyType
	{
		override lazy val sqlType = SqlPropertyType(unit match {
			case TimeUnit.NANOSECONDS | TimeUnit.MICROSECONDS | TimeUnit.MILLISECONDS => "BIGINT"
			case _ => "INT"
		}, "0", unit match {
			case TimeUnit.NANOSECONDS => "nanos"
			case TimeUnit.MICROSECONDS => "microseconds"
			case TimeUnit.MILLISECONDS => "millis"
			case TimeUnit.SECONDS => "seconds"
			case TimeUnit.MINUTES => "minutes"
			case TimeUnit.HOURS => "hours"
			case TimeUnit.DAYS => "days"
			case _ => unit.toString.toLowerCase
		})
		
		private def unitConversionCode = s".toUnit(TimeUnit.${unit.name})"
		
		override def scalaType = Reference.finiteDuration
		
		override def yieldsTryFromValue = false
		
		override def nonEmptyDefaultValue = CodePiece("Duration.Zero", Set(Reference.duration))
		override def emptyValue = CodePiece.empty
		
		override def defaultPropertyName = "duration"
		
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
		
		override def sqlConversions =
			super.sqlConversions.map { _.modifyTarget(indexByDefault = false) }
		
		override def optional =
			if (referencedType.isOptional) this else copy(referencedType = referencedType.optional)
		override def concrete =
			if (referencedType.isConcrete) this else copy(referencedType = referencedType.concrete)
		
		override def defaultPropertyName: Name = referencedTableName + "id"
		
		override def writeDefaultDescription(className: Name, propName: Name) =
			s"${referencedColumnName.toText.singular.capitalize} of the $referencedTableName linked with this $className"
	}
	
	/**
	  * Property type that accepts enumeration values
	  * @param enumeration Enumeration from which the values are picked
	  */
	case class EnumValue(enumeration: Enum) extends ConcreteSingleColumnPropertyType
	{
		// IMPLEMENTED  ---------------------------
		
		// Since there usually aren't a huge number of enumeration values, TINYINT is used
		override def sqlType = SqlPropertyType("TINYINT", columnNameSuffix = "id")
		override def scalaType = enumeration.reference
		
		override def yieldsTryFromValue = true
		
		override def nonEmptyDefaultValue = CodePiece.empty
		override def emptyValue = CodePiece.empty
		
		override def defaultPropertyName = enumeration.name.uncapitalize
		
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