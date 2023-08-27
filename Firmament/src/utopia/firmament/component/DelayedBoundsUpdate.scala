package utopia.firmament.component

import utopia.firmament.component.stack.HasStackSize
import utopia.firmament.model.stack.StackSize
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size

object DelayedBoundsUpdate
{
	/**
	  * @param item An item to wrap
	  * @return A wrapper that only applies bounds updates when called
	  */
	def apply(item: HasMutableBounds with HasStackSize) = new DelayedBoundsUpdate(item, item.stackSize)
}

/**
  * A wrapper which updates the bounds of the underlying component only when called.
  * This is useful when  a large number of movements is to be reduced into a single movement.
  * @author Mikko Hilpinen
  * @since 3.5.2023, v1.1
  */
class DelayedBoundsUpdate(wrapped: HasMutableBounds, getStackSize: => StackSize)
	extends HasMutableBounds with HasStackSize
{
	// ATTRIBUTES   ---------------------------
	
	private var newPosition: Option[Point] = None
	private var newSize: Option[Size] = None
	
	
	// IMPLEMENTED  ---------------------------
	
	override def bounds: Bounds = newPosition match {
		case Some(p) =>
			val s = size
			Bounds(p, s)
		case None =>
			newSize match {
				case Some(s) => Bounds(wrapped.position, s)
				case None => wrapped.bounds
			}
	}
	override def bounds_=(b: Bounds): Unit = {
		newPosition = Some(b.position)
		newSize = Some(b.size)
	}
	
	override def position = newPosition.getOrElse(wrapped.position)
	override def position_=(p: Point): Unit = newPosition = Some(p)
	
	override def size = newSize.getOrElse(wrapped.size)
	override def size_=(s: Size): Unit = newSize = Some(s)
	
	override def stackSize: StackSize = getStackSize
	
	
	// OTHER    --------------------------
	
	/**
	  * Performs the bounds updates
	  */
	def apply() = {
		newPosition.filterNot { _ == wrapped.position }.foreach { wrapped.position = _ }
		newSize.filterNot { _ == wrapped.size }.foreach { wrapped.size = _ }
	}
}
