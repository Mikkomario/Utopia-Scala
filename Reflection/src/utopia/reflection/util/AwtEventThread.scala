package utopia.reflection.util

import java.time.Instant

import javax.swing.SwingUtilities
import utopia.flow.async.AsyncExtensions._
import utopia.flow.collection.VolatileList
import utopia.flow.util.TimeExtensions._

import scala.concurrent.{Future, Promise}
import scala.concurrent.duration.Duration

/**
  * Methods for performing operations on the awt event thread
  * @author Mikko Hilpinen
  * @since 30.9.2020, v1.3
  */
object AwtEventThread
{
	// ATTRIBUTES	------------------------
	
	/**
	  * Whether debugging mode should be enabled
	  */
	var debugMode = false
	
	private val taskKeepDuration = 3.seconds
	private val tasks = VolatileList[TaskWrapper[_]]()
	
	
	// COMPUTED	----------------------------
	
	/**
	  * @return A string representation of the current state of this interface. Only works while debug mode is active.
	  */
	def debugString =
	{
		val now = Instant.now()
		s"[${tasks.updateAndGet { _.dropWhile { _.endTime.exists { now - _ > taskKeepDuration } } }.mkString(", ")}]"
	}
	
	
	// OTHER	----------------------------
	
	/**
	  * Performs an operation in the awt event thread. Ignores operation return value.
	  * @param operation An operation that should be performed in the awt event thread
	  * @tparam U Arbitrary result type
	  */
	def async[U](operation: => U): Unit =
	{
		if (SwingUtilities.isEventDispatchThread)
			operation
		else
			_async(operation)
	}
	
	private def _async[U](operation: => U): Unit =
	{
		if (debugMode)
		{
			val task = new TaskWrapper(operation)
			tasks.update { old =>
				val now = Instant.now()
				old.dropWhile { _.endTime.exists { now - _ > taskKeepDuration } } :+ task
			}
			SwingUtilities.invokeLater(task)
		}
		else
			SwingUtilities.invokeLater(() => operation)
	}
	
	/**
	  * Performs an operation in the awt event thread and returns the operation completion as a future
	  * @param operation An operation that will be performed
	  * @tparam A Operation result type
	  * @return Future with the eventual return value of the operation
	  */
	def future[A](operation: => A) =
	{
		if (SwingUtilities.isEventDispatchThread)
			Future.successful(operation)
		else
		{
			val completionPromise = Promise[A]()
			_async { completionPromise.success(operation) }
			completionPromise.future
		}
	}
	
	private def _future[A](operation: => A) =
	{
		val completionPromise = Promise[A]()
		_async { completionPromise.success(operation) }
		completionPromise.future
	}
	
	/**
	  * Performs the operation in the awt event thread, blocking until the operation has completed
	  * @param operation An operation to perform
	  * @tparam A Operation result type
	  * @return Operation results
	  */
	def blocking[A](operation: => A) =
	{
		if (SwingUtilities.isEventDispatchThread)
			operation
		else
			_future(operation).waitFor().get
	}
	
	
	// NESTED	----------------------------
	
	private class TaskWrapper[U](operation: => U) extends Runnable
	{
		// ATTRIBUTES	--------------------
		
		private val created = Instant.now()
		private var startTime: Option[Instant] = None
		private var _endTime: Option[Instant] = None
		
		
		// COMPUTED	------------------------
		
		def endTime = _endTime
		
		def isWaiting = startTime.isEmpty
		
		def waitTime = startTime.getOrElse(Instant.now()) - created
		
		def runTime: Duration = startTime match
		{
			case Some(started) => _endTime.getOrElse(Instant.now()) - started
			case None => Duration.Zero
		}
		
		
		// IMPLEMENTED	--------------------
		
		override def toString = endTime match
		{
			case Some(ended) => s"Task completed in ${runTime.description} ${(Instant.now() - ended).description} ago"
			case None =>
				if (isWaiting)
					s"Task that has been waiting for ${waitTime.description}"
				else
					s"Task that has been running for ${runTime.description}"
		}
		
		override def run() =
		{
			startTime = Some(Instant.now())
			operation
			_endTime = Some(Instant.now())
		}
	}
}
