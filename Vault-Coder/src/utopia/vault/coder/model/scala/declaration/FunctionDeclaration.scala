package utopia.vault.coder.model.scala.declaration

import utopia.flow.collection.immutable.Pair
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.controller.writer.CodeBuilder
import utopia.vault.coder.model.merging.{MergeConflict, Mergeable}
import utopia.vault.coder.model.scala.code.Code
import utopia.vault.coder.model.scala.doc.ScalaDocKeyword.Return
import utopia.vault.coder.model.scala.datatype.{GenericType, ScalaType}
import utopia.vault.coder.model.scala.doc.ScalaDocPart
import utopia.vault.coder.model.scala.{Annotation, Parameters, Visibility}

import scala.collection.immutable.VectorBuilder

/**
  * Declares a scala method or a property of some sort
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
trait FunctionDeclaration[+Repr]
	extends Declaration with Mergeable[FunctionDeclaration[_], Repr]
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
	  * @return Whether this declares an implicit function or property
	  */
	def isImplicit: Boolean
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
	  * @param genericTypes New generic type declarations
	  * @param parameters New parameters
	  * @param bodyCode New code
	  * @param explicitOutputType New output type
	  * @param annotations Annotations to apply to this function
	  * @param description New description
	  * @param returnDescription New return description
	  * @param headerComments New header comments
	  * @param isOverridden Whether new version should be overridden
	  * @param isImplicit Whether this version should be implicit
	  * @return Copy of this declaration
	  */
	protected def makeCopy(visibility: Visibility, genericTypes: Seq[GenericType], parameters: Option[Parameters],
	                       bodyCode: Code, explicitOutputType: Option[ScalaType], annotations: Seq[Annotation],
	                       description: String, returnDescription: String, headerComments: Vector[String],
	                       isOverridden: Boolean, isImplicit: Boolean): Repr
	
	
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
		genericTypes.foreach { resultBuilder ++= _.documentation }
		resultBuilder.result()
	}
	
	override def toCode =
	{
		val builder = new CodeBuilder()
		// Adds the documentation first
		builder ++= scalaDoc
		// Then possible header comments
		headerComments.foreach { c => builder += s"// $c" }
		// Then possible annotations
		builder ++= annotationsPart
		// Then the header and body
		if (isImplicit)
			builder.appendPartial("implicit ")
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
		val (priority, lowPriority) = {
			if (isLowMergePriority) {
				if (other.isLowMergePriority) this -> other else other -> this
			}
			else
				this -> other
		}
		val parties = Pair(priority, lowPriority)
		
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
		
		val (mergedAnnotations, annotationConflicts) = Annotation.merge(parties.map { _.annotations })
		if (parties.first == this)
			annotationConflicts.foreach { case Pair(_, lowPrio) =>
				conflictsBuilder += MergeConflict.note(s"Annotation $lowPrio was (partially) overwritten")
			}
		
		val newVisibility = {
			if (other.isLowMergePriority)
				visibility
			else if (isLowMergePriority)
				other.visibility
			else
				visibility min other.visibility
		}
		makeCopy(newVisibility,
			priority.genericTypes ++
				lowPriority.genericTypes.filterNot { t => priority.genericTypes.exists { _.name == t.name } },
			priority.params, priority.bodyCode, priority.explicitOutputType.orElse(lowPriority.explicitOutputType),
			mergedAnnotations,
			priority.description.nonEmptyOrElse(lowPriority.description),
			priority.returnDescription.nonEmptyOrElse(lowPriority.returnDescription),
			other.headerComments.filterNot(headerComments.contains) ++ headerComments,
			isOverridden || other.isOverridden, priority.isImplicit) -> conflictsBuilder.result()
	}
}
