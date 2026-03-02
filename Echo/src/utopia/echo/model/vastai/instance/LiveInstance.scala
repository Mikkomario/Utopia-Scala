package utopia.echo.model.vastai.instance

import utopia.echo.model.vastai.process.VastAiProcessState
import utopia.flow.event.model.ChangeResponse.{Continue, Detach}
import utopia.flow.time.Now
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.eventful.SettableFlag
import utopia.flow.view.template.Extender
import utopia.flow.view.template.eventful.Changing

import java.time.Instant

/**
 * Represents an actively updating Vast AI instance. Has a mutating state. Read only.
 * @param instancePointer A pointer that contains the latest version / state of this instance
 * @param processStatePointer A pointer that contains the current hosting process state
 * @author Mikko Hilpinen
 * @since 26.02.2026, v1.5
 */
class LiveInstance(val instancePointer: Changing[VastAiInstance], val processStatePointer: Changing[VastAiProcessState],
                   val processStarted: Instant, val created: Instant = Now)
                  (implicit log: Logger)
	extends Extender[VastAiInstance]
{
	// ATTRIBUTES   ---------------------
	
	private val _loadedFlag = SettableFlag.lockable()
	/**
	 * A flag that contains true once this instance has loaded and become active.
	 * May be locked in a `false` state, if the instance is destroyed or queued for destruction before it became usable.
	 */
	val loadedFlag = _loadedFlag.view
	/**
	 * A future that resolves once the instance has loaded or failed to load.
	 * Contains whether loading was successful.
	 */
	val loadedFuture = loadedFlag.finalValueFuture
	
	/**
	 * A pointer that contains the instance's current status
	 */
	lazy val statusPointer = instancePointer.lightMap { _.status }
	
	
	// INITIAL CODE ---------------------
	
	// Updates the loaded flag
	processStatePointer.addListener { event =>
		if (event.newValue.shouldBeUsed) {
			_loadedFlag.trySet()
			Detach
		}
		else if (event.newValue.wontBeUsable) {
			_loadedFlag.lock()
			Detach
		}
		else
			Continue
	}
	
	
	// COMPUTED -------------------------
	
	/**
	 * @return The current state of this instance's managing process
	 */
	def processState = processStatePointer.value
	
	/**
	 * @return Whether the instance has completed loading
	 */
	def hasLoaded = loadedFlag.value
	/**
	 * @return Whether the instance is still loading
	 */
	def loading = !hasLoaded
	
	
	// IMPLEMENTED  ---------------------
	
	override def wrapped: VastAiInstance = instancePointer.value
}
