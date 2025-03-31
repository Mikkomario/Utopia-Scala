package utopia.disciple.controller.parse

import utopia.access.model.event.StreamingServerSentEvent
import utopia.disciple.controller.parse.ServerSentEventsParser.EventBuilder
import utopia.flow.collection.immutable.Pair
import utopia.flow.event.model.ChangeResponse.{Continue, Detach}
import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.caching.ResettableLazy
import utopia.flow.view.mutable.eventful.SettableFlag
import utopia.flow.view.template.eventful.Flag

import java.io.InputStream

object ServerSentEventsParser
{
	// NESTED   -------------------------------
	
	private class EventBuilder(implicit log: Logger)
	{
		// ATTRIBUTES   -----------------------
		
		private val typePointer = Pointer.lockable("")
		private val dataPointer = Pointer.lockable(Value.empty)
		private val lastDataPointer = Pointer.lockable(Value.empty)
		private val dataEntriesPointer = Pointer.lockable(0)
		private val idPointer = Pointer.lockable("")
		private val nonEmptyFlag = Flag.lockable()
		private val completionFlag = SettableFlag()
		
		lazy val event = new StreamingServerSentEvent(typePointer.readOnly, dataPointer.readOnly,
			lastDataPointer.readOnly, dataEntriesPointer.readOnly, nonEmptyFlag.view, idPointer.readOnly,
			completionFlag.view)
			
		
		// INITIAL CODE -----------------------
		
		// Automatically sets the non-empty -flag
		dataEntriesPointer.addListener { e =>
			if (e.newValue > 0) {
				nonEmptyFlag.set()
				Detach
			}
			else
				Continue
		}
			
		
		// OTHER    ---------------------------
		
		def assignEventType(eventType: String) = typePointer.value = eventType
		def assignData(data: Value) = {
			lastDataPointer.value = data
			dataEntriesPointer.value match {
				case 0 => dataPointer.value = data
				case 1 => dataPointer.update { Pair(_, data) }
				case 2 => dataPointer.update { _.getPair :+ data }
				case _ => dataPointer.update { _.getVector :+ data }
			}
			dataEntriesPointer.update { _ + 1 }
		}
		def assignId(id: String) = idPointer.value = id
		
		def complete() = {
			idPointer.lock()
			typePointer.lock()
			lastDataPointer.lock()
			dataPointer.lock()
			dataEntriesPointer.lock()
			nonEmptyFlag.lock()
			completionFlag.set()
		}
	}
}

/**
  * A response parser that processes incoming server-sent events.
  * Delivers these events in streamed format.
  * @author Mikko Hilpinen
  * @since 30.03.2025, v1.9
  */
class ServerSentEventsParser(input: InputStream, retry: Option[() => InputStream])(implicit log: Logger)
{
	// ATTRIBUTES   ---------------------------
	
	private val completionFlag = SettableFlag()
	private val currentEventBuilder = ResettableLazy { new EventBuilder() }
}
