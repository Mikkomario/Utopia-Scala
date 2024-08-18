package utopia.reach.component.hierarchy

import utopia.firmament.model.CoordinateTransform
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.mutable.iterator.OptionsIterator
import utopia.flow.view.template.eventful.FlagLike
import utopia.genesis.graphics.Priority.Normal
import utopia.genesis.graphics.{FontMetricsWrapper, Priority}
import utopia.genesis.text.Font
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.template.vector.DoubleVectorLike
import utopia.reach.component.template.ReachComponentLike
import utopia.reach.container.ReachCanvas

import scala.annotation.tailrec
import scala.collection.immutable.VectorBuilder

/**
  * Represents a sequence of components that forms a linear hierarchy
  * @author Mikko Hilpinen
  * @since 3.10.2020, v0.1
  */
trait ComponentHierarchy
{
	// ABSTRACT	--------------------------
	
	/**
	  * @return The next "block" in this hierarchy (either Left: Canvas at the top or
	  *         Right: an intermediate block + a component associated with that block)
	  */
	def parent: Either[ReachCanvas, (ComponentHierarchy, ReachComponentLike)]
	
	/**
	  * @return A pointer that shows whether this hierarchy is currently active / linked to the top window.
	  *         Should take into account possible parent state.
	  */
	// TODO: Rename to linkedFlag
	def linkPointer: FlagLike
	// TODO: Consider adding the following two methods
	/*
	  * @return A pointer that contains the direct parent component's position relative to the root canvas component
	  */
	// def parentPositionInCanvasPointer: Changing[Point]
	/*
	  * @return A pointer that contains this component's position on the screen
	  */
	// def absoluteParentPositionPointer: Changing[Point]
	
	/**
	  * @return Whether the link between this component and the parent component should be considered active
	  */
	def isThisLevelLinked: Boolean
	
	/**
	  * @return Coordinate transform converting parent coordinates to relative, local coordinates.
	  *         None if no coordinate transform is necessary.
	  */
	def coordinateTransform: Option[CoordinateTransform]
	
	
	// COMPUTED	--------------------------
	
	/**
	  * @return The component directly over this component
	  */
	def parentComponent = parent.leftOrMap { _._2 }
	/**
	  * @return An iterator that returns components from this level's parents upward.
	  *         Will stop at (i.e. not include) the ReachCanvas instance.
	  */
	def parentsIterator =
		OptionsIterator.iterate(parent.toOption) { _._1.parent.toOption }.map { _._2 }
	/**
	  * @return The canvas at the top of this hierarchy
	  */
	def top: ReachCanvas = parent match {
		case Left(canvas) => canvas
		case Right((block, _)) => block.top
	}
	
	/**
	  * @return Whether this hierarchy currently reaches the top component without any broken links
	  */
	def isLinked = linkPointer.value
	/**
	  * @return Whether this hierarchy doesn't reach the top component at this time
	  */
	def isDetached = !isLinked
	
	/**
	  * @return The window that contains this component hierarchy. None if not connected to a window.
	  */
	def parentWindow = top.parentWindow
	
	/**
	  * @return A modifier used when calculating the position of the bottom component (outside of this hierarchy)
	  *         relative to hierarchy top
	  */
	def positionToTopModifier: Vector2D = transform(defaultPositionToTopModifier)
	/**
	  * @return A modifier used for calculating the absolute position of the bottom component (not on this hierarchy)
	  */
	def absolutePositionModifier = transform(defaultPositionToTopModifier + top.absolutePosition)
	
	/**
	  * @return A linear component sequence based on this component hierarchy. The higher hierarchy components are
	  *         placed in the beginning and the last element is the first direct parent component. If this hierarchy
	  *         doesn't have parents before the canvas, returns an empty vector.
	  */
	def toVector: Vector[ReachComponentLike] = parentsIterator.toVector.reverse
	
	private def defaultPositionToTopModifier = parent match {
		case Left(_) => Vector2D.zero
		case Right((block, component)) => block.positionToTopModifier + component.position
	}
	
	
	// OTHER	--------------------------
	
	/**
	  * @param hierarchy A component hierarchy
	  * @return Whether this component hierarchy block is under specified component hierarchy
	  */
	@tailrec
	final def isChildOf(hierarchy: ComponentHierarchy): Boolean = parent match {
		case Right((parentHierarchy, _)) => parentHierarchy == hierarchy || parentHierarchy.isChildOf(hierarchy)
		case Left(_) => false
	}
	/**
	  * @param component A component
	  * @return Whether this component hierarchy block is under specified component
	  */
	@tailrec
	final def isChildOf(component: ReachComponentLike): Boolean = parent match {
		case Right((parentHierarchy, parentComponent)) =>
			parentComponent == component || parentHierarchy.isChildOf(component)
		case Left(_) => false
	}
	/**
	  * @param component A component
	  * @return Whether that component is part of this hierarchy (either below or above)
	  */
	def contains(component: ReachComponentLike) =
		component.parentHierarchy == this || component.isChildOf(this) || isChildOf(component)
	
	/**
	  * @param component A component
	  * @return A modifier to apply to this hierarchy's child's position in order to get the position in the specified
	  *         component. None if this hierarchy is not a child of the specified component.
	  */
	def positionInComponentModifier(component: ReachComponentLike): Option[Vector2D] = parent match {
		case Right((parentHierarchy, parentComponent)) =>
			val default = {
				if (parentComponent == component)
					Some(Vector2D.zero)
				else
					parentHierarchy.positionInComponentModifier(component).map { _ + parentComponent.position }
			}
			default.map(transform)
		case Left(_) => None
	}
	
	/**
	  * Revalidates this component hierarchy (provided this part of the hierarchy is currently linked to the main
	  * stack hierarchy)
	  */
	def revalidate(layoutUpdateComponents: Seq[ReachComponentLike]): Unit = {
		val branchBuilder = new VectorBuilder[ReachComponentLike]()
		layoutUpdateComponents.reverseIterator.foreach { branchBuilder += _ }
		_revalidate(branchBuilder) { _.revalidate(_) }
	}
	/**
	  * Revalidates this component hierarchy (provided this part of the hierarchy is currently linked to the main
	  * stack hierarchy), then calls the specified function
	  * @param f A function called once this hierarchy has been updated. Please note that this function might not
	  *          get called at all.
	  */
	def revalidateAndThen(layoutUpdateComponents: Seq[ReachComponentLike])(f: => Unit): Unit = {
		val branchBuilder = new VectorBuilder[ReachComponentLike]()
		layoutUpdateComponents.reverseIterator.foreach { branchBuilder += _ }
		_revalidate(branchBuilder) { _.revalidateAndThen(_)(f) }
	}
	@tailrec
	private def _revalidate(branchBuilder: VectorBuilder[ReachComponentLike])
	                       (callCanvas: (ReachCanvas, Vector[ReachComponentLike]) => Unit): Unit =
	{
		// Terminates if not linked
		if (isThisLevelLinked)
			parent match {
				// Case: Top reached => Performs the revalidation function
				case Left(canvas) =>
					val branch = branchBuilder.result().reverse
					callCanvas(canvas, branch)
				// Case: Parent is not canvas => Adds the component to the queue and continues recursively
				case Right((block, component)) =>
					component.resetCachedSize()
					branchBuilder += component
					block._revalidate(branchBuilder)(callCanvas)
			}
	}
	
	/**
	  * Repaints the whole component hierarchy (if linked)
	  */
	def repaintAll() = if (isLinked) top.repaint()
	/**
	  * Repaints a sub-section of the bottom component (if linked to top)
	  * @param area Area inside the bottom component
	  * @param priority Priority used for this repaint operation. Higher priority areas are painted first
	  *                 (default = Normal)
	  */
	def repaint(area: => Bounds, priority: Priority = Normal) = {
		if (isLinked) {
			val areaInTop = inverseTransform(area) + defaultPositionToTopModifier
			top.repaint(areaInTop, priority)
		}
	}
	/**
	  * Repaints the bottom component
	  */
	def repaintBottom(priority: Priority = Normal) = {
		if (isLinked)
			parent match {
				case Left(canvas) => canvas.repaint()
				case Right((block, component)) => block.repaint(component.bounds, priority)
			}
	}
	
	/**
	  * Shifts a painted area by specified amount
	  * @param area A region in this component hierarchy (lowest parent's position system) to shift
	  * @param translation The amount of translation applied to the area
	  */
	def shiftArea(area: => Bounds, translation: => Vector2D) = {
		if (isLinked)
			top.shiftArea(inverseTransform(area) + defaultPositionToTopModifier, translation)
	}
	
	/**
	  * @param font Font to use
	  * @return Metrics for that font
	  */
	// TODO: Refactor this once component class hierarchy has been updated
	def fontMetricsWith(font: Font): FontMetricsWrapper = top.component.getFontMetrics(font.toAwt)
	
	// Transforms a coordinate from parent space to the relative space under this hierarchy
	private def transform[V <: DoubleVectorLike[V]](coordinate: V) = coordinateTransform match {
		case Some(transform) => transform(coordinate)
		case None => coordinate
	}
	// Transforms a relative area to the parent's coordinate space
	private def inverseTransform(area: Bounds) = coordinateTransform.map { _.invert(area) } match {
		case Some(transformed) => transformed.bounds
		case None => area
	}
}
