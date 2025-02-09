package utopia.reach.component.label.image

import utopia.firmament.component.stack.ConstrainableWrapper
import utopia.firmament.context.base.BaseContextPropsView
import utopia.firmament.model.stack.StackLength
import utopia.firmament.model.stack.modifier.{MinOptimalSizeModifier, NoShrinkingLengthModifier}
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.template.eventful.Flag
import utopia.genesis.handling.action.{Actor, ActorHandler}
import utopia.genesis.image.Image
import utopia.paradigm.angular.DirectionalRotation
import utopia.paradigm.animation.TimedAnimation
import utopia.paradigm.shape.shape2d.Matrix2D
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.factory.FromContextComponentFactoryFactory.Ccff
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.ReachComponentWrapper

import scala.concurrent.duration.{Duration, FiniteDuration}

case class AnimatedImageLabelFactory(hierarchy: ComponentHierarchy, context: BaseContextPropsView,
                                     settings: ViewImageLabelSettings = ViewImageLabelSettings.default,
                                     loops: Boolean = true)
	extends ViewImageLabelSettingsWrapper[AnimatedImageLabelFactory]
{
	// COMPUTED ------------------------
	
	/**
	  * @return A copy of this factory where the label only plays the animation once
	  */
	def once = copy(loops = false)
	
	
	// IMPLEMENTED  --------------------
	
	override def self: AnimatedImageLabelFactory = this
	
	override def withSettings(settings: ViewImageLabelSettings): AnimatedImageLabelFactory = copy(settings = settings)
	override def *(mod: Double): AnimatedImageLabelFactory = mapSettings { _ * mod }
	
	
	// OTHER    ------------------------
	
	/**
	  * Creates a new animated label
	  * @param animation Animation to play
	  * @param transformAnimation Animated transformation to apply (optional)
	  * @return A new animated label
	  */
	def apply(animation: TimedAnimation[Image], transformAnimation: Option[TimedAnimation[Matrix2D]] = None) =
		new AnimatedImageLabel(hierarchy, context.actorHandler, animation, transformAnimation, settings, loops)
	/**
	  * Creates a new animated label, based on an animated transformation
	  * @param image Image to draw
	  * @param transform Animated transformation to apply
	  * @return A new animated label
	  */
	def transforming(image: Image, transform: TimedAnimation[Matrix2D]) =
		apply(TimedAnimation.fixed(image, transform.duration), Some(transform))
	
	/**
	  * Creates a new animated label displaying a rotating image.
	  * @param image Image to draw
	  * @param rotation Animated rotation to apply
	  * @param centerOrigin Whether image origin should be set to the center.
	  *                     Default = false = keep image origin where it is.
	  * @return A new animated label
	  */
	def rotating(image: Image, rotation: TimedAnimation[DirectionalRotation], centerOrigin: Boolean = false) =
	{
		val appliedImage = if (centerOrigin) image.withCenterOrigin else image
		val label = transforming(appliedImage, rotation.map(Matrix2D.rotation))
		// Makes it so that the label won't change in size due to the rotation
		val imageRadius = {
			if (centerOrigin)
				appliedImage.origin.length
			else
				appliedImage.bounds.corners.iterator.map { _.length }.max
		}
		label.addConstraint(MinOptimalSizeModifier(Size.square(imageRadius * 2)))
		// label.addConstraint(new NoShrinkingLengthModifier(StackLength.any(imageRadius * 2)).symmetric)
		label
	}
}

object AnimatedImageLabel extends Ccff[BaseContextPropsView, AnimatedImageLabelFactory]
{
	override def withContext(hierarchy: ComponentHierarchy, context: BaseContextPropsView): AnimatedImageLabelFactory =
		AnimatedImageLabelFactory(hierarchy, context)
}
/**
  * A label that displays an image + transformation -based animation
  * @author Mikko Hilpinen
  * @since 31.01.2025, v1.5.1
  */
class AnimatedImageLabel(hierarchy: ComponentHierarchy, actorHandler: ActorHandler, animation: TimedAnimation[Image],
                         transformAnimation: Option[TimedAnimation[Matrix2D]],
                         settings: ViewImageLabelSettings, looping: Boolean)
	extends ReachComponentWrapper with ConstrainableWrapper
{
	// ATTRIBUTES   -------------------------
	
	// Combines the transformation from the settings with the possible animated transformation
	private lazy val appliedSettings = Animator.transformP match {
		case Some(transformP) =>
			settings.mapTransformationPointer { customP =>
				customP.mergeWith(transformP) { (custom, animated) =>
					Some(custom match {
						case Some(custom) => custom(animated)
						case None => animated
					})
				}
			}
		case None => settings
	}
	
	override protected lazy val wrapped = ViewImageLabel(hierarchy).withSettings(appliedSettings).apply(Animator.imageP)
	
	
	// INITIAL CODE -------------------------
	
	actorHandler += Animator
	
	
	// NESTED   -----------------------------
	
	private object Animator extends Actor
	{
		// ATTRIBUTES   ---------------------
		
		private lazy val maxDuration = transformAnimation match {
			case Some(transformAnimation) => animation.duration max transformAnimation.duration
			case None => animation.duration
		}
		
		// If this is a one-time animation, supports pointer-locking
		private val lockableAdvanceP = if (looping) None else Some(Pointer.lockable[Duration](Duration.Zero))
		private val advanceP = lockableAdvanceP.getOrElse { Pointer.eventful[Duration](Duration.Zero) }
		lazy val imageP = {
			if (transformAnimation.exists { _.duration > animation.duration })
				advanceP.map(animation.repeating)
			else
				advanceP.map(animation.apply)
		}
		lazy val transformP = transformAnimation.map { transformAnimation =>
			if (transformAnimation.duration < animation.duration)
				advanceP.map(transformAnimation.repeating)
			else
				advanceP.map(transformAnimation.apply)
		}
		
		override lazy val handleCondition: Flag = {
			if (looping)
				hierarchy.linkedFlag
			else
				hierarchy.linkedFlag && advanceP.map { _ < maxDuration }
		}
		
		
		// IMPLEMENTED  ---------------------
		
		override def act(duration: FiniteDuration): Unit = {
			// Advances the animation, if possible
			val isFinished = advanceP.mutate { advance =>
				val newAdvance = advance + duration
				// Checks for looping / finishing
				if (newAdvance >= maxDuration) {
					if (looping)
						false -> (newAdvance - maxDuration)
					else
						true -> maxDuration
				}
				else
					false -> newAdvance
			}
			// Once/if finished, locks the advance pointer
			if (isFinished)
				lockableAdvanceP.foreach { _.lock() }
		}
	}
}
