package utopia.reach.component.hierarchy

import utopia.firmament.context.ComponentCreationDefaults
import utopia.firmament.model.CoordinateTransform
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.eventful.OnceFlatteningPointer
import utopia.flow.view.template.eventful.{Changing, ChangingWrapper, Flag}
import utopia.reach.component.template.ReachComponent
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
	
	private var foundParent: Option[Either[ReachCanvas, (ComponentHierarchy, ReachComponent)]] = None
	// Set when this block is replaced with another block
	private var replacement: Option[ComponentHierarchy] = None
	
	
	// IMPLEMENTED	-------------------------
	
	override def parent = replacement match {
		case Some(r) => r.parent
		case None => foundParent.getOrElse(Left(top))
	}
	override def linkedFlag: Flag = replacement match {
		case Some(replacement) => replacement.linkedFlag
		case None => LinkManager
	}
	override def isThisLevelLinked = replacement match {
		case Some(r) => r.isThisLevelLinked
		case None => LinkManager.isThisLevelLinked
	}
	
	override def coordinateTransform: Option[CoordinateTransform] = replacement.flatMap { _.coordinateTransform }
	
	override def complete(parent: ReachComponent): Unit = complete(parent, AlwaysTrue)
	
	
	// OTHER	------------------------------
	
	// TODO: Add IllegalStateException throwing if completing after replacing
	/**
	  * Completes this component hierarchy by attaching it to a parent component
	  * @param parent Parent component to connect to
	  * @param switchConditionFlag A pointer that determines whether this connection is active. Default = always active.
	  * @throws IllegalStateException If this hierarchy was already completed (These hierarchies mustn't be completed twice)
	  */
	@throws[IllegalStateException]("If already completed previously")
	def complete(parent: ReachComponent, switchConditionFlag: Changing[Boolean]) ={
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
				val parentHierarchy = parent.hierarchy
				foundParent = Some(Right(parentHierarchy -> parent))
				// Informs the link manager about the new link connection
				LinkManager.onParentFound(parentHierarchy.linkedFlag, switchConditionFlag)
		}
	}
	/**
	  * Completes this component hierarchy by attaching it directly to the canvas at the top
	  * @param switchConditionFlag An additional link condition pointer. Default = always linked.
	  * @throws IllegalStateException If this hierarchy was already completed (These hierarchies mustn't be completed twice)
	  */
	@throws[IllegalStateException]("If already completed previously")
	def lockToTop(switchConditionFlag: Changing[Boolean] = AlwaysTrue) = foundParent match {
		case Some(existingParent) =>
			existingParent match {
				case Left(_) => ()
				case Right((_, component)) => throw new IllegalStateException(s"Already connected to component $component")
			}
		case None =>
			// Directly attaches to the top
			foundParent = Some(Left(top))
			// Also informs the link manager
			LinkManager.onLinkedToCanvas(switchConditionFlag)
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
	
	private object LinkManager extends ChangingWrapper[Boolean] with Flag
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
		
		def onParentFound(defaultConditionFlag: Flag,
		                  additionalConditionFlag: Changing[Boolean]) =
			specifyFinalFlag(defaultConditionFlag && additionalConditionFlag, additionalConditionFlag)
		
		def onLinkedToCanvas(additionalConditionFlag: Changing[Boolean]) =
			specifyFinalFlag(top.linkedFlag && additionalConditionFlag, additionalConditionFlag)
		
		def onReplacement(replacement: ComponentHierarchy) =
			specifyFinalFlag(replacement.linkedFlag, View { replacement.isThisLevelLinked })
		
		private def specifyFinalFlag(flag: Changing[Boolean], thisLevelFlag: View[Boolean]) = {
			// Updates the pointer(s)
			_wrapped.complete(flag)
			finalLinkConditionPointer = Some(thisLevelFlag)
		}
	}
}
