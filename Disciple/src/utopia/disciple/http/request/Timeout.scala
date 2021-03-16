package utopia.disciple.http.request

import scala.concurrent.duration.{Duration, FiniteDuration}

import utopia.flow.time.TimeExtensions._
import TimeoutType._

object Timeout
{
	/**
	  * An object where no timeouts are specified
	  */
	val empty = new Timeout(Map())
	
	/**
	  * @param timeoutType Type of timeout
	  * @param threshold Timeout threshold
	  * @return A new timeout for that type
	  */
	def apply(timeoutType: TimeoutType, threshold: FiniteDuration): Timeout = Timeout(Map(timeoutType -> threshold))
	
	/**
	  * @param first First timout pair
	  * @param second Second timeout pair
	  * @param more More timeout pairs
	  * @return A new timeout with specified values
	  */
	def apply(first: (TimeoutType, FiniteDuration), second: (TimeoutType, FiniteDuration),
			  more: (TimeoutType, FiniteDuration)*): Timeout = Timeout((more :+ first :+ second).toMap)
	
	/**
	  * @param connection Connection timeout (default = infinite)
	  * @param read Read timeout (default = infinite)
	  * @param manager Manager timeout (default = infinite)
	  * @return Timeout based on specified values
	  */
	def apply(connection: Duration = Duration.Inf, read: Duration = Duration.Inf,
			  manager: Duration = Duration.Inf): Timeout = new Timeout(Map(ConnectionTimeout -> connection,
		ReadTimeout -> read, ManagerTimeout -> manager).flatMap { case (k, v) => v.finite.map { k -> _ } })
	
	/**
	  * @param connectionTimeout Connection timeout threshold
	  * @return A timeout with only connection timeout set
	  */
	def forConnection(connectionTimeout: FiniteDuration) = apply(ConnectionTimeout, connectionTimeout)
	
	/**
	  * @param readTimeout Read timeout threshold
	  * @return A timeout with only read timeout set
	  */
	def forRead(readTimeout: FiniteDuration) = apply(ReadTimeout, readTimeout)
	
	/**
	  * @param managerTimeout Manager timeout threshold
	  * @return A timeout with only manager timeout set
	  */
	def forManager(managerTimeout: FiniteDuration) = apply(ManagerTimeout, managerTimeout)
}

/**
  * Used for specifying timeout thresholds when making requests
  * @author Mikko Hilpinen
  * @since 15.5.2020, v1.3
  */
case class Timeout(thresholds: Map[TimeoutType, FiniteDuration])
{
	// COMPUTED	----------------------------
	
	/**
	  * @return Whether this timeout is empty
	  */
	def isEmpty = thresholds.isEmpty
	
	/**
	  * @return Whether this timeout is not empty
	  */
	def nonEmpty = thresholds.nonEmpty
	
	/**
	  * @return Connection timeout threshold. Infinite if not otherwise specified.
	  */
	def forConnection = apply(ConnectionTimeout)
	
	/**
	  * @return Read timeout threshold. Infinite if not otherwise specified.
	  */
	def forRead = apply(ReadTimeout)
	
	/**
	  * @return Manager timeout threshold. Infinite if not otherwise specified.
	  */
	def forManager = apply(ManagerTimeout)
	
	/**
	  * @return Copy of this timeout with no limit on connection timeout
	  */
	def withoutConnectionTimeout = without(ConnectionTimeout)
	
	/**
	  * @return Copy of this timeout with no limit on read timeout
	  */
	def withoutReadTimeout = without(ReadTimeout)
	
	/**
	  * @return Copy of this timeout with no limit on manager timeout
	  */
	def withoutManagerTimeout = without(ManagerTimeout)
	
	
	// IMPLEMENTED	------------------------
	
	override def toString =
	{
		if (nonEmpty)
			thresholds.map { case (timeoutType, duration) =>
				s"$timeoutType: ${duration.description}"
			}.mkString(", ")
		else
			"No timeout"
	}
	
	
	// OTHER	----------------------------
	
	/**
	  * @param timeoutType Type of timeout
	  * @return Timeout for that timeout type. Infinite if not otherwise specified.
	  */
	def apply(timeoutType: TimeoutType) = get(timeoutType).getOrElse(Duration.Inf)
	
	/**
	  * @param timeoutType Type of timeout
	  * @return A specified timeout for that timeout type. None if no timeout has been specified
	  */
	def get(timeoutType: TimeoutType) = thresholds.get(timeoutType)
	
	/**
	  * @param other Another timeout
	  * @return A copy of this timeout with specified values from the other timeout overwriting those in this timeout
	  */
	def withThresholds(other: Timeout) = copy(thresholds = thresholds ++ other.thresholds)
	
	/**
	  * @param timeoutType Type of timeout to overwrite
	  * @param threshold A new timeout threshold for that type
	  * @return a copy of this timeout with specified timeout overwritten
	  */
	def withThreshold(timeoutType: TimeoutType, threshold: FiniteDuration) = copy(
		thresholds = thresholds + (timeoutType -> threshold))
	
	/**
	  * @param threshold New connection timeout
	  * @return A copy of this timeout with specified connection timeout
	  */
	def withConnectionTimeout(threshold: FiniteDuration) = withThreshold(ConnectionTimeout, threshold)
	
	/**
	  * @param threshold New read timeout
	  * @return A copy of this timeout with specified read timeout
	  */
	def withReadTimeout(threshold: FiniteDuration) = withThreshold(ReadTimeout, threshold)
	
	/**
	  * @param threshold New manager timeout
	  * @return A copy of this timeout with specified manager timeout
	  */
	def withManagerTimeout(threshold: FiniteDuration) = withThreshold(ManagerTimeout, threshold)
	
	/**
	  * @param timeoutType Timeout type
	  * @return A copy of this timeout without specified timeout type specified
	  */
	def without(timeoutType: TimeoutType) = copy(thresholds = thresholds - timeoutType)
	
	/**
	  * @param other Another timeout
	  * @return A combination of these timeouts where the smaller value is used for each specified type
	  */
	def min(other: Timeout) =
	{
		val allKeys = thresholds.keySet ++ other.thresholds.keySet
		val newThresholds = allKeys.map { key =>
			val value = Vector(get(key), other.get(key)).flatten.min
			key -> value
		}
		Timeout(newThresholds.toMap)
	}
	
	/**
	  * @param other Another timeout
	  * @return A combination of these timeouts where the larger value is used for each type specified in both timeouts.
	  *         If a timeout threshold is only specified in a single timeout instance, it is left unspecified in the
	  *         combined instance.
	  */
	def max(other: Timeout) =
	{
		val keys = thresholds.keySet & other.thresholds.keySet
		val newThresholds = keys.map { key =>
			val value = Vector(get(key), get(key)).flatten.max
			key -> value
		}
		Timeout(newThresholds.toMap)
	}
	
	/**
	  * @param other Another timeout
	  * @return A combination of these timeouts where values specified in both timeouts are combined together using +.
	  *         If a timeout threshold only exists in a single timeout instance, that threshold is used as is.
	  *         Therefore calling this method may actually shorten the resulting timeout from infinite to a specified value.
	  */
	def +(other: Timeout) =
	{
		val keys = thresholds.keySet ++ other.thresholds.keySet
		val newThresholds = keys.map { key =>
			val value = Vector(get(key), get(key)).flatten.reduce { _ + _ }
			key -> value
		}
		Timeout(newThresholds.toMap)
	}
}
