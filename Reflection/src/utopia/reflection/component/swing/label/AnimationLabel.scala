package utopia.reflection.component.swing.label

import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.genesis.animation.animator.{Animator, SpriteDrawer, TransformingImageAnimator}
import utopia.genesis.graphics.Drawer
import utopia.genesis.handling.Actor
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.image.{Image, Strip}
import utopia.genesis.util.Fps
import utopia.inception.handling.HandlerType
import utopia.paradigm.angular.Rotation
import utopia.paradigm.animation.TimedAnimation
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.enumeration.Alignment.Center
import utopia.paradigm.shape.shape2d.{Bounds, Matrix2D, Point}
import utopia.paradigm.transform.AffineTransformation
import utopia.reflection.component.context.BaseContextLike
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.component.template.layout.stack.ReflectionStackable
import utopia.reflection.event.StackHierarchyListener
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.shape.stack.StackSize
import utopia.reflection.util.ComponentCreationDefaults

import java.time.Instant
import scala.concurrent.duration.FiniteDuration
import scala.math.Ordering.Double.TotalOrdering

object AnimationLabel
{
	/**
	  * Creates a new label that rotates a static image
	  * @param actorHandler Actor handler that will deliver action events
	  * @param image Image to be drawn
	  * @param rotation Rotation animation
	  * @param alignment Alignment to use when positioning the image in the label (default = Center)
	  * @param maxFps Maximum repaint speed for this element (default = 120 frames per second)
	  * @return A new label
	  */
	def withRotatingImage(actorHandler: ActorHandler, image: Image, rotation: TimedAnimation[Rotation],
						  alignment: Alignment = Center, maxFps: Fps = ComponentCreationDefaults.maxAnimationRefreshRate) =
	{
		val animator = TransformingImageAnimator(image, rotation.map { Matrix2D.rotation(_).to3D })
		val maxRadius = image.size.toBounds().corners.map { p => (p - image.origin).length }.max
		val stackSize = (maxRadius * 2).any.square
		new AnimationLabel(actorHandler, animator, stackSize, Point(maxRadius, maxRadius), alignment, maxFps)
	}
	
	/**
	  * Creates a new label that draws a looping sprite / strip
	  * @param actorHandler Actor handler that will deliver action events
	  * @param strip Image strip
	  * @param animationSpeed Animation speed in frames per second
	  * @param alignment Alignment to use when positioning image in this label (default = Center)
	  * @return A new label
	  */
	def withSprite(actorHandler: ActorHandler, strip: Strip, animationSpeed: Fps,
				   alignment: Alignment = Center) =
	{
		val animator = SpriteDrawer(strip.toTimedAnimation(animationSpeed))
		new AnimationLabel(actorHandler, animator, StackSize.any(strip.size), strip.drawPosition, alignment,
			animationSpeed)
	}
	
	/**
	  * Creates a new label that rotates a static image
	  * @param image Image to be drawn
	  * @param rotation Rotation animation
	  * @param alignment Alignment to use when positioning the image in the label (default = Center)
	  * @param context Implicit component creation context
	  * @return A new label
	  */
	def contextualWithRotatingImage(image: Image, rotation: TimedAnimation[Rotation], alignment: Alignment = Center,
									maxFps: Fps = ComponentCreationDefaults.maxAnimationRefreshRate)
								   (implicit context: BaseContextLike) =
		withRotatingImage(context.actorHandler, image, rotation, alignment, maxFps)
	
	/**
	  * Creates a new label that draws a looping sprite / strip
	  * @param strip Image strip
	  * @param animationSpeed Animation speed in frames per second
	  * @param alignment Alignment to use when positioning image in this label (default = Center)
	  * @param context Implicit component creation context
	  * @return A new label
	  */
	def contextualWithSprite(strip: Strip, animationSpeed: Fps, alignment: Alignment = Center)
							(implicit context: BaseContextLike) =
		withSprite(context.actorHandler, strip, animationSpeed, alignment)
}

/**
  * This label draws an animation on top of itself
  * @author Mikko Hilpinen
  * @since 15.6.2020, v1.2
  * @param actorHandler An actor handler that will deliver action events to progress the animation
  * @param animator Animator used for the actual drawing
  * @param stackSize Size of this label (expected to be the size of the drawn area)
  * @param drawOrigin The point in this label (in optimal size) where the drawer (0, 0) should be located when calling
  *                   animator.draw(...) (default = top left corner of this label)
  * @param alignment Alignment used when positioning the drawn content
  * @param maxFps Maximum repaint speed for this element (default = 120 frames per second)
  */
class AnimationLabel[A](actorHandler: ActorHandler, animator: Animator[A], override val stackSize: StackSize,
                        drawOrigin: Point = Point.origin, alignment: Alignment = Center, maxFps: Fps = Fps(120))
	extends Label with ReflectionStackable
{
	// ATTRIBUTES	-------------------------
	
	private var _isAttached = false
	
	override var stackHierarchyListeners = Vector[StackHierarchyListener]()
	
	
	// INITIAL CODE	-------------------------
	
	addCustomDrawer(ContentDrawer)
	
	
	// IMPLEMENTED	-------------------------
	
	override def updateLayout() = ()
	
	override def resetCachedSize() = ()
	
	override def stackId = hashCode()
	
	override def isAttachedToMainHierarchy = _isAttached
	
	override def isAttachedToMainHierarchy_=(newAttachmentStatus: Boolean) = {
		if (_isAttached != newAttachmentStatus) {
			_isAttached = newAttachmentStatus
			// Animator is only attached to the actor handler when this component is displayable
			if (newAttachmentStatus) {
				actorHandler += animator
				actorHandler += Repainter
			}
			else {
				actorHandler -= animator
				actorHandler -= Repainter
			}
			fireStackHierarchyChangeEvent(newAttachmentStatus)
		}
	}
	
	
	// NESTED	----------------------------
	
	private object ContentDrawer extends CustomDrawer
	{
		override def drawLevel = Normal
		
		override def opaque = false
		
		override def draw(drawer: Drawer, bounds: Bounds) =
		{
			// Determines the draw location and scaling
			val originalSize = stackSize.optimal
			val displaySize = originalSize.fittingWithin(bounds.size)
			val drawPosition = alignment.position(displaySize, bounds)
			val scaling = (displaySize / originalSize).toVector
			// Performs the actual drawing
			animator.draw(drawer * AffineTransformation(drawPosition.toVector + drawOrigin * scaling, scaling = scaling))
		}
	}
	
	private object Repainter extends Actor
	{
		private val threshold = maxFps.interval
		private var lastDraw = Now - threshold
		
		override def act(duration: FiniteDuration) = {
			val time = Instant.now()
			if (time >= lastDraw + threshold)
				repaint()
			lastDraw = time
		}
		
		override def allowsHandlingFrom(handlerType: HandlerType) = visible
	}
}
