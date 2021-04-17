package utopia.reach.component.hierarchy

import utopia.flow.async.DelayedView
import utopia.flow.event.{AlwaysTrue, ChangeDependency, ChangeEvent, ChangeListener, ChangingLike, LazyMergeMirror, LazyMirror, MergeMirror, Mirror, TripleMergeMirror}
import utopia.reach.component.template.ReachComponentLike
import utopia.reach.container.ReachCanvas

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

/**
  * This component hierarchy block doesn't initially know it's parent hierarchy, but expects to connect to it
  * after a while. This is useful when components are created in bottom to top order, where the parent component
  * is created only after the child component, which is often the case in wrappers.
  * @author Mikko Hilpinen
  * @since 7.10.2020, v0.1
  */
class SeedHierarchyBlock(override val top: ReachCanvas) extends CompletableComponentHierarchy
{
	// ATTRIBUTES	--------------------------
	
	private var foundParent: Option[Either[ReachCanvas, (ComponentHierarchy, ReachComponentLike)]] = None
	
	
	// IMPLEMENTED	-------------------------
	
	override def parent = foundParent.getOrElse(Left(top))
	
	override def linkPointer: ChangingLike[Boolean] = LinkManager
	
	override def isThisLevelLinked = LinkManager.isThisLevelLinked
	
	override def complete(parent: ReachComponentLike): Unit = complete(parent, AlwaysTrue)
	
	
	// OTHER	------------------------------
	
	/**
	  * Completes this component hierarchy by attaching it to a parent component
	  * @param parent Parent component to connect to
	  * @param switchConditionPointer A pointer that determines whether this connection is active. Default = always active.
	  * @throws IllegalStateException If this hierarchy was already completed (These hierarchies mustn't be completed twice)
	  */
	@throws[IllegalStateException]("If already completed previously")
	def complete(parent: ReachComponentLike, switchConditionPointer: ChangingLike[Boolean]) =
	{
		foundParent match
		{
			// Throws if there already existed a parent connection
			case Some(existingParent) =>
				val existingHierarchyString = existingParent match
				{
					case Left(_) => "direct ReachCanvas connection"
					case Right((_, component)) => s"connection to component $component"
				}
				throw new IllegalStateException(s"Trying to override $existingHierarchyString with $parent")
			case None =>
				// Remembers the parent
				val parentHierarchy = parent.parentHierarchy
				foundParent = Some(Right(parentHierarchy -> parent))
				// Informs the link manager about the new link connection
				LinkManager.onParentFound(parentHierarchy.linkPointer, switchConditionPointer)
		}
	}
	
	/**
	  * Completes this component hierarchy by attaching it directly to the canvas at the top
	  * @param switchConditionPointer An additional link condition pointer. Default = always linked.
	  * @throws IllegalStateException If this hierarchy was already completed (These hierarchies mustn't be completed twice)
	  */
	@throws[IllegalStateException]("If already completed previously")
	def lockToTop(switchConditionPointer: ChangingLike[Boolean] = AlwaysTrue) = foundParent match
	{
		case Some(existingParent) =>
			existingParent match
			{
				case Left(_) => ()
				case Right((_, component)) => throw new IllegalStateException(s"Already connected to component $component")
			}
		case None =>
			// Directly attaches to the top
			foundParent = Some(Left(top))
			// Also informs the link manager
			LinkManager.onLinkedToCanvas(switchConditionPointer)
	}
	
	
	// NESTED	------------------------------
	
	private object LinkManager extends ChangingLike[Boolean]
	{
		// ATTRIBUTES	----------------------
		
		// Contains final link status (will be set once hierarchy completes)
		private var finalManagedPointer: Option[ChangingLike[Boolean]] = None
		
		// Holds listeners temporarily while there is no pointer to receive them yet
		private var queuedListeners = Vector[ChangeListener[Boolean]]()
		private var queuedDependencies = Vector[ChangeDependency[Boolean]]()
		
		
		// COMPUTED	--------------------------
		
		def isThisLevelLinked = finalManagedPointer.exists { _.value }
		
		
		// IMPLEMENTED	----------------------
		
		override def isChanging = finalManagedPointer.forall { _.isChanging }
		
		override def value = finalManagedPointer match
		{
			case Some(pointer) => pointer.value
			case None => false
		}
		
		override def addListener(changeListener: => ChangeListener[Boolean]) = finalManagedPointer match
		{
			case Some(pointer) => pointer.addListener(changeListener)
			case None => queuedListeners :+= changeListener
		}
		
		override def addListenerAndSimulateEvent[B >: Boolean](simulatedOldValue: B)(changeListener: => ChangeListener[B]) =
		{
			val listener = changeListener
			addListener(listener)
			simulateChangeEventFor(listener, simulatedOldValue)
		}
		
		override def removeListener(changeListener: Any) =
		{
			queuedListeners = queuedListeners.filterNot { _ == changeListener }
			finalManagedPointer.foreach { _.removeListener(changeListener) }
		}
		
		override def addDependency(dependency: => ChangeDependency[Boolean]) = finalManagedPointer match
		{
			case Some(pointer) => pointer.addDependency(dependency)
			case None => queuedDependencies :+= dependency
		}
		
		override def futureWhere(valueCondition: Boolean => Boolean)(implicit exc: ExecutionContext) =
			defaultFutureWhere(valueCondition)
		
		override def map[B](f: Boolean => B) = Mirror.of(this)(f)
		
		override def lazyMap[B](f: Boolean => B) = LazyMirror.of(this)(f)
		
		override def mergeWith[B, R](other: ChangingLike[B])(f: (Boolean, B) => R) =
			MergeMirror.of(this, other)(f)
		
		override def mergeWith[B, C, R](first: ChangingLike[B], second: ChangingLike[C])(merge: (Boolean, B, C) => R) =
			TripleMergeMirror.of(this, first, second)(merge)
		
		override def lazyMergeWith[B, R](other: ChangingLike[B])(f: (Boolean, B) => R) =
			LazyMergeMirror.of(this, other)(f)
		
		override def delayedBy(threshold: Duration)(implicit exc: ExecutionContext) =
			DelayedView.of(this, threshold)
		
		
		// OTHER	--------------------------
		
		def onParentFound(defaultPointer: ChangingLike[Boolean],
						  additionalConditionPointer: ChangingLike[Boolean]) =
			specifyFinalPointer(defaultPointer && additionalConditionPointer)
		
		def onLinkedToCanvas(additionalConditionPointer: ChangingLike[Boolean]) =
			specifyFinalPointer(top.attachmentPointer && additionalConditionPointer)
		
		private def specifyFinalPointer(pointer: ChangingLike[Boolean]) =
		{
			// Updates the pointer(s)
			finalManagedPointer = Some(pointer)
			
			// Transfers dependencies, if any were queued
			val afterEffects =
			{
				if (queuedDependencies.nonEmpty)
				{
					queuedDependencies.foreach { pointer.addDependency(_) }
					// Informs the dependencies of this new change
					val afterEffects =
					{
						if (pointer.value)
						{
							val event = ChangeEvent(false, true)
							queuedDependencies.flatMap { _.beforeChangeEvent(event) }
						}
						else
							Vector()
					}
					afterEffects
				}
				else
					Vector()
			}
			
			// Transfers listeners, if any were queued
			if (queuedListeners.nonEmpty)
			{
				queuedListeners.foreach { pointer.addListener(_) }
				// Informs the listeners of this new change
				if (pointer.value)
				{
					val event = ChangeEvent(false, true)
					queuedListeners.foreach { _.onChangeEvent(event) }
				}
				queuedListeners = Vector()
			}
			
			// Performs the dependency after effects
			afterEffects.foreach { _() }
		}
	}
}
