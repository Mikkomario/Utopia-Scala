package utopia.vault.coder.model.scala.datatype

import utopia.vault.coder.model.scala.template.ScalaConvertible

/**
  * An enumeration for type limit styles
  * @author Mikko Hilpinen
  * @since 12.2.2022, v1.5
  */
sealed trait InheritanceLimitType extends ScalaConvertible

object InheritanceLimitType
{
	// ATTRIBUTES   ----------------------
	
	/**
	  * All available limit type values
	  */
	val values = Vector(RequiredParent, RequiredChild)
	
	
	// NESTED   --------------------------
	
	/**
	  * This limit type specifies a required parent class / trait
	  */
	case object RequiredParent extends InheritanceLimitType
	{
		override val toScala = "<:"
	}
	/**
	  * This limit type specifies a required child class / trait
	  */
	case object RequiredChild extends InheritanceLimitType
	{
		override val toScala = ">:"
	}
}
