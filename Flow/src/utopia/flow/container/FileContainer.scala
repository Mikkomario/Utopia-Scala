package utopia.flow.container

import java.nio.file.Path

import utopia.flow.async.{Breakable, CloseHook, Volatile, VolatileFlag}
import utopia.flow.container.SaveTiming.{Delayed, Immediate, OnJvmClose, OnlyOnTrigger}
import utopia.flow.datastructure.immutable.Value
import utopia.flow.event.{ChangeEvent, ChangeListener}
import utopia.flow.parse.JsonParser
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.FileExtensions._
import utopia.flow.util.WaitUtils

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

/**
  * This container mirrors the stored value in a local file. Remember to call setupAutoSave(...)
  * when extending this class.
  * @author Mikko Hilpinen
  * @since 13.6.2020, v1.8
  */
abstract class FileContainer[A](fileLocation: Path, jsonParser: JsonParser)
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
	
	/**
	  * This function is called when data reading or writing fails
	  * @param error Thrown error
	  */
	protected def handleError(error: Throwable): Unit
	
	
	// ATTRIBUTES	---------------------------
	
	/**
	  * Currently stored item (as a volatile pointer)
	  */
	protected lazy val _current = new Volatile(fromFile)
	private val saveCompletion = Volatile(Future.successful(()))
	
	
	// COMPUTED	-------------------------------
	
	/**
	  * @return The currently stored data
	  */
	def current = _current.get
	def current_=(newContent: A) = _current.set(newContent)
	
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
		val newSavePromise = Promise[Unit]()
		saveCompletion.getAndSet(newSavePromise.future).onComplete { _ =>
			// Saves current status to file as json
			val dataToSave = toValue(_current.get)
			fileLocation.createParentDirectories()
			fileLocation.writeJSON(dataToSave).failure.foreach(handleError)
			// Completes the promise so that the next save process can start
			newSavePromise.success(())
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
			listener.registerToStopOnceJVMCloses()
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
				case Failure(error) =>
					handleError(error)
					empty
			}
		}
		else
			empty
	}
	
	
	// NESTED	--------------------------------
	
	private class DelayedSaveHandler(delay: FiniteDuration)(implicit exc: ExecutionContext)
		extends ChangeListener[A] with Breakable
	{
		// ATTRIBUTES	------------------------
		
		private val waitLock = new AnyRef
		private val waitingFlag = new VolatileFlag()
		
		
		// IMPLEMENTED	------------------------
		
		override def onChangeEvent(event: ChangeEvent[A]) =
		{
			waitingFlag.runAndSet {
				WaitUtils.delayed(delay, waitLock) {
					waitingFlag.reset()
					saveStatus()
				}
			}
		}
		
		// When this handler is commanded to stop, skips the wait and performs the last save if one was queued
		override def stop() =
		{
			_current.removeListener(this)
			WaitUtils.notify(waitLock)
			waitingFlag.futureWhere { !_ }
		}
	}
}
