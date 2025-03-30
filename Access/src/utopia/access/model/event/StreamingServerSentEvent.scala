package utopia.access.model.event

import utopia.flow.generic.model.immutable.Value
import utopia.flow.operator.MaybeEmpty
import utopia.flow.view.template.eventful.{Changing, Flag}

/**
  * Represents a server-sent event (SSE).
  * Utilizes pointers for lazy / streaming data-handling.
  * Note: All pointers are expected to stop changing once this event is completed,
  * but it is the event creator's responsibility to ensure that this is the case.
  * @author Mikko Hilpinen
  * @since 30.03.2025, v1.6
  * @param typePointer A pointer that contains this event's type
  * @param dataPointer A pointer that contains all this event's data entries
  * @param lastDataPointer A pointer that contains the latest data entry within this event.
  *                        Contains an empty value while no data has been read.
  * @param nonEmptyFlag A flag that is set to true once this event contains data
  * @param completionFlag A flag that is set to true once this event is fully built
  * @param idPointer A pointer that contains this event's id. Contains an empty string while no id has been assigned.
  */
class StreamingServerSentEvent(val typePointer: Changing[String], val dataPointer: Changing[Seq[Value]],
                               val lastDataPointer: Changing[Value], val nonEmptyFlag: Flag, val completionFlag: Flag,
                               val idPointer: Changing[String])
	extends MaybeEmpty[StreamingServerSentEvent]
{
	// ATTRIBUTES   ----------------------
	
	/**
	  * A future that resolves into a completed copy of this event
	  */
	lazy val future = completionFlag.findMapFuture { if (_) Some(current) else None }
	
	
	// COMPUTED --------------------------
	
	/**
	  * @return The current state of this event
	  */
	def current = ServerSentEvent(eventType, data, id, completed)
	
	/**
	  * @return The current type of this event
	  */
	def eventType = typePointer.value
	
	/**
	  * @return The currently available event data / content, where each line exists as a separate entry
	  */
	def data = dataPointer.value
	/**
	  * @return The latest data entry / line within this event.
	  *         Empty if this event doesn't yet contain any data.
	  */
	def lastData = lastDataPointer.value
	
	/**
	  * @return Current id of this event. Empty string if no id has been assigned.
	  */
	def id = idPointer.value
	
	/**
	  * @return True if this event has been completed
	  */
	def completed = completionFlag.isSet
	/**
	  * @return True while this event is building / streaming
	  */
	def incomplete = completionFlag.isNotSet
	
	
	// IMPLEMENTED  ---------------------
	
	override def self: StreamingServerSentEvent = this
	
	override def isEmpty: Boolean = nonEmptyFlag.isNotSet
}
