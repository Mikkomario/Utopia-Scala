package utopia.echo.model.vastai.instance

import utopia.flow.collection.immutable.{Pair, Single}

/**
 * An enumeration for different instance states
 * @author Mikko Hilpinen
 * @since 24.02.2026, v1.5
 */
sealed trait InstanceState
{
	/**
	 * @return Keys used for this status by Vast AI
	 */
	def keys: Seq[String]
	
	/**
	 * @return Whether the instance is (technically) usable in this state
	 */
	def instanceIsUsable: Boolean
	/**
	 * @return Whether the instance should be continued to be used in this state.
	 *         False if the instance is unusable OR if the instance may become unusable in the near future.
	 */
	def instanceShouldBeUsed: Boolean
}

object InstanceState
{
	// ATTRIBUTES   ---------------------
	
	/**
	 * Known instance states
	 */
	val knownValues = Vector[InstanceState](Loading, Active, Stopped, Disconnected, Restarting)
	
	
	// OTHER    -------------------------
	
	/**
	 * @param status An instance status string
	 * @return Instance state that matches that status string
	 */
	def forStatus(status: String) = {
		val lower = status.toLowerCase
		knownValues.find { _.keys.contains(lower) }.getOrElse { Unknown(lower) }
	}
	
	
	// VALUES   -------------------------
	
	/**
	 * State while the instance is being set up
	 */
	case object Loading extends InstanceState
	{
		override val keys: Seq[String] = Vector("loading", "creating", "")
		override val instanceIsUsable: Boolean = false
		override val instanceShouldBeUsed: Boolean = false
	}
	/**
	 * State when the instance is usable
	 */
	case object Active extends InstanceState
	{
		override val keys: Seq[String] = Vector("running", "open", "connect")
		override val instanceIsUsable: Boolean = true
		override val instanceShouldBeUsed: Boolean = true
	}
	/**
	 * State once the instance has been stopped / deactivated. Still preserves data & incurs costs.
	 */
	case object Stopped extends InstanceState
	{
		override val keys: Seq[String] = Pair("stopped", "inactive")
		override val instanceIsUsable: Boolean = false
		override val instanceShouldBeUsed: Boolean = false
	}
	
	/**
	 * State when an instance has lost connection to Vast AI. Indicative of a failure state.
	 */
	case object Disconnected extends InstanceState
	{
		override val keys: Seq[String] = Pair("offline", "connecting")
		override val instanceIsUsable: Boolean = false
		override val instanceShouldBeUsed: Boolean = false
	}
	
	/**
	 * State when an instance is rebooting or recycling.
	 */
	case object Restarting extends InstanceState
	{
		override val keys: Seq[String] = Vector("scheduling", "rebooting", "recycling")
		override val instanceIsUsable: Boolean = true
		override val instanceShouldBeUsed: Boolean = false
	}
	
	/**
	 * Status used for covering status keys that are not included in known values.
	 * @param status An unrecognized status key
	 */
	case class Unknown(status: String) extends InstanceState
	{
		override def keys: Seq[String] = Single(status)
		override val instanceIsUsable: Boolean = true
		override val instanceShouldBeUsed: Boolean = false
	}
}