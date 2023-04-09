package utopia.genesis.animation.animator

import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.genesis.graphics.Drawer
import utopia.genesis.image.{Image, Strip}
import utopia.genesis.shape.shape2D.MutableTransformable
import utopia.paradigm.animation.TimedAnimation
import utopia.paradigm.shape.shape2d.Point
import utopia.paradigm.transform.AffineTransformation

import scala.concurrent.duration.Duration

object SpriteDrawer
{
	/**
	  * @param sprite A sprite (image) animation
	  * @param transformation Initial transformation state (default = identity transformation)
	  * @return A new sprite drawer
	  */
	def apply(sprite: TimedAnimation[Image], transformation: AffineTransformation = AffineTransformation.identity) =
		new SpriteDrawer(new PointerWithEvents(sprite), new PointerWithEvents(transformation))
}

/**
  * Used for drawing animated sprites using a specific transformation state
  * @author Mikko Hilpinen
  * @since 28.3.2020, v2.1
  */
class SpriteDrawer(val spritePointer: PointerWithEvents[TimedAnimation[Image]],
				   val transformationPointer: PointerWithEvents[AffineTransformation] = new PointerWithEvents(AffineTransformation.identity))
	extends Animator[Image] with MutableTransformable
{
	// COMPUTED	---------------------------
	
	/**
	  * @return The current sprite animation used
	  */
	def sprite = spritePointer.value
	def sprite_=(newSprite: TimedAnimation[Image]) = spritePointer.value = newSprite
	
	
	// IMPLEMENTED	-----------------------
	
	override def animationDuration = sprite.duration
	
	override protected def apply(progress: Double) = sprite(progress)
	
	override protected def draw(drawer: Drawer, item: Image) =
		item.drawWith(drawer * transformation)
	
	override def transformation = transformationPointer.value
	override def transformation_=(newTransformation: AffineTransformation) =
		transformationPointer.value = newTransformation
	
	
	// OTHER	---------------------------
	
	/**
	  * Changes the sprite being drawn
	  * @param newSprite The new sprite being drawn (image, origin and time information)
	  * @param resetProgress Whether the new animation should be started from the beginning (true) or from the
	  *                      current animation point (false). Default = true.
	  */
	def setSprite(newSprite: TimedAnimation[Image], resetProgress: Boolean = true) = {
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
	@deprecated("Please use setSprite instead", "v2.4")
	def setStrip(newStrip: Strip, origin: Point, duration: Duration, resetProgress: Boolean = true) =
		setSprite(newStrip.withOrigin(origin).over(duration), resetProgress)
}
