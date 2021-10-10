package utopia.vault.coder.util

import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.controller.CodeBuilder
import utopia.vault.coder.model.data.{Class, Property}
import utopia.vault.coder.model.enumeration.PropertyType.EnumValue
import utopia.vault.coder.model.scala.Visibility.Public
import utopia.vault.coder.model.scala.{Parameter, Reference}
import utopia.vault.coder.model.scala.code.CodePiece
import utopia.vault.coder.model.scala.declaration.MethodDeclaration

/**
  * Used for constructing class-specific methods
  * @author Mikko Hilpinen
  * @since 9.10.2021, v1.2
  */
object ClassMethodFactory
{
	/**
	  * Creates a new method that parses an instance from a model
	  * @param targetClass Class being parsed
	  * @param validatedModelCode Code that provides a try containing a validated model based on the input parameter
	  * @param methodName Name of this method
	  * @param param (Model) parameter accepted by this method (default = template model named 'model')
	  * @param propNameInModel A function for determining the name of the specified property within the validated
	  *                        model. The result will be wrapped in double quotes.
	  * @param wrapAssignments A function that accepts assignments that provide enough data for a data instance
	  *                        creation (e.g. "param1Value, param2Value, param3Value") and produces the code
	  *                        resulting in the desired output type (like stored model, for example)
	  * @return A method for parsing class data from models
	  */
	def classFromModel(targetClass: Class, validatedModelCode: CodePiece,
	                   methodName: String = "apply",
	                   param: Parameter = Parameter("model", Reference.templateModel(Reference.property)))
	                  (propNameInModel: Property => String)
	                  (wrapAssignments: CodePiece => CodePiece) =
	{
		// Case: Class contains no properties
		if (targetClass.properties.isEmpty)
		{
			val code = wrapAssignments(CodePiece.empty)
			MethodDeclaration(methodName, code.references, isOverridden = true)(param)(code.text)
		}
		// Case: Enumerations are used => has to process enumeration values separately in custom apply method
		else
			new MethodDeclaration(Public, methodName, param,
				enumAwareApplyCode(targetClass, validatedModelCode)(propNameInModel)(wrapAssignments), None,
				"", "", isOverridden = true)
	}
	
	/**
	  * Creates a new method that parses instances from a validated model
	  * @param targetClass Class being parsed
	  * @param methodName Name of the method (default = fromValidatedModel)
	  * @param param Parameter accepted by the method (default = model called "valid")
	  * @param propNameInModel A function for determining the name of a property in the parameter model.
	  *                        Double quotes are added after the function call.
	  * @param wrapAssignments A function that accepts assignments that provide enough data for a data instance
	  *                        creation (e.g. "param1Value, param2Value, param3Value") and produces the code
	  *                        resulting in the desired output type (like stored model, for example)
	  * @return A method declaration
	  */
	def classFromValidatedModel(targetClass: Class, methodName: String = "fromValidatedModel",
	                            param: Parameter = Parameter("valid", Reference.model(Reference.constant)))
	                           (propNameInModel: Property => String)
	                           (wrapAssignments: CodePiece => CodePiece) =
	{
		// Case: Class contains no properties
		if (targetClass.properties.isEmpty)
		{
			val code = wrapAssignments(CodePiece.empty)
			MethodDeclaration(methodName, code.references, isOverridden = true)(param)(code.text)
		}
		// Case: No enumerations are used => implements a simpler fromValidatedModel
		else
		{
			val modelName = param.name
			val dataCreation = targetClass.properties
				.map { prop => prop.dataType.fromValueCode(s"$modelName(${propNameInModel(prop).quoted})") }
				.reduceLeft { _.append(_, ", ") }
			val code = wrapAssignments(dataCreation)
			MethodDeclaration(methodName, code.references, isOverridden = true)(param)(code.text)
		}
	}
	
	private def enumAwareApplyCode(classToWrite: Class, validatedModelCode: CodePiece)
	                              (propNameInModel: Property => String)
	                              (wrapAssignments: CodePiece => CodePiece) =
	{
		// Divides the class properties into enumeration-based values and standard values
		val dividedProperties = classToWrite.properties.map { prop => prop.dataType match
		{
			case enumVal: EnumValue => Left(prop -> enumVal)
			case _ => Right(prop)
		} }
		val enumProperties = dividedProperties.flatMap { _.leftOption }
		// Non-nullable enum-based values need to be parsed separately, because they may prevent model parsing
		val requiredEnumProperties = enumProperties.filter { !_._2.isNullable }
		
		val builder = new CodeBuilder()
		
		// Needs to validate the specified model
		val validateMapMethod = if (requiredEnumProperties.isEmpty) ".map" else ".flatMap"
		builder += validatedModelCode + validateMapMethod + "{ valid => "
		builder.indent()
		
		declareEnumerations(builder, requiredEnumProperties.dropRight(1), "flatMap")(propNameInModel)
		declareEnumerations(builder, requiredEnumProperties.lastOption, "map")(propNameInModel)
		val innerIndentCount = requiredEnumProperties.size + 1
		
		// Stores nullable enum values to increase readability
		enumProperties.filter { _._2.isNullable }.foreach { case (prop, enumVal) =>
			builder += s"val ${prop.name} = valid(${propNameInModel(prop).quoted}).int.flatMap(${
				enumVal.enumeration.name}.findForId)"
			builder.addReference(enumVal.enumeration.reference)
		}
		
		// Writes the instance creation now that the enum-based properties have been declared
		val assignments = dividedProperties.map {
			case Left((prop, _)) => CodePiece(prop.name.singular)
			case Right(prop) => prop.dataType.fromValueCode(s"valid(${propNameInModel(prop).quoted})")
		}.reduceLeft { _.append(_, ", ") }
		builder += wrapAssignments(assignments)
		
		// Closes open blocks
		(0 until innerIndentCount).foreach { _ => builder.closeBlock() }
		
		// References the enumerations used
		builder.result()
	}
	
	// NB: Indents for each declared enumeration
	private def declareEnumerations(builder: CodeBuilder, enumProps: Iterable[(Property, EnumValue)],
	                                mapMethod: String)(propNameInModel: Property => String) =
		enumProps.foreach { case (prop, enumVal) =>
			builder += s"${enumVal.enumeration.name}.forId(valid(${
				propNameInModel(prop).quoted}).getInt).$mapMethod { ${prop.name} => "
			builder.addReference(enumVal.enumeration.reference)
			builder.indent()
		}
}
