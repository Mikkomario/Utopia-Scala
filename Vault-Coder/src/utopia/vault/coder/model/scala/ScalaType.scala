package utopia.vault.coder.model.scala

import utopia.vault.coder.model.scala.template.ScalaConvertible

import scala.language.implicitConversions

object ScalaType
{
	// ATTRIBUTES   ---------------------------
	
	val string = basic("String")
	val int = basic("Int")
	val long = basic("Long")
	val double = basic("Double")
	val boolean = basic("Boolean")
	
	
	// IMPLICIT  ------------------------------
	
	// Implicitly converts from a reference
	implicit def referenceToType(reference: Reference): ScalaType = apply(reference)
	
	
	// OTHER    -------------------------------
	
	/**
	  * @param contentType Option content type
	  * @return An option type
	  */
	def option(contentType: ScalaType) = generic("Option", contentType)
	/**
	  * @param contentType Vector content type
	  * @return A vector type
	  */
	def vector(contentType: ScalaType) = generic("Vector", contentType)
	
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
	
	/**
	  * Creates a generic type
	  * @param reference Base type reference
	  * @param firstParam First type parameter
	  * @param moreParams More type parameters
	  * @return A generic type
	  */
	def generic(reference: Reference, firstParam: ScalaType, moreParams: ScalaType*) =
		apply(Right(reference), firstParam +: moreParams.toVector)
	/**
	  * Creates a generic type
	  * @param name Name of the basic generic class
	  * @param firstParam First type parameter
	  * @param moreParams More type parameters
	  * @return A generic type
	  */
	def generic(name: String, firstParam: ScalaType, moreParams: ScalaType*) =
		apply(Left(name), firstParam +: moreParams.toVector)
}

/**
  * Represents either a basic data type, which doesn't require an import, or a custom reference
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
case class ScalaType(data: Either[String, Reference], typeParameters: Vector[ScalaType] = Vector())
	extends ScalaConvertible
{
	// COMPUTED ---------------------------------
	
	/**
	  * @return Reference used by this type. None if not referring to any external type.
	  */
	def references: Set[Reference] = typeParameters.flatMap { _.references }.toSet ++ data.toOption
	
	
	// IMPLEMENTED  -----------------------------
	
	override def toScala =
	{
		val base = data match
		{
			case Left(basic) => basic
			case Right(reference) => reference.target
		}
		if (typeParameters.isEmpty)
			base
		else
			s"$base[${typeParameters.map { _.toScala }.mkString(", ")}]"
	}
}
