package utopia.citadel.coder.model.scala

/**
  * An enumeration for different types of property declarations
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
sealed trait PropertyDeclarationType extends ScalaConvertible

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
