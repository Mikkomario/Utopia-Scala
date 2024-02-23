package utopia.genesis.graphics

import utopia.flow.operator.Steppable
import utopia.flow.operator.enumeration.Extreme
import utopia.flow.operator.enumeration.Extreme.{Max, Min}
import utopia.flow.operator.ordering.SelfComparable
import utopia.flow.operator.sign.Sign
import utopia.flow.operator.sign.Sign.{Negative, Positive}

/**
  * An enumeration for different levels of priority used in various contexts
  * @author Mikko Hilpinen
  * @since 25.11.2020, v0.1
  */
sealed trait Priority extends SelfComparable[Priority] with Steppable[Priority]
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
		
		override def next(direction: Sign): Priority = direction match {
			case Positive => this
			case Negative => High
		}
		override def is(extreme: Extreme): Boolean = extreme == Max
	}
	/**
	  * A higher than normal priority
	  */
	case object High extends Priority
	{
		override val index = 1
		override def self = this
		
		override def next(direction: Sign): Priority = direction match {
			case Positive => VeryHigh
			case Negative => Normal
		}
		override def is(extreme: Extreme): Boolean = false
	}
	/**
	  * The "0 level" priority
	  */
	case object Normal extends Priority
	{
		override val index = 0
		override def self = this
		
		override def next(direction: Sign): Priority = direction match {
			case Positive => High
			case Negative => Low
		}
		override def is(extreme: Extreme): Boolean = false
	}
	/**
	  * A lowered priority
	  */
	case object Low extends Priority
	{
		override val index = -1
		override def self = this
		
		override def next(direction: Sign): Priority = direction match {
			case Positive => Normal
			case Negative => VeryLow
		}
		override def is(extreme: Extreme): Boolean = false
	}
	/**
	  * The lowest priority
	  */
	case object VeryLow extends Priority
	{
		override val index = -2
		override def self = this
		
		override def next(direction: Sign): Priority = direction match {
			case Positive => Low
			case Negative => this
		}
		override def is(extreme: Extreme): Boolean = extreme == Min
	}
}
