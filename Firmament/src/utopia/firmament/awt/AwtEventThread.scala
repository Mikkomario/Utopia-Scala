package utopia.firmament.awt

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.context.ThreadPool
import utopia.flow.async.process.{Delay, WaitUtils}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.mutable.VolatileList
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}

import java.time.Instant
import javax.swing.SwingUtilities
import scala.concurrent.Promise
import scala.concurrent.duration.Duration
import scala.util.Try

/**
  * Methods for performing operations on the awt event thread
  * @author Mikko Hilpinen
  * @since 30.9.2020, Reflection v1.3
  */
object AwtEventThread
{
	// ATTRIBUTES	------------------------
	
	/**
	  * Whether debugging mode should be enabled
	  */
	var debugMode = false
	/**
	  * The logging implementation used for handling errors that would otherwise be thrown to the AWT event thread.
	  */
	implicit var logger: Logger = SysErrLogger
	
	private lazy val taskKeepDuration = 3.seconds
	private lazy val tasks = VolatileList[TaskWrapper[_]]()
	
	
	// COMPUTED	----------------------------
	
	/**
	  * @return A string representation of the current state of this interface. Only works while debug mode is active.
	  */
	def debugString = {
		val now = Instant.now()
		val currentTasks = tasks.updateAndGet { _.dropWhile { _.endTime.exists { now - _ > taskKeepDuration } } }
		val (active, waiting) = currentTasks.divideBy { _.isWaiting }
		val (running, completed) = active.divideBy { _.endTime.isDefined }
		
		val waitingString = {
			if (waiting.isEmpty)
				""
			else if (waiting.size == 1)
				s"Waiting(${waiting.head.waitTime.description})"
			else
				s"${waiting.size} waiting (${waiting.map { _.waitTime }.max.description})"
		}
		val runningString = running.headOption match {
			case Some(task) => s"Running(${task.runTime.description})"
			case None => ""
		}
		val completedString = s"${completed.size} completed recently"
		
		Vector(waitingString, runningString, completedString).filter { _.nonEmpty }.mkString(", ")
	}
	
	
	// OTHER	----------------------------
	
	/**
	  * Performs an operation in the awt event thread.
	  * Ignores operation return value and logs possibly encountered failures.
	  * @param operation An operation that should be performed in the awt event thread
	  * @tparam U Arbitrary result type
	  */
	def async[U](operation: => U): Unit = {
		if (SwingUtilities.isEventDispatchThread)
			Try { operation }.failure.foreach {
				case e: InterruptedException => throw e
				case e => logger(e)
			}
		else
			_async(operation) { logger(_) }
	}
	/**
	  * Performs an operation in the awt event thread and returns the operation completion as a future
	  * @param operation An operation that will be performed
	  * @tparam A Operation result type
	  * @return Future with the eventual return value of the operation
	  */
	def future[A](operation: => A) = {
		if (SwingUtilities.isEventDispatchThread)
			Try(operation).toCompletedFuture
		else {
			val completionPromise = Promise[A]()
			_async { completionPromise.success(operation) }(completionPromise.failure)
			completionPromise.future
		}
	}
	/**
	  * Performs the operation in the awt event thread, blocking until the operation has completed
	  * @param operation An operation to perform
	  * @tparam A Operation result type
	  * @return Operation results
	  */
	def blocking[A](operation: => A) = {
		if (SwingUtilities.isEventDispatchThread)
			operation
		else
			_future(operation).waitFor().get
	}
	
	private def _future[A](operation: => A) = {
		val completionPromise = Promise[A]()
		// Possible failures are forwarded to the promise / future
		_async { completionPromise.success(operation) }(completionPromise.failure)
		completionPromise.future
	}
	
	private def _async[U](operation: => U)(onFailure: Throwable => Unit): Unit = {
		if (debugMode) {
			val task = new TaskWrapper(operation, onFailure)
			tasks.update { old =>
				lazy val now = Instant.now()
				old.dropWhile { _.endTime.exists { now - _ > taskKeepDuration } } :+ task
			}
			SwingUtilities.invokeLater(task)
		}
		else
			SwingUtilities.invokeLater { () =>
				Try { operation }.failure.foreach {
					// InterruptedExceptions are not caught but rethrown
					case e: InterruptedException => throw e
					case e => onFailure(e)
				}
			}
	}
	
	
	// NESTED	----------------------------
	
	private object TaskWrapper {
		private lazy val exc = new ThreadPool("AwtEventThreadMonitor", coreSize = 0, maxSize = 10)(SysErrLogger)
	}
	
	private class TaskWrapper[U](operation: => U, onFailure: Throwable => Unit) extends Runnable
	{
		// ATTRIBUTES	--------------------
		
		private val created = Instant.now()
		private var startTime: Option[Instant] = None
		private var _endTime: Option[Instant] = None
		
		
		// COMPUTED	------------------------
		
		def endTime = _endTime
		
		def isWaiting = startTime.isEmpty
		
		def waitTime = startTime.getOrElse(Instant.now()) - created
		
		def runTime: Duration = startTime match {
			case Some(started) => _endTime.getOrElse(Instant.now()) - started
			case None => Duration.Zero
		}
		
		
		// IMPLEMENTED	--------------------
		
		override def toString = endTime match {
			case Some(_) => s"Completed(${runTime.description})"
			case None =>
				if (isWaiting)
					s"Wait(${waitTime.description})"
				else
					s"RUN(${runTime.description})"
		}
		
		override def run() = {
			Try {
				startTime = Some(Now)
				val t = Thread.currentThread()
				val waitLock = new AnyRef
				Delay(3.seconds, waitLock) {
					if (_endTime.isEmpty)
						t.interrupt()
				}(TaskWrapper.exc, SysErrLogger)
				operation
				_endTime = Some(Now)
				WaitUtils.notify(waitLock)
			}.failure.foreach {
				// InterruptedExceptions are not caught but forwarded further
				case e: InterruptedException => throw e
				case e => onFailure(e)
			}
		}
	}
}
