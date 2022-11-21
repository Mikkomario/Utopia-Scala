package utopia.annex.model.schrodinger

import utopia.annex.model.schrodinger.SchrodingerState.{Alive, Dead, Final, Flux}
import utopia.flow.util.UncertainBoolean
import utopia.flow.util.UncertainBoolean.{Certain, Uncertain}

/**
  * A common trait for items which have a Schrödinger state, i.e. may be dead, alive or undetermined (Flux).
  * @author Mikko Hilpinen
  * @since 20.11.2022, v1.4
  */
trait HasSchrodingerState
{
	// ABSTRACT ------------------------
	
	def state: SchrodingerState
	
	
	// COMPUTED ------------------------
	
	/**
	  * @return Whether this is the final manifest of the applicable Schrödinger item
	  */
	def isFinal = state.isFinal
	/**
	  * @return Whether this item is in a state of flux,
	  *         meaning that it is undetermined whether it is or will be alive (successful) or dead (failure)
	  */
	def isFlux = state.isFlux
	
	/**
	  * @return Certain(true) if this is the final success state,
	  *         Certain(false) if this is the final failure state,
	  *         Uncertain otherwise
	  */
	def isAlive: UncertainBoolean = state match {
		case Alive => Certain(true)
		case Dead => Certain(false)
		case _ => Uncertain
	}
	/**
	  * @return Certain(false) if this is the final success state,
	  *         Certain(true) if this is the final failure state,
	  *         Uncertain otherwise
	  */
	def isDead = !isAlive
	
	/**
	  * @return If this manifest, or some of the consecutive manifests, has the potential of being successful
	  */
	def mayLive = isAlive.mayBeTrue
	/**
	  * @return If this manifest, or some of the consecutive manifests, has the potential of failing
	  */
	def mayBeDead = isAlive.mayBeFalse
	
	/**
	  * @return Some(Alive) if this item is alive, or is expected to stay alive
	  *         Some(Dead) if this item is dead, or is expected to die,
	  *         None if this item is in a state of flux and no life expectancy may be given
	  */
	def lifeExpectancy: Option[Final] = state match {
		case f: Final => Some(f)
		case f: Flux => f.expectancy
	}
}
