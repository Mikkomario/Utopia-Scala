package utopia.genesis.animation.animator

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.genesis.animation.TimedAnimation
import utopia.genesis.image.{Image, Strip}
import utopia.genesis.shape.shape2D.{Point, Transformable, Transformation}
import utopia.genesis.util.Drawer

import scala.concurrent.duration.Duration

object SpriteDrawer
{
	/**
	  * @param sprite A sprite animation (image + image origin)
	  * @param transformation Initial transformation state
	  * @return A new sprite drawer
	  */
	def apply(sprite: TimedAnimation[(Image, Point)], transformation: Transformation) =
		new SpriteDrawer(new PointerWithEvents(sprite), new PointerWithEvents(transformation))
	
	/**
	  * @param strip A strip of images
	  * @param origin Image origin (relative to image top-left)
	  * @param duration Strip completion duration
	  * @param transformation Initial transformation state
	  * @return A new sprite drawer
	  */
	def apply(strip: Strip, origin: Point, duration: Duration, transformation: Transformation): SpriteDrawer =
		apply(strip.map { _ -> origin }.over(duration), transformation)
}

/**
  * Used for drawing animated sprites using a specific transformation state
  * @author Mikko Hilpinen
  * @since 28.3.2020, v2.1
  */
class SpriteDrawer(val spritePointer: PointerWithEvents[TimedAnimation[(Image, Point)]],
				   val transformationPointer: PointerWithEvents[Transformation] = new PointerWithEvents(Transformation.identity))
	extends Animator[(Image, Point)] with Transformable
{
	// COMPUTED	---------------------------
	
	/**
	  * @return The current sprite animation used
	  */
	def sprite = spritePointer.value
	def sprite_=(newSprite: TimedAnimation[(Image, Point)]) = spritePointer.value = newSprite
	
	
	// IMPLEMENTED	-----------------------
	
	override def animationDuration = sprite.duration
	
	override protected def apply(progress: Double) = sprite(progress)
	
	override protected def draw(drawer: Drawer, item: (Image, Point)) =
		item._1.drawWith(drawer.transformed(transformation), origin = item._2)
	
	override def transformation = transformationPointer.value
	override def transformation_=(newTransformation: Transformation) = transformationPointer.value = newTransformation
	
	
	// OTHER	---------------------------
	
	/**
	  * Changes the sprite being drawn
	  * @param newSprite The new sprite being drawn (image, origin and time information)
	  * @param resetProgress Whether the new animation should be started from the beginning (true) or from the
	  *                      current animation point (false). Default = true.
	  */
	def setSprite(newSprite: TimedAnimation[(Image, Point)], resetProgress: Boolean = true) =
	{
		sprite = newSprite
		if (resetProgress)
			reset()
	}
	
	/**
	  * Changes the strip being drawn
	  * @param newStrip New strip to draw
	  * @param origin Origin point used when drawing the strip (relative to image top-left corner)
	  * @param duration      Duration it takes to fully animate the strip
	  * @param resetProgress Whether the new animation should be started from the beginning (true) or from the
	  *                      current animation point (false). Default = true.
	  */
	def setStrip(newStrip: Strip, origin: Point, duration: Duration, resetProgress: Boolean = true) =
		setSprite(newStrip.map { _ -> origin }.over(duration), resetProgress)
}
