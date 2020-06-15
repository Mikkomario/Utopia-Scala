package utopia.reflection.component.swing.label

import utopia.genesis.animation.animator.Animator
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.shape.shape2D.{Bounds, Point, Transformation}
import utopia.genesis.util.Drawer
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.component.stack.StackLeaf
import utopia.reflection.shape.Alignment.Center
import utopia.reflection.shape.{Alignment, StackSize}

/**
  * This label draws an animation on top of itself
  * @author Mikko Hilpinen
  * @since 15.6.2020, v1.2
  * @param actorHandler An actor handler that will deliver action events to progress the animation
  * @param animator Animator used for the actual drawing
  * @param stackSize Size of this label (expected to be the size of the drawn area)
  * @param drawOrigin The point in this label (in optimal size) where the drawer (0, 0) should be located when calling
  *                   animator.draw(...)
  * @param alignment Alignment used when positioning the drawn content
  */
class AnimationLabel[A](actorHandler: ActorHandler, animator: Animator[A], override val stackSize: StackSize,
						drawOrigin: Point = Point.origin, alignment: Alignment = Center)
	extends Label with StackLeaf
{
	// INITIAL CODE	-------------------------
	
	actorHandler += animator
	addCustomDrawer(ContentDrawer)
	
	
	// IMPLEMENTED	-------------------------
	
	override def updateLayout() = ()
	
	override def resetCachedSize() = ()
	
	override def stackId = hashCode()
	
	
	// NESTED	----------------------------
	
	object ContentDrawer extends CustomDrawer
	{
		override def drawLevel = Normal
		
		override def draw(drawer: Drawer, bounds: Bounds) =
		{
			// Determines the draw location and scaling
			val originalSize = stackSize.optimal
			val drawBounds = alignment.position(originalSize, bounds)
			val scaling = (drawBounds.size / originalSize).toVector
			// Performs the actual drawing
			drawer.transformed(Transformation.position(drawBounds.position + drawOrigin * scaling).scaled(scaling))
				.disposeAfter(animator.draw)
		}
	}
}
