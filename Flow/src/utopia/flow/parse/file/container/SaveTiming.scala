package utopia.flow.parse.file.container

import scala.concurrent.duration.FiniteDuration

/**
  * An enumeration for different ways of timing when cached data is saved to local filesystem
  * @author Mikko Hilpinen
  * @since 13.6.2020, v1.8
  */
sealed trait SaveTiming

object SaveTiming
{
	/**
	  * This timing logic saves changes whenever they occur. Best for files that contain critical information that
	  * don't get updated too often.
	  */
	case object Immediate extends SaveTiming
	
	/**
	  * This timing logic saves changes some time after they occur, hoping to decrease the number of saves required.
	  * Best for files that get updated often.
	  * @param duration The maximum duration before a change must be saved
	  */
	case class Delayed(duration: FiniteDuration) extends SaveTiming
	
	/**
	  * This timing logic only saves changes when the jvm is about to close.
	  */
	case object OnJvmClose extends SaveTiming
	
	/**
	  * This logic doesn't automatically save file data. Each save must be manually triggered.
	  */
	case object OnlyOnTrigger extends SaveTiming
}
