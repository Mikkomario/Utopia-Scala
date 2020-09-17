package utopia.reflection.component.swing.label

import java.time.Instant

import scala.math.Ordering.Double.TotalOrdering
import utopia.flow.util.TimeExtensions._
import utopia.genesis.animation.TimedAnimation
import utopia.genesis.animation.animator.{Animator, SpriteDrawer, TransformingImageAnimator}
import utopia.genesis.handling.Actor
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.image.{Image, Strip}
import utopia.genesis.shape.shape1D.Rotation
import utopia.genesis.shape.shape2D.{Bounds, Point, Transformation}
import utopia.genesis.util.{Drawer, Fps}
import utopia.inception.handling.HandlerType
import utopia.reflection.component.context.BaseContextLike
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.component.template.layout.stack.Stackable
import utopia.reflection.event.StackHierarchyListener
import utopia.reflection.shape.Alignment.Center
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.shape.stack.StackSize
import utopia.reflection.util.ComponentCreationDefaults

import scala.concurrent.duration.FiniteDuration

object AnimationLabel
{
	/**
	  * Creates a new label that rotates a static image
	  * @param actorHandler Actor handler that will deliver action events
	  * @param image Image to be drawn
	  * @param origin Image origin (relative to image top-left corner)
	  * @param rotation Rotation animation
	  * @param alignment Alignment to use when positioning the image in the label (default = Center)
	  * @param maxFps Maximum repaint speed for this element (default = 120 frames per second)
	  * @return A new label
	  */
	def withRotatingImage(actorHandler: ActorHandler, image: Image, origin: Point, rotation: TimedAnimation[Rotation],
						  alignment: Alignment = Center, maxFps: Fps = ComponentCreationDefaults.maxAnimationRefreshRate) =
	{
		val animator = TransformingImageAnimator(image, origin, rotation.map(Transformation.rotation))
		val maxRadius = image.size.toBounds().corners.map { p => (p - origin).length }.max
		val stackSize = (maxRadius * 2).any.square
		new AnimationLabel(actorHandler, animator, stackSize, Point(maxRadius, maxRadius), alignment, maxFps)
	}
	
	/**
	  * Creates a new label that draws a looping sprite / strip
	  * @param actorHandler Actor handler that will deliver action events
	  * @param strip Image strip
	  * @param spriteOrigin Image origin to use
	  * @param animationSpeed Animation speed in frames per second
	  * @param alignment Alignment to use when positioning image in this label (default = Center)
	  * @return A new label
	  */
	def withSprite(actorHandler: ActorHandler, strip: Strip, spriteOrigin: Point, animationSpeed: Fps,
				   alignment: Alignment = Center) =
	{
		val animator = SpriteDrawer(strip.toTimedAnimation(animationSpeed).map { i => i -> spriteOrigin },
			Transformation.identity)
		new AnimationLabel(actorHandler, animator, StackSize.any(strip.imageSize), spriteOrigin, alignment,
			animationSpeed)
	}
	
	/**
	  * Creates a new label that rotates a static image
	  * @param image Image to be drawn
	  * @param origin Image origin (relative to image top-left corner)
	  * @param rotation Rotation animation
	  * @param alignment Alignment to use when positioning the image in the label (default = Center)
	  * @param context Implicit component creation context
	  * @return A new label
	  */
	def contextualWithRotatingImage(image: Image, origin: Point, rotation: TimedAnimation[Rotation],
									alignment: Alignment = Center,
									maxFps: Fps = ComponentCreationDefaults.maxAnimationRefreshRate)
								   (implicit context: BaseContextLike) =
		withRotatingImage(context.actorHandler, image, origin, rotation, alignment, maxFps)
	
	/**
	  * Creates a new label that draws a looping sprite / strip
	  * @param strip Image strip
	  * @param spriteOrigin Image origin to use
	  * @param animationSpeed Animation speed in frames per second
	  * @param alignment Alignment to use when positioning image in this label (default = Center)
	  * @param context Implicit component creation context
	  * @return A new label
	  */
	def contextualWithSprite(strip: Strip, spriteOrigin: Point, animationSpeed: Fps, alignment: Alignment = Center)
							(implicit context: BaseContextLike) =
		withSprite(context.actorHandler, strip, spriteOrigin, animationSpeed, alignment)
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
	extends Label with Stackable
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
	
	override def isAttachedToMainHierarchy_=(newAttachmentStatus: Boolean) =
	{
		if (_isAttached != newAttachmentStatus)
		{
			_isAttached = newAttachmentStatus
			// Animator is only attached to the actor handler when this component is displayable
			if (newAttachmentStatus)
			{
				actorHandler += animator
				actorHandler += Repainter
			}
			else
			{
				actorHandler -= animator
				actorHandler -= Repainter
			}
			fireStackHierarchyChangeEvent(newAttachmentStatus)
		}
	}
	
	
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
	
	object Repainter extends Actor
	{
		private val threshold = maxFps.interval
		private var lastDraw = Instant.now() - threshold
		
		override def act(duration: FiniteDuration) =
		{
			val time = Instant.now()
			if (time >= lastDraw + threshold)
				repaint()
			lastDraw = time
		}
		
		override def allowsHandlingFrom(handlerType: HandlerType) = visible
	}
}
