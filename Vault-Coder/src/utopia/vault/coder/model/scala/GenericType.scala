package utopia.vault.coder.model.scala

import utopia.vault.coder.model.scala.ScalaDocKeyword.TypeParam
import utopia.vault.coder.model.scala.TypeVariance.Invariance
import utopia.vault.coder.model.scala.code.CodePiece
import utopia.vault.coder.model.scala.template.{ScalaConvertible, ScalaDocConvertible}

/**
  * Used for defining generic types, used in generic classes and methods
  * @author Mikko Hilpinen
  * @since 12.2.2022, v1.5
  * @param name Name used for this generic type (e.g. "A")
  * @param requirement A type requirement that restricts this generic type (optional)
  * @param variance Type variance applied (default = invariant)
  * @param description Description of this generic type (in documentation, default = empty)
  */
case class GenericType(name: String, requirement: Option[TypeRequirement] = None, variance: TypeVariance = Invariance,
                       description: String = "")
	extends ScalaConvertible with ScalaDocConvertible
{
	// IMPLEMENTED  ------------------------------
	
	override def toScala = {
		val mainPart = CodePiece(variance.typePrefix + name)
		requirement match {
			case Some(r) => mainPart.append(r.toScala, " ")
			case None => mainPart
		}
	}
	
	override def documentation =
		if (description.isEmpty) Vector() else Vector(ScalaDocPart(TypeParam(name), description))
}
