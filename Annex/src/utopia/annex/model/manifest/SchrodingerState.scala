package utopia.annex.model.manifest

import utopia.flow.operator.Sign.{Negative, Positive}
import utopia.flow.operator.{BinarySigned, Sign}

import scala.language.implicitConversions

/**
  * An enumeration for different states a Schrödinger item may be or appear in
  * @author Mikko Hilpinen
  * @since 20.11.2022, v1.4
  */
sealed trait SchrodingerState
{
	// ABSTRACT ------------------------
	
	/**
	  * @return Whether this is (potentially) a temporary state, and may still change
	  */
	def isFlux: Boolean
	
	/**
	  * @return The sign, or expectancy, of this state.
	  *         If Positive, it is apparently more likely for this state to resolve to Alive.
	  *         If Negative, it is apparently more likely for this state to resolve to Dead.
	  *         If None, if is undetermined whether this state is likely to resolve to Alive or to Dead.
	  */
	def signOption: Option[Sign]
	
	
	// COMPUTED -----------------------
	
	/**
	  * @return Whether this is a permanent state and will not change (anymore)
	  */
	def isFinal = !isFlux
	
	/**
	  * @return Whether this state has a positive or negative expectancy
	  */
	def isSigned = signOption.isDefined
}

object SchrodingerState
{
	object Flux
	{
		// ATTRIBUTES   -----------------
		
		private val instance: Flux = new Flux {
			override def signOption = None
			override def expectancy = None
		}
		
		
		// IMPLEMENTED  -----------------
		
		implicit def instantiate(f: Flux.type): Flux = f.instance
		
		
		// OTHER    ---------------------
		
		/**
		  * @param sign A sign
		  * @return A flux state with that sign
		  */
		def apply(sign: Sign): SignedFlux = sign match {
			case Positive => PositiveFlux
			case Negative => NegativeFlux
		}
		/**
		  * @param sign A sign, or None if undefined
		  * @return A flux state with that sign (or lack thereof)
		  */
		def apply(sign: Option[Sign]): Flux = sign match {
			case Some(sign) => apply(sign)
			case None => instance
		}
		/**
		  * @param isPositive Whether this flux is expected to resolve into a success
		  * @return A flux with positive or negative expectancy
		  */
		def apply(isPositive: Boolean): Flux = apply(Sign(isPositive))
	}
	/**
	  * A common trait for fluctuating / temporary Schrödinger states.
	  * Also selfesents a Schrödinger state by itself.
	  */
	sealed trait Flux extends SchrodingerState {
		// ABSTRACT ---------------------
		
		/**
		  * @return The expectancy of which state this state may resolve to
		  */
		def expectancy: Option[Final]
		
		
		// IMPLEMENTED  -----------------
		
		override def isFlux = true
	}
	/**
	  * A common trait for Flux states which have a sign.
	  */
	sealed trait SignedFlux extends Flux with BinarySigned[SignedFlux] {
		override def self = this
		override def signOption = Some(sign)
		override def expectancy = Some(Final(sign))
	}
	object Final {
		/**
		  * @param sign A sign
		  * @return A final Schrödinger state matching that sign
		  */
		def apply(sign: Sign): Final = sign match {
			case Positive => Alive
			case Negative => Dead
		}
		/**
		  * @param isAlive Whether to return Alive (true) or Dead (false)
		  * @return Alive or Dead
		  */
		def apply(isAlive: Boolean) = if (isAlive) Alive else Dead
	}
	/**
	  * A common trait for final (stable / resolved) Schrödinger states
	  */
	sealed trait Final extends SchrodingerState with BinarySigned[Final] {
		override def self = this
		override def isFlux = false
		override def signOption = Some(sign)
	}
	
	/**
	  * A flux state with positive expectancy
	  */
	case object PositiveFlux extends SignedFlux {
		override def isPositive = true
		override def unary_- = NegativeFlux
	}
	/**
	  * A flux state with negative expectancy
	  */
	case object NegativeFlux extends SignedFlux {
		override def isPositive = false
		override def unary_- = PositiveFlux
	}
	/**
	  * The success state (the cat was found to be alive)
	  */
	case object Alive extends Final {
		override def isPositive = true
		override def unary_- = Dead
	}
	/**
	  * The (unrecoverable) failure state (the cat was found to be dead)
	  */
	case object Dead extends Final {
		override def isPositive = false
		override def unary_- = Alive
	}
}
