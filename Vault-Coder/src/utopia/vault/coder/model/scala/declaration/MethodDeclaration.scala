package utopia.vault.coder.model.scala.declaration

import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.merging.{MergeConflict, Mergeable}
import utopia.vault.coder.model.scala.code.Code
import utopia.vault.coder.model.scala.Visibility.Public
import utopia.vault.coder.model.scala.{Parameters, Reference, ScalaType, Visibility}

import scala.collection.immutable.VectorBuilder

object MethodDeclaration
{
	/**
	  * Creates a new method declaration
	  * @param name Method name
	  * @param codeReferences References made within the code (default = empty)
	  * @param visibility Method visibility (default = public)
	  * @param explicitOutputType Data type returned by this method, when explicitly defined (optional)
	  * @param description Description of this method (default = empty)
	  * @param returnDescription Description of the return value of this method (default = empty)
	  * @param headerComments Lines of comments to insert before the declaration (default = empty)
	  * @param isOverridden Whether this method overrides a base member (default = false)
	  * @param params Method parameters (0-n)
	  * @param firstLine First line of code
	  * @param moreLines More lines of code (0-n)
	  * @return A new method
	  */
	def apply(name: String, codeReferences: Set[Reference] = Set(), visibility: Visibility = Public,
	          explicitOutputType: Option[ScalaType] = None, description: String = "", returnDescription: String = "",
	          headerComments: Vector[String] = Vector(),
	          isOverridden: Boolean = false)(params: Parameters = Parameters.empty)
	         (firstLine: String, moreLines: String*): MethodDeclaration =
		apply(visibility, name, params, Code.from(firstLine +: moreLines.toVector).referringTo(codeReferences),
			explicitOutputType, description, returnDescription, headerComments, isOverridden)
}

/**
  * Represents a scala method
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  * @param visibility Visibility of this method
  * @param name Method name
  * @param parameters Parameters accepted by this method
  * @param bodyCode Code executed within this method
  * @param explicitOutputType Data type returned by this method, when explicitly defined (optional)
  * @param description Description of this method (may be empty)
  * @param returnDescription Description of the return value of this method (may be empty)
  * @param headerComments Lines of comments to insert before the declaration (default = empty)
  * @param isOverridden Whether this method overrides a base version
  */
case class MethodDeclaration(visibility: Visibility, name: String, parameters: Parameters, bodyCode: Code,
                             explicitOutputType: Option[ScalaType], description: String, returnDescription: String,
                             headerComments: Vector[String], isOverridden: Boolean)
	extends FunctionDeclaration with Mergeable[MethodDeclaration, MethodDeclaration]
{
	override def keyword = "def"
	
	override protected def params = Some(parameters)
	
	override def mergeWith(other: MethodDeclaration) =
	{
		val conflictsBuilder = new VectorBuilder[MergeConflict]()
		val myBase = basePart
		val theirBase = other.basePart
		if (myBase != theirBase)
			conflictsBuilder += MergeConflict.line(theirBase.text, myBase.text, s"$name declarations differ")
		if (parameters != other.parameters)
			conflictsBuilder += MergeConflict.line(other.parameters.toString, parameters.toString,
				s"$name parameters differ")
		if (bodyCode != other.bodyCode)
			conflictsBuilder ++= bodyCode.conflictWith(other.bodyCode, s"$name implementations differ")
		if (explicitOutputType.exists { myType => other.explicitOutputType.exists { _ != myType } })
			conflictsBuilder += MergeConflict.line(other.explicitOutputType.get.toString,
				explicitOutputType.get.toString, s"$name implementations specify different return types")
		
		MethodDeclaration(visibility min other.visibility, name, parameters, bodyCode,
			explicitOutputType.orElse(other.explicitOutputType), description.notEmpty.getOrElse(other.description),
			returnDescription.notEmpty.getOrElse(other.returnDescription),
			headerComments ++ other.headerComments.filterNot(headerComments.contains),
			isOverridden || other.isOverridden) -> conflictsBuilder.result()
	}
}
