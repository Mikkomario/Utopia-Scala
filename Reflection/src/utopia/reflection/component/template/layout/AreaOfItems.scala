package utopia.reflection.component.template.layout

import utopia.paradigm.shape.shape2d.{Bounds, Point}

/**
  * A common trait for various containers that have a specific area reserved for each item
  * @author Mikko Hilpinen
  * @since 20.4.2020, v1.2
  */
trait AreaOfItems[C]
{
	// ABSTRACT	---------------------------
	
	/**
	  * Finds the area of a single element in this container, including the area around the object
	  * @param item An item in this stack
	  * @return The bounds around the item. None if the item isn't in this container
	  */
	def areaOf(item: C): Option[Bounds]
	
	/**
	  * Finds the item that's nearest to a <b>relative</b> point in this container
	  * @param relativePoint A point relative to this container's position ((0, 0) = container origin)
	  * @return The component that's nearest to the provided point. None if this container is empty
	  */
	def itemNearestTo(relativePoint: Point): Option[C]
	
	
	// OTHER	---------------------------
	
	/**
	  * @param relativePoint A point relative to this component's position
	  * @return A component area that is closest to the specified position. None if this container is empty.
	  */
	def areaNearestTo(relativePoint: Point) = itemNearestTo(relativePoint).flatMap(areaOf)
}
