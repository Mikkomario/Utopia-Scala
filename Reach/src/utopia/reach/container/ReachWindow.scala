package utopia.reach.container

import utopia.flow.event.listener.ChangeListener
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.view.immutable.eventful.AlwaysFalse
import utopia.flow.view.mutable.caching.ResettableLazy
import utopia.flow.view.template.eventful.{AbstractChanging, Changing}

import scala.concurrent.{ExecutionContext, Promise}

// TODO: Create constructor

/**
  * A simulated window within a reach canvas
  * @author Mikko Hilpinen
  * @since 6.2.2021, v0.1
  */
// TODO: Likely remove this class entirely
class ReachWindow private(visibilityPointer: Changing[Boolean])
                         (makeContent: Changing[Boolean] => ReachCanvas2)
                         (implicit exc: ExecutionContext)
{
	// ATTRIBUTES	-------------------------------
	
	private var open = false
	private val closePromise = Promise[Unit]()
	
	val canvas = makeContent(WindowConnectionState)
	
	
	// INITIAL CODE	-------------------------------
	
	
	// COMPUTED	-----------------------------------
	
	def closeFuture = closePromise.future
	
	
	// OTHER	-----------------------------------
	
	def display() =
	{
		// TODO: implement
	}
	
	
	// NESTED	-----------------------------------
	
	// TODO: Change state to false until displayed
	private object WindowConnectionState extends AbstractChanging[Boolean]
	{
		// ATTRIBUTES	---------------------------
		
		private val source =
		{
			if (visibilityPointer.isFixed)
				Right(
					if (visibilityPointer.value)
						Changing.future(false, closeFuture) { _ => true }
					else
						AlwaysFalse
				)
			else
				Left(ResettableLazy { !closeFuture.isCompleted && visibilityPointer.value })
		}
		
		
		// INITIAL CODE	---------------------------
		
		// If visibility pointer is changing, needs to reset the cache on changes
		source.leftOption.foreach { cache =>
			val resetCacheListener = ChangeListener.onAnyChange { cache.reset() }
			visibilityPointer.addListener(resetCacheListener)
			closeFuture.foreach { _ =>
				cache.reset()
				visibilityPointer.removeListener(resetCacheListener)
			}
		}
		
		
		// IMPLEMENTED	---------------------------
		
		override def isChanging = source match
		{
			case Left(_) => !closeFuture.isCompleted
			case Right(pointer) => pointer.isChanging
		}
		
		override def value = source match
		{
			case Left(cache) => cache.value
			case Right(pointer) => pointer.value
		}
	}
}