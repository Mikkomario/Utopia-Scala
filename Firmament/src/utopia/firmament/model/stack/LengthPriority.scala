package utopia.firmament.model.stack

/**
  * Enumeration trait for various priority settings for dynamic lengths (Eg. StackLength)
  * @author Mikko Hilpinen
  * @since 20.3.2020, Reflection v1
  */
sealed trait LengthPriority
{
	// ABSTRACT	------------------------
	
	/**
	  * @return Whether this priority lengths should be the first to shrink
	  */
	def shrinksFirst: Boolean
	/**
	  * @return Whether this priority lengths should be the first to expand
	  */
	def expandsFirst: Boolean
	
	/**
	  * @return A copy of this priority that shrinks easily
	  */
	def shrinking: LengthPriority
	/**
	  * @return A copy of this priority that expands easily
	  */
	def expanding: LengthPriority
	/**
	  * @return A copy of this priority that doesn't shrink easily
	  */
	def notShrinking: LengthPriority
	/**
	  * @return A copy of this priority that doesn't expand easily
	  */
	def notExpanding: LengthPriority
	
	
	// OTHER	------------------------
	
	/**
	  * @param adjustment Proposed adjustment
	  * @return Whether this priority is first to be adjusted by specified type of adjustment
	  */
	def isFirstAdjustedBy(adjustment: Double) = if (adjustment <= 0) shrinksFirst else expandsFirst
	
	/**
	  * @param other Another priority
	  * @return A minimum (more easily adjusted) between these two priorities
	  */
	def min(other: LengthPriority) = LengthPriority(
		shrinksFirst || other.shrinksFirst, expandsFirst || other.expandsFirst)
	
	/**
	  * @param other Another priority
	  * @return A maximum (less easily adjusted) between these two priorities
	  */
	def max(other: LengthPriority) = LengthPriority(
		shrinksFirst && other.shrinksFirst, expandsFirst && other.expandsFirst)
	
	/**
	  * @param other Another length priority
	  * @return Whether this priority allows expansion easier than the other
	  */
	def expandsEasierThan(other: LengthPriority) = expandsFirst && !other.expandsFirst
	
	/**
	  * @param other Another length priority
	  * @return Whether this priority allows shrinking easier than the other
	  */
	def shrinksEasierThan(other: LengthPriority) = shrinksFirst && !other.shrinksFirst
}

object LengthPriority
{
	/**
	  * Normal length priority resists both shrinking and expanding
	  */
	case object Normal extends LengthPriority
	{
		override def shrinksFirst = false
		
		override def expandsFirst = false
		
		override def shrinking = Shrinking
		
		override def expanding = Expanding
		
		override def notShrinking = this
		
		override def notExpanding = this
	}
	
	/**
	  * Low length priority is among the first to both shrink and to expand
	  */
	case object Low extends LengthPriority
	{
		override def shrinksFirst = true
		
		override def expandsFirst = true
		
		override def shrinking = this
		
		override def expanding = this
		
		override def notShrinking = Expanding
		
		override def notExpanding = Shrinking
	}
	
	/**
	  * Used when length should be easily shrinked but resist expanding
	  */
	case object Shrinking extends LengthPriority
	{
		override def shrinksFirst = true
		
		override def expandsFirst = false
		
		override def shrinking = this
		
		override def expanding = Low
		
		override def notShrinking = Normal
		
		override def notExpanding = this
	}
	
	/**
	  * Used when length should be easily expanded but resist shrinking
	  */
	case object Expanding extends LengthPriority
	{
		override def shrinksFirst = false
		
		override def expandsFirst = true
		
		override def shrinking = Low
		
		override def expanding = this
		
		override def notShrinking = this
		
		override def notExpanding = Normal
	}
	
	/**
	  * All currently introduced values of this enumeration
	  */
	val values = Vector(Normal, Expanding, Shrinking, Low)
	
	/**
	  * Finds a priority that has the specified properties
	  * @param shouldShrinkFirst Whether length should shrink easily
	  * @param shouldExpandFirst Whether length should expand easily
	  * @return A length priority with specified properties
	  */
	def apply(shouldShrinkFirst: Boolean, shouldExpandFirst: Boolean) =
	{
		if (shouldShrinkFirst)
		{
			if (shouldExpandFirst)
				Low
			else
				Shrinking
		}
		else if (shouldExpandFirst)
			Expanding
		else
			Normal
	}
}
