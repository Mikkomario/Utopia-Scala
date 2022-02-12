package utopia.vault.coder.model.scala.datatype

import utopia.vault.coder.model.scala.code.CodePiece
import utopia.vault.coder.model.scala.datatype.TypeVariance.{Contravariance, Covariance, Invariance}
import utopia.vault.coder.model.scala.doc.ScalaDocKeyword.TypeParam
import utopia.vault.coder.model.scala.doc.ScalaDocPart
import utopia.vault.coder.model.scala.template.{ScalaConvertible, ScalaDocConvertible}

object GenericType
{
	/**
	  * Creates a covariant (+) generic type
	  * @param name Name of this type
	  * @param requirement A type requirement to apply (optional)
	  * @return A new type
	  */
	def covariant(name: String, requirement: Option[TypeRequirement] = None) =
		apply(name, requirement, Covariance)
	/**
	  * Creates a contravariant (-) generic type
	  * @param name Name of this type
	  * @param requirement A type requirement to apply (optional)
	  * @return A new type
	  */
	def contravariant(name: String, requirement: Option[TypeRequirement] = None) =
		apply(name, requirement, Contravariance)
	/**
	  * Creates a generic type which is a subtype of another type
	  * @param name Name of this type
	  * @param parent Required parent type
	  * @param variance Variance of this type (default = invariance)
	  * @return A new type
	  */
	def childOf(name: String, parent: ScalaType, variance: TypeVariance = Invariance) =
		apply(name, Some(TypeRequirement.childOf(parent)), variance)
	/**
	  * Creates a generic type which is a supertype of another type
	  * @param name Name of this type
	  * @param child Required child type
	  * @param variance Variance of this type (default = invariance)
	  * @return A new type
	  */
	def parentOf(name: String, child: ScalaType, variance: TypeVariance = Invariance) =
		apply(name, Some(TypeRequirement.parentOf(child)), variance)
}

/**
  * Used for defining generic types, used in generic classes and methods
  * @author Mikko Hilpinen
  * @since 12.2.2022, v1.5
  * @param name        Name used for this generic type (e.g. "A")
  * @param requirement A type requirement that restricts this generic type (optional)
  * @param variance    Type variance applied (default = invariant)
  * @param description Description of this generic type (in documentation, default = empty)
  */
case class GenericType(name: String, requirement: Option[TypeRequirement] = None, variance: TypeVariance = Invariance,
                       description: String = "")
	extends ScalaConvertible with ScalaDocConvertible
{
	// COMPUTED ----------------------------------
	
	/**
	  * @return A scala type representing this generic type
	  */
	def toScalaType = ScalaType.basic(name)
	
	
	// IMPLEMENTED  ------------------------------
	
	override def toScala =
	{
		val mainPart = CodePiece(variance.typePrefix + name)
		requirement match {
			case Some(r) => mainPart.append(r.toScala, " ")
			case None => mainPart
		}
	}
	
	override def documentation =
		if (description.isEmpty) Vector() else Vector(ScalaDocPart(TypeParam(name), description))
}
