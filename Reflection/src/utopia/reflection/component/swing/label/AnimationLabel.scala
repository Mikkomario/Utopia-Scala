package utopia.reflection.component.swing.label

import utopia.firmament.component.stack.ConstrainableWrapper
import utopia.firmament.context.base.BaseContextPropsView
import utopia.firmament.model.stack.LengthExtensions._
import utopia.firmament.model.stack.StackSize
import utopia.firmament.model.stack.modifier.OverwriteSizeModifier
import utopia.flow.view.immutable.eventful.Fixed
import utopia.genesis.handling.action.ActorHandler
import utopia.genesis.handling.event.animation.{Animator, AnimatorInstruction}
import utopia.genesis.image.{Image, Strip}
import utopia.genesis.util.Fps
import utopia.paradigm.angular.DirectionalRotation
import utopia.paradigm.animation.TimedAnimation
import utopia.paradigm.shape.shape2d.Matrix2D
import utopia.reflection.component.swing.template.AwtComponentRelated
import utopia.reflection.component.template.layout.stack.ReflectionStackableWrapper

import java.awt.Component
import scala.math.Ordering.Double.TotalOrdering

object AnimationLabel
{
	/**
	  * Creates a new label that rotates a static image
	  * @param actorHandler Actor handler that will deliver action events
	  * @param image Image to be drawn
	  * @param rotation Rotation animation
	  * @return A new label
	  */
	def withRotatingImage(actorHandler: ActorHandler, image: Image, rotation: TimedAnimation[DirectionalRotation]) =
	{
		val instruction = AnimatorInstruction(rotation.map { r => (image, Some(Matrix2D.rotation(r))) }, loops = true)
		val animator = new Animator(Fixed(instruction))
		
		val label = new AnimationLabel(actorHandler, animator)
		
		// Uses a fixed stack size
		val maxRadius = image.bounds.corners.iterator.map { _.length }.max
		val stackSize = (maxRadius * 2).ceil.any.square
		label.addConstraint(OverwriteSizeModifier(stackSize))
		
		label
	}
	
	/**
	  * Creates a new label that draws a looping sprite / strip
	  * @param actorHandler Actor handler that will deliver action events
	  * @param strip Image strip
	  * @param animationSpeed Animation speed in frames per second
	  * @return A new label
	  */
	def withSprite(actorHandler: ActorHandler, strip: Strip, animationSpeed: Fps) = {
		val instruction = AnimatorInstruction(strip.toTimedAnimation(animationSpeed).map { _ -> None }, loops = true)
		val animator = new Animator(Fixed(instruction))
		val label = new AnimationLabel(actorHandler, animator)
		
		label.addConstraint(OverwriteSizeModifier(StackSize.any(strip.size)))
		label
	}
	
	/**
	  * Creates a new label that rotates a static image
	  * @param image Image to be drawn
	  * @param rotation Rotation animation
	  * @param context Implicit component creation context
	  * @return A new label
	  */
	def contextualWithRotatingImage(image: Image, rotation: TimedAnimation[DirectionalRotation])
								   (implicit context: BaseContextPropsView) =
		withRotatingImage(context.actorHandler, image, rotation)
	
	/**
	  * Creates a new label that draws a looping sprite / strip
	  * @param strip Image strip
	  * @param animationSpeed Animation speed in frames per second
	  * @param context Implicit component creation context
	  * @return A new label
	  */
	def contextualWithSprite(strip: Strip, animationSpeed: Fps)
							(implicit context: BaseContextPropsView) =
		withSprite(context.actorHandler, strip, animationSpeed)
}

/**
  * This label draws an animation on top of itself
  * @author Mikko Hilpinen
  * @since 15.6.2020, v1.2
  * @param actorHandler An actor handler that will deliver action events to progress the animation
  * @param animator Animator used for the actual drawing
  * @param allowUpscaling Whether the image should be allowed to scale above its size (default = false)
  * @param isLowPriority Whether this label's stack size should be low priority
  */
class AnimationLabel(actorHandler: ActorHandler, animator: Animator[(Image, Option[Matrix2D])],
                     allowUpscaling: Boolean = false, isLowPriority: Boolean = false)
	extends ReflectionStackableWrapper with ConstrainableWrapper with AwtComponentRelated
{
	// ATTRIBUTES	-------------------------
	
	private val label = new ImageLabel2(animator.value._1, allowUpscaling, isLowPriority)
	
	
	// INITIAL CODE -------------------------
	
	label.transformationPointer.value = animator.value._2
	animator.addListener { event =>
		label.image = event.newValue._1
		label.transformationPointer.value = event.newValue._2
	}
	
	addStackHierarchyChangeListener { attached =>
		if (attached)
			actorHandler += animator
		else
			actorHandler -= animator
	}
	
	
	// IMPLEMENTED	-------------------------
	
	override protected def wrapped = label
	override def component: Component = label.component
}
