package utopia.flow.container

import java.nio.file.Path
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.{CloseHook, DelayedProcess, Volatile}
import utopia.flow.container.SaveTiming.{Delayed, Immediate, OnJvmClose, OnlyOnTrigger}
import utopia.flow.datastructure.immutable.Value
import utopia.flow.event.{ChangeEvent, ChangeListener}
import utopia.flow.parse.JsonParser
import utopia.flow.util.FileExtensions._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

/**
  * This container mirrors the stored value in a local file. Remember to call setupAutoSave(...)
  * when extending this class.
  * @author Mikko Hilpinen
  * @since 13.6.2020, v1.8
  * @param fileLocation Location in the file system where this container's back up file should be located
  * @param jsonParser A parser used for handling json reading (implicit)
  * @tparam A Type of item stored in this container
  */
abstract class FileContainer[A](fileLocation: Path)(implicit jsonParser: JsonParser)
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
	private val saveCompletion = Volatile(Future.successful[Try[Unit]](Success(())))
	
	
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
	def saveStatus()(implicit exc: ExecutionContext) =
	{
		// Will only perform one saving at a time
		val newSavePromise = Promise[Try[Unit]]()
		saveCompletion.getAndSet(newSavePromise.future).onComplete { _ =>
			// Saves current status to file as json
			val dataToSave = toValue(_current.value)
			fileLocation.createParentDirectories()
			// Completes the promise so that the next save process can start
			newSavePromise.success(fileLocation.writeJson(dataToSave).map { _ => () })
		}
		newSavePromise.future
	}
	
	/**
	  * Sets up file save logic
	  * @param saveLogic Logic to use with autosave
	  * @param exc Implicit execution context
	  */
	protected def setupAutoSave(saveLogic: SaveTiming)(implicit exc: ExecutionContext) = saveLogic match
	{
		case Immediate => _current.addListener { _ => saveStatus() }
		case Delayed(duration) =>
			val listener = new DelayedSaveHandler(duration)
			_current.addListener(listener)
		case OnJvmClose => CloseHook.registerAsyncAction { saveStatus() }
		case OnlyOnTrigger => ()
	}
	
	private def fromFile =
	{
		if (fileLocation.exists)
		{
			jsonParser(fileLocation.toFile) match
			{
				case Success(value) => fromValue(value)
				case Failure(_) =>
					// Read errors are ignored here
					empty
			}
		}
		else
			empty
	}
	
	
	// NESTED	--------------------------------
	
	private class DelayedSaveHandler(delay: FiniteDuration)(implicit exc: ExecutionContext)
		extends ChangeListener[A]
	{
		// ATTRIBUTES	------------------------
		
		private lazy val saveProcess = DelayedProcess.hurriable(delay) { _ => saveStatus().waitFor() }
		
		
		// IMPLEMENTED	------------------------
		
		override def onChangeEvent(event: ChangeEvent[A]) = saveProcess.runAsync()
	}
}
