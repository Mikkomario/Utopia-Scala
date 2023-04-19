package utopia.reach.dnd

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.event.model.{ChangeEvent, DetachmentChoice}
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.paradigm.shape.shape2d.Point
import utopia.reach.component.template.ReachComponentLike
import utopia.reach.container.ReachCanvas
import utopia.reach.dnd.DragAndDropEvent._

import java.awt.dnd._
import java.io.File
import scala.jdk.CollectionConverters._
import scala.util.Try

object DragAndDropManager
{
	/**
	  * @param canvas A Reach canvas for which the drag-and-drop process is managed
	  * @param log    Implicit logging implementation
	  * @return A new drag-and-drop manager that supervises the specified canvas
	  */
	def apply(canvas: => ReachCanvas)(implicit log: Logger) = new DragAndDropManager(canvas.component)
}

/**
  * A drag-and-drop process manager that supervises a single ReachCanvas instance, and a set of drag-and-drop targets
  * @author Mikko Hilpinen
  * @since 19.2.2023, v0.5.1
  * @param component The component for which the drag-and-drop process is managed
  * @param log Implicit logging implementation
  */
class DragAndDropManager(component: => java.awt.Component)(implicit log: Logger)
{
	// ATTRIBUTES   --------------------------
	
	private var mousePosition = Point.origin
	private var hoverTargets = Set[DragAndDropTarget]()
	
	private val targetsPointer = new PointerWithEvents(Vector[DragAndDropTarget]())
	
	
	// INITIAL CODE -------------------------
	
	// While there are drop targets registered, listens to drag-and-drop events
	targetsPointer.addListener { e =>
		if (e.newValue.nonEmpty) {
			Try { new DropTarget(component, DnDListener) }
				.failure.foreach { log(_, "Failed to start drag-and-drop managing") }
			DetachmentChoice.detach
		}
		else
			DetachmentChoice.continue
	}
	
	
	// COMPUTED ------------------------------
	
	private def targets = targetsPointer.value
	private def targets_=(newTargets: Vector[DragAndDropTarget]) = targetsPointer.value = newTargets
	
	
	// OTHER    ------------------------------
	
	/**
	  * Adds a new drag-and-drop target to manage
	  * @param target New drag-and-drop target
	  */
	def addTarget(target: DragAndDropTarget) = targets +:= target
	/**
	  * Adds a new drag-and-drop target to manage.
	  * Only manages the target while it is attached to the main component hierarchy.
	  * @param target New drag-and-drop target
	  */
	def addTargetWhileAttached(target: DragAndDropTarget with ReachComponentLike) =
		target.parentHierarchy.linkPointer.addListenerAndSimulateEvent(false) { isLinked =>
			if (isLinked.newValue)
				addTarget(target)
			else
				removeTarget(target)
			DetachmentChoice.continue
		}
	
	/**
	  * Removes a drag-and-drop target from the managed targets
	  * @param target A target to remove
	  */
	def removeTarget(target: DragAndDropTarget) = targets = targets.filterNot { _ == target }
	
	
	// NESTED   ------------------------------
	
	private object DnDListener extends DropTargetListener
	{
		override def dragEnter(dtde: DropTargetDragEvent) = {
			// Generates the EnterCanvas -event
			mousePosition = Point.of(dtde.getLocation)
			targets.foreach { _.onDragAndDropEvent(EnterCanvas) }
			// If some target was already entered, informs it
			lazy val positionChange = ChangeEvent(Pair.twice(mousePosition))
			val initialHoverTargets = targets
				.flatMap { t => Some(t.dropArea).filter { _.contains(mousePosition) }.map { t -> _ } }
			hoverTargets = initialHoverTargets.iterator.map { _._1 }.toSet
			initialHoverTargets.foreach { case (target, area) =>
				target.onDragAndDropEvent(
					Enter(ChangeEvent(Pair.twice(mousePosition - area.position)), positionChange))
			}
		}
		
		override def dragOver(dtde: DropTargetDragEvent) = {
			// Generates Enter, Exit, Over & Outside -events
			val lastPosition = mousePosition
			mousePosition = Point.of(dtde.getLocation)
			val positionChange = ChangeEvent(lastPosition, mousePosition)
			targets.foreach { target =>
				val area = target.dropArea
				val relativeChange = positionChange.map { area.position - _ }
				if (area.contains(mousePosition)) {
					if (hoverTargets.contains(target))
						target.onDragAndDropEvent(Over(relativeChange, positionChange))
					else {
						hoverTargets += target
						target.onDragAndDropEvent(Enter(relativeChange, positionChange))
					}
				}
				else if (hoverTargets.contains(target)) {
					hoverTargets -= target
					target.onDragAndDropEvent(Exit(relativeChange, positionChange))
				}
				else
					target.onDragAndDropEvent(Outside(relativeChange, positionChange))
			}
		}
		
		override def dragExit(dte: DropTargetEvent) = {
			// Generates Exit & ExitCanvas -events
			lazy val positionChange = ChangeEvent(Pair.twice(mousePosition))
			hoverTargets.foreach { t =>
				val relativePosition = mousePosition - t.dropArea.position
				t.onDragAndDropEvent(Exit(ChangeEvent(Pair.twice(relativePosition)), positionChange))
			}
			hoverTargets = Set()
			targets.foreach { _.onDragAndDropEvent(ExitCanvas) }
		}
		
		override def drop(dtde: DropTargetDropEvent) = {
			// Finds the target that shall receive the drop
			val dropTarget = {
				// Case: No target is directly under the mouse
				// => Finds if any of them is willing to accept a nearby drop
				if (hoverTargets.isEmpty)
					targets.map { t => t -> t.dropArea }
						.sortBy { case (_, area) => (area.center - mousePosition).length }
						.find { case (target, area) => target.onDropNearby(mousePosition - area.position, mousePosition) }
				// Case: There are multiple drop targets under the mouse => Targets the closes
				else if (hoverTargets.hasSize > 1)
					Some(hoverTargets.map { t => t -> t.dropArea }
						.minBy { case (_, area) => (area.center - mousePosition).length })
				// Case: Exactly one drop target under the mouse => Targets that one
				else {
					val target = hoverTargets.head
					Some(target -> target.dropArea)
				}
			}
			// Informs the drop target
			dropTarget.foreach { case (target, area) =>
				// Reads the files from the drop
				dtde.acceptDrop(DnDConstants.ACTION_COPY)
				val item = dtde.getTransferable
				val paths = item.getTransferDataFlavors.toVector.filter { _.isFlavorJavaFileListType }.flatMap { flavor =>
					Try {
						item.getTransferData(flavor).asInstanceOf[java.lang.Iterable[File]].asScala.map { _.toPath }
					}.getOrMap { error =>
						log(error, "Failed to receive a file drop")
						None
					}
				}
				target.onDragAndDropEvent(Drop(mousePosition - area.position, mousePosition, paths))
			}
			// Informs the other targets
			targets.iterator.filterNot(dropTarget.contains).foreach { _.onDragAndDropEvent(DropToOther) }
		}
		
		override def dropActionChanged(dtde: DropTargetDragEvent) = ()
	}
}
