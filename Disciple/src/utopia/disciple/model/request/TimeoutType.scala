package utopia.disciple.model.request

import utopia.flow.collection.immutable.Pair

/**
  * An enumeration for the supported timeout types
  * @author Mikko Hilpinen
  * @since 15.5.2020, v1.3
  */
sealed trait TimeoutType
{
	/**
	  * @return A string representation of this timeout type
	  */
	def name: String
	
	override def toString = name
}

object TimeoutType
{
	/**
	  * Timeout used when reading data from the server (maximum duration between two data packets)
	  */
	case object ReadTimeout extends TimeoutType
	{
		override val name = "Read"
	}
	/**
	  * Timeout used before connection is attempted (when other requests are queued in the manager)
	  */
	case object ManagerTimeout extends TimeoutType
	{
		override val name = "Make Request"
	}
	
	/**
	 * Timeout used when establishing connection with the server
	 */
	@deprecated("Deprecated for removal. Request-specific connection timeout was removed in Apache HttpClient v6", "v1.9.3")
	case object ConnectionTimeout extends TimeoutType
	{
		override val name = "Connect"
	}
	
	/**
	  * All timeout types
	  */
	val values = Pair[TimeoutType](ManagerTimeout, ReadTimeout)
}
