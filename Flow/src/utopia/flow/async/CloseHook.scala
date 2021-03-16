package utopia.flow.async

import utopia.flow.time.TimeExtensions._
import utopia.flow.async.AsyncExtensions._
import utopia.flow.collection.WeakList
import utopia.flow.time.{Now, WaitUtils}

import scala.concurrent.{ExecutionContext, Future}

/**
* This object stops some operations before jvm closes
* @author Mikko Hilpinen
* @since 31.3.2019
**/
object CloseHook
{
	// ATTRIBUTES    ----------------
    
    /**
      * Maximum duration the shutdown process can take
      */
    var maxShutdownTime = 5.seconds
    
    private val additionalShutdownTime = 200.millis
    private var loops = WeakList[Breakable]()
    private var hooks = Vector[() => Future[Any]]()
    
    
    
    // INITIAL CODE -----------------
    
    // Registers all breakable items to stop
    Runtime.getRuntime.addShutdownHook(new Thread(() => shutdown()))
    
    
    // OPERATORS    -----------------
    
    /**
      * Registers a new breakable item to be stopped once the JVM closes. The item is only
      * weakly referenced.
      * @param breakable Breakable item
      */
    def +=(breakable: Breakable) = loops :+= breakable
    
    
    // OTHER    ---------------------
    
    /**
      * Registers an action to be performed when the JVM is about to close
      * @param onCloseAction Action that will be called asynchronously when the JVM is about to close (call by name)
      */
    def registerAction(onCloseAction: => Any)(implicit exc: ExecutionContext) =
        hooks :+= { () => Future { onCloseAction } }
    
    /**
      * Registers an asynchronous action to be perfomed when the JVM is about to close
      * @param onCloseAction An action that will be called when the JVM is about to close. Shouldn't block. Call by name.
      */
    def registerAsyncAction(onCloseAction: => Future[Any]) = hooks :+= { () => onCloseAction }
    
    /**
      * Stops all registered breakable items
      */
    def shutdown() =
    {
        // Stops all registered loops
        val completions = loops.strong.map { _.stop() } ++ hooks.map { _() }
        loops = WeakList()
        hooks = Vector()
        
        if (completions.nonEmpty)
        {
            // Waits until all of the completions are done
            val shutdownDeadline = Now + maxShutdownTime
            completions.foreach { _.waitFor(shutdownDeadline - Now) }
            
            // Waits additional shutdown time
            WaitUtils.wait(additionalShutdownTime, new AnyRef())
        }
    }
}