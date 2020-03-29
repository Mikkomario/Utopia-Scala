package utopia.flow.async

import java.time.Instant

import utopia.flow.util.TimeExtensions._
import utopia.flow.async.AsyncExtensions._
import utopia.flow.collection.WeakList
import utopia.flow.util.WaitUtils

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
      * Stops all registered breakable items
      */
    def shutdown() =
    {
        // Stops all registered loops
        val completions = loops.strong.map { _.stop() }
        loops = WeakList()
        
        if (completions.nonEmpty)
        {
            // Waits until all of the completions are done
            val shutdownDeadline = Instant.now() + maxShutdownTime
            completions.foreach { _.waitFor(shutdownDeadline - Instant.now()) }
            
            // Waits additional shutdown time
            WaitUtils.wait(additionalShutdownTime, new AnyRef())
        }
    }
}