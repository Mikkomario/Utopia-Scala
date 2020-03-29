package utopia.inception.handling

/**
  * Handleable objects are called through handlers
  * @author Mikko Hilpinen
  * @since 5.4.2019, v2+
  */
trait Handleable
{
	/**
	  * @return The handleable instance this handleable is dependent from, if there is one
	  */
	def parent: Option[Handleable]
	
	/**
	  * @param handlerType The type of handler doing the handling
	  * @return Whether this handleable instance may be called by a handler of the target handler type,
	  *         provided the handler supports this handleable instance
	  */
	def allowsHandlingFrom(handlerType: HandlerType): Boolean
}
