package utopia.coder.model.scala

import utopia.coder.model.scala.template.ScalaConvertible
import utopia.flow.operator.ordering.SelfComparable

/**
  * An enumeration representing different method / property visibility options in Scala
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
sealed trait Visibility extends ScalaConvertible with SelfComparable[Visibility]

object Visibility
{
	/**
	  * Declares that an item is visible to all
	  */
	case object Public extends Visibility
	{
		override def toScala = ""
		
		override def self = this
		
		override def compareTo(o: Visibility) = o match
		{
			case Public => 0
			case _ => 1
		}
	}
	/**
	  * Declares that an item is only visible within the class
	  */
	case object Private extends Visibility
	{
		override def toScala = "private"
		
		override def self = this
		
		override def compareTo(o: Visibility) = o match
		{
			case Private => 0
			case _ => -1
		}
	}
	/**
	  * Declares that an item is visible only within the class and its sub-classes
	  */
	case object Protected extends Visibility
	{
		override def toScala = "protected"
		
		override def self = this
		
		override def compareTo(o: Visibility) = o match
		{
			case Public => -1
			case Private => 1
			case _ => 0
		}
	}
}
