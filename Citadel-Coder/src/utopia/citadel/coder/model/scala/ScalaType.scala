package utopia.citadel.coder.model.scala

import scala.language.implicitConversions

object ScalaType
{
	// IMPLICIT  ------------------------------
	
	// Implicitly converts from a reference
	implicit def referenceToType(reference: Reference): ScalaType = apply(reference)
	
	
	// OTHER    -------------------------------
	
	/**
	  * @param name Name of the (basic) data type
	  * @return A data type with that name and no import
	  */
	def basic(name: String) = apply(Left(name))
	
	/**
	  * @param reference A reference
	  * @return A data type based on that reference
	  */
	def apply(reference: Reference): ScalaType = apply(Right(reference))
}

/**
  * Represents either a basic data type, which doesn't require an import, or a custom reference
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
case class ScalaType(data: Either[String, Reference]) extends ScalaConvertible
{
	// COMPUTED ---------------------------------
	
	/**
	  * @return Reference used by this type. None if not referring to any external type.
	  */
	def references = data.toOption
	
	
	// IMPLEMENTED  -----------------------------
	
	override def toScala = data match
	{
		case Left(basic) => basic
		case Right(reference) => reference.target
	}
}
