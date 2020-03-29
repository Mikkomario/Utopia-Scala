package utopia.genesis.handling.mutable

import utopia.genesis.handling
import utopia.genesis.util.{DepthRange, Drawer}
import utopia.inception.handling.mutable.DeepHandler

object DrawableHandler
{
	/**
	  * @param elements The drawable elements in this handler (default = empty)
	  * @param drawDepth The draw depth of this handler (default = 0)
	  * @param customizer A function that customizes drawers used by this handler (default = None = no customization)
	  * @return A new handler
	  */
	def apply(elements: TraversableOnce[handling.Drawable] = Vector(), drawDepth: Int = DepthRange.default,
			  customizer: Option[Drawer => Drawer] = None) = new DrawableHandler(elements, drawDepth, customizer)
	
	/**
	  * @param element A drawable element
	  * @return a new handler with a single drawable element
	  */
	def apply(element: handling.Drawable): DrawableHandler = apply(Vector(element))
	
	/**
	  * @return A new handler with all specified drawables
	  */
	def apply(first: handling.Drawable, second: handling.Drawable, more: handling.Drawable*): DrawableHandler = apply(Vector(first, second) ++ more)
}

/**
  * This is a mutable implementation of the drawable handler trait
  * @param initialElements The elements initially placed in this handler
  * @param drawDepth The drawing depth of this handler
  * @param customizer A function for customizing drawers used by this handler. None means that no customizing is done
  */
class DrawableHandler(initialElements: TraversableOnce[handling.Drawable], override val drawDepth: Int,
					  val customizer: Option[Drawer => Drawer])
	extends DeepHandler[handling.Drawable](initialElements) with handling.DrawableHandler
{
	/**
	  * Draws the drawable instance using a specific graphics object. The graphics transformations
	  * should always be set back to original after drawing
	  * @param drawer The drawer object used for drawing this instance
	  */
	override def draw(drawer: Drawer) =
	{
		// May customise the graphics context
		val customDrawer = customizer.map { _(drawer) }
		
		// Draws all the elements inside the handler.
		// If the depth order was wrong, fixes it for the next iteration
		var lastDepth = Int.MaxValue
		var sortDepth = false
		
		handle
		{
			drawable =>
				drawable.draw(customDrawer getOrElse drawer)
				if (!sortDepth)
				{
					if (drawable.drawDepth > lastDepth)
						sortDepth = true
					else
						lastDepth = drawable.drawDepth
				}
		}
		
		if (sortDepth)
			sortWith { _.drawDepth > _.drawDepth }
		
		// Clears the customised graphics context, if applicable
		customDrawer.foreach { _.dispose() }
	}
}
