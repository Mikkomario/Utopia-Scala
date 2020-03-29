package utopia.inception.handling

/**
  * Mortal instances can die eventually. In the context of Inception, this means that those instances
  * will be removed from any cooperative Handler
  * @author Mikko Hilpinen
  * @since 5.4.2019, v2+
  */
trait Mortal
{
	/**
	  * @return Whether this mortal instance should be considered dead
	  */
	def isDead: Boolean
	
	/**
	  * @return Whether this mortal instance should be considered alive
	  */
	def isAlive = !isDead
}
