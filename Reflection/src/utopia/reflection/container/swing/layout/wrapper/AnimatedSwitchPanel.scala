package utopia.reflection.container.swing.layout.wrapper

import utopia.flow.async.Volatile
import utopia.genesis.animation.transform.AnimatedTransform
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.image.Image
import utopia.genesis.shape.shape2D.Size
import utopia.genesis.util.Fps
import utopia.reflection.component.context.AnimationContextLike
import utopia.reflection.component.swing.label.EmptyLabel
import utopia.reflection.component.swing.template.{AwtComponentRelated, StackableAwtComponentWrapperWrapper, SwingComponentRelated}
import utopia.reflection.component.template.layout.stack.AnimatedTransitionLike
import utopia.reflection.container.swing.AwtContainerRelated
import utopia.reflection.container.swing.layout.multi.Stack.AwtStackable
import utopia.reflection.shape.StackSize
import utopia.reflection.util.{ComponentCreationDefaults, ComponentToImage}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

object AnimatedSwitchPanel
{
	/**
	  * Creates a new animated switch panel with contextual information
	  * @param initialContent Content displayed initially
	  * @param imageTransition Transition used with component images
	  * @param sizeTransition Transition used with component sizes
	  * @param exc Implicit execution context
	  * @param context Implicit animation context
	  * @tparam C Type of switched component
	  * @return A new animated switch panel
	  */
	def contextual[C <: AwtStackable](initialContent: C, imageTransition: AnimatedTransform[(Image, Image), Image],
									  sizeTransition: AnimatedTransform[(Size, Size), Size])
									 (implicit exc: ExecutionContext, context: AnimationContextLike) =
		new AnimatedSwitchPanel(initialContent, context.actorHandler, imageTransition, sizeTransition,
			context.animationDuration, context.maxAnimationRefreshRate)
}

/**
  * This component wrapper animates transitions between components
  * @author Mikko Hilpinen
  * @since 19.4.2020, v1.2
  */
class AnimatedSwitchPanel[C <: AwtStackable](initialContent: C, actorHandler: ActorHandler,
											 imageTransition: AnimatedTransform[(Image, Image), Image],
											 sizeTransition: AnimatedTransform[(Size, Size), Size],
											 duration: FiniteDuration = ComponentCreationDefaults.transitionDuration,
											 maxRefreshRate: Fps = ComponentCreationDefaults.maxAnimationRefreshRate)
											(implicit exc: ExecutionContext)
	extends StackableAwtComponentWrapperWrapper with AwtContainerRelated with SwingComponentRelated
{
	// ATTRIBUTES	---------------------
	
	private val panel = new SwitchPanel[AwtStackable]()
	
	private var currentContent = initialContent
	private var targetContent = initialContent
	private val currentTransition = Volatile(Future.successful(currentContent))
	
	
	// INITIAL CODE	---------------------
	
	panel.set(currentContent)
	
	
	// COMPUTED	-------------------------
	
	/**
	  * @return Current content within this panel
	  */
	def content = targetContent
	
	
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
		actorHandler += transition
		panel.set(transition)
		
		transition.start().map { _ =>
			currentContent = to
			to
		}
	}
	
	
	// NESTED	----------------------------------
	
	private class Transition(from: Image, to: Image) extends AnimatedTransitionLike with AwtComponentRelated
	{
		// ATTRIBUTES	----------------------------
		
		private val label = new EmptyLabel()
		
		
		// IMPLEMENTED	----------------------------
		
		override def component = label.component
		
		override protected def duration = AnimatedSwitchPanel.this.duration
		
		override protected val imageAnimation = imageTransition.toAnimation(from, to)
		
		override protected val sizeAnimation = sizeTransition.toAnimation(from.size, to.size).map { StackSize.any(_) }
		
		override protected def maxRefreshRate = AnimatedSwitchPanel.this.maxRefreshRate
		
		override protected def wrapped = label
		
		override def drawable = label
	}
}
