package utopia.genesis.test

import utopia.flow.async.ThreadPool
import utopia.genesis.animation.animator.{SpriteDrawer, TransformingImageAnimator}
import utopia.genesis.image.Image
import utopia.genesis.shape.shape2D.{Size, Transformation, Vector2D}
import utopia.genesis.util.DefaultSetup
import utopia.flow.util.FileExtensions._
import utopia.flow.util.TimeExtensions._
import utopia.genesis.animation.Animation
import utopia.genesis.shape.shape1D.Rotation

import scala.concurrent.ExecutionContext

/**
  * Testing animation and sprite drawing
  * @author Mikko Hilpinen
  * @since 28.3.2020, v2.1
  */
object SpriteTest extends App
{
	// Sets up the program
	val gameWorldSize = Size(800, 600)
	val setup = new DefaultSetup(gameWorldSize, "Sprite Test")
	
	// Creates sprite drawers
	val strip = Image.readFrom("Genesis/test-images/more-dot-strip-4.png").get.split(4).withCenterOrigin
	val drawer1 = SpriteDrawer(strip over 4.seconds,
		Transformation(translation = gameWorldSize.toVector / 2, scaling = Vector2D(2, 2)))
	
	val image2 = Image.readFrom("Genesis/test-images/mushrooms.png").get.fitting(Size.square(200)).withCenterOrigin
	val drawer2 = TransformingImageAnimator(image2,
		new RotationAnimation(Transformation.translation(200, 200)).over(3.seconds))
	
	setup.registerObjects(drawer1, drawer2)
	
	// Starts the program
	implicit val context: ExecutionContext = new ThreadPool("Test").executionContext
	setup.start()
}

private class RotationAnimation(base: Transformation) extends Animation[Transformation]
{
	override def apply(progress: Double) = base.rotated(Rotation.ofCircles(progress))
}