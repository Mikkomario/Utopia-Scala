package utopia.reach.component.template

import utopia.flow.datastructure.immutable.View
import utopia.flow.datastructure.template.Viewable
import utopia.genesis.image.Image
import utopia.genesis.shape.shape2D.{Bounds, Point}
import utopia.reach.cursor.{Cursor, CursorType}
import utopia.reflection.color.ColorShadeVariant

object CursorDefining
{
	// OTHER	--------------------------------
	
	/**
	  * Specifies cursor settings for a component that doesn't normally define cursor
	  * @param component Component to define
	  * @param cursorTypePointer Pointer to the type of cursor to use
	  * @param shadePointer Pointer to the shade to expect the component to be (light or dark)
	  */
	def defineCursorFor(component: ReachComponentLike, cursorTypePointer: Viewable[CursorType],
						shadePointer: Viewable[ColorShadeVariant]) =
	{
		// Only works if cursor management is enabled in canvas
		component.parentCanvas.cursorManager.foreach { manager =>
			val wrapped = new CursorDefiningWrapper(component, cursorTypePointer, shadePointer)
			// Adds the component to the manager whenever it is attached to the main stack hierarchy
			component.addHierarchyListener { isAttached =>
				if (isAttached)
					manager += wrapped
				else
					manager -= wrapped
			}
		}
	}
	
	/**
	  * Specifies cursor settings for a component that doesn't normally define cursor
	  * @param component Component to define
	  * @param cursorType type of cursor to use
	  * @param shade shade to expect the component to be (light or dark)
	  */
	def defineCursorFor(component: ReachComponentLike, cursorType: CursorType, shade: ColorShadeVariant): Unit =
		defineCursorFor(component, View(cursorType), View(shade))
	
	
	// NESTED	--------------------------------
	
	private class CursorDefiningWrapper(wrapped: ReachComponentLike, cursorTypePointer: Viewable[CursorType],
										shadePointer: Viewable[ColorShadeVariant]) extends CursorDefining
	{
		override def cursorType = cursorTypePointer.value
		
		override def cursorBounds = wrapped.boundsInsideTop
		
		override def cursorToImage(cursor: Cursor, position: Point) = cursor(shadePointer.value)
	}
}

/**
  * A common trait for components that specify what type of cursor should be used inside their bounds
  * @author Mikko Hilpinen
  * @since 11.11.2020, v2
  */
trait CursorDefining
{
	/**
	  * @return The type of cursor displayed over this component
	  */
	def cursorType: CursorType
	
	/**
	  * @return The area <b>relative to the cursor managed context</b> defined by this component. Usually this means
	  *         the component's location and size in relation to the managed canvas element.
	  */
	def cursorBounds: Bounds
	
	/**
	  * Determines the image to use for the cursor
	  * @param cursor Proposed cursor data
	  * @param position Position of the cursor, relative to this component's cursorBounds property
	  * @return The image to draw as the cursor over that location
	  */
	def cursorToImage(cursor: Cursor, position: Point): Image
}
