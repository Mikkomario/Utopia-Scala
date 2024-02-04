package utopia.genesis.event.keyboard

/**
  * Common trait for keyboard key -related events
  * @author Mikko Hilpinen
  * @since 03/02/2024, v3.6
  */
trait KeyEvent
{
	/**
	  * @return Index of the key associated with this event
	  */
	def index: Int
}
