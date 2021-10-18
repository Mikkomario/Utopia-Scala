package utopia.vault.coder.model.scala.declaration

import utopia.vault.coder.model.scala.code.Code
import utopia.vault.coder.model.scala.Visibility.Public
import utopia.vault.coder.model.scala.{Parameter, Reference, ScalaType, Visibility}
import utopia.vault.coder.model.scala.template.ScalaConvertible

/**
  * An enumeration for different types of property declarations
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
sealed trait PropertyDeclarationType extends ScalaConvertible
{
	/**
	  * Creates a new property declaration based on this type
	  * @param name Name of the declared property
	  * @param references References made within the code (default = empty)
	  * @param visibility Visibility of this property (default = public)
	  * @param explicitOutputType Data type returned by this function when explicitly defined (optional)
	  * @param description Documentation for this property
	  * @param implicitParams Implicit parameters accepted by this (computed) property
	  * @param isOverridden Whether this property overrides a base member (default = false)
	  * @param line1 First line of code
	  * @param moreLines More lines of code
	  * @return A new property declaration
	  */
	def apply(name: String, references: Set[Reference] = Set(), visibility: Visibility = Public,
	          explicitOutputType: Option[ScalaType] = None, description: String = "",
	          implicitParams: Vector[Parameter] = Vector(), isOverridden: Boolean = false)
	         (line1: String, moreLines: String*) =
		PropertyDeclaration(this, name, Code.from(line1 +: moreLines.toVector).referringTo(references), visibility,
			explicitOutputType, description, implicitParams, isOverridden)
}

object PropertyDeclarationType
{
	/**
	  * Used for defining immutable values
	  */
	case object ImmutableValue extends PropertyDeclarationType
	{
		override def toScala = "val"
	}
	
	/**
	  * Used for defining mutable properties / variables
	  */
	case object Variable extends PropertyDeclarationType
	{
		override def toScala = "var"
	}
	
	/**
	  * Used for defining properties which are calculated when called
	  */
	case object ComputedProperty extends PropertyDeclarationType
	{
		override def toScala = "def"
	}
}
