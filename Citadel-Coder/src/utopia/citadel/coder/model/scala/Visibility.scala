package utopia.citadel.coder.model.scala

/**
  * An enumeration representing different method / property visibility options in Scala
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
sealed trait Visibility extends ScalaConvertible

object Visibility
{
	/**
	  * Declares that an item is visible to all
	  */
	case object Public extends Visibility
	{
		override def toScala = ""
	}
	
	/**
	  * Declares that an item is only visible within the class
	  */
	case object Private extends Visibility
	{
		override def toScala = "private"
	}
	
	/**
	  * Declares that an item is visible only within the class and its sub-classes
	  */
	case object Protected extends Visibility
	{
		override def toScala = "protected"
	}
}
