package utopia.flow.event.model

import utopia.flow.event.model.Destiny.Sealed
import utopia.flow.operator.combine.Combinable

/**
  * An enumeration for different types of changing item states / future predictions
  * @author Mikko Hilpinen
  * @since 15.11.2023, v2.3
  */
sealed trait Destiny extends Combinable[Destiny, Destiny]
{
	// ABSTRACT ----------------------
	
	/**
	  * @return Whether this destiny has been sealed already
	  */
	def hasBeenSealed: Boolean
	/**
	  * @return Whether it is possible to seal this destiny.
	  *         Also returns true if already sealed.
	  */
	def isPossibleToSeal: Boolean
	
	/**
	  * @return Copy of this destiny that is possible to seal
	  */
	def possibleToSeal: Destiny
	/**
	  * @return Copy of this destiny that is currently in a flux state. I.e. not sealed (yet).
	  *         Doesn't prevent sealing again in the future.
	  */
	def flux: Destiny
	
	
	// COMPUTED --------------------
	
	/**
	  * @return Whether this destiny has not (yet) been sealed
	  */
	def hasNotBeenSealed = !hasBeenSealed
	/**
	  * @return Whether it is impossible for this destiny to be or to become sealed
	  */
	def cannotSeal = !isPossibleToSeal
	
	
	// OTHER    --------------------
	
	/**
	  * Converts this destiny to sealed if the specified condition is met
	  * @param condition A condition that marks this destiny as sealed.
	  *                  Only called for non-sealed destinies.
	  * @return Copy of this destiny that may have been marked as sealed
	  */
	def sealedIf(condition: => Boolean) =
		if (hasBeenSealed) this else if (condition) Sealed else this
	/**
	  * Converts this destiny to something that's possible to seal if the specified condition is met
	  * @param condition A condition that makes this destiny possible to seal (i.e. to make final)
	  * @return Copy of this destiny that gained the ability to become sealed if the specified condition was met
	  */
	def possibleToSealIf(condition: => Boolean) =
		if (isPossibleToSeal) this else if (condition) possibleToSeal else this
	/**
	  * Converts this destiny to something that's not yet sealed, if the specified condition is met
	  * @param condition A condition that makes this destiny not yet sealed
	  * @return Copy of this destiny that is no longer sealed if the specified condition was met
	  */
	def fluxIf(condition: => Boolean) = if (hasNotBeenSealed) this else if (condition) flux else this
}

object Destiny
{
	// ATTRIBUTES   ----------------------
	
	/**
	  * All possible values of this enumeration
	  */
	lazy val values = Vector[Destiny](Sealed, MaySeal, ForeverFlux)
	
	
	// VALUES   --------------------------
	
	/**
	  * State where the item in question has been sealed/fixed to remain at it's current value
	  */
	case object Sealed extends Destiny
	{
		// IMPLEMENTED  --------------------
		
		override def hasBeenSealed: Boolean = true
		override def isPossibleToSeal: Boolean = true
		override def possibleToSeal = this
		override def flux = MaySeal
		
		override def +(other: Destiny): Destiny = if (other.hasBeenSealed) this else other
	}
	/**
	  * State where the item in question may, at some point in the future,
	  * become sealed so that it might not be altered again.
	  */
	case object MaySeal extends Destiny
	{
		override def hasBeenSealed: Boolean = false
		override def isPossibleToSeal: Boolean = true
		override def possibleToSeal = this
		override def flux = this
		
		override def +(other: Destiny): Destiny = if (other.isPossibleToSeal) this else other
	}
	/**
	  * State where an item is known to have an uncertain element now and forever in the future.
	  * I.e. that it will never be sealed to a certain value.
	  */
	case object ForeverFlux extends Destiny
	{
		override def hasBeenSealed: Boolean = false
		override def isPossibleToSeal: Boolean = false
		override def possibleToSeal = MaySeal
		override def flux = this
		
		override def +(other: Destiny): Destiny = this
	}
}