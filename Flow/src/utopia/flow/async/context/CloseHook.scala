package utopia.flow.async.context

import utopia.flow.async.process.Breakable
import utopia.flow.async.AsyncExtensions._
import utopia.flow.collection.immutable.WeakList
import utopia.flow.collection.mutable.VolatileList
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.mutable.async.{Volatile, VolatileFlag}

import scala.concurrent.{ExecutionContext, Future}

/**
  * This object stops some operations before jvm closes
  * @author Mikko Hilpinen
  * @since 31.3.2019
  * */
object CloseHook
{
	// ATTRIBUTES    ----------------
	
	private val additionalShutdownTime = 200.millis
	
	private val _shutdownPointer = VolatileFlag()
	private val breakables = Volatile(WeakList[Breakable]())
	private val hooks = VolatileList[() => Future[Any]]()
	
	/**
	  * Maximum duration the shutdown process can take
	  */
	var maxShutdownTime = 5.seconds
	
	
	// INITIAL CODE -----------------
	
	// Registers all breakable items to stop
	Runtime.getRuntime.addShutdownHook(new Thread(() => shutdown()))
	
	
	// COMPUTED ----------------------
	
	/**
	  * @return A pointer that contains true after the shutdown procedure has been initiated
	  */
	def shutdownPointer = _shutdownPointer.valueView
	
	/**
	  * @return Whether the shutdown procedure has started
	  */
	def isShutdown = _shutdownPointer.value
	
	/**
	  * @return Whether the shutdown procedure has not been started
	  */
	def nonShutdown = !isShutdown
	
	
	// OPERATORS    -----------------
	
	/**
	  * Registers a new breakable item to be stopped once the JVM closes. The item is only
	  * weakly referenced.
	  * @param breakable Breakable item
	  */
	def +=(breakable: Breakable) = breakables.update { _ :+ breakable }
	
	/**
	  * Removes a breakable from the list of breakables to stop when the JVM closes.
	  * @param breakable Breakable item to remove
	  */
	def -=(breakable: Breakable) = breakables.update { _.filterNot { _ == breakable } }
	
	
	// OTHER    ---------------------
	
	/**
	  * Registers an action to be performed when the JVM is about to close
	  * @param onCloseAction Action that will be called asynchronously when the JVM is about to close (call by name)
	  */
	def registerAction(onCloseAction: => Any)(implicit exc: ExecutionContext) =
		hooks +:= { () => Future { onCloseAction } }
	
	/**
	  * Registers an asynchronous action to be perfomed when the JVM is about to close
	  * @param onCloseAction An action that will be called when the JVM is about to close. Shouldn't block. Call by name.
	  */
	def registerAsyncAction(onCloseAction: => Future[Any]) = hooks :+= { () => onCloseAction }
	
	/**
	  * Stops all registered breakable items
	  */
	def shutdown() = {
		// Updates the pointer
		_shutdownPointer.set()
		
		// Stops all registered loops
		val completions = breakables.getAndSet(WeakList()).strong.map { _.stop() } ++ hooks.popAll().map { _() }
		if (completions.nonEmpty) {
			// Waits until all of the completions are done
			val shutdownDeadline = Now + maxShutdownTime
			completions.foreach { completion =>
				if (shutdownDeadline.isFuture)
					completion.waitFor(shutdownDeadline - Now)
				// TODO: Should log if not performed
			}
			
			// Waits additional shutdown time
			val lock = new AnyRef
			lock.synchronized {
				lock.wait(additionalShutdownTime.toMillis)
			}
		}
	}
}
