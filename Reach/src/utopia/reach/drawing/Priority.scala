package utopia.reach.drawing

import utopia.flow.operator.ordering.SelfComparable

/**
  * An enumeration for different levels of priority used in various contexts
  * @author Mikko Hilpinen
  * @since 25.11.2020, v0.1
  */
sealed trait Priority extends SelfComparable[Priority]
{
	// ABSTRACT	-----------------------------
	
	/**
	  * A priority index matching this priority. Higher values mean higher priorities.
	  */
	val index: Int
	
	
	// IMPLEMENTED	------------------------
	
	override def compareTo(o: Priority) = index - o.index
}

object Priority
{
	// ATTRIBUTES	------------------------
	
	/**
	  * Available priorities in descending order
	  */
	val descending = Vector[Priority](VeryHigh, High, Normal, Low, VeryLow)
	
	
	// NESTED	----------------------------
	
	/**
	  * The highest priority
	  */
	case object VeryHigh extends Priority
	{
		override val index = 2
		override def self = this
	}
	
	/**
	  * A higher than normal priority
	  */
	case object High extends Priority
	{
		override val index = 1
		override def self = this
	}
	
	/**
	  * The "0 level" priority
	  */
	case object Normal extends Priority
	{
		override val index = 0
		override def self = this
	}
	
	/**
	  * A lowered priority
	  */
	case object Low extends Priority
	{
		override val index = -1
		override def self = this
	}
	
	/**
	  * The lowest priority
	  */
	case object VeryLow extends Priority
	{
		override val index = -2
		override def self = this
	}
}
