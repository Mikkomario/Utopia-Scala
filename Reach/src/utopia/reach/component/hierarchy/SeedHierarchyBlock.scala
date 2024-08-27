package utopia.reach.component.hierarchy

import utopia.firmament.context.ComponentCreationDefaults
import utopia.firmament.model.CoordinateTransform
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.eventful.OnceFlatteningPointer
import utopia.flow.view.template.eventful.FlagLike.wrap
import utopia.flow.view.template.eventful.{Changing, ChangingWrapper, FlagLike}
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
	override def linkPointer: FlagLike = replacement match {
		case Some(replacement) => replacement.linkPointer
		case None => LinkManager
	}
	override def isThisLevelLinked = replacement match {
		case Some(r) => r.isThisLevelLinked
		case None => LinkManager.isThisLevelLinked
	}
	
	override def coordinateTransform: Option[CoordinateTransform] = replacement.flatMap { _.coordinateTransform }
	
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
	  * Completes this hierarchy by replacing this block with another.
	  * Useful when you want to apply custom functionality.
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
	
	private object LinkManager extends ChangingWrapper[Boolean] with FlagLike
	{
		// ATTRIBUTES	----------------------
		
		// Contains final link status (will be set once hierarchy completes)
		private val _wrapped = OnceFlatteningPointer(false)
		
		// Contains "this level linked" -status after connected to a parent
		private var finalLinkConditionPointer: Option[View[Boolean]] = None
		
		
		// COMPUTED	--------------------------
		
		def isThisLevelLinked = finalLinkConditionPointer.exists { _.value }
		
		
		// IMPLEMENTED  ----------------------
		
		override implicit def listenerLogger: Logger = ComponentCreationDefaults.componentLogger
		override protected def wrapped: Changing[Boolean] = _wrapped
		
		
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
			_wrapped.complete(pointer)
			finalLinkConditionPointer = Some(thisLevelPointer)
		}
	}
}
