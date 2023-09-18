package utopia.vault.coder.model.datatype

import utopia.coder.model.data.{Name, NamingRules}
import utopia.coder.model.enumeration.NamingConvention.CamelCase
import utopia.coder.model.scala.code.CodePiece
import utopia.coder.model.scala.datatype.Reference.Flow._
import utopia.coder.model.scala.datatype.Reference._
import utopia.coder.model.scala.datatype.{Reference, ScalaType}
import utopia.coder.model.scala.template.ValueConvertibleType
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.operator.EqualsExtensions._
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.controller.writer.model.EnumerationWriter
import utopia.vault.coder.model.data.{Class, Enum}
import utopia.vault.coder.model.datatype.StandardPropertyType.BasicPropertyType.{Date, DateTime, DoubleNumber, IntNumber, LongNumber}
import utopia.vault.coder.model.datatype.StandardPropertyType.TimeDuration.{fromValueReferences, toValueReferences}
import utopia.vault.coder.model.enumeration.IntSize
import utopia.vault.coder.model.enumeration.IntSize.Default
import utopia.vault.coder.util.VaultReferences._

import java.util.concurrent.TimeUnit

/**
  * Lists property types that introduced in this module
  * @author Mikko Hilpinen
  * @since 24.5.2023, v1.10
  */
object StandardPropertyType
{
	// OTHER    -------------------------------
	
	/**
	  * @param typeName     A property type name / string (case-insensitive)
	  * @param length       Associated property length (optional)
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
			case "value" | "val" => Some(GenericValue(appliedLength))
			case "model" | "values" => Some(GenericModel(appliedLength))
			case "days" => Some(DayCount)
			case "daterange" | "dates" => Some(DateRange)
			case _ =>
				if (lowerTypeName.startsWith("text") || lowerTypeName.startsWith("string") || lowerTypeName.startsWith("varchar"))
					Some(Text(appliedLength))
				else if (lowerTypeName.startsWith("option"))
					interpretGenericType(lowerTypeName, length, propertyName).map { _.optional }
				else if (lowerTypeName.startsWith("vector") ||
					(lowerTypeName.startsWith("[") && lowerTypeName.endsWith("]")))
					interpretGenericType(lowerTypeName, length, propertyName).map { t => VectorType(t, appliedLength) }
				else if (lowerTypeName.startsWith("pair"))
					interpretGenericType(lowerTypeName, length, propertyName).map(Paired)
				else if (lowerTypeName.startsWith("span") || lowerTypeName.startsWith("range")) {
					interpretGenericType(lowerTypeName, length, propertyName).map {
						case t: IntNumber => Spanning(t, isNumeric = true)
						case t => Spanning(t, isNumeric = t == DoubleNumber || t == LongNumber)
					}
				}
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
	
	// Interprets the generic type parameter. E.g. Option[String] would be interpreted as String (i.e. Text)
	private def interpretGenericType(lowerTypeName: String, length: Option[Int], propertyName: Option[Name]) =
		interpret(lowerTypeName.afterFirst("[").untilLast("]"), length, propertyName)
	
	
	// NESTED   -----------------------------
	
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
		
		/**
		  * @return The name of the DataType object used by the Value version of this type.
		  *         E.g. "StringType"
		  */
		def valueDataTypeName: String
		
		
		// IMPLEMENTED  --------------------------
		
		override def valueDataType = dataType / valueDataTypeName
		
		override def yieldsTryFromValue = false
		
		override def yieldsTryFromJsonValue: Boolean = false
		
		override def fromValueCode(valueCode: String, isFromJson: Boolean) =
			CodePiece(s"$valueCode.get${ fromValuePropName.capitalize }")
		
		override def toValueCode(instanceCode: String) = CodePiece(instanceCode, Set(valueConversions))
		
		override def toJsonValueCode(instanceCode: String): CodePiece = toValueCode(instanceCode)
		
		override def optionFromValueCode(valueCode: String, isFromJson: Boolean) = s"$valueCode.$fromValuePropName"
		
		override def optionToValueCode(optionCode: String, isToJson: Boolean) = CodePiece(optionCode, Set(valueConversions))
		
		override def writeDefaultDescription(className: Name, propName: Name)(implicit naming: NamingRules) = ""
	}
	
	object BasicPropertyType
	{
		// COMPUTED -----------------------
		
		private def objectValues = Vector(LongNumber, DoubleNumber, Bool, DateTime, Date, Time)
		
		
		// OTHER    -----------------------
		
		/**
		  * @param typeName        A property type name / string
		  * @param specifiedLength Associated property length, if specified (optional)
		  * @param propertyName    Name specified for the property (optional)
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
			override lazy val valueDataTypeName = "LongType"
			
			override def supportsDefaultJsonValues = true
			
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
			override lazy val valueDataTypeName = "DoubleType"
			
			override def supportsDefaultJsonValues = true
			
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
			override lazy val valueDataTypeName = "BooleanType"
			
			override def supportsDefaultJsonValues = true
			
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
			override lazy val valueDataTypeName = "InstantType"
			
			override def supportsDefaultJsonValues = false
			
			override def scalaType = Reference.instant
			
			override def nonEmptyDefaultValue = now.targetCode
			
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
			override lazy val valueDataTypeName = "LocalDateType"
			
			override def supportsDefaultJsonValues = false
			
			override def scalaType = Reference.localDate
			
			override def nonEmptyDefaultValue = today.targetCode
			
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
			override lazy val valueDataTypeName = "LocalTimeType"
			
			override def supportsDefaultJsonValues = false
			
			override def scalaType = Reference.localTime
			
			override def nonEmptyDefaultValue = now.targetCode
			
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
				case Some(max) => s"${ size.toSql }(${ max.toString.length })"
				case None => size.toSql
			})
			override lazy val valueDataTypeName = "IntType"
			
			override def supportsDefaultJsonValues = true
			
			override def scalaType = ScalaType.int
			
			override def nonEmptyDefaultValue = CodePiece.empty
			
			override def emptyValue = CodePiece.empty
			
			override def fromValuePropName = "int"
			
			override def defaultPropertyName = Name("index", "indices", CamelCase.lower)
		}
	}
	
	/**
	  * Property that always sets to the instance creation time
	  */
	case object CreationTime extends ConcreteSingleColumnPropertyType
	{
		override lazy val sqlType = SqlPropertyType("TIMESTAMP", "CURRENT_TIMESTAMP", indexByDefault = true)
		override lazy val defaultPropertyName = Name("created", "creationTimes", CamelCase.lower)
		
		override def valueDataType = instantType
		
		override def supportsDefaultJsonValues = false
		
		override def scalaType = Reference.instant
		
		override def yieldsTryFromValue = false
		
		override def yieldsTryFromJsonValue: Boolean = false
		
		override def emptyValue = CodePiece.empty
		
		override def nonEmptyDefaultValue = now.targetCode
		
		override def fromValueCode(valueCode: String, isFromJson: Boolean) = CodePiece(s"$valueCode.getInstant")
		
		override def toValueCode(instanceCode: String) = CodePiece(instanceCode, Set(valueConversions))
		
		override def toJsonValueCode(instanceCode: String): CodePiece = toValueCode(instanceCode)
		
		override def optionFromValueCode(valueCode: String, isFromJson: Boolean) = s"$valueCode.instant"
		
		override def optionToValueCode(optionCode: String, isToJson: Boolean) =
			CodePiece(optionCode, Set(valueConversions))
		
		override def writeDefaultDescription(className: Name, propName: Name)(implicit naming: NamingRules) =
			s"Time when this ${ className.doc } was added to the database"
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
			s"Time when this ${ className.doc } was last updated"
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
			s"Time when this ${ className.doc } became deprecated. None while this ${ className.doc } is still valid."
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
			s"Time when this ${ className.doc } expires / becomes invalid"
	}
	
	/**
	  * Represents a number of days
	  */
	case object DayCount extends ConcreteSingleColumnPropertyType
	{
		override val sqlType = SqlPropertyType("INT", "0", "days")
		override lazy val nonEmptyDefaultValue = CodePiece("Days.zero", Set(days))
		override lazy val valueDataType = dataType / "DaysType"
		
		override def supportsDefaultJsonValues = true
		
		override def yieldsTryFromValue = false
		
		override def yieldsTryFromJsonValue: Boolean = false
		
		override def scalaType = days
		
		override def emptyValue = CodePiece.empty
		
		override def defaultPropertyName = "duration"
		
		override def toValueCode(instanceCode: String) =
			CodePiece(s"$instanceCode.length", Set(valueConversions))
		
		override def toJsonValueCode(instanceCode: String): CodePiece = CodePiece(instanceCode, Set(valueConversions))
		
		override def optionToValueCode(optionCode: String, isToJson: Boolean) = {
			if (isToJson)
				CodePiece(optionCode, Set(valueConversions))
			else
				CodePiece(s"$optionCode.map { _.length }", Set(valueConversions))
		}
		
		override def fromValueCode(valueCode: String, isFromJson: Boolean) = {
			// Json uses a direct value conversion
			if (isFromJson)
				CodePiece(s"$valueCode.getDays")
			else
				CodePiece(s"Days($valueCode.getInt)", Set(days))
		}
		
		override def optionFromValueCode(valueCode: String, isFromJson: Boolean) = {
			if (isFromJson)
				CodePiece(s"$valueCode.days")
			else
				CodePiece(s"$valueCode.int.map { Days(_) }", Set(days))
		}
		
		override def writeDefaultDescription(className: Name, propName: Name)(implicit naming: NamingRules) = ""
	}
	
	object Text
	{
		private val emptyValue = "\"\""
	}
	
	// This text is never wrapped in an option. An empty string is considered an empty value.
	case class Text(length: Int = 255) extends DirectlySqlConvertiblePropertyType
	{
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
		
		override def valueDataType = stringType
		
		override def supportsDefaultJsonValues = true
		
		override def emptyValue = Text.emptyValue
		
		override def nonEmptyDefaultValue = CodePiece.empty
		
		override def defaultPropertyName = "text"
		
		override def concrete = this
		
		override def yieldsTryFromValue = false
		
		override def yieldsTryFromJsonValue: Boolean = false
		
		override def toValueCode(instanceCode: String) = CodePiece(instanceCode, Set(valueConversions))
		
		override def toJsonValueCode(instanceCode: String): CodePiece = toValueCode(instanceCode)
		
		override def fromValueCode(valueCode: String, isFromJson: Boolean) = s"$valueCode.getString"
		
		override def fromValuesCode(valuesCode: String) = s"$valuesCode.flatMap { _.string }"
		
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
		
		override def valueDataType = stringType
		
		override def sqlConversion: SqlTypeConversion = SqlConversion
		
		// Empty value is not allowed
		override def emptyValue = CodePiece.empty
		
		override def nonEmptyDefaultValue = CodePiece.empty
		
		override def supportsDefaultJsonValues = true
		
		override def defaultPropertyName = if (length < 100) "name" else "text"
		
		override def optional = allowingEmpty
		
		override def concrete = this
		
		override def yieldsTryFromValue = allowingEmpty.yieldsTryFromValue
		
		override def yieldsTryFromJsonValue: Boolean = allowingEmpty.yieldsTryFromJsonValue
		
		// Delegates value conversion
		override def fromValueCode(valueCode: String, isFromJson: Boolean) =
			allowingEmpty.fromValueCode(valueCode, isFromJson)
		
		override def fromValuesCode(valuesCode: String) = allowingEmpty.fromValuesCode(valuesCode)
		
		override def toValueCode(instanceCode: String) = allowingEmpty.toValueCode(instanceCode)
		
		override def toJsonValueCode(instanceCode: String): CodePiece = allowingEmpty.toJsonValueCode(instanceCode)
		
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
		override lazy val valueDataType = dataType / "AnyType"
		
		override def scalaType = value
		
		override def yieldsTryFromValue = false
		override def yieldsTryFromJsonValue: Boolean = false
		
		override def nonEmptyDefaultValue = CodePiece.empty
		override def emptyValue = CodePiece("Value.empty", Set(value))
		override def supportsDefaultJsonValues = true
		
		override def concrete = this
		
		override def defaultPropertyName = "value"
		
		// Converts the value to a json string before converting it back to a value
		override def toValueCode(instanceCode: String) =
			CodePiece(s"$instanceCode.mapIfNotEmpty { _.toJson }", Set(valueConversions))
		override def toJsonValueCode(instanceCode: String): CodePiece = instanceCode
		override def fromValueCode(valueCode: String, isFromJson: Boolean) = {
			// When the value originates from the database, expects it to be represented as a json string,
			// which still needs parsing
			if (isFromJson)
				valueCode
			else
				CodePiece(s"$valueCode.mapIfNotEmpty { v => JsonBunny.sureMunch(v.getString) }",
					Set(bunnyMunch.jsonBunny))
		}
		// Expects a vector of json string values
		override def fromValuesCode(valuesCode: String) =
			fromValueCode("v").mapText { fromValue => s"$valuesCode.map { v => $fromValue }" }
		
		override def writeDefaultDescription(className: Name, propName: Name)(implicit naming: NamingRules) =
			s"Generic ${ propName.doc } of this ${ className.doc }"
	}
	
	// WET WET (from GenericValue)
	case class GenericModel(length: Int = 255) extends DirectlySqlConvertiblePropertyType
	{
		override lazy val sqlType = SqlPropertyType(s"VARCHAR($length)", isNullable = true)
		override lazy val valueDataType = dataType / "ModelType"
		
		override def scalaType = model
		
		override def yieldsTryFromValue = false
		override def yieldsTryFromJsonValue: Boolean = false
		
		override def nonEmptyDefaultValue = CodePiece.empty
		override def emptyValue = CodePiece("Model.empty", Set(model))
		
		override def supportsDefaultJsonValues = true
		
		override def concrete = this
		
		override def defaultPropertyName = "values"
		
		// Converts the value to a json string before converting it back to a value
		// Empty models are not represented by json, are empty
		override def toValueCode(instanceCode: String) =
			CodePiece(s"$instanceCode.notEmpty.map { _.toJson }", Set(valueConversions))
		override def toJsonValueCode(instanceCode: String): CodePiece = instanceCode
		
		override def fromValueCode(valueCode: String, isFromJson: Boolean) = {
			// When the value originates from the database, expects it to be represented as a json string,
			// which still needs parsing
			if (isFromJson)
				s"$valueCode.getModel"
			else
				CodePiece(s"$valueCode.notEmpty match { case Some(v) => JsonBunny.sureMunch(v.getString).getModel; case None => Model.empty }",
					Set(bunnyMunch.jsonBunny, model))
		}
		// Expects a vector of json string values
		override def fromValuesCode(valuesCode: String) =
			fromValueCode("v").mapText { fromValue => s"$valuesCode.map { v => $fromValue }" }
		
		override def writeDefaultDescription(className: Name, propName: Name)(implicit naming: NamingRules) = ""
	}
	
	object TimeDuration
	{
		private val fromValueReferences = Set(Reference.timeUnit, Reference.finiteDuration)
		private val toValueReferences = Set(valueConversions, Reference.timeUnit)
		
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
		// ATTRIBUTES   ------------------------
		
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
		override lazy val valueDataType = dataType / "DurationType"
		
		
		// COMPUTED ----------------------------
		
		private def unitConversionCode = s".toUnit(TimeUnit.${ unit.name })"
		
		
		// IMPLEMENTED  ------------------------
		
		override def defaultPropertyName = "duration"
		
		override def supportsDefaultJsonValues = true
		
		override def scalaType = Reference.finiteDuration
		
		override def nonEmptyDefaultValue = CodePiece("Duration.Zero", Set(Reference.duration))
		
		override def emptyValue = CodePiece.empty
		
		override def yieldsTryFromValue = false
		
		override def yieldsTryFromJsonValue: Boolean = false
		
		override def toValueCode(instanceCode: String) =
			CodePiece(s"$instanceCode$unitConversionCode", toValueReferences)
		
		override def toJsonValueCode(instanceCode: String): CodePiece = CodePiece(instanceCode, Set(valueConversions))
		
		override def optionToValueCode(optionCode: String, isToJson: Boolean) = {
			if (isToJson)
				CodePiece(optionCode, Set(valueConversions))
			else
				CodePiece(s"$optionCode.map { _$unitConversionCode }", toValueReferences)
		}
		
		override def fromValueCode(valueCode: String, isFromJson: Boolean) = {
			if (isFromJson)
				s"$valueCode.getDuration"
			else
				CodePiece(s"FiniteDuration($valueCode.getLong, TimeUnit.${ unit.name })", fromValueReferences)
		}
		
		override def optionFromValueCode(valueCode: String, isFromJson: Boolean) = {
			if (isFromJson)
				s"$valueCode.duration"
			else
				CodePiece(s"$valueCode.long.map { FiniteDuration(_, TimeUnit.${ unit.name }) }", fromValueReferences)
		}
		
		override def writeDefaultDescription(className: Name, propName: Name)(implicit naming: NamingRules) =
			s"Duration of this ${ className.doc }"
	}
	
	/**
	  * Property that refers another class / table
	  * @param referencedTableName  Name of the referenced table
	  * @param referencedColumnName Name of the column being referred to (default = id)
	  * @param referencedType       The type of the referenced column (default = standard integer)
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
			s"${ referencedColumnName.doc.capitalize } of the ${ referencedTableName.doc } linked with this ${ className.doc }"
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
		
		private lazy val findForId = s"${ enumeration.name.enumName }.${ EnumerationWriter.findForIdName(enumeration) }"
		// private lazy val forIdName = EnumerationWriter.forIdName(enumeration)
		
		
		// IMPLEMENTED  ---------------------------
		
		private def colNameSuffix = enumeration.idPropName.column
		
		override def scalaType = enumeration.reference
		
		override def valueDataType = enumeration.idType.valueDataType
		
		override def supportsDefaultJsonValues = true
		
		override def optional: PropertyType = Optional
		
		override def concrete = this
		
		override def nonEmptyDefaultValue = enumeration.defaultValue match {
			case Some(default) =>
				val valueName = default.name.enumValue
				CodePiece(valueName, Set(enumeration.reference / valueName))
			case None => CodePiece.empty
		}
		
		override def emptyValue = CodePiece.empty
		
		override def defaultPropertyName = enumeration.name
		
		override def yieldsTryFromValue = enumeration.hasNoDefault
		
		override def yieldsTryFromJsonValue: Boolean = yieldsTryFromValue
		
		override def toValueCode(instanceCode: String) =
			enumeration.idType.toValueCode(s"$instanceCode.${ enumeration.idPropName.prop }")
		
		override def toJsonValueCode(instanceCode: String): CodePiece = toValueCode(instanceCode)
		
		// NB: Doesn't support multi-column enumeration id types
		override def fromValueCode(valueCodes: Vector[String]) =
			CodePiece(s"${ enumeration.name.enumName }.fromValue(${ valueCodes.head })", Set(enumeration.reference))
		
		override def fromJsonValueCode(valueCode: String): CodePiece = fromValueCode(Vector(valueCode))
		
		override def fromValuesCode(valuesCode: String) = {
			val idFromValueCode = enumeration.idType.fromValueCode(Vector("v"))
			idFromValueCode.mapText { convertToId =>
				s"$valuesCode.map { v => $convertToId }.flatMap($findForId)"
			}.referringTo(enumeration.reference)
		}
		
		override def writeDefaultDescription(className: Name, propName: Name)(implicit naming: NamingRules) =
			s"${ enumeration.name.doc.capitalize } of this ${ className.doc }"
		
		private object Optional extends PropertyType
		{
			private lazy val idType = enumeration.idType.optional
			override lazy val sqlConversions: Vector[SqlTypeConversion] =
				idType.sqlConversions.map { new EnumIdOptionSqlConversion(_) }
			
			override def scalaType = ScalaType.option(EnumValue.this.scalaType)
			
			override def valueDataType = EnumValue.this.valueDataType
			
			override def supportsDefaultJsonValues = false
			
			override def nonEmptyDefaultValue = CodePiece.empty
			
			override def emptyValue = CodePiece.none
			
			override def defaultPropertyName = EnumValue.this.defaultPropertyName
			
			override def optional = this
			
			override def concrete = EnumValue.this
			
			override def yieldsTryFromValue = false
			
			override def yieldsTryFromJsonValue: Boolean = false
			
			override def toValueCode(instanceCode: String) =
				idType.toValueCode(s"e.${ enumeration.idPropName.prop }")
					.mapText { idToValue => s"$instanceCode.map { e => $idToValue }.getOrElse(Value.empty)" }
					.referringTo(value)
			
			override def toJsonValueCode(instanceCode: String): CodePiece = toValueCode(instanceCode)
			
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
			
			override def fromJsonValueCode(valueCode: String): CodePiece = fromValueCode(Vector(valueCode))
			
			override def fromValuesCode(valuesCode: String) =
				idType.fromValuesCode(valuesCode)
					.mapText { ids => s"$ids.flatMap($findForId)" }
					.referringTo(enumeration.reference)
			
			override def writeDefaultDescription(className: Name, propName: Name)(implicit naming: NamingRules) =
				EnumValue.this.writeDefaultDescription(className, propName)
			
			private class EnumIdOptionSqlConversion(idConversion: SqlTypeConversion) extends SqlTypeConversion
			{
				override lazy val target = idConversion.target.copy(columnNameSuffix = colNameSuffix)
				
				override def origin = scalaType
				
				override def intermediate = idConversion.intermediate
				
				override def midConversion(originCode: String) =
					idConversion.midConversion(s"e.${ enumeration.idPropName.prop }")
						.mapText { fromId => s"$originCode.map { e => $fromId }" }
			}
		}
		
		private class EnumIdSqlConversion(idConversion: SqlTypeConversion) extends SqlTypeConversion
		{
			override lazy val target = idConversion.target.copy(columnNameSuffix = colNameSuffix)
			
			override def origin = scalaType
			
			override def intermediate = idConversion.intermediate
			
			override def midConversion(originCode: String) =
				idConversion.midConversion(s"$originCode.${ enumeration.idPropName.prop }")
		}
	}
	
	/**
	  * A data type which yields Vectors of a specific type
	  * @param innerType    The type of individual items in this vector
	  * @param columnLength The maximum database column length used when storing this vector. Default = 255.
	  */
	case class VectorType(innerType: PropertyType, columnLength: Int = 255) extends DirectlySqlConvertiblePropertyType
	{
		// ATTRIBUTES   --------------------------
		
		override val scalaType = ScalaType.basic("Vector")(innerType.scalaType)
		override val valueDataType = dataType / "VectorType"
		override val sqlType: SqlPropertyType = {
			// WET WET (from Text)
			val typeName = {
				if (columnLength > 16777215)
					"LONGTEXT"
				else if (columnLength > 65535)
					"MEDIUMTEXT"
				else
					s"VARCHAR($columnLength)"
			}
			SqlPropertyType(typeName, isNullable = true)
		}
		
		
		// IMPLEMENTED  ---------------------------
		
		override def defaultPropertyName = "values"
		
		override def emptyValue = CodePiece("Vector.empty")
		
		override def nonEmptyDefaultValue = CodePiece.empty
		
		override def concrete = this
		
		override def yieldsTryFromValue = innerType.yieldsTryFromJsonValue
		
		override def yieldsTryFromJsonValue = innerType.yieldsTryFromJsonValue
		
		override def supportsDefaultJsonValues = true
		
		// Converts to json when storing to DB
		// Empty vectors are treated as empty values
		override def toValueCode(instanceCode: String) = {
			innerType.toJsonValueCode("v").mapText { itemToValue =>
				s"NotEmpty($instanceCode) match { case Some(v) => ((v.map[Value] { v => $itemToValue }: Value).toJson): Value; case None => Value.empty }"
			}.referringTo(Vector(valueConversions, notEmpty, value))
		}
		
		override def toJsonValueCode(instanceCode: String) =
			innerType.toJsonValueCode("v").mapText { itemToValue =>
				s"$instanceCode.map[Value] { v => $itemToValue }"
			}.referringTo(valueConversions)
		
		override def fromValueCode(valueCode: String, isFromJson: Boolean) = {
			// Case: Parsing from json => Converts directly to a vector
			if (isFromJson) {
				// Case: Item parsing may fail => Vector parsing may fail as well
				if (yieldsTryFromJsonValue)
					innerType.fromJsonValueCode("v").mapText { itemFromValue =>
						s"$valueCode.tryVectorWith { v => $itemFromValue }"
					}
				// Case: Item parsing always succeeds => Simply maps each item
				else
					innerType.fromJsonValueCode("v").mapText { itemFromValue =>
						s"$valueCode.getVector.map { v => $itemFromValue }"
					}
			}
			// Case: Parsing from a database model => Has to first parse the json into a vector
			else {
				// Case: Item parsing may fail => Vector and json parsing may fail as well
				if (yieldsTryFromValue)
					innerType.fromJsonValueCode("v").mapText { itemFromValue =>
						s"$valueCode.notEmpty match { case Some(v) => JsonBunny.munch(v.getString).flatMap { v => v.tryVectorWith { v => $itemFromValue } }; case None => Success(Vector.empty) }"
					}.referringTo(Vector(bunnyMunch.jsonBunny, success))
				// Case: Item parsing always succeeds => Simply maps the parsed vector
				else {
					innerType.fromJsonValueCode("v").mapText { itemFromValue =>
						s"$valueCode.notEmpty match { case Some(v) => JsonBunny.sureMunch(v.getString).getVector.map { v => $itemFromValue }; case None => Vector.empty }"
					}.referringTo(bunnyMunch.jsonBunny)
				}
			}
		}
		
		override def fromValuesCode(valuesCode: String) =
			innerType.fromJsonValueCode("v").mapText { itemFromValue =>
				// Case: Individual item parsing may fail => Ignores failures because has to succeed
				if (innerType.yieldsTryFromJsonValue)
					s"$valuesCode.flatMap { v => $itemFromValue.toOption }"
				// Case: Individual item parsing always succeeds => Simply maps the input vector
				else
					s"$valuesCode.map { v => $itemFromValue }"
			}
		
		override def writeDefaultDescription(className: Name, propName: Name)(implicit naming: NamingRules) = ""
	}
	
	abstract class PairLikeType(innerType: PropertyType, pairType: Reference, propertyNames: Pair[String],
	                            singleValueConstructorName: String)
		extends PropertyType
	{
		// ATTRIBUTES   ----------------------
		
		override lazy val scalaType: ScalaType = pairType(innerType.scalaType)
		
		override lazy val sqlConversions: Vector[SqlTypeConversion] =
			propertyNames.flatMap { propName => innerType.sqlConversions.map { ElementConversion(propName, _) } }
				.toVector
		
		override lazy val nonEmptyDefaultValue: CodePiece =
			innerType.nonEmptyDefaultValue.mapIfNotEmpty {
				_.mapText { innerDefault =>
					s"${ pairType.target }.$singleValueConstructorName($innerDefault)"
				}.referringTo(pairType)
			}
		
		
		// ABSTRACT --------------------------
		
		protected def toPairCode(instanceCode: String): CodePiece
		protected def fromPairCode(pairCode: String): CodePiece
		
		
		// IMPLEMENTED  ----------------------
		
		override def valueDataType: Reference = dataType/"PairType"
		
		override def defaultPropertyName: Name = {
			val inner = innerType.defaultPropertyName
			Name(inner.plural, inner.plural, inner.style)
		}
		
		// TODO: This type doesn't support option-wrapping at this time - Add when needed
		override def optional: PropertyType = this
		override def concrete: PropertyType = this
		
		override def supportsDefaultJsonValues: Boolean = true
		
		override def emptyValue: CodePiece = CodePiece.empty
		
		override def yieldsTryFromValue: Boolean = innerType.yieldsTryFromValue
		override def yieldsTryFromJsonValue: Boolean = innerType.yieldsTryFromJsonValue
		
		override def toValueCode(instanceCode: String): CodePiece = toValueCode(instanceCode, isForJson = false)
		override def toJsonValueCode(instanceCode: String): CodePiece = toValueCode(instanceCode, isForJson = true)
		
		override def fromValueCode(valueCodes: Vector[String]): CodePiece = {
			// Spits the input values into two parts
			val codesPerPart = valueCodes.size / 2
			val splitValueCodes = Pair(valueCodes.take(codesPerPart), valueCodes.drop(codesPerPart))
			// Forms each part from the available values
			splitValueCodes.map(innerType.fromValueCode).merge { (firstPartCode, secondPartCode) =>
				val codeText = {
					// Case: Parts are provided as instances of Try => uses flatMap
					if (innerType.yieldsTryFromValue)
						s"$firstPartCode.flatMap { v1 => $secondPartCode.map { v2 => ${pairType.target}(v1, v2) } }"
					// Case: Parts are available directly => Wraps them
					else
						s"${pairType.target}($firstPartCode, $secondPartCode)"
				}
				CodePiece(codeText, firstPartCode.references ++ secondPartCode.references + pairType)
			}
		}
		override def fromJsonValueCode(valueCode: String): CodePiece = {
			// Converts the value into a pair of values and
			// converts the values into correct types
			val pairCode = innerType.fromJsonValueCode("v").mapText { innerFromValue =>
				if (innerType.yieldsTryFromJsonValue)
					s"$valueCode.tryPairWith { v => $innerFromValue }"
				else
					s"$valueCode.getPair.map { v => $innerFromValue }"
			}
			// Converts the pair into the correct type
			fromPairCode(pairCode.text).referringTo(pairCode.references)
		}
		override def fromValuesCode(valuesCode: String): CodePiece = fromJsonValueCode("v").mapText { fromValue =>
			if (yieldsTryFromJsonValue)
				s"$valuesCode.flatMap { v => $fromValue.toOption }"
			else
				s"$valuesCode.map { v => $fromValue }"
		}
		
		override def writeDefaultDescription(className: Name, propName: Name)(implicit naming: NamingRules): String = ""
		
		
		// OTHER    ----------------------------
		
		private def innerToValue(instanceCode: String, isForJson: Boolean) =
			if (isForJson) innerType.toJsonValueCode(instanceCode) else innerType.toValueCode(instanceCode)
		
		private def toValueCode(instanceCode: String, isForJson: Boolean) = {
			val toPair = toPairCode(instanceCode)
			innerToValue("v", isForJson).mapText { innerToValue =>
				s"$toPair.map[Value] { v => $innerToValue }"
			}.referringTo(toPair.references + valueConversions)
		}
		
		
		// NESTED   ----------------------------
		
		// Converts a single element (e.g. the first element of a pair)
		// For wrapped multi-column types, converts a single element of a single element
		// (e.g. the first part of the first element of a pair)
		private case class ElementConversion(propertyName: String, innerConversion: SqlTypeConversion)
			extends SqlTypeConversion
		{
			override def origin: ScalaType = pairType
			override def intermediate: ValueConvertibleType = innerConversion.intermediate
			override def target: SqlPropertyType = innerConversion.target
			
			override def midConversion(originCode: String): CodePiece =
				innerConversion.midConversion(s"$originCode.$propertyName").referringTo(pairType)
		}
	}
	case class Paired(innerType: PropertyType) extends PairLikeType(innerType, pair, Pair("first", "second"), "twice")
	{
		override protected def toPairCode(instanceCode: String): CodePiece = instanceCode
		override protected def fromPairCode(pairCode: String): CodePiece = pairCode
	}
	case class Spanning(innerType: PropertyType, isNumeric: Boolean = false)
		extends PairLikeType(innerType, if (isNumeric) numericSpan else span,
			Pair("start", "end"), "singleValue")
	{
		private val reference = if (isNumeric) numericSpan else span
		
		override protected def toPairCode(instanceCode: String): CodePiece = s"$instanceCode.toPair"
		override protected def fromPairCode(pairCode: String): CodePiece = {
			CodePiece(s"${reference.target}($pairCode)", Set(reference))
		}
	}
	object DateRange extends PairLikeType(Date, dateRange, Pair("start", "end"), "single")
	{
		override protected def toPairCode(instanceCode: String): CodePiece = s"$instanceCode.toPair"
		override protected def fromPairCode(pairCode: String): CodePiece =
			CodePiece(s"${dateRange.target}.exclusive($pairCode)", Set(dateRange))
	}
}
