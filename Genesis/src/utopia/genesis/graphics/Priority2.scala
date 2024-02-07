package utopia.genesis.graphics

import utopia.flow.operator.ordering.SelfComparable

/**
  * An enumeration for different levels of priority used in various contexts
  * @author Mikko Hilpinen
  * @since 25.11.2020, v0.1
  */
sealed trait Priority2 extends SelfComparable[Priority2]
{
	// ABSTRACT	-----------------------------
	
	/**
	  * A priority index matching this priority. Higher values mean higher priorities.
	  */
	val index: Int
	
	
	// IMPLEMENTED	------------------------
	
	override def compareTo(o: Priority2) = index - o.index
}

object Priority2
{
	// ATTRIBUTES	------------------------
	
	/**
	  * Available priorities in descending order
	  */
	val descending = Vector[Priority2](VeryHigh, High, Normal, Low, VeryLow)
	
	
	// NESTED	----------------------------
	
	/**
	  * The highest priority
	  */
	case object VeryHigh extends Priority2
	{
		override val index = 2
		override def self = this
	}
	/**
	  * A higher than normal priority
	  */
	case object High extends Priority2
	{
		override val index = 1
		override def self = this
	}
	/**
	  * The "0 level" priority
	  */
	case object Normal extends Priority2
	{
		override val index = 0
		override def self = this
	}
	/**
	  * A lowered priority
	  */
	case object Low extends Priority2
	{
		override val index = -1
		override def self = this
	}
	/**
	  * The lowest priority
	  */
	case object VeryLow extends Priority2
	{
		override val index = -2
		override def self = this
	}
}
