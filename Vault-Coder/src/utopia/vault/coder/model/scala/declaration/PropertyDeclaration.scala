package utopia.vault.coder.model.scala.declaration

import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.merging.Mergeable
import utopia.vault.coder.model.scala.code.Code
import utopia.vault.coder.model.scala.Visibility.{Protected, Public}
import utopia.vault.coder.model.scala.datatype.{GenericType, ScalaType}
import utopia.vault.coder.model.scala.{Parameter, Parameters, Visibility}
import utopia.vault.coder.model.scala.declaration.PropertyDeclarationType.ComputedProperty

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
	  * @param isLowMergePriority Whether this declaration should be considered the lower priority
	  *                           implementation when merging with another version
	  * @return A new property declaration
	  */
	def newAbstract(name: String, outputType: ScalaType, implicitParams: Vector[Parameter] = Vector(),
	                description: String = "", headerComments: Vector[String] = Vector(),
	                isProtected: Boolean = false, isOverridden: Boolean = false, isLowMergePriority: Boolean = false) =
		apply(ComputedProperty, name, Code.empty, if (isProtected) Protected else Public, Some(outputType),
			implicitParams, description, headerComments, isOverridden, isLowMergePriority)
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
                               isOverridden: Boolean = false, isImplicit: Boolean = false,
                               isLowMergePriority: Boolean = false)
	extends FunctionDeclaration[PropertyDeclaration] with Mergeable[PropertyDeclaration, PropertyDeclaration]
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
	
	// Properties don't support generic types at this time
	override def genericTypes = Vector()
	
	override protected def params =
		if (implicitParams.nonEmpty) Some(Parameters(implicits = implicitParams)) else None
	
	override protected def makeCopy(visibility: Visibility, genericTypes: Seq[GenericType],
	                                parameters: Option[Parameters], bodyCode: Code,
	                                explicitOutputType: Option[ScalaType], description: String,
	                                returnDescription: String, headerComments: Vector[String],
	                                isOverridden: Boolean, isImplicit: Boolean) =
		PropertyDeclaration(declarationType, name, bodyCode, visibility, explicitOutputType,
			parameters.map { _.implicits }.getOrElse(implicitParams),
			description.notEmpty.getOrElse(returnDescription), headerComments, isOverridden, isImplicit,
			isLowMergePriority)
}
