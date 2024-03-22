package utopia.vault.coder.model.datatype

import utopia.coder.model.data.{Name, NamingRules}
import utopia.coder.model.scala.code.CodePiece
import utopia.coder.model.scala.datatype.{Reference, ScalaType}
import utopia.coder.model.scala.template.{ScalaTypeConvertible, ValueConvertibleType}

/**
  * A common trait for property types which support both nullable (optional) and non-nullable (concrete) variants
  * @author Mikko Hilpinen
  * @since 29.8.2021, v0.1
  */
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
	  * @return Names used for multi-column parts of this type when no other names have been specified, if applicable.
	  */
	def defaultPartNames: Seq[Name]
	
	/**
	  * @return Whether the conversion from a Value may fail, meaning that fromValueCode
	  *         yields instances of Try instead of instances of this type.
	  */
	def yieldsTryFromValue: Boolean
	/**
	  * @return Whether the conversion from a json-originated Value may fail.
	  *         If true, the code generated through .fromJsonValueCode(...) yields an instance of Try.
	  */
	def yieldsTryFromJsonValue: Boolean
	
	/**
	  * @return An optional copy of this property type (one that accepts None or other such empty value)
	  */
	def optional: PropertyType
	/**
	  * @return A non-optional (ie. concrete) version of this data type
	  */
	def concrete: PropertyType
	
	/**
	  * @return Reference to the data type matching the Value generated by this type.
	  *         For example, if this.toValue yields a string Value, StringType
	  *         (from utopia.flow.generic.model.mutable.DataType) would be returned.
	  */
	def valueDataType: Reference
	/**
	  * @return Whether this data type supports the usage of default values when parsing data from json
	  */
	def supportsDefaultJsonValues: Boolean
	
	/**
	  * @param instanceCode Code that represents the instance of this data type to convert to a value
	  * @return Code that converts an instance of this type into a value that's suitable for json generation.
	  */
	def toJsonValueCode(instanceCode: String): CodePiece
	
	/**
	  * Writes a code that reads an instance of this type from a value or a sequence of values
	  * (which still represent a single instance).
	  *
	  * If 'yieldsTryFromValue' is true, the resulting code will yield a Try.
	  * Otherwise the resulting code will yield a direct instance of this type.
	  *
	  * @param valueCodes Code for accessing the parameter values. The number of proposed values must match the number
	  *                   of parts or components used by this type.
	  * @return Code for accessing a value and converting it to this type (in Scala)
	  */
	def fromValueCode(valueCodes: Vector[String]): CodePiece
	/**
	  * Writes a code that reads a vector of instances of this type from a vector of values
	  * @param valuesCode Code that returns a vector of values
	  * @return Code for accessing the specified values and converting them to a vector of this type's instances in Scala
	  */
	def fromValuesCode(valuesCode: String): CodePiece
	/**
	  * Writes code that parses an instance of this type from a value acquired through json parsing
	  * @param valueCode Code for accessing the value acquired through json parsing
	  * @return Code that parses an instance of this type (or an instance of Try) from the specified value.
	  */
	def fromJsonValueCode(valueCode: String): CodePiece
	/**
	  * Writes code that accepts a concrete instance of this data type (acquired using .concrete)
	  * and returns a type of this data type.
	  * E.g. If this data type is option-wrapped, this would return Some(concreteValueCode).
	  * If this type is concrete, may simply return the specified code without modifications.
	  * @param concreteCode Code that refers to the concrete value
	  * @return Code that converts the specified concrete instance into this type
	  */
	def fromConcreteCode(concreteCode: String): CodePiece
	
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

/**
  * Common trait for data types that may be represented using a single database column
  */
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
	  * @param isFromJson Whether the specified value code represents a value parsed from json (default = false)
	  * @return Code for accessing a value and converting it to this type (in scala)
	  */
	def fromValueCode(valueCode: String, isFromJson: Boolean = false): CodePiece
	
	
	// IMPLEMENTED  --------------------
	
	override def sqlConversions = Vector(sqlConversion)
	
	override def defaultPartNames: Vector[Name] = Vector()
	
	override def fromValueCode(valueCodes: Vector[String]): CodePiece = valueCodes.headOption match {
		case Some(valueCode) => fromValueCode(valueCode)
		case None => emptyValue
	}
	override def fromJsonValueCode(valueCode: String): CodePiece = fromValueCode(valueCode, isFromJson = true)
}

/**
  * Common trait for data types that don't have an intermediate state (in a database model) before they are converted
  * to an sql-compatible value.
  *
  * Please note that wrapping an item in an Option is considered an intermediate state in this context.
  */
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
	  * @param isFromJson Whether the specified value code represents a json-parsed value (default = false)
	  * @return Code that converts that value into an Option
	  */
	def optionFromValueCode(valueCode: String, isFromJson: Boolean = false): CodePiece
	/**
	  * Writes code that takes an Option (which may contain an instance of this type) and yields a Value
	  * @param optionCode Reference to the option to convert
	  * @param isToJson Whether the generated code is used in a setting where the value is then written to json
	  *                 (default = false)
	  * @return Code that converts an Option to a Value
	  */
	def optionToValueCode(optionCode: String, isToJson: Boolean = false): CodePiece
	
	
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
	override def fromConcreteCode(concreteCode: String): CodePiece = concreteCode
	
	
	// NESTED   -----------------------
	
	/**
	  * An option-wrapped version of the parent type
	  */
	object OptionWrapped extends DirectlySqlConvertiblePropertyType
	{
		override def scalaType = ScalaType.option(ConcreteSingleColumnPropertyType.this.scalaType)
		override def sqlType = ConcreteSingleColumnPropertyType.this.sqlType.copy(defaultValue = "", isNullable = true)
		
		override def yieldsTryFromValue = false
		override def yieldsTryFromJsonValue: Boolean = false
		
		override def nonEmptyDefaultValue = CodePiece.empty
		override def emptyValue = CodePiece.none
		
		override def defaultPropertyName = ConcreteSingleColumnPropertyType.this.defaultPropertyName
		
		override def concrete = ConcreteSingleColumnPropertyType.this
		
		override def valueDataType = ConcreteSingleColumnPropertyType.this.valueDataType
		override def supportsDefaultJsonValues = false
		
		override def writeDefaultDescription(className: Name, propName: Name)(implicit naming: NamingRules) =
			ConcreteSingleColumnPropertyType.this.writeDefaultDescription(className, propName)
		
		override def fromValueCode(valueCode: String, isFromJson: Boolean) =
			optionFromValueCode(valueCode, isFromJson)
		override def fromValuesCode(valuesCode: String) =
			fromValueCode("v").mapText { fromValue => s"$valuesCode.flatMap { v => $fromValue }" }
		override def fromConcreteCode(concreteCode: String): CodePiece = s"Some($concreteCode)"
		
		override def toValueCode(instanceCode: String) = optionToValueCode(instanceCode)
		override def toJsonValueCode(instanceCode: String): CodePiece = optionToValueCode(instanceCode, isToJson = true)
		
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
	override def valueDataType = wrapped.valueDataType
	override def supportsDefaultJsonValues = wrapped.supportsDefaultJsonValues
	override def sqlConversions = wrapped.sqlConversions
	
	override def yieldsTryFromValue = wrapped.yieldsTryFromValue
	override def yieldsTryFromJsonValue: Boolean = wrapped.yieldsTryFromJsonValue
	
	override def emptyValue = wrapped.emptyValue
	override def nonEmptyDefaultValue = wrapped.nonEmptyDefaultValue
	
	override def toValueCode(instanceCode: String) = wrapped.toValueCode(instanceCode)
	override def toJsonValueCode(instanceCode: String): CodePiece = wrapped.toJsonValueCode(instanceCode)
	
	override def fromValueCode(valueCodes: Vector[String]) = wrapped.fromValueCode(valueCodes)
	override def fromValuesCode(valuesCode: String) = wrapped.fromValuesCode(valuesCode)
	override def fromJsonValueCode(valueCode: String): CodePiece = wrapped.fromJsonValueCode(valueCode)
	override def fromConcreteCode(concreteCode: String): CodePiece = wrapped.fromConcreteCode(concreteCode)
}

/**
  * Common trait for properties which are based on another, single column property type
  */
trait SingleColumnPropertyTypeWrapper extends PropertyTypeWrapper with SingleColumnPropertyType
{
	// ABSTRACT --------------------------
	
	override protected def wrapped: SingleColumnPropertyType
	
	
	// IMPLEMENTED  ----------------------
	
	override def sqlConversion = wrapped.sqlConversion
	override def sqlConversions = super[SingleColumnPropertyType].sqlConversions
	
	override def fromValueCode(valueCode: String, isFromJson: Boolean) =
		wrapped.fromValueCode(valueCode, isFromJson)
}

/**
  * Common trait for concrete property types that, in their SQL conversions, refer to another property type.
  * E.g. Classes that can be represented using simple double numbers may extend this trait and delegate SQL conversions
  * to DoubleNumber.
  */
trait FacadePropertyType extends PropertyType
{
	// ABSTRACT ---------------------------
	
	/**
	  * @return The data type that handles the "mid-conversion" and final SQL target conversion
	  */
	protected def delegate: PropertyType
	
	/**
	  * @return Whether [[fromDelegateCode]] yields a Try
	  */
	protected def yieldsTryFromDelegate: Boolean
	
	/**
	  * @param instanceCode Code that refers to an instance of [[scalaType]]
	  * @return Code that converts the specified Scala value into an instance of [[delegate]].[[scalaType]]
	  */
	protected def toDelegateCode(instanceCode: String): CodePiece
	/**
	  * @param delegateCode Code that refers to a [[delegate]].[[scalaType]] instance
	  * @return Code that converts the specified delegate instance into [[scalaType]] (or a Try)
	  */
	protected def fromDelegateCode(delegateCode: String): CodePiece
	
	
	// IMPLEMENTED  -----------------------
	
	override def optional: PropertyType = OptionWrapper
	override def concrete: PropertyType = this
	
	override def valueDataType = delegate.valueDataType
	override def sqlConversions = delegate.sqlConversions
		.map { lowerConversion => SqlTypeConversion.delegatingTo(lowerConversion, scalaType)(toDelegateCode) }
	
	override def yieldsTryFromValue: Boolean = delegate.yieldsTryFromValue || yieldsTryFromDelegate
	override def yieldsTryFromJsonValue: Boolean = delegate.yieldsTryFromJsonValue || yieldsTryFromDelegate
	
	override def toValueCode(instanceCode: String): CodePiece =
		toDelegateCode(instanceCode).flatMapText(delegate.toValueCode)
	override def toJsonValueCode(instanceCode: String): CodePiece =
		toDelegateCode(instanceCode).flatMapText(delegate.toJsonValueCode)
	
	override def fromValueCode(valueCodes: Vector[String]): CodePiece =
		safeFromDelegateCode(delegate.fromValueCode(valueCodes), isTry = delegate.yieldsTryFromValue)
	override def fromValuesCode(valuesCode: String): CodePiece =
		delegate.fromValuesCode(valuesCode).flatMapText { delegates =>
			if (yieldsTryFromDelegate)
				fromDelegateCode("v").mapText { tryFromDelegate =>
					s"$delegates.flatMap { v => $tryFromDelegate.toOption }"
				}
			else
				fromDelegateCode("v").mapText { fromDelegate =>
					s"$delegates.map { v => $fromDelegate }"
				}
		}
	override def fromJsonValueCode(valueCode: String): CodePiece =
		safeFromDelegateCode(delegate.fromJsonValueCode(valueCode), isTry = delegate.yieldsTryFromJsonValue)
	override def fromConcreteCode(concreteCode: String): CodePiece = concreteCode
	
	
	// OTHER    ---------------------------
	
	private def safeFromDelegateCode(delegateCode: CodePiece, isTry: Boolean) = {
		// Case: Conversion to delegate may fail => Handles the result using .map from Try
		if (isTry) {
			delegateCode.flatMapText { delegate =>
				// Depending on whether conversion from delegate may fail as well, uses either map or flatMap
				val mapFunctionName = if (yieldsTryFromDelegate) "flatMap" else "map"
				fromDelegateCode("v").mapText { fromDelegateSuccess =>
					s"$delegate.$mapFunctionName { v => $fromDelegateSuccess }"
				}
			}
		}
		// Case: Conversion to delegate always succeeds => Simply converts the value further
		else
			delegateCode.flatMapText(fromDelegateCode)
	}
	
	
	// NESTED   ----------------------------
	
	private object OptionWrapper extends PropertyType
	{
		// ATTRIBUTES   --------------------
		
		private lazy val optionDelegate = delegate.optional
		override lazy val scalaType: ScalaType = ScalaType.option(FacadePropertyType.this.scalaType)
		
		
		// IMPLEMENTED  --------------------
		
		override def valueDataType: Reference = FacadePropertyType.this.valueDataType
		
		override def sqlConversions: Vector[SqlTypeConversion] =
			optionDelegate.sqlConversions
				.map { lowerConversion => SqlTypeConversion.delegatingTo(lowerConversion, scalaType) { delegateOption =>
					fromDelegateCode("v")
						.mapText { fromDelegate => s"$delegateOption.map { v => $fromDelegate }" }
				} }
		
		override def emptyValue: CodePiece = CodePiece.none
		override def nonEmptyDefaultValue: CodePiece = CodePiece.empty
		
		override def defaultPropertyName: Name = FacadePropertyType.this.defaultPropertyName
		override def defaultPartNames: Seq[Name] = FacadePropertyType.this.defaultPartNames
		
		override def optional: PropertyType = this
		override def concrete: PropertyType = FacadePropertyType.this
		
		override def yieldsTryFromValue: Boolean = false
		override def yieldsTryFromJsonValue: Boolean = false
		override def supportsDefaultJsonValues: Boolean = false
		
		override def toValueCode(instanceCode: String): CodePiece =
			_toValueCode(instanceCode)(FacadePropertyType.this.toValueCode)
		override def toJsonValueCode(instanceCode: String): CodePiece =
			_toValueCode(instanceCode)(FacadePropertyType.this.toJsonValueCode)
		
		override def fromValueCode(valueCodes: Vector[String]): CodePiece =
			_fromValueCode(FacadePropertyType.this.fromValueCode(valueCodes),
				isTry = FacadePropertyType.this.yieldsTryFromValue)
		override def fromValuesCode(valuesCode: String): CodePiece =
			delegate.fromValuesCode(valuesCode).flatMapText { delegates =>
				fromDelegateCode("v").mapText { fromDelegate =>
					if (yieldsTryFromDelegate)
						s"$delegates.map { v => $fromDelegate.toOption }"
					else
						s"$delegates.map { v => Some($fromDelegate) }"
				}
			}
		override def fromJsonValueCode(valueCode: String): CodePiece =
			_fromValueCode(FacadePropertyType.this.fromJsonValueCode(valueCode),
				isTry = FacadePropertyType.this.yieldsTryFromJsonValue)
		override def fromConcreteCode(concreteCode: String): CodePiece = s"Some($concreteCode)"
		
		override def writeDefaultDescription(className: Name, propName: Name)(implicit naming: NamingRules): String =
			FacadePropertyType.this.writeDefaultDescription(className, propName)
		
		
		// OTHER    ------------------------
		
		private def _toValueCode(instanceCode: String)(wrappedToValue: String => CodePiece) =
			wrappedToValue("v").mapText { instanceToValue =>
				s"$instanceCode match { case Some(v) => $instanceToValue; case None => Value.empty }"
			}.referringTo(Reference.flow.value)
		private def _fromValueCode(wrappedCode: CodePiece, isTry: Boolean) = {
			// Case: Converting Try to Option
			if (isTry)
				wrappedCode.mapText { tryInstance => s"$tryInstance.toOption" }
			// Case: Converting an instance to Option
			else
				wrappedCode.mapText { instance => s"Some($instance)" }
		}
	}
}