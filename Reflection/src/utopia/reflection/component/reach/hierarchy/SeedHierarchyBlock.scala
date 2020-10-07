package utopia.reflection.component.reach.hierarchy

import utopia.flow.event.{ChangeEvent, ChangeListener, Changing}
import utopia.reflection.component.reach.template.ReachComponentLike
import utopia.reflection.container.swing.ReachCanvas

/**
  * This component hierarchy block doesn't initially know it's parent hierarchy, but expects to connect to it
  * after a while. This is useful when components are created in bottom to top order, where the parent component
  * is created only after the child component, which is often the case in wrappers.
  * @author Mikko Hilpinen
  * @since 7.10.2020, v2
  */
class SeedHierarchyBlock(override val top: ReachCanvas) extends CompletableComponentHierarchy
{
	// ATTRIBUTES	--------------------------
	
	private var foundParent: Option[Either[ReachCanvas, (ComponentHierarchy, ReachComponentLike)]] = None
	
	
	// IMPLEMENTED	-------------------------
	
	override def parent = foundParent.getOrElse(Left(top))
	
	override def linkPointer: Changing[Boolean] = LinkManager
	
	override def isThisLevelLinked = LinkManager.isThisLevelLinked
	
	override def complete(parent: ReachComponentLike): Unit = complete(parent, None)
	
	
	// OTHER	------------------------------
	
	/**
	  * Completes this component hierarchy by attaching it to a parent component
	  * @param parent Parent component to connect to
	  * @param switchConditionPointer A pointer that determines whether this connection is active (optional). None if
	  *                               this connection should be static (always active)
	  * @throws IllegalStateException If this hierarchy was already completed (These hierarchies mustn't be completed twice)
	  */
	@throws[IllegalStateException]("If already completed previously")
	def complete(parent: ReachComponentLike, switchConditionPointer: Option[Changing[Boolean]]) =
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
	  * Completes this component hierarchy by attaching it to a parent component
	  * @param parent Parent component to connect to
	  * @param switchConditionPointer A pointer that determines whether this connection is active (optional). None if
	  *                               this connection should be static (always active)
	  * @throws IllegalStateException If this hierarchy was already completed (These hierarchies mustn't be completed twice)
	  */
	@throws[IllegalStateException]("If already completed previously")
	def complete(parent: ReachComponentLike, switchConditionPointer: Changing[Boolean]): Unit =
		complete(parent, Some(switchConditionPointer))
	
	/**
	  * Completes this component hierarchy by attaching it directly to the canvas at the top
	  * @throws IllegalStateException If this hierarchy was already completed (These hierarchies mustn't be completed twice)
	  */
	@throws[IllegalStateException]("If already completed previously")
	def lockToTop() = foundParent match
	{
		case Some(existingParent) =>
			existingParent match
			{
				case Left(_) => ()
				case Right((_, component)) => throw new IllegalStateException(s"Already connected to component $component")
			}
		case None => foundParent = Some(Left(top))
	}
	
	
	// NESTED	------------------------------
	
	private object LinkManager extends Changing[Boolean]
	{
		// ATTRIBUTES	----------------------
		
		// Contains final link status (will be set once hierarchy completes)
		private var parentPointer: Option[Changing[Boolean]] = None
		// Contains custom link switch status, if there is one (may remain None)
		private var switchPointer: Option[Changing[Boolean]] = None
		// Holds listeners temporarily while there is no pointer to receive them yet
		private var queuedListeners = Vector[ChangeListener[Boolean]]()
		
		
		// COMPUTED	--------------------------
		
		def isThisLevelLinked = parentPointer.isDefined && switchPointer.forall { _.value }
		
		
		// IMPLEMENTED	----------------------
		
		override def value = parentPointer match
		{
			case Some(pointer) => pointer.value
			case None => false
		}
		
		override def listeners = parentPointer match
		{
			case Some(pointer) => pointer.listeners
			case None => queuedListeners
		}
		
		override def listeners_=(newListeners: Vector[ChangeListener[Boolean]]) = parentPointer match
		{
			case Some(pointer) => pointer.listeners = newListeners
			case None => queuedListeners = newListeners
		}
		
		
		// OTHER	--------------------------
		
		def onParentFound(defaultPointer: Changing[Boolean], additionalConditionPointer: Option[Changing[Boolean]]) =
		{
			val finalPointer = additionalConditionPointer match
			{
				case Some(additional) => defaultPointer.mergeWith(additional) { _ && _ }
				case None => defaultPointer
			}
			
			// Updates the pointer(s)
			parentPointer = Some(finalPointer)
			switchPointer = additionalConditionPointer
			// Transfers listeners, if any were queued
			if (queuedListeners.nonEmpty)
			{
				finalPointer.listeners = finalPointer.listeners ++ queuedListeners
				// Informs the listeners of this new change
				if (finalPointer.value)
				{
					val event = ChangeEvent(false, true)
					queuedListeners.foreach { _.onChangeEvent(event) }
				}
				queuedListeners = Vector()
			}
		}
	}
}
