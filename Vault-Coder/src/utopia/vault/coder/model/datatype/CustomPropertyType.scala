package utopia.vault.coder.model.datatype

import utopia.coder.model.data.{Name, NamingRules}
import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{ModelDeclaration, ModelValidationFailedException}
import utopia.flow.generic.model.template.{ModelLike, Property}
import utopia.flow.generic.model.mutable.DataType.StringType
import utopia.flow.parse.string.Regex
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.datatype.CustomPropertyType.{pluralRegex, singularRegex, valueRegex}
import utopia.vault.coder.model.datatype.CustomPropertyType.CustomPartConversion
import utopia.coder.model.enumeration.NamingConvention.CamelCase
import utopia.coder.model.scala.code.CodePiece
import utopia.coder.model.scala.datatype.{Reference, ScalaType}
import utopia.coder.model.scala.template.ValueConvertibleType
import Reference.Flow._
import utopia.coder.model.enumeration.NameContext.ClassPropName

import scala.util.{Failure, Success}

object CustomPropertyType extends FromModelFactory[CustomPropertyType]
{
	// ATTRIUTES    ---------------------------
	
	private val parameterIndicatorRegex = Regex.escape('$')
	/**
	  * Regular expression used for finding value (parameter) references within user-defined code pieces
	  */
	private val valueRegex = parameterIndicatorRegex + "v"
	private val singularRegex = parameterIndicatorRegex + "s"
	private val pluralRegex = parameterIndicatorRegex + "p"
	
	private lazy val schema = ModelDeclaration("type" -> StringType)
	
	
	// IMPLEMENTED  -------------------------
	
	override def apply(model: ModelLike[Property]) =
		schema.validate(model).flatMap { model =>
			// from_value, from_values and to_value must all exist and contain the appropriate parameter placeholder
			ensureFunctions(model, Vector("from_value", "to_value", "option_from_value")).flatMap { _ =>
				// There must be either "parts" -property (multi-column) or "sql" property (single-column)
				// Plus, the parts must be parseable
				model("parts").getVector.tryMap { v => CustomPartConversion(v.getModel) }.flatMap { parts =>
					if (parts.nonEmpty || model.containsNonEmpty("sql")) {
						val valueDataType = {
							val code = model("value_data_type", "value_type", "data_type").getString
							if (code.isEmpty)
								dataType/"AnyType"
							else if (code.contains('.'))
								Reference(code)
							else
								dataType/code
						}
						val default: CodePiece = model("default", "default_value")
						// Handles the single-column use-case, if necessary
						val conversion = if (parts.isEmpty) Left(sqlTypeFrom(model, default)) else Right(parts)
						val fromValueYieldsTry = model("from_value_can_fail", "yields_try", "try").getBoolean
						Success(apply(
							ScalaType(model("type")), conversion, valueDataType,
							model("from_value"), model("option_from_value"),
							model("to_value"), model("to_json_value"),
							model("option_to_value"), model("option_to_json_value"),
							model("from_json_value"), model("option_from_json_value"),
							model("empty", "empty_value"), default,
							model("prop_name", "property_name", "default_prop_name", "default_name"),
							parts.flatMap { _.defaultName },
							model("description", "doc", "desc"),
							fromValueYieldsTry,
							model("from_json_value_can_fail", "json_yields_try", "json_try")
								.booleanOr(fromValueYieldsTry),
							model("function_generation_enabled", "functions_enabled", "functions", "filter")
								.booleanOr(true)
						))
					}
					else
						Failure(new ModelValidationFailedException(
							"A custom data type must either define the 'sql' or the 'parts' -property"))
				}
			}
		}
	
	
	// OTHER    --------------------------
	
	private def ensureFunctions(model: ModelLike[Property], functionNames: Iterable[String]) = {
		functionNames
			.findMap { propName =>
				model(propName).string match {
					case Some(code) =>
						if (valueRegex.existsIn(code))
							None
						else
							Some(s"$propName doesn't contain the appropriate value reference ('$valueRegex')")
					case None => Some(s"$propName is missing from $model")
				}
			} match
		{
			case Some(failureMessage) => Failure(new ModelValidationFailedException(failureMessage))
			case None => Success(())
		}
	}
	
	private def sqlTypeFrom(model: ModelLike[Property], defaultValue: CodePiece = CodePiece.empty) = {
		val sqlDefault = model("sql_default").stringOr { defaultValue.toSql.getOrElse("") }
		SqlPropertyType(model("sql"), sqlDefault,
			model("col_suffix", "column_suffix", "suffix"), model("optional", "nullable", "allows_null"),
			model("index", "is_index", "default_index", "default_indexing"))
	}
	
	
	// NESTED   --------------------------
	
	object CustomPartConversion extends FromModelFactory[CustomPartConversion]
	{
		implicit val naming: NamingRules = NamingRules.default
		
		// NB: Type refers to the mid-state. i.e. what the type is in the dbModel construction parameter
		private val schema = ModelDeclaration("type" -> StringType, "sql" -> StringType)
		
		override def apply(model: ModelLike[Property]) =
			schema.validate(model).flatMap { model =>
			// extract, extract_from_option must exist and contain the appropriate parameter placeholder ($v)
			ensureFunctions(model, Vector("extract", "extract_from_option")).map { _ =>
				val toValueInput: CodePiece = model("to_value")
				val emptyValue = CodePiece.fromValue(model("empty_value", "empty"))
					.filter { _.nonEmpty }.getOrElse(CodePiece.none)
				apply(ScalaType(model("type")), sqlTypeFrom(model), model("extract"), model("extract_from_option"),
					toValueInput.notEmpty
						.getOrElse { CodePiece("$v", Set(valueConversions)) },
					emptyValue, ClassPropName.from(model))
				}
			}
	}
	
	case class CustomPartConversion(midType: ScalaType, sqlType: SqlPropertyType, extractPart: CodePiece,
	                                extractPartFromOption: CodePiece,
	                                partToValue: CodePiece = CodePiece("$v", Set(valueConversions)),
	                                emptyMidValue: CodePiece = CodePiece.none,
	                                defaultName: Option[Name] = None)
}

/**
  * Represents a user-created property type
  * @author Mikko Hilpinen
  * @since 17.7.2022, v1.15.1
  * @param scalaType The wrapped scala data type
  * @param conversion Either Left: The underlying SQL data type (just the type) behind this data type
  *                   (single-column use-case) or Right: The parts (column-representations) that form this combined
  *                   data type (multi-column use-case)
  * @param valueDataType Full reference to the data type object associated with the Value that is generated from this type
  * @param fromValue Code that takes a Value (represented by $v) and returns an instance of this type.
  *                  For multi-column data types, the values are represented by $v1, $v2, $v3 and so on.
  * @param optionFromValue Code that takes a Value ($v) and returns an option containing an instance of this type.
  *                        For multi-column data types, the values are represented by $v1, $v2, $v3 and so on.
  * @param toValue Code that takes an instance of this type (represented by $v) and returns a Value
  * @param toJsonValue Code that takes an instance of this type (represented by $v) and returns a Value.
  *                    This variant is used when generating values for json conversion.
  *                    Default = empty code = use 'toValue'
  * @param optionToValue Code that takes an option (with this instance) ($v) and returns a Value
  *                      (default = empty = autogenerate)
  * @param optionToJsonValue Code that takes an option of this instance ($v) and returns a Value.
  *                          This variant is used when creating values for json conversion.
  *                          Default = empty = use 'optionToValue'
  * @param fromJsonValue Code that accepts a Value ($v) and returns an instance of this type.
  *                      This variant is used when parsing data from json.
  *                      Default = empty = use 'fromValue'
  * @param optionFromJsonValue Code that accepts a Value ($v) and yields an Option.
  *                            This variant is used when parsing data from json.
  *                            Default = empty = use 'optionFromValue'
  * @param emptyValue An "empty" value applicable to this type. Empty if there is no applicable value (default).
  * @param nonEmptyDefaultValue The default value for this data type within scala code, but only if different from
  *                             emptyValue (default = empty = no default value)
  * @param defaultPropName The default property name generated by this data type (default = None = same as data type name)
  * @param autoDescription Automated documentation written for properties of this type by default.
  *                        Each occurrence of $s is replaced with a singular class name and each occurrence of $p is
  *                        replaced with a plural class name.
  * @param yieldsTryFromValue Whether fromValue code yields a Try instead of an instance of this type (default = false)
  * @param isFilterGenerationSupported Whether automatic withX filter function-writing should be enabled
  *                                    for indexed properties (default = true).
  *                                    Set to false if this type is based on fractional values where individual
  *                                    value search is not a realistic use case.
  */
case class CustomPropertyType(scalaType: ScalaType, conversion: Either[SqlPropertyType, Vector[CustomPartConversion]],
                              valueDataType: Reference,
                              fromValue: CodePiece, optionFromValue: CodePiece,
                              toValue: CodePiece, toJsonValue: CodePiece = CodePiece.empty,
                              optionToValue: CodePiece = CodePiece.empty, optionToJsonValue: CodePiece = CodePiece.empty,
                              fromJsonValue: CodePiece = CodePiece.empty, optionFromJsonValue: CodePiece = CodePiece.empty,
                              emptyValue: CodePiece = CodePiece.empty,
                              nonEmptyDefaultValue: CodePiece = CodePiece.empty, defaultPropName: Option[Name] = None,
                              defaultPartNames: Seq[Name] = Vector(),
                              autoDescription: String = "", yieldsTryFromValue: Boolean = false,
                              yieldsTryFromJsonValue: Boolean = false, isFilterGenerationSupported: Boolean = true)
	extends PropertyType
{
	// ATTRIBUTES   ----------------------------
	
	override lazy val sqlConversions = conversion match {
		case Left(targetType) => Vector(new OptionWrappingSqlConversion(targetType))
		case Right(parts) => parts.map { new PartToSqlConversion(_) }
	}
	
	
	// IMPLEMENTED  ----------------------------
	
	override def concrete = this
	override def optional: PropertyType = OptionWrapped
	
	override def defaultPropertyName =
		defaultPropName.getOrElse { Name.interpret(scalaType.toString, CamelCase.capitalized) }
	
	override def supportsDefaultJsonValues = true
	
	override def toValueCode(instanceCode: String) = finalizeCode(toValue, instanceCode)
	override def toJsonValueCode(instanceCode: String): CodePiece = toJsonValue.notEmpty match {
		case Some(toValue) => finalizeCode(toValue, instanceCode)
		case None => toValueCode(instanceCode)
	}
	private def optionToValueCode(optionCode: String, isToJson: Boolean = false) = {
		val appliedOptionToValue = if (isToJson) optionToJsonValue.nonEmptyOrElse(optionToValue) else optionToValue
		appliedOptionToValue.notEmpty match {
			// Case: Using a user-defined conversion
			case Some(toValue) => finalizeCode(toValue, optionCode)
			// Case: No user-defined conversion available
			case None =>
				toValueCode("v")
					.mapText { toValue => s"$optionCode.map { v => $toValue }.getOrElse(Value.empty)" }
					.referringTo(value)
		}
	}
	
	override def fromValueCode(valueCodes: Vector[String]): CodePiece = fromValueCode(fromValue, valueCodes)
	override def fromJsonValueCode(valueCode: String): CodePiece = fromJsonValue.notEmpty match {
		case Some(fromValue) => fromValueCode(fromValue, Vector(valueCode))
		case None => fromValueCode(Vector(valueCode))
	}
	// TODO: Current version doesn't support multi-column types, hence the Vector("v")
	override def fromValuesCode(valuesCode: String) =
		fromValueCode(Vector("v")).mapText { fromValue => s"$valuesCode.map { v => $fromValue }" }
	private def optionFromValueCode(valueCodes: Vector[String], isFromJson: Boolean = false): CodePiece = {
		val appliedOptionFromValue = if (isFromJson) optionFromJsonValue.nonEmptyOrElse(optionFromValue) else optionFromValue
		fromValueCode(appliedOptionFromValue, valueCodes)
	}
	override def fromConcreteCode(concreteCode: String): CodePiece = concreteCode
	
	override def writeDefaultDescription(className: Name, propName: Name)(implicit naming: NamingRules) =
		autoDescription
			.replaceAll(singularRegex, className.doc)
			.replaceAll(pluralRegex, className.pluralDoc)
	
	
	// OTHER    ----------------------------
	
	private def fromValueCode(userCode: CodePiece, valueCodes: Vector[String]) = {
		// Case: Parsing from multiple parameters (multiple parts)
		if (valueCodes.size > 1)
			valueCodes.zipWithIndex.foldLeft(userCode) { case (code, (valueCode, index)) =>
				finalizeCode(code, valueCode, valueRegex + (index + 1).toString)
			}
		else
			valueCodes.headOption match {
				// Case: Parsing from a single part
				case Some(valueCode) => finalizeCode(userCode, valueCode)
				// Case: No values provided (shouldn't arrive here)
				case None => emptyValue
			}
	}
	
	private def finalizeCode(userCode: CodePiece, parameterCode: String, parameterRegex: Regex = valueRegex) =
		userCode.mapText { _.replaceAll(parameterRegex, parameterCode) }
	
	
	// NESTED   ----------------------------
	
	private object OptionWrapped extends PropertyType
	{
		override lazy val scalaType = ScalaType.option(CustomPropertyType.this.scalaType)
		override lazy val sqlConversions = conversion match {
			case Left(targetType) => Vector(DirectSqlTypeConversion(OptionWrappedMidType, targetType.nullable))
			case Right(parts) => parts.map { new PartToSqlConversion(_, extractFromOption = true) }
		}
		
		override def valueDataType = CustomPropertyType.this.valueDataType
		override def isFilterGenerationSupported: Boolean = CustomPropertyType.this.isFilterGenerationSupported
		override def supportsDefaultJsonValues = false
		
		override def emptyValue = CodePiece.none
		override def nonEmptyDefaultValue = CodePiece.empty
		
		override def defaultPropertyName = CustomPropertyType.this.defaultPropertyName
		override def defaultPartNames: Seq[Name] = CustomPropertyType.this.defaultPartNames
		
		override def optional = this
		override def concrete = CustomPropertyType.this
		
		override def yieldsTryFromValue = false
		override def yieldsTryFromJsonValue: Boolean = false
		
		override def toValueCode(instanceCode: String) = optionToValueCode(instanceCode)
		override def toJsonValueCode(instanceCode: String): CodePiece = optionToValueCode(instanceCode, isToJson = true)
		
		override def fromValueCode(valueCodes: Vector[String]) = optionFromValueCode(valueCodes)
		override def fromJsonValueCode(valueCode: String): CodePiece =
			optionFromValueCode(Vector(valueCode), isFromJson = true)
		// TODO: No multi-column support exists here either
		override def fromValuesCode(valuesCode: String) =
			fromValueCode(Vector("v")).mapText { fromValue => s"$valuesCode.flatMap { v => $fromValue }" }
		override def fromConcreteCode(concreteCode: String): CodePiece = s"Some($concreteCode)"
		
		override def writeDefaultDescription(className: Name, propName: Name)(implicit naming: NamingRules) =
			CustomPropertyType.this.writeDefaultDescription(className, propName)
	}
	
	private object OptionWrappedMidType extends ValueConvertibleType
	{
		override def scalaType = OptionWrapped.scalaType
		
		override def emptyValue = CodePiece.none
		
		override def toValueCode(instanceCode: String) = optionToValueCode(instanceCode)
	}
	
	private class OptionWrappingSqlConversion(targetType: SqlPropertyType) extends SqlTypeConversion
	{
		override def origin = CustomPropertyType.this.scalaType
		override def intermediate = OptionWrappedMidType
		override def target = targetType
		
		override def midConversion(originCode: String) = s"Some($originCode)"
	}
	
	private class PartToSqlConversion(part: CustomPartConversion, extractFromOption: Boolean = false)
		extends SqlTypeConversion
	{
		override def origin =
			if (extractFromOption) OptionWrapped.scalaType else CustomPropertyType.this.scalaType
		override def intermediate: ValueConvertibleType = IntermediateState
		override def target = if (extractFromOption) part.sqlType.nullable else part.sqlType
		
		override def midConversion(originCode: String) =
			finalizeCode(if (extractFromOption) part.extractPartFromOption else part.extractPart, originCode)
		
		
		// NESTED   ------------------------
		
		private object IntermediateState extends ValueConvertibleType
		{
			override def scalaType = part.midType
			override def emptyValue = part.emptyMidValue
			
			override def toValueCode(instanceCode: String) = finalizeCode(part.partToValue, instanceCode)
		}
	}
}