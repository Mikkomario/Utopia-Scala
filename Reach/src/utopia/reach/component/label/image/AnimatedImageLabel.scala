package utopia.reach.component.label.image

import utopia.flow.view.mutable.Pointer
import utopia.flow.view.template.eventful.Flag
import utopia.genesis.handling.action.Actor
import utopia.genesis.image.Image
import utopia.paradigm.animation.TimedAnimation
import utopia.paradigm.shape.shape2d.Matrix2D
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{ReachComponentLike, ReachComponentWrapper}

import scala.concurrent.duration.{Duration, FiniteDuration}

/**
  * A label that displays an image + transformation -based animation
  * @author Mikko Hilpinen
  * @since 31.01.2025, v1.5.1
  */
class AnimatedImageLabel(hierarchy: ComponentHierarchy, animation: TimedAnimation[Image],
                         transformAnimation: Option[TimedAnimation[Matrix2D]],
                         settings: ViewImageLabelSettings, looping: Boolean)
	extends ReachComponentWrapper
{
	// ATTRIBUTES   -------------------------
	
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
	
	override protected lazy val wrapped: ReachComponentLike = ViewImageLabel(hierarchy)
		.withSettings(appliedSettings).apply(Animator.imageP)
	
	
	// NESTED   -----------------------------
	
	private object Animator extends Actor
	{
		// ATTRIBUTES   ---------------------
		
		private lazy val maxDuration = transformAnimation match {
			case Some(transformAnimation) => animation.duration max transformAnimation.duration
			case None => animation.duration
		}
		
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
				linkedFlag
			else
				linkedFlag && advanceP.map { _ < maxDuration }
		}
		
		
		// IMPLEMENTED  ---------------------
		
		override def act(duration: FiniteDuration): Unit = {
			val isFinished = advanceP.mutate { advance =>
				val newAdvance = advance + duration
				if (newAdvance >= maxDuration) {
					if (looping)
						false -> (newAdvance - maxDuration)
					else
						true -> maxDuration
				}
				else
					false -> newAdvance
			}
			if (isFinished)
				lockableAdvanceP.foreach { _.lock() }
		}
	}
}
