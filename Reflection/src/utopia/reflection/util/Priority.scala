package utopia.reflection.util

import utopia.flow.util.RichComparable

/**
  * An enumeration for different levels of priority used in various contexts
  * @author Mikko Hilpinen
  * @since 25.11.2020, v2
  */
sealed trait Priority extends RichComparable[Priority]
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
	/**
	  * The highest priority
	  */
	case object VeryHigh extends Priority
	{
		override val index = 2
	}
	
	/**
	  * A higher than normal priority
	  */
	case object High extends Priority
	{
		override val index = 1
	}
	
	/**
	  * The "0 level" priority
	  */
	case object Normal extends Priority
	{
		override val index = 0
	}
	
	/**
	  * A lowered priority
	  */
	case object Low extends Priority
	{
		override val index = -1
	}
	
	/**
	  * The lowest priority
	  */
	case object VeryLow extends Priority
	{
		override val index = -2
	}
}
