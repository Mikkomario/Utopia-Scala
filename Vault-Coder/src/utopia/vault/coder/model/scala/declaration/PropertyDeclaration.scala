package utopia.vault.coder.model.scala.declaration

import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.merging.{MergeConflict, Mergeable}
import utopia.vault.coder.model.scala.code.Code
import utopia.vault.coder.model.scala.Visibility.{Protected, Public}
import utopia.vault.coder.model.scala.{Parameter, Parameters, ScalaType, Visibility}
import utopia.vault.coder.model.scala.declaration.PropertyDeclarationType.ComputedProperty

import scala.collection.immutable.VectorBuilder

object PropertyDeclaration
{
	/**
	  * Creates a new abstract computed property
	  * @param name Name of this property
	  * @param outputType Data type of this property
	  * @param implicitParams Implicit parameters accepted (default = empty)
	  * @param description Description of this property (default = empty)
	  * @param isProtected Whether this property should be protected instead of public (default = false)
	  * @param isOverridden Whether this property overrides a base member (default = false)
	  * @return A new property declaration
	  */
	def newAbstract(name: String, outputType: ScalaType, implicitParams: Vector[Parameter] = Vector(),
	                description: String = "", headerComments: Vector[String] = Vector(),
	                isProtected: Boolean = false, isOverridden: Boolean = false) =
		apply(ComputedProperty, name, Code.empty, if (isProtected) Protected else Public, Some(outputType),
			implicitParams, description, headerComments, isOverridden)
}

/**
  * Used for declaring properties in scala code files
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
case class PropertyDeclaration(declarationType: PropertyDeclarationType, name: String, bodyCode: Code,
                               visibility: Visibility = Public, explicitOutputType: Option[ScalaType] = None,
                               implicitParams: Vector[Parameter] = Vector(), description: String = "",
                               headerComments: Vector[String] = Vector(),
                               isOverridden: Boolean = false)
	extends FunctionDeclaration with Mergeable[PropertyDeclaration, PropertyDeclaration]
{
	// COMPUTED -------------------------------------------
	
	/**
	  * @return Whether this is a computed property
	  */
	def isComputed = declarationType == ComputedProperty
	
	/**
	  * @return Whether the value of this property is stored (not computed)
	  */
	def isStored = !isComputed
	
	
	// IMPLEMENTED  ---------------------------------------
	
	override def keyword = declarationType.toScala
	
	override def returnDescription = ""
	
	override protected def params =
		if (implicitParams.nonEmpty) Some(Parameters(implicits = implicitParams)) else None
	
	override def mergeWith(other: PropertyDeclaration) =
	{
		val conflictsBuilder = new VectorBuilder[MergeConflict]()
		// Checks whether the base declaration is the same
		val myBase = basePart
		val theirBase = other.basePart
		if (myBase != theirBase)
			conflictsBuilder += MergeConflict.line(theirBase.text, myBase.text, s"$name declarations differ")
		// FIXME: This yields consistently wrong results
		if (bodyCode != other.bodyCode)
			conflictsBuilder ++= bodyCode.conflictWith(other.bodyCode, s"$name implementation differs")
		if (explicitOutputType.exists { myType => other.explicitOutputType.exists { _ != myType } })
			conflictsBuilder += MergeConflict.line(other.explicitOutputType.get.toString,
				explicitOutputType.get.toString, "Implementations specify different return type")
		
		PropertyDeclaration(declarationType, name, bodyCode, visibility min other.visibility,
			explicitOutputType.orElse(other.explicitOutputType),
			implicitParams ++
				other.implicitParams.filterNot { p => implicitParams.exists { _.dataType == p.dataType } },
			description.notEmpty.getOrElse(other.description),
			headerComments ++ other.headerComments.filterNot(headerComments.contains),
			isOverridden || other.isOverridden
		) -> conflictsBuilder.result()
	}
}
