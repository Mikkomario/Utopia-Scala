package utopia.genesis.util

import utopia.flow.operator.LinearScalable

/**
  * Scalable instances can be scaled linearly
  * @author Mikko Hilpinen
  * @since 20.6.2019, v2.1+
  */
@deprecated("Please use LinearScalable instead", "v2.6")
trait Scalable[+Repr] extends LinearScalable[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * @return A representation of 'this'
	  */
	def repr: Repr
}
