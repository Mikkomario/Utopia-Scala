package utopia.vault.coder.model.datatype

import utopia.flow.generic.ValueConversions._
import utopia.flow.operator.EqualsExtensions._
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.controller.writer.model.EnumerationWriter
import utopia.vault.coder.model.data.{Class, Enum, Name, NamingRules}
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
	  * Writes a code that reads this an instance of this type from a value or a sequence of values
	  * (which still represent a single instance).
	  * @param valueCodes Code for accessing the parameter values. The number of proposed values must match the number
	  *                   of parts or components used by this type.
	  * @return Code for accessing a value and converting it to this type (in scala)
	  */
	def fromValueCode(valueCodes: Vector[String]): CodePiece
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
	def writeDefaultDescription(className: Name, propName: Name)(implicit naming: NamingRules): String
	
	
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
	  * @return Conversion used for converting this type into an sql type
	  */
	def sqlConversion: SqlTypeConversion
	
	/**
	  * Writes a code that reads this an instance of this type from a value.
	  * @param valueCode Code for accessing a value
	  * @return Code for accessing a value and converting it to this type (in scala)
	  */
	def fromValueCode(valueCode: String): CodePiece
	
	
	// IMPLEMENTED  --------------------
	
	override def sqlConversions = Vector(sqlConversion)
	
	override def fromValueCode(valueCodes: Vector[String]): CodePiece = valueCodes.headOption match {
		case Some(valueCode) => fromValueCode(valueCode)
		case None => emptyValue
	}
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
		
		override def writeDefaultDescription(className: Name, propName: Name)(implicit naming: NamingRules) =
			ConcreteSingleColumnPropertyType.this.writeDefaultDescription(className, propName)
		
		override def fromValueCode(valueCode: String) = optionFromValueCode(valueCode)
		override def fromValuesCode(valuesCode: String) =
			fromValueCode("v").mapText { fromValue => s"$valuesCode.flatMap { v => $fromValue }" }
		
		override def toValueCode(instanceCode: String) = optionToValueCode(instanceCode)
		
		override def toString = s"Option[$concrete]"
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
	
	override def writeDefaultDescription(className: Name, propName: Name)(implicit naming: NamingRules) = ""
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
	def interpret(typeName: String, specifiedLength: Option[Int] = None, propertyName: Option[Name] = None) =
	{
		val lowerName = typeName
		def _findWith(searches: Iterable[BasicPropertyType => Boolean]) =
			searches.findMap { search => objectValues.find(search) }
		
		if (lowerName.contains("int")) {
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
				propertyName.flatMap { name =>
					val lowerName = name.singularIn(CamelCase.lower).toLowerCase
					if (lowerName.startsWith("is") || lowerName.startsWith("was"))
						Some(Bool)
					else
						objectValues.filter { _.defaultPropertyName ~== name }
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
	
	/*
	case class Text(length: Int = 255) extends BasicPropertyType {
		
		override val sqlType = SqlPropertyType(s"VARCHAR($length)")
		override val emptyValue = "\"\""
		
		override def scalaType = ScalaType.string
		
		override def nonEmptyDefaultValue = CodePiece.empty
		
		override def fromValuePropName = "string"
		override def defaultPropertyName = if (length < 100) "name" else "text"
	}*/
	
	object IntNumber
	{
		/**
		  * Creates a new int type with a specific maximum value
		  * @param maxValue Maximum integer value
		  * @return Type incorporating that maximum value
		  */
		def apply(maxValue: Int): IntNumber = apply(IntSize.fitting(maxValue).getOrElse(Default), Some(maxValue))
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
	
	override def fromValueCode(valueCodes: Vector[String]) = wrapped.fromValueCode(valueCodes)
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
	
	override def fromValueCode(valueCode: String) = wrapped.fromValueCode(valueCode)
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
	def interpret(typeName: String, length: Option[Int] = None, propertyName: Option[Name] = None): Option[PropertyType] =
	{
		// Text length may be specified within parentheses after the type (E.g. "String(3)")
		def appliedLength = typeName.afterFirst("(").untilFirst(")").int.orElse(length).getOrElse(255)
		
		val lowerTypeName = typeName.toLowerCase
		lowerTypeName.untilFirst("(") match {
			case "requiredstring" | "nonemptystring" | "stringnotempty" | "textnotempty" =>
				Some(NonEmptyText(appliedLength))
			case "creation" | "created" => Some(CreationTime)
			case "updated" | "modification" | "update" => Some(UpdateTime)
			case "deprecation" | "deprecated" => Some(Deprecation)
			case "expiration" | "expired" => Some(Expiration)
			case "value" => Some(GenericValue(appliedLength))
			case "days" => Some(DayCount)
			case _ =>
				if (lowerTypeName.startsWith("text") || lowerTypeName.startsWith("string") || lowerTypeName.startsWith("varchar"))
					Some(Text(appliedLength))
				else if (lowerTypeName.startsWith("option"))
					interpret(lowerTypeName.afterFirst("[").untilLast("]"), length, propertyName).map { _.optional }
				else if (lowerTypeName.startsWith("duration"))
					Some(typeName.afterFirst("[").untilLast("]") match {
						case "s" | "second" | "seconds" => TimeDuration.seconds
						case "m" | "min" | "minute" | "minutes" => TimeDuration.minutes
						case "h" | "hour" | "hours" => TimeDuration.hours
						case _ => TimeDuration.millis
					})
				else
					BasicPropertyType.interpret(lowerTypeName, length, propertyName)
						.orElse {
							// If nothing else works, attempts to find the match with the specified property name
							propertyName.flatMap { name =>
								val options = Vector(Text(length.getOrElse(255)), CreationTime, UpdateTime,
									Deprecation, Expiration,
									GenericValue(length.getOrElse(255)), DayCount) ++ TimeDuration.values
								options.filter { _.defaultPropertyName ~== name }
									.maxByOption { _.defaultPropertyName.singular.length }
							}
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
		override def writeDefaultDescription(className: Name, propName: Name)(implicit naming: NamingRules) =
			s"Time when this ${className.doc} was added to the database"
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
		
		override def writeDefaultDescription(className: Name, propName: Name)(implicit naming: NamingRules) =
			s"Time when this ${className.doc} was last updated"
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
		
		override def writeDefaultDescription(className: Name, propName: Name)(implicit naming: NamingRules) =
			s"Time when this ${className.doc} became deprecated. None while this ${className.doc} is still valid."
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
		
		override def writeDefaultDescription(className: Name, propName: Name)(implicit naming: NamingRules) =
			s"Time when this ${className.doc} expires / becomes invalid"
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
		
		override def writeDefaultDescription(className: Name, propName: Name)(implicit naming: NamingRules) = ""
	}
	
	object Text
	{
		private val emptyValue = "\"\""
	}
	
	// This text is never wrapped in an option. An empty string is considered an empty value.
	case class Text(length: Int = 255) extends DirectlySqlConvertiblePropertyType {
		
		override val sqlType = {
			val typeName = {
				if (length > 16777215)
					"LONGTEXT"
				else if (length > 65535)
					"MEDIUMTEXT"
				else
					s"VARCHAR($length)"
			}
			SqlPropertyType(typeName, isNullable = true)
		}
		
		override def scalaType = ScalaType.string
		
		override def emptyValue = Text.emptyValue
		override def nonEmptyDefaultValue = CodePiece.empty
		
		override def defaultPropertyName = "text"
		
		override def concrete = this
		
		override def yieldsTryFromValue = false
		override def fromValueCode(valueCode: String) = s"$valueCode.getString"
		override def fromValuesCode(valuesCode: String) = s"$valuesCode.flatMap { _.string }"
		override def toValueCode(instanceCode: String) = CodePiece(instanceCode, Set(Reference.valueConversions))
		
		override def writeDefaultDescription(className: Name, propName: Name)(implicit naming: NamingRules) = ""
	}
	
	// Works exactly like Text, except that
	// a) No default empty value is given (this type is not considered optional)
	// b) NOT NULL is added to the generated sql type
	case class NonEmptyText(length: Int = 255) extends SingleColumnPropertyType
	{
		// ATTRIBUTES   ------------------------
		
		private val allowingEmpty = Text(length)
		
		
		// IMPLEMENTED  ------------------------
		
		override def scalaType = allowingEmpty.scalaType
		override def sqlConversion: SqlTypeConversion = SqlConversion
		
		// Empty value is not allowed
		override def emptyValue = CodePiece.empty
		override def nonEmptyDefaultValue = CodePiece.empty
		
		override def defaultPropertyName =  if (length < 100) "name" else "text"
		
		override def optional = allowingEmpty
		override def concrete = this
		
		override def yieldsTryFromValue = allowingEmpty.yieldsTryFromValue
		
		// Delegates value conversion
		override def fromValueCode(valueCode: String) = allowingEmpty.fromValueCode(valueCode)
		override def fromValuesCode(valuesCode: String) = allowingEmpty.fromValuesCode(valuesCode)
		override def toValueCode(instanceCode: String) = allowingEmpty.toValueCode(instanceCode)
		
		override def writeDefaultDescription(className: Name, propName: Name)(implicit naming: NamingRules) = ""
		
		
		// NESTED   -------------------------
		
		// Simply modifies the "NOT NULL" -part from a standard string sql conversion, otherwise works exactly the same
		private object SqlConversion extends SqlTypeConversion
		{
			// COMPUTED ---------------------
			
			private def parent = NonEmptyText.this
			
			
			// IMPLEMENTED  -----------------
			
			override def origin = parent.scalaType
			override def intermediate = parent.allowingEmpty
			override def target = intermediate.sqlType.notNullable
			
			override def midConversion(originCode: String) = originCode
		}
	}
	
	case class GenericValue(length: Int = 255) extends DirectlySqlConvertiblePropertyType
	{
		override lazy val sqlType = SqlPropertyType(s"VARCHAR($length)", isNullable = true)
		
		override def scalaType = Reference.value
		
		override def yieldsTryFromValue = false
		
		override def nonEmptyDefaultValue = CodePiece.empty
		override def emptyValue = CodePiece("Value.empty", Set(Reference.value))
		
		override def concrete = this
		
		override def defaultPropertyName = "value"
		
		override def fromValueCode(valueCode: String) = valueCode
		override def fromValuesCode(valuesCode: String) = valuesCode
		override def toValueCode(instanceCode: String) = instanceCode
		
		override def writeDefaultDescription(className: Name, propName: Name)(implicit naming: NamingRules) =
			s"Generic ${propName.doc} of this ${className.doc}"
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
		
		override def writeDefaultDescription(className: Name, propName: Name)(implicit naming: NamingRules) =
			s"Duration of this ${className.doc}"
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
		
		override def writeDefaultDescription(className: Name, propName: Name)(implicit naming: NamingRules) =
			s"${referencedColumnName.doc.capitalize} of the ${referencedTableName.doc} linked with this ${className.doc}"
	}
	
	/**
	  * Property type that accepts enumeration values
	  * @param enumeration Enumeration from which the values are picked
	  */
	case class EnumValue(enumeration: Enum)(implicit naming: NamingRules) extends PropertyType
	{
		// ATTRIBUTES   ----------------------------
		
		override lazy val sqlConversions: Vector[SqlTypeConversion] =
			enumeration.idType.sqlConversions.map { new EnumIdSqlConversion(_) }
		
		private lazy val findForId = s"${enumeration.name.enumName}.${EnumerationWriter.findForIdName(enumeration)}"
		// private lazy val forIdName = EnumerationWriter.forIdName(enumeration)
		
		
		// IMPLEMENTED  ---------------------------
		
		private def colNameSuffix = enumeration.idPropName.column
		
		override def scalaType = enumeration.reference
		
		override def optional: PropertyType = Optional
		override def concrete = this
		
		override def nonEmptyDefaultValue = enumeration.defaultValue match {
			case Some(default) =>
				val valueName = default.name.enumValue
				CodePiece(valueName, Set(enumeration.reference/valueName))
			case None => CodePiece.empty
		}
		override def emptyValue = CodePiece.empty
		
		override def defaultPropertyName = enumeration.name
		
		override def yieldsTryFromValue = enumeration.hasNoDefault
		// NB: Doesn't support multi-column enumeration id types
		override def fromValueCode(valueCodes: Vector[String]) =
			CodePiece(s"${enumeration.name.enumName}.fromValue(${valueCodes.head})", Set(enumeration.reference))
		override def fromValuesCode(valuesCode: String) = {
			val idFromValueCode = enumeration.idType.fromValueCode(Vector("v"))
			idFromValueCode.mapText { convertToId =>
				s"$valuesCode.map { v => $convertToId }.flatMap($findForId)"
			}.referringTo(enumeration.reference)
		}
		override def toValueCode(instanceCode: String) =
			enumeration.idType.toValueCode(s"$instanceCode.${enumeration.idPropName.prop}")
		
		override def writeDefaultDescription(className: Name, propName: Name)(implicit naming: NamingRules) =
			s"${enumeration.name.doc.capitalize} of this ${className.doc}"
		
		private object Optional extends PropertyType
		{
			private lazy val idType = enumeration.idType.optional
			override lazy val sqlConversions: Vector[SqlTypeConversion] =
				idType.sqlConversions.map { new EnumIdOptionSqlConversion(_) }
			
			override def scalaType = ScalaType.option(EnumValue.this.scalaType)
			
			override def nonEmptyDefaultValue = CodePiece.empty
			override def emptyValue = CodePiece.none
			
			override def defaultPropertyName = EnumValue.this.defaultPropertyName
			
			override def optional = this
			override def concrete = EnumValue.this
			
			override def yieldsTryFromValue = false
			
			override def fromValueCode(valueCodes: Vector[String]) =
				idType.fromValueCode(valueCodes)
					.mapText { id =>
						// Types which are at the same time concrete and non-concrete, are handled a bit differently
						def fromConcrete = s"$findForId($id)"
						idType match {
							case Text(_) => fromConcrete
							case NonEmptyText(_) => fromConcrete
							case GenericValue(_) => fromConcrete
							case t =>
								if (t.concrete == t)
									fromConcrete
								else
									s"$id.flatMap($findForId)"
						}
					}
					.referringTo(enumeration.reference)
			override def fromValuesCode(valuesCode: String) =
				idType.fromValuesCode(valuesCode)
					.mapText { ids => s"$ids.flatMap($findForId)" }
					.referringTo(enumeration.reference)
			override def toValueCode(instanceCode: String) =
				idType.toValueCode(s"e.${enumeration.idPropName.prop}")
					.mapText { idToValue => s"$instanceCode.map { e => $idToValue }.getOrElse(Value.empty)" }
					.referringTo(Reference.value)
			
			override def writeDefaultDescription(className: Name, propName: Name)(implicit naming: NamingRules) =
				EnumValue.this.writeDefaultDescription(className, propName)
			
			private class EnumIdOptionSqlConversion(idConversion: SqlTypeConversion) extends SqlTypeConversion
			{
				override lazy val target = idConversion.target.copy(columnNameSuffix = colNameSuffix)
				
				override def origin = scalaType
				override def intermediate = idConversion.intermediate
				
				override def midConversion(originCode: String) =
					idConversion.midConversion(s"e.${enumeration.idPropName.prop}")
						.mapText { fromId => s"$originCode.map { e => $fromId }" }
			}
		}
		
		private class EnumIdSqlConversion(idConversion: SqlTypeConversion) extends SqlTypeConversion
		{
			override lazy val target = idConversion.target.copy(columnNameSuffix = colNameSuffix)
			
			override def origin = scalaType
			override def intermediate = idConversion.intermediate
			
			override def midConversion(originCode: String) =
				idConversion.midConversion(s"$originCode.${enumeration.idPropName.prop}")
		}
	}
}
