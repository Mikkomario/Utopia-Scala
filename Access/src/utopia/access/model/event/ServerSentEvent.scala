package utopia.access.model.event

import utopia.flow.collection.immutable.Empty
import utopia.flow.generic.model.immutable.Value
import utopia.flow.operator.MaybeEmpty

/**
  * Represents a completed server-sent event (SSE), or a snapshot of such an event
  * @author Mikko Hilpinen
  * @since 30.03.2025, v1.6
  * @param eventType Type of this event. May be empty if this event doesn't have a type.
  * @param data Contents of this event. Each line is represented with a separate entry.
  * @param id Id of this event. May be empty.
  * @param completed Whether this represents a completed event (true) or a snapshot (false)
  */
case class ServerSentEvent(eventType: String = "", data: Seq[Value] = Empty, id: String = "", completed: Boolean = true)
	extends MaybeEmpty[ServerSentEvent]
{
	// COMPUTED -------------------------
	
	/**
	  * @return Whether this represents a snapshot of an incomplete event
	  */
	def incomplete = !completed
	
	
	// IMPLEMENTED  --------------------
	
	override def self: ServerSentEvent = this
	override def isEmpty: Boolean = data.isEmpty
}