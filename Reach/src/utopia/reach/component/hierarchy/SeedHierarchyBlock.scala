package utopia.reach.component.hierarchy

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeEvent
import utopia.flow.operator.End
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.Changing
import utopia.flow.view.template.eventful.FlagLike.wrap
import utopia.reach.component.template.ReachComponentLike
import utopia.reach.container.ReachCanvas

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
	// Set when this block is replaced with another block
	private var replacement: Option[ComponentHierarchy] = None
	
	
	// IMPLEMENTED	-------------------------
	
	override def parent = replacement match {
		case Some(r) => r.parent
		case None => foundParent.getOrElse(Left(top))
	}
	override def linkPointer: Changing[Boolean] = replacement match {
		case Some(replacement) => replacement.linkPointer
		case None => LinkManager
	}
	override def isThisLevelLinked = replacement match {
		case Some(r) => r.isThisLevelLinked
		case None => LinkManager.isThisLevelLinked
	}
	
	override def complete(parent: ReachComponentLike): Unit = complete(parent, AlwaysTrue)
	
	
	// OTHER	------------------------------
	
	// TODO: Add IllegalStateException throwing if completing after replacing
	/**
	  * Completes this component hierarchy by attaching it to a parent component
	  * @param parent Parent component to connect to
	  * @param switchConditionPointer A pointer that determines whether this connection is active. Default = always active.
	  * @throws IllegalStateException If this hierarchy was already completed (These hierarchies mustn't be completed twice)
	  */
	@throws[IllegalStateException]("If already completed previously")
	def complete(parent: ReachComponentLike, switchConditionPointer: Changing[Boolean]) ={
		foundParent match {
			// Throws if there already existed a parent connection
			case Some(existingParent) =>
				val existingHierarchyString = existingParent match {
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
	def lockToTop(switchConditionPointer: Changing[Boolean] = AlwaysTrue) = foundParent match {
		case Some(existingParent) =>
			existingParent match {
				case Left(_) => ()
				case Right((_, component)) => throw new IllegalStateException(s"Already connected to component $component")
			}
		case None =>
			// Directly attaches to the top
			foundParent = Some(Left(top))
			// Also informs the link manager
			LinkManager.onLinkedToCanvas(switchConditionPointer)
	}
	/**
	  * @param other Another hierarchy block that will effectively replace this one
	  * @throws IllegalStateException If already completed or replaced before
	  */
	@throws[IllegalStateException]("If already connected or replaced previously")
	def replaceWith(other: ComponentHierarchy) = foundParent match {
		case Some(_) => throw new IllegalStateException("Already attached")
		case None =>
			if (replacement.nonEmpty)
				throw new IllegalStateException("Already replaced")
			else {
				replacement = Some(other)
				LinkManager.onReplacement(other)
			}
	}
	
	
	// NESTED	------------------------------
	
	private object LinkManager extends Changing[Boolean]
	{
		// ATTRIBUTES	----------------------
		
		// Contains final link status (will be set once hierarchy completes)
		private var finalManagedPointer: Option[Changing[Boolean]] = None
		// Contains "this level linked" -status after connected to a parent
		private var finalLinkConditionPointer: Option[View[Boolean]] = None
		
		// Holds listeners temporarily while there is no pointer to receive them yet
		private var queuedListeners = Pair.twice(Vector.empty[ChangeListener[Boolean]])
		
		
		// COMPUTED	--------------------------
		
		def isThisLevelLinked = finalLinkConditionPointer.exists { _.value }
		
		
		// IMPLEMENTED	----------------------
		
		override def isChanging = finalManagedPointer.forall { _.isChanging }
		
		override def value = finalManagedPointer match {
			case Some(pointer) => pointer.value
			case None => false
		}
		
		override def addListenerOfPriority(priority: End)(listener: => ChangeListener[Boolean]) =
			finalManagedPointer match {
				case Some(pointer) => pointer.addListenerOfPriority(priority)(listener)
				case None => queuedListeners = queuedListeners.mapSide(priority) { _ :+ listener }
			}
		
		override def removeListener(changeListener: Any) = {
			queuedListeners = queuedListeners.map { _.filterNot { _ == changeListener } }
			finalManagedPointer.foreach { _.removeListener(changeListener) }
		}
		
		
		// OTHER	--------------------------
		
		def onParentFound(defaultPointer: Changing[Boolean],
		                  additionalConditionPointer: Changing[Boolean]) =
			specifyFinalPointer(defaultPointer && additionalConditionPointer, additionalConditionPointer)
		
		def onLinkedToCanvas(additionalConditionPointer: Changing[Boolean]) =
			specifyFinalPointer(top.attachmentPointer && additionalConditionPointer, additionalConditionPointer)
		
		def onReplacement(replacement: ComponentHierarchy) =
			specifyFinalPointer(replacement.linkPointer, View { replacement.isThisLevelLinked })
		
		private def specifyFinalPointer(pointer: Changing[Boolean], thisLevelPointer: View[Boolean]) = {
			// Updates the pointer(s)
			finalManagedPointer = Some(pointer)
			finalLinkConditionPointer = Some(thisLevelPointer)
			
			// Transfers listeners, if any were queued
			val queued = queuedListeners
			queuedListeners = Pair.twice(Vector.empty)
			// Case: Attachment status changes => Informs the listeners
			if (pointer.value) {
				val event = ChangeEvent(false, true)
				val afterEffects = End.values.flatMap { prio =>
					// Only transfers those listeners that didn't get detached upon this change
					val (remainingListeners, afterEffects) = queued(prio).splitFlatMap { listener =>
						val response = listener.onChangeEvent(event)
						(if (response.shouldContinueListening) Some(listener) else None) -> response.afterEffects
					}
					remainingListeners.foreach { pointer.addListenerOfPriority(prio)(_) }
					afterEffects
				}
				// Triggers all after-effects once the listeners have been handled
				afterEffects.foreach { _() }
			}
			// Case: Attachment status stays the same => Simply transfers the listeners
			else
				End.values.foreach { prio => queued(prio).foreach { pointer.addListenerOfPriority(prio)(_) } }
		}
	}
}
