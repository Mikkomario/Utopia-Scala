package utopia.genesis.handling.immutable

import utopia.genesis.graphics.Drawer3
import utopia.genesis.handling
import utopia.genesis.handling.Drawable2
import utopia.genesis.util.DepthRange
import utopia.inception.handling.immutable.{Handleable, Handler}

object DrawableHandler2
{
	/**
	  * An empty drawable handler
	  */
	val empty = new DrawableHandler2(Vector(), DepthRange.default, None)
	
	/**
	  * @param elements The elements in the handler. Will be depth sorted.
	  * @param drawDepth The draw depth of the handler (default = 0)
	  * @param customizer A function that customizes drawers used by the handler (default = None = No customization)
	  * @param parent A handleable this handler is dependent from (default = None = handler is independent)
	  * @return A new handler
	  */
	def apply(elements: Seq[Drawable2], drawDepth: Int = DepthRange.default,
	          customizer: Option[Drawer3 => Drawer3] = None,
	          parent: Option[Handleable] = None) =
		new DrawableHandler2(elements, drawDepth, customizer)
	
	/**
	  * @param element The drawable element in the handler
	  * @return A new handler
	  */
	def apply(element: Drawable2): DrawableHandler2 = apply(Vector(element))
	
	/**
	  * @return A drawable handler with all specified elements
	  */
	def apply(first: Drawable2, second: Drawable2, more: Drawable2*): DrawableHandler2 =
		apply(Vector(first, second) ++ more)
}

/**
  * This is an immutable implementation of the DrawableHandler trait
  * @param initialElements The initial drawable elements. Will be sorted based on drawing depth.
  * @param drawDepth The draw depth of this handler
  * @param customizer A function for customizing drawers used by this handler. None if no customization should be done
  */
class DrawableHandler2(initialElements: Seq[Drawable2], drawDepth: Int, customizer: Option[Drawer3 => Drawer3])
	extends Handler[Drawable2](initialElements.sortWith { _.drawDepth > _.drawDepth })
		with handling.DrawableHandler2 with Handleable
{
	/**
	  * Draws the drawable instance using a specific graphics object. The graphics transformations
	  * should always be set back to original after drawing
	  * @param drawer The drawer object used for drawing this instance
	  */
	override def draw(drawer: Drawer3) =
	{
		// May use a custom drawer
		val customDrawer = customizer.map { _(drawer) }
		handle { _.draw(customDrawer getOrElse drawer) }
		customDrawer.foreach { _.close() }
	}
}
