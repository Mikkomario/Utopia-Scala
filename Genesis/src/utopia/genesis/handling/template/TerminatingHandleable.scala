package utopia.genesis.handling.template

import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.eventful.SettableFlag
import utopia.flow.view.template.eventful.Flag

/**
 * An abstract implementation of a [[Handleable]], which may stop processing events
 * @param standardHandleCondition Condition that must be met for this handleable to be handled
 *                                (in addition to not having stopped).
 *                                Default = always true = no other condition besides the stop condition is applied.
 * @param log Implicit logging implementation used for handling errors relating to the stop flag
 * @author Mikko Hilpinen
 * @since 17.09.2025, v4.2.2
 */
abstract class TerminatingHandleable(standardHandleCondition: Flag = AlwaysTrue)(implicit log: Logger)
	extends Handleable
{
	// ATTRIBUTES   ------------------------
	
	/**
	 * A flag that is set once this handleable has stopped
	 */
	protected val stopFlag = SettableFlag()
	override lazy val handleCondition: Flag = !stopFlag && standardHandleCondition
	
	
	// COMPUTED ----------------------------
	
	/**
	 * @return Whether this handleable has stopped processing events
	 */
	def hasStopped = stopFlag.isSet
	/**
	 * @return Whether this handleable has not yet been stopped from processing events
	 */
	def hasNotStopped = !hasStopped
	
	
	// OTHER    ----------------------------
	
	/**
	 * Stops this handleable from processing further events
	 * @return True if this handleable stopped. False if it was already stopped.
	 */
	protected def stop() = stopFlag.set()
}
