package utopia.reach.container

import utopia.flow.async.ChangeFuture
import utopia.flow.datastructure.mutable.ResettableLazy
import utopia.flow.event.{AlwaysFalse, ChangeDependency, ChangeListener, Changing, ChangingLike}
import utopia.flow.util.CollectionExtensions._
import utopia.reach.component.template.ReachComponentLike
import utopia.reach.component.wrapper.OpenComponent

import scala.concurrent.{ExecutionContext, Promise}

/**
  * A simulated window within a reach canvas
  * @author Mikko Hilpinen
  * @since 6.2.2021, v1
  */
class ReachWindow(content: OpenComponent[ReachComponentLike, _], visibilityPointer: ChangingLike[Boolean])
				 (implicit exc: ExecutionContext)
{
	// ATTRIBUTES	-------------------------------
	
	private var open = false
	private val closePromise = Promise[Unit]()
	
	
	// INITIAL CODE	-------------------------------
	
	
	// COMPUTED	-----------------------------------
	
	def closeFuture = closePromise.future
	
	
	// OTHER	-----------------------------------
	
	def display() =
	{
		if (!open)
		{
			open = true
			content.hierarchy.lockToTop(WindowConnectionState)
			// TODO: Handle focus acquisition
		}
	}
	
	
	// NESTED	-----------------------------------
	
	private object WindowConnectionState extends Changing[Boolean]
	{
		// ATTRIBUTES	---------------------------
		
		private val source =
		{
			if (visibilityPointer.isFixed)
				Right(
					if (visibilityPointer.value)
						ChangeFuture.wrap(closeFuture.map { _ => false }, true)
					else
						AlwaysFalse
				)
			else
				Left(ResettableLazy { !closeFuture.isCompleted && visibilityPointer.value })
		}
		
		override var listeners = Vector[ChangeListener[Boolean]]()
		override var dependencies = Vector[ChangeDependency[Boolean]]()
		
		
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