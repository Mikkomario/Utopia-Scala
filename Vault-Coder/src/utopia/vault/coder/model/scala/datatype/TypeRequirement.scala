package utopia.vault.coder.model.scala.datatype

import utopia.vault.coder.model.scala.datatype.InheritanceLimitType.{RequiredChild, RequiredParent}
import utopia.vault.coder.model.scala.template.ScalaConvertible

object TypeRequirement
{
	/**
	  * @param parent Required parent type
	  * @return A requirement that allows child classes of the specified type
	  */
	def childOf(parent: ScalaType) = apply(parent, RequiredParent)
	/**
	  * @param child Required child type
	  * @return A requirement that allows supertypes of the specified type
	  */
	def parentOf(child: ScalaType) = apply(child, RequiredChild)
}

/**
  * Used for indicating that a (generic) type must conform or relate to another type
  * @author Mikko Hilpinen
  * @since 12.2.2022, v1.5
  */
case class TypeRequirement(restrictingType: ScalaType, restrictionType: InheritanceLimitType) extends ScalaConvertible
{
	override def toScala = restrictionType.toScala.append(restrictingType.toScala, " ")
}
