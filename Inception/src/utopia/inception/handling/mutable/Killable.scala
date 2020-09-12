package utopia.inception.handling.mutable

import utopia.inception.handling.Mortal

/**
  * Killable instances can be forcibly killed, which removes them from any associated Handler
  * @author Mikko Hilpinen
  * @since 5.4.2019, v2+
  */
trait Killable extends Mortal
{
	// ABSTRACT	-------------------------
	
	/**
	  * Kills this instance, after which calls to isDead should return true
	  */
	def kill(): Unit
}
