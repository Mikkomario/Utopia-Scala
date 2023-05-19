package utopia.vault.coder.util

import utopia.coder.model.data.NamingRules
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.{Class, Property}
import utopia.coder.model.scala.Visibility.{Protected, Public}
import utopia.coder.model.scala.Parameter
import utopia.coder.model.scala.code.{CodeBuilder, CodePiece}
import utopia.coder.model.scala.datatype.Reference
import utopia.coder.model.scala.declaration.MethodDeclaration
import Reference.Flow._

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
	  * @param propNamesInModel A function for determining the name(s) of the specified properties within the validated
	  *                        model. The result(s) will be wrapped in double quotes.
	  * @param wrapAssignments A function that accepts assignments that provide enough data for a data instance
	  *                        creation (e.g. "param1Value, param2Value, param3Value") and produces the code
	  *                        resulting in the desired output type (like stored model, for example)
	  * @return A method for parsing class data from models
	  */
	def classFromModel(targetClass: Class, validatedModelCode: CodePiece,
	                   methodName: String = "apply",
	                   param: Parameter = Parameter("model", templateModel(property)))
	                  (propNamesInModel: Property => Vector[String])
	                  (wrapAssignments: CodePiece => CodePiece)
	                  (implicit naming: NamingRules) =
	{
		// Case: Class contains no properties
		if (targetClass.properties.isEmpty) {
			val code = wrapAssignments(CodePiece.empty)
			MethodDeclaration(methodName, code.references, isOverridden = true)(param)(code.text)
		}
		// Case: Enumerations are used => has to process enumeration values separately in custom apply method
		else
			new MethodDeclaration(Public, methodName, Vector(), param,
				tryApplyCode(targetClass, validatedModelCode)(propNamesInModel)(wrapAssignments), None, Vector(),
				"", "", Vector(), isOverridden = true, isImplicit = false,
				isLowMergePriority = false)
	}
	
	/**
	  * Creates a new method that parses instances from a validated model
	  * @param targetClass Class being parsed
	  * @param methodName Name of the method (default = fromValidatedModel)
	  * @param param Parameter accepted by the method (default = model called "valid")
	  * @param propNamesInModel A function for determining the name(s) of the specified properties within the validated
	  *                        model. The result(s) will be wrapped in double quotes.
	  * @param wrapAssignments A function that accepts assignments that provide enough data for a data instance
	  *                        creation (e.g. "param1Value, param2Value, param3Value") and produces the code
	  *                        resulting in the desired output type (like stored model, for example)
	  * @return A method declaration
	  */
	def classFromValidatedModel(targetClass: Class, methodName: String = "fromValidatedModel",
	                            param: Parameter = Parameter("valid", model))
	                           (propNamesInModel: Property => Vector[String])
	                           (wrapAssignments: CodePiece => CodePiece) =
	{
		// Case: Class contains no properties
		if (targetClass.properties.isEmpty) {
			val code = wrapAssignments(CodePiece.empty)
			MethodDeclaration(methodName, code.references, visibility = Protected, isOverridden = true)(param)(code.text)
		}
		// Case: No try-based or potentially failing reads are used => implements a simpler fromValidatedModel
		else {
			val modelName = param.name
			val dataCreation = targetClass.properties
				.map { prop => propFromValidModelCode(prop, modelName)(propNamesInModel) }
				.reduceLeft { _.append(_, ", ") }
			val code = wrapAssignments(dataCreation)
			MethodDeclaration(methodName, code.references, visibility = Protected, isOverridden = true)(param)(code.text)
		}
	}
	
	private def tryApplyCode(classToWrite: Class, validatedModelCode: CodePiece)
	                        (propNamesInModel: Property => Vector[String])
	                        (wrapAssignments: CodePiece => CodePiece)
	                        (implicit naming: NamingRules) =
	{
		// Divides the class properties into try-based values and standard values
		val tryProperties = classToWrite.properties.filter { _.dataType.yieldsTryFromValue }
		
		val builder = new CodeBuilder()
		
		// Needs to validate the specified model
		val validateMapMethod = if (tryProperties.isEmpty) ".map" else ".flatMap"
		builder += validatedModelCode + validateMapMethod + " { valid => "
		builder.indent()
		
		declareTryProps(builder, tryProperties.dropRight(1), "flatMap")(propNamesInModel)
		declareTryProps(builder, tryProperties.lastOption, "map")(propNamesInModel)
		val innerIndentCount = tryProperties.size + 1
		
		// Writes the instance creation now that the "try-properties" properties have been declared
		val assignments = classToWrite.properties.map { prop =>
			// Case: Try-based property / value => already defined
			if (prop.dataType.yieldsTryFromValue)
				CodePiece(prop.name.prop)
			// Case: Normal property / value => reads the value from the model
			else
				propFromValidModelCode(prop)(propNamesInModel)
		}.reduceLeft { _.append(_, ", ") }
		builder += wrapAssignments(assignments)
		
		// Closes open blocks
		(0 until innerIndentCount).foreach { _ => builder.closeBlock() }
		
		// References the enumerations used
		builder.result()
	}
	
	// NB: Indents for each declared property
	private def declareTryProps(builder: CodeBuilder, tryProps: Iterable[Property], mapMethod: String)
	                           (propNamesInModel: Property => Vector[String])
	                           (implicit naming: NamingRules) =
		tryProps.foreach { prop =>
			val fromValueCode = propFromValidModelCode(prop)(propNamesInModel)
			builder += fromValueCode + s".$mapMethod { ${prop.name.prop} => "
			builder.indent()
		}
	
	private def propFromValidModelCode(prop: Property, modelName: String = "valid")
	                                  (propNamesInModel: Property => Vector[String]) =
		prop.dataType.fromValueCode(propNamesInModel(prop).map { name => s"$modelName(${name.quoted})" })
}
