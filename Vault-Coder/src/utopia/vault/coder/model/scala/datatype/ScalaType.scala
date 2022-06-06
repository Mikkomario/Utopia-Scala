package utopia.vault.coder.model.scala.datatype

import utopia.vault.coder.model.scala.code.CodePiece
import utopia.vault.coder.model.scala.datatype.ScalaTypeCategory.{CallByName, Standard}
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
	  * @param contentType Collection content type
	  * @return Iterable type
	  */
	def iterable(contentType: ScalaType) = generic("Iterable", contentType)
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
	  * @param contentType Set content type
	  * @return A set type
	  */
	def set(contentType: ScalaType) = generic("Set", contentType)
	
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
	
	/**
	  * Creates a function scala type
	  * @param paramTypes Accepted parameter types (default = empty)
	  * @param resultType Resulting data type (raw)
	  * @param typeParams Generic type arguments for the resulting data type (default = empty)
	  * @return A functional scala type
	  */
	def function(paramTypes: ScalaType*)(resultType: Either[String, Reference], typeParams: ScalaType*) =
		apply(resultType, typeParams.toVector, ScalaTypeCategory.Function(paramTypes.toVector))
}

/**
  * Represents either a basic data type, which doesn't require an import, or a custom reference
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
case class ScalaType(data: Either[String, Reference], typeParameters: Vector[ScalaType] = Vector(),
                     category: ScalaTypeCategory = Standard)
	extends ScalaConvertible
{
	// ATTRIBUTES   ------------------------------
	
	override lazy val toScala: CodePiece =
	{
		val base = data match
		{
			case Left(basic) => CodePiece(basic)
			case Right(reference) => CodePiece(reference.target, Set(reference))
		}
		val withTypeParams = {
			if (typeParameters.isEmpty)
				base
			else
				base + typeParameters.map { _.toScala }.reduceLeft { _.append(_, ", ") }.withinSquareBrackets
		}
		category match
		{
			case Standard => withTypeParams
			case CallByName => withTypeParams.withPrefix("=> ")
			case ScalaTypeCategory.Function(parameterTypes) =>
				val parameterList = if (parameterTypes.isEmpty) CodePiece("()") else
					parameterTypes.map { _.toScala }.reduceLeft { _.append(_, ", ") }.withinParenthesis
				parameterList.append(withTypeParams, " => ")
		}
	}
	
	
	// OTHER    ------------------------
	
	/**
	  * @param parameterTypes A list of accepted parameter types
	  * @return A functional data type that returns this data type
	  */
	def fromParameters(parameterTypes: Vector[ScalaType]) = category match
	{
		// Functions that return functions don't handle references properly at this time
		case _ :ScalaTypeCategory.Function =>
			ScalaType(Left(toScala.text), category = ScalaTypeCategory.Function(parameterTypes))
		case _ => copy(category = ScalaTypeCategory.Function(parameterTypes))
	}
	
	/**
	  * @param other Another type
	  * @return Whether these types are similar, when considering their base types
	  */
	def isSimilarTo(other: ScalaType) = {
		val myPart = data match {
			case Right(ref) => ref.target
			case Left(str) => str
		}
		val theirPart = other.data match {
			case Right(ref) => ref.target
			case Left(str) => str
		}
		myPart == theirPart
	}
}
