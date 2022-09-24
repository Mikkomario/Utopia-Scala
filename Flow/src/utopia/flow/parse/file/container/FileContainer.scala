package utopia.flow.parse.file.container

import java.nio.file.Path
import utopia.flow.async.ProcessState.NotStarted
import utopia.flow.async.ShutdownReaction.DelayShutdown
import utopia.flow.async.process.Process
import utopia.flow.async.context.CloseHook
import utopia.flow.parse.file.container.SaveTiming.{Delayed, Immediate, OnJvmClose, OnlyOnTrigger}
import utopia.flow.generic.model.immutable.Value
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.CollectionExtensions._
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.async.Volatile

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * This container mirrors the stored value in a local file. Remember to call setupAutoSave(...)
  * when extending this class.
  * @author Mikko Hilpinen
  * @since 13.6.2020, v1.8
  * @param fileLocation Location in the file system where this container's back up file should be located
  * @param jsonParser A parser used for handling json reading (implicit)
  * @tparam A Type of item stored in this container
  */
abstract class FileContainer[A](fileLocation: Path)(implicit jsonParser: JsonParser, logger: Logger)
{
	// ABSTRACT	-------------------------------
	
	/**
	  * @param item An item to convert into a value
	  * @return Value based on the item
	  */
	protected def toValue(item: A): Value
	
	/**
	  * @return function for converting read value to an item
	  */
	protected def fromValue(value: Value): A
	
	/**
	  * @return An empty item
	  */
	protected def empty: A
	
	
	// ATTRIBUTES	---------------------------
	
	/**
	  * Currently stored item (as a volatile pointer)
	  */
	protected lazy val _current = new Volatile(fromFile)
	private val saveProcess = VolatileOption[Process]()
	
	
	// COMPUTED	-------------------------------
	
	/**
	  * @return The currently stored data
	  */
	def current = _current.value
	def current_=(newContent: A) = _current.value = newContent
	
	/**
	  * @return A pointer that holds the current value in this container
	  */
	def pointer = _current
	
	/**
	  * @return If there is a save process running, returns the completion of that process. Otherwise returns a
	  *         completed future with the current process state.
	  */
	def activeSaveCompletionFuture = saveProcess.value match {
		case Some(process) =>
			if (process.state.isRunning)
				process.completionFuture
			else
				Future.successful(process.state)
		case None => Future.successful(NotStarted)
	}
	
	
	// OTHER	-------------------------------
	
	/**
	  * @param newContent New container content
	  * @return Container content before setting new content
	  */
	def getAndSet(newContent: A) = _current.getAndSet(newContent)
	
	/**
	  * Saves this container's current status to the local file. Saving is done in background.
	  * @param exc Implicit execution context
	  * @return A future of the eventual save completion
	  */
	def saveStatus()(implicit exc: ExecutionContext) = {
		val process = saveProcess.value.getOrElse { Process(shutdownReaction = DelayShutdown) { _ => _save() } }
		process.runAsync(loopIfRunning = true)
		process.completionFuture
	}
	
	/**
	  * Sets up file save logic
	  * @param saveLogic Logic to use with autosave
	  * @param exc Implicit execution context
	  */
	protected def setupAutoSave(saveLogic: SaveTiming)(implicit exc: ExecutionContext) = {
		val listen = saveLogic match {
			case Immediate =>
				saveProcess.setOne(Process(shutdownReaction = DelayShutdown) { _ => _save() })
				true
			case Delayed(duration) =>
				saveProcess.setOne(DelayedProcess.hurriable(duration) { _ => _save() })
				true
			case OnJvmClose =>
				CloseHook.registerAsyncAction { Future {
					_save().failure.foreach { logger(_, "Failed to save FileContainer status on jvm shutdown") } } }
				saveProcess.clear()
				false
			case OnlyOnTrigger =>
				saveProcess.clear()
				false
		}
		if (listen)
			_current.addListenerAndSimulateEvent(empty) { _ =>
				saveProcess.lock { _.foreach { _.runAsync(loopIfRunning = true) } }
				true
			}
	}
	
	private def _save() = {
		// Saves current status to file as json
		val dataToSave = toValue(_current.value)
		fileLocation.createParentDirectories().flatMap { _.writeJson(dataToSave) }
	}
	
	private def fromFile = {
		if (fileLocation.exists) {
			jsonParser(fileLocation.toFile) match {
				case Success(value) => fromValue(value)
				case Failure(_) =>
					// Read errors are ignored here
					empty
			}
		}
		else
			empty
	}
}
