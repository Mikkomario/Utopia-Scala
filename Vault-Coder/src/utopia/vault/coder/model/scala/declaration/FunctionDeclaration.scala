package utopia.vault.coder.model.scala.declaration

import utopia.flow.util.StringExtensions._
import utopia.vault.coder.controller.CodeBuilder
import utopia.vault.coder.model.merging.{MergeConflict, Mergeable}
import utopia.vault.coder.model.scala.code.Code
import utopia.vault.coder.model.scala.ScalaDocKeyword.Return
import utopia.vault.coder.model.scala.{Parameters, ScalaDocPart, ScalaType, Visibility}
import utopia.vault.coder.model.scala.template.{CodeConvertible, ScalaDocConvertible}

import scala.collection.immutable.VectorBuilder

/**
  * Declares a scala method or a property of some sort
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
trait FunctionDeclaration[+Repr]
	extends Declaration with CodeConvertible with ScalaDocConvertible with Mergeable[FunctionDeclaration[_], Repr]
{
	// ABSTRACT ------------------------------
	
	/**
	  * @return Code that forms this method / property
	  */
	def bodyCode: Code
	/**
	  * @return Comments presented above this function declaration, but not included in the scaladoc
	  */
	def headerComments: Vector[String]
	
	/**
	  * @return Whether this declaration overrides a base declaration
	  */
	def isOverridden: Boolean
	/**
	  * @return Whether this function implementation should be considered low priority when merging
	  */
	def isLowMergePriority: Boolean
	
	/**
	  * @return Documentation describing this function (may be empty)
	  */
	def description: String
	/**
	  * @return Documentation describing the return value of this function (may be empty)
	  */
	def returnDescription: String
	
	/**
	  * @return Data type explicitly returned by this function
	  */
	def explicitOutputType: Option[ScalaType]
	/**
	  * @return Parameters accepted by this function. None if this function is parameterless.
	  */
	protected def params: Option[Parameters]
	
	/**
	  * Creates a modified copy of this declaration
	  * @param visibility New visibility
	  * @param parameters New parameters
	  * @param bodyCode New code
	  * @param explicitOutputType New output type
	  * @param description New description
	  * @param returnDescription New return description
	  * @param headerComments New header comments
	  * @param isOverridden Whether new version should be overridden
	  * @return Copy of this declaration
	  */
	protected def makeCopy(visibility: Visibility, parameters: Option[Parameters], bodyCode: Code,
	                       explicitOutputType: Option[ScalaType], description: String, returnDescription: String,
	                       headerComments: Vector[String], isOverridden: Boolean): Repr
	
	
	// COMPUTED ------------------------------
	
	/**
	  * @return Whether this function is abstract (doesn't specify a body)
	  */
	def isAbstract = bodyCode.isEmpty
	
	
	// IMPLEMENTED  --------------------------
	
	override def documentation =
	{
		val desc = description
		val returnDesc = returnDescription
		val resultBuilder = new VectorBuilder[ScalaDocPart]()
		if (desc.nonEmpty)
			resultBuilder += ScalaDocPart.description(desc)
		params.foreach { resultBuilder ++= _.documentation }
		if (returnDesc.nonEmpty)
			resultBuilder += ScalaDocPart(Return, returnDesc)
		resultBuilder.result()
	}
	
	override def toCode =
	{
		val builder = new CodeBuilder()
		// Adds the documentation first
		builder ++= scalaDoc
		// Then possible header comments
		headerComments.foreach { c => builder += s"// $c" }
		// Then the header and body
		if (isOverridden)
			builder.appendPartial("override ")
		builder += basePart
		params.foreach { builder += _.toScala }
		explicitOutputType.foreach { outputType => builder.appendPartial(outputType.toScala, ": ") }
		
		// Case: Concrete function
		if (bodyCode.nonEmpty)
		{
			builder.appendPartial(" = ")
			if (bodyCode.isSingleLine)
			{
				builder.appendPartial(bodyCode.lines.head.code, allowLineSplit = true)
				builder.addReferences(bodyCode.references)
			} // Case: Multi-line function
			else
				builder.addBlock(bodyCode)
		}
		
		builder.result()
	}
	
	override def mergeWith(other: FunctionDeclaration[_]) =
	{
		val (priority, lowPriority) =
		{
			if (isLowMergePriority)
			{
				if (other.isLowMergePriority) this -> other else other -> this
			}
			else
				this -> other
		}
		
		val conflictsBuilder = new VectorBuilder[MergeConflict]()
		lazy val prioString = if (isLowMergePriority) " (low priority)" else ""
		val myBase = basePart
		val theirBase = other.basePart
		if (myBase != theirBase)
			conflictsBuilder += MergeConflict.line(theirBase.text, myBase.text, s"$name declarations differ")
		if (params.exists { !other.params.contains(_) })
			conflictsBuilder ++= params.get.conflictWith(other.params.get,
				s"$name parameters differ$prioString")
		if (bodyCode.conflictsWith(other.bodyCode))
			conflictsBuilder ++= bodyCode.conflictWith(other.bodyCode,
				s"$name implementations differ$prioString")
		if (explicitOutputType.exists { myType => other.explicitOutputType.exists { _ != myType } })
			conflictsBuilder += MergeConflict.line(other.explicitOutputType.get.toString,
				explicitOutputType.get.toString,
				s"$name implementations specify different return types$prioString")
		
		makeCopy(visibility min other.visibility, priority.params, priority.bodyCode,
			priority.explicitOutputType.orElse(lowPriority.explicitOutputType),
			priority.description.notEmpty.getOrElse(lowPriority.description),
			priority.returnDescription.notEmpty.getOrElse(lowPriority.returnDescription),
			other.headerComments.filterNot(headerComments.contains) ++ headerComments,
			isOverridden || other.isOverridden) -> conflictsBuilder.result()
	}
}
