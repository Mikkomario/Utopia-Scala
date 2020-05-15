package utopia.disciple.http.request

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
	  * Timeout used when establishing connection with the server
	  */
	case object ConnectionTimeout extends TimeoutType
	{
		override def name = "Connect"
	}
	
	/**
	  * Timeout used when reading data from the server (maximum duration between two data packets)
	  */
	case object ReadTimeout extends TimeoutType
	{
		override def name = "Read"
	}
	
	/**
	  * Timeout used before connection is attempted (when other requests are queued in the manager)
	  */
	case object ManagerTimeout extends TimeoutType
	{
		override def name = "Make Request"
	}
	
	/**
	  * All timeout types
	  */
	val values = Vector[TimeoutType](ManagerTimeout, ConnectionTimeout, ReadTimeout)
}
