package utopia.genesis.event

object ConsumeEvent
{
	/**
	  * Creates a new consume event
	  * @param getConsumer Provides description of consumer (call-by-name)
	  * @return A new consume event
	  */
	def apply(getConsumer: => String) = new ConsumeEvent(getConsumer)
}

/**
  * An event generated when another event is consumed
  * @author Mikko Hilpinen
  * @since 14.11.2019, v2.1+
  * @param getConsumer Entity that consumed the related item (call-by-name)
  */
class ConsumeEvent(getConsumer: => String)
{
	// ATTRIBUTES	----------------------
	
	/**
	  * Description of entity that consumed the related event
	  */
	lazy val consumer = getConsumer
}
