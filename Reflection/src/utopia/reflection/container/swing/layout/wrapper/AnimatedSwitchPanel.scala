package utopia.reflection.container.swing.layout.wrapper

import utopia.firmament.context.{AnimationContext, ComponentCreationDefaults}
import utopia.firmament.model.stack.StackSize
import utopia.flow.view.mutable.async.Volatile
import utopia.genesis.handling.action.ActorHandler
import utopia.genesis.image.Image
import utopia.genesis.util.Fps
import utopia.paradigm.animation.Animation
import utopia.paradigm.path.SPath
import utopia.reflection.component.swing.label.EmptyLabel
import utopia.reflection.component.swing.template.{AwtComponentRelated, StackableAwtComponentWrapperWrapper, SwingComponentRelated}
import utopia.reflection.component.template.layout.stack.AnimatedTransitionLike
import utopia.reflection.container.swing.AwtContainerRelated
import utopia.reflection.container.swing.layout.multi.Stack.AwtStackable
import utopia.reflection.util.ComponentToImage

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

object AnimatedSwitchPanel
{
	// ATTRIBUTES	---------------------------
	
	/**
	  * Creates a new animated switch panel with contextual information
	  * @param initialContent Content displayed initially
	  * @param exc Implicit execution context
	  * @param context Implicit animation context
	  * @tparam C Type of switched component
	  * @return A new animated switch panel
	  */
	def contextual[C <: AwtStackable](initialContent: C)(implicit exc: ExecutionContext, context: AnimationContext) =
		new AnimatedSwitchPanel[C](initialContent, context.actorHandler,
			context.animationDuration, context.maxAnimationRefreshRate)
}

/**
  * This component wrapper animates transitions between components
  * @author Mikko Hilpinen
  * @since 19.4.2020, v1.2
  */
class AnimatedSwitchPanel[C <: AwtStackable](initialContent: C, actorHandler: ActorHandler,
                                             duration: FiniteDuration = ComponentCreationDefaults.transitionDuration,
                                             maxRefreshRate: Fps = ComponentCreationDefaults.maxAnimationRefreshRate)
											(implicit exc: ExecutionContext)
	extends StackableAwtComponentWrapperWrapper with AwtContainerRelated with SwingComponentRelated
{
	// ATTRIBUTES	---------------------
	
	private var currentContent = initialContent
	private var targetContent = initialContent
	
	private val panel = new SwitchPanel[AwtStackable](currentContent)
	private val currentTransition = Volatile(Future.successful(currentContent))
	
	
	// COMPUTED	-------------------------
	
	/**
	  * @return Current content within this panel
	  */
	def content = targetContent
	
	private def curve = SPath.verySmooth
	
	
	// IMPLEMENTED	---------------------
	
	override protected def wrapped = panel
	
	override def component = panel.component
	
	
	// OTHER	-------------------------
	
	/**
	  * Updates the content in this panel
	  * @param newContent New content for this panel
	  */
	def set(newContent: C) =
	{
		// Extends the ongoing transition or starts a new one
		targetContent = newContent
		currentTransition.update { lastTransition =>
			// Case: There is no ongoing transition -> starts a new one
			if (lastTransition.isCompleted)
			{
				if (currentContent == newContent)
					lastTransition
				else
					startTransition(currentContent, newContent)
			}
			// Case: There's still an ongoing transition -> queues another transition right after the previous
			else
				lastTransition.flatMap { midContent =>
					if (midContent == newContent)
						Future.successful(midContent)
					else
						startTransition(midContent, newContent)
				}
		}
	}
	
	private def startTransition(from: C, to: C) =
	{
		val transition = new Transition(ComponentToImage(from, size),
			ComponentToImage(to, to.stackSize.optimal))
		panel.set(transition)
		
		transition.start(actorHandler).map { _ =>
			currentContent = to
			panel.set(to)
			to
		}
	}
	
	
	// NESTED	----------------------------------
	
	private class Transition(from: Image, to: Image) extends AnimatedTransitionLike with AwtComponentRelated
	{
		// ATTRIBUTES	----------------------------
		
		private val label = new EmptyLabel()
		
		override protected val imageAnimation = Animation { progress =>
			Vector(from.withAlpha(1 - progress), to.withAlpha(progress)) }.curved(curve)
		
		override protected val sizeAnimation = Animation { progress =>
			StackSize.any(from.size * (1 - progress) + to.size * progress)
		}
		
		
		// INITIAL CODE	---------------------------
		
		enableDrawing()
		
		
		// IMPLEMENTED	----------------------------
		
		override def component = label.component
		
		override protected def duration = AnimatedSwitchPanel.this.duration
		
		override protected def maxRefreshRate = AnimatedSwitchPanel.this.maxRefreshRate
		
		override protected def wrapped = label
		
		override def drawable = label
	}
}
