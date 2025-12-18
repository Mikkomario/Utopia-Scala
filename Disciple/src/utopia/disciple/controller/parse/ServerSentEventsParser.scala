package utopia.disciple.controller.parse

import utopia.access.model.Headers
import utopia.access.model.enumeration.Status
import utopia.access.model.event.StreamingServerSentEvent
import utopia.flow.async.TryFuture
import utopia.flow.async.process.Wait
import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.mutable.iterator.OptionsIterator
import utopia.flow.event.model.ChangeResponse.{Continue, Detach}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.parse.string.Lines
import utopia.flow.time.Duration
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.StringExtensions._
import utopia.flow.util.result.TryExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.eventful.SettableFlag
import utopia.flow.view.template.eventful.Flag

import java.io.InputStream
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.io.Codec
import scala.util.{Failure, Success, Try}

/**
  * A response parser that processes incoming server-sent events.
  * Delivers these events in streamed format.
  * @author Mikko Hilpinen
  * @since 30.03.2025, v1.9
  */
object ServerSentEventsParser
{
	// ATTRIBUTES   ---------------------------
	
	private val _retry = "retry"
	
	
	// OTHER    -------------------------------
	
	/**
	  * @param listeners Listeners to inform of the read events
	  * @param exc Implicit execution context
	  * @param log Implicit logging implementation used for non-critical errors
	  * @return A new events parser, implementing the [[ResponseParser]] trait.
	  */
	def withListeners(listeners: Iterable[ServerSentEventListener])
	                 (implicit exc: ExecutionContext, log: Logger) =
		preparingListenersWith { (_, _) => Success(listeners) }
	/**
	  * @param prepare A function that accepts a response status + headers and yields the listeners to register to
	  *                listen for events, or a failure
	  * @param exc Implicit execution context
	  * @param log Implicit logging implementation used for non-critical errors
	  * @return A new events parser, implementing the [[ResponseParser]] trait.
	  */
	def preparingListenersWith(prepare: (Status, Headers) => Try[Iterable[ServerSentEventListener]])
	                          (implicit exc: ExecutionContext, log: Logger) =
		new PreparedServerSentEventsParser(prepare)
	
	/**
	  * Asynchronously parses server-sent events from an input stream
	  * @param stream Stream to process.
	  *               Note: This stream will be closed by this function, once it has been processed.
	  * @param listeners Listeners to inform of received events
	  * @param retry A retry implementation (optional).
	  *              Accepts the id of the last open event (or an empty string).
	  *              Yields a new stream to read, or a failure.
	  * @param log Implicit logging implementation for handling various non-critical failures.
	  * @param codec Implicit codec used when interpreting stream contents
	  * @param exc Implicit execution context
	  * @return A future that resolves into a success or a failure, once the stream has been processed.
	  */
	def apply(stream: InputStream, listeners: Iterable[ServerSentEventListener],
	          retry: Option[String => Try[InputStream]] = None)
	         (implicit log: Logger, codec: Codec, exc: ExecutionContext) =
	{
		// Prepares the completion future / promise
		val completionPromise = Promise[Try[Unit]]()
		var collectedFailure: Option[Throwable] = None
		
		// Performs the actual parsing asynchronously
		val completionFuture = Future {
			OptionsIterator
				.iterate(Some(stream)) { stream =>
					// Processes this stream
					val result = _apply(stream, listeners, canRetry = retry.isDefined)
					// Closes this stream after processing completes
					Try { stream.close() }.logWithMessage("Failed to close the processed stream")
					result match {
						// Case: Processing succeeded => Checks whether a retry was queued
						case Success(retryInstruction) =>
							retryInstruction.flatMap { case (delay, id) =>
								retry.flatMap { retry =>
									// Case: Retry queued
									//       => Waits the specified amount of time and attempts to get the next stream
									if (Wait(delay))
										retry(id) match {
											// Case: New stream acquired => Processes it in the next iteration
											case Success(newStream) => Some(newStream)
											// Case: Failed to reconnect => Fails
											case Failure(error) =>
												collectedFailure = Some(error)
												None
										}
									// Case: Wait interrupted => Fails
									else {
										collectedFailure = Some(new InterruptedException("Retry wait interrupted"))
										None
									}
								}
							}
						// Case: Processing failed => Fails
						case Failure(error) =>
							collectedFailure = Some(error)
							None
					}
				}
				// Processes as many streams as necessary
				.foreach { _ => () }
		}
		// Handles the process completion, including the possible failure completion
		completionFuture.onComplete {
			case Success(_) =>
				completionPromise.success(collectedFailure match {
					case Some(error) => Failure(error)
					case None => Success(())
				})
			case Failure(error) => completionPromise.success(Failure(error))
		}
		completionPromise.future
	}
	
	/**
	  * Parses the contents of a single stream, informing the specified listeners of received server-sent events.
	  * @param stream Stream to process
	  * @param listeners Listeners to inform
	  * @param canRetry Whether retrying is allowed. If set to false, all retry prompts are ignored.
	  * @param codec Implicit codec to utilize
	  * @param log Implicit logging implementation used
	  * @return Success or a failure. Success may contain a retry request (delay + last id).
	  */
	private def _apply(stream: InputStream, listeners: Iterable[ServerSentEventListener], canRetry: Boolean)
	                  (implicit codec: Codec, log: Logger) =
	{
		// Opens the stream
		Lines.iterate.stream(stream) { linesIter =>
			var currentEventBuilder: Option[EventBuilder] = None
			var queuedRetryDelay: Option[Duration] = None
			
			// Reads the stream one line at a time until the stream has been consumed
			// or until a retry request is encountered
			while (queuedRetryDelay.isEmpty && linesIter.hasNext) {
				val line = linesIter.next()
				// Case: Empty line => The currently built event is completed
				if (line.isEmpty) {
					val event = currentEventBuilder.map { builder =>
						builder.complete()
						builder.event.current
					}
					currentEventBuilder = None
					event.foreach { event =>
						// Informs the listeners of this event
						listeners.foreach { listener =>
							Try { listener.onEventCompleted(event) }
								.logWithMessage("Listener threw an exception while processing a server-sent event")
						}
					}
				}
				else {
					val (fieldName, data) = line.splitAtFirst(":").map { _.trim }.toTuple
					// Ignores comments (i.e. lines with no field name)
					if (fieldName.nonEmpty) {
						// Case: Retry request => Queues it, if retry is supported
						if (fieldName == _retry) {
							if (canRetry)
								queuedRetryDelay = data.int.map { _.millis }
						}
						else {
							var ignore = false
							lazy val builder = currentEventBuilder.getOrElse { new EventBuilder() }
							// Processes the read field
							fieldName match {
								case "event" => builder.assignEventType(data)
								case "data" => builder.assignData(data)
								case "id" => builder.assignId(data)
								case _ => ignore = true
							}
							// If a new event was started, informs the listeners
							if (!ignore && currentEventBuilder.isEmpty) {
								currentEventBuilder = Some(builder)
								listeners.foreach { listener =>
									Try { listener.onEventStarted(builder.event) }
										.logWithMessage("Listener threw an exception while processing a server-sent event")
								}
							}
						}
					}
				}
			}
			// If necessary, acquires the last event id for the retry instruction
			queuedRetryDelay.map { delay =>
				val id = currentEventBuilder match {
					case Some(builder) => builder.event.id
					case None => ""
				}
				delay -> id
			}
		}
	}
	
	
	// NESTED   -------------------------------
	
	class PreparedServerSentEventsParser(prepareListeners: (Status, Headers) => Try[Iterable[ServerSentEventListener]],
	                                     retry: Option[String => Try[InputStream]] = None)
	                                    (implicit log: Logger, exc: ExecutionContext)
		extends ResponseParser[Future[Try[Unit]]]
	{
		// IMPLEMENTED  -----------------------
		
		override def apply(status: Status, headers: Headers, stream: Option[InputStream]) = {
			// Prepares the listeners based on status & headers
			prepareListeners(status, headers) match {
				case Success(listeners) =>
					stream match {
						case Some(stream) =>
							// Default: Listeners prepared => Processes the server-sent events from the stream
							if (listeners.nonEmpty)
								ResponseParseResult.future(ServerSentEventsParser(stream, listeners, retry))
							// Case: No listeners assigned => Immediately closes the stream and returns
							else {
								Try { stream.close() }.logWithMessage("Failed to close the response stream")
								ResponseParseResult.buffered(TryFuture.successCompletion)
							}
						
						// Case: No stream available => Completes immediately
						case None => ResponseParseResult.buffered(TryFuture.successCompletion)
					}
				
				// Case: Listener-preparation yielded a failure => Yields a failure result
				case Failure(error) => ResponseParseResult.buffered(TryFuture.failure(error))
			}
		}
		
		
		// OTHER    ---------------------------
		
		/**
		  * @param retry A retry function that accepts the latest event id (which may be empty) and yields,
		  *              if successful, a new input stream to process.
		  * @return Copy of this parser which utilizes the specified retry logic
		  */
		def withRetryLogic(retry: String => Try[InputStream]) =
			new PreparedServerSentEventsParser(prepareListeners, Some(retry))
	}
	
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