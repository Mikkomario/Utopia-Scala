package utopia.reach.component.template

import utopia.flow.view.immutable.View
import utopia.genesis.image.Image
import utopia.paradigm.color.ColorShade
import utopia.paradigm.shape.shape2d.{Bounds, Point}
import utopia.reach.cursor.{Cursor, CursorType}

object CursorDefining
{
	// OTHER	--------------------------------
	
	/**
	  * Specifies cursor settings for a component that doesn't normally define cursor
	  * @param component Component to define
	  * @param cursorTypePointer Pointer to the type of cursor to use
	  * @param shadePointer Pointer to the shade to expect the component to be (light or dark)
	 *  @param customization A function that accepts a cursor and the cursor position
	 *                       (within and relative to component's bounds) and returns a custom cursor image to apply,
	 *                       if appropriate. Returns None if no custom cursor should be applied.
	 *                       None if no customization function should be used.
	  */
	def defineCursorFor(component: ReachComponentLike, cursorTypePointer: View[CursorType],
	                    shadePointer: View[ColorShade],
	                    customization: Option[(Cursor, Point) => Option[Image]] = None) =
	{
		// Only works if cursor management is enabled in canvas
		component.parentCanvas.cursorManager.foreach { manager =>
			val wrapped = new CursorDefiningWrapper(component, cursorTypePointer, shadePointer, customization)
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
	def defineCursorFor(component: ReachComponentLike, cursorType: CursorType, shade: ColorShade): Unit =
		defineCursorFor(component, View.fixed(cursorType), View.fixed(shade))
	
	
	// NESTED	--------------------------------
	
	private class CursorDefiningWrapper(wrapped: ReachComponentLike, cursorTypePointer: View[CursorType],
	                                    shadePointer: View[ColorShade],
	                                    customization: Option[(Cursor, Point) => Option[Image]] = None)
		extends CursorDefining
	{
		// IMPLEMENTED  -------------------------
		
		override def cursorType = cursorTypePointer.value
		
		override def cursorBounds = wrapped.boundsInsideTop
		
		override def cursorToImage(cursor: Cursor, position: Point) = customization match {
			case Some(custom) => custom(cursor, position).getOrElse { defaultCursorToImage(cursor) }
			case None => defaultCursorToImage(cursor)
		}
		
		
		// OTHER    ----------------------------
		
		private def defaultCursorToImage(cursor: Cursor) = cursor.over(shadePointer.value)
	}
	
	
	// EXTENSIONS   ----------------------------
	
	implicit class CursorDefiningComponent(val c: ReachComponentLike with CursorDefining) extends AnyVal
	{
		/**
		 * Registers this component to the parent canvases cursor manager, if available.
		 * Only keeps this component managed while attached to the main stack hierarchy.
		 */
		def register() = {
			c.parentCanvas.cursorManager.foreach { manager =>
				c.addHierarchyListener { isAttached => if (isAttached) manager += c else manager -= c }
			}
		}
	}
}

/**
  * A common trait for components that specify what type of cursor should be used inside their bounds
  * @author Mikko Hilpinen
  * @since 11.11.2020, v0.1
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
