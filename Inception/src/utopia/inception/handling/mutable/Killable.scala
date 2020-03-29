package utopia.inception.handling.mutable

import utopia.inception.handling.Mortal

/**
  * Killable instances can be forcibly killed, which removes them from any associated Handler
  * @author Mikko Hilpinen
  * @since 5.4.2019, v2+
  */
trait Killable extends Mortal
{
	// ATTRIBUTES	----------------
	
	private var _dead = false
	
	
	// IMPLEMENTED	----------------
	
	override def isDead = _dead
	
	
	// OTHER	--------------------
	
	/**
	  * Kills this instance, resulting in instant death
	  */
	def kill() = _dead = true
}
