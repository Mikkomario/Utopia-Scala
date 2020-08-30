package utopia.reflection.container.swing.layout.wrapper

import utopia.flow.async.VolatileFlag
import utopia.genesis.handling.Actor
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.shape.path.ProjectilePath
import utopia.genesis.util.Fps
import utopia.inception.handling.immutable.Handleable
import utopia.reflection.component.context.AnimationContextLike
import utopia.reflection.component.swing.template.AwtComponentWrapperWrapper
import utopia.reflection.container.stack.template.SingleStackContainer
import utopia.reflection.container.swing.layout.multi.Stack.AwtStackable
import utopia.reflection.container.swing.{AwtContainerRelated, Panel}
import utopia.reflection.shape.StackSize
import utopia.reflection.util.ComponentCreationDefaults

import scala.concurrent.duration.{Duration, FiniteDuration}

object AnimatedSizeContainer
{
	/**
	  * Creates a new animated size container that is ready to be used
	  * @param component Component placed inside the container
	  * @param actorHandler Actor handler that will deliver action events for this container
	  * @param transitionDuration Duration it takes to transition sizes (default = 0.25 seconds)
	  * @param maxRefreshRate Maximum amount of revalidation calls per second (defaults to global default)
	  * @tparam C Type of container content
	  * @return Newly created container
	  */
	def apply[C <: AwtStackable](component: C, actorHandler: ActorHandler,
								 transitionDuration: FiniteDuration = ComponentCreationDefaults.transitionDuration,
								 maxRefreshRate: Fps = ComponentCreationDefaults.maxAnimationRefreshRate) =
	{
		val container = new AnimatedSizeContainer[C](actorHandler, transitionDuration, maxRefreshRate)
		container.set(component)
		container
	}
	
	/**
	  * Creates a new animated size container with contextual information
	  * @param component A component being wrapped in this container
	  * @param context Component creation context
	  * @tparam C Type of component being wrapped
	  * @return A new animated changes container
	  */
	def contextual[C <: AwtStackable](component: C)(implicit context: AnimationContextLike) =
		apply(component, context.actorHandler, context.animationDuration, context.maxAnimationRefreshRate)
}

/**
  * A container which wraps a single component and provides animations for it. <b>Remember to add this container
  * to a working ActorHandler</b>
  * @author Mikko Hilpinen
  * @since 18.4.2020, v1.2
  */
class AnimatedSizeContainer[C <: AwtStackable](actorHandler: ActorHandler,
											   transitionDuration: FiniteDuration = ComponentCreationDefaults.transitionDuration,
											   maxRefreshRate: Fps = ComponentCreationDefaults.maxAnimationRefreshRate)
	extends SingleStackContainer[C] with AwtComponentWrapperWrapper with AwtContainerRelated
{
	// ATTRIBUTES	--------------------
	
	private val panel = new Panel[C]
	private val curve = ProjectilePath()
	
	private var startSize = StackSize.any
	private var targetSize = StackSize.any
	private var timePassed = Duration.Zero
	private var nextRevalidationThreshold = Duration.Zero
	
	private val transitioningFlag = new VolatileFlag()
	
	
	// INITIAL CODE	--------------------
	
	addResizeListener { _ => updateLayout() }
	
	
	// COMPUTED	------------------------
	
	private def transitioning = transitioningFlag.get
	
	private def transitioning_=(newState: Boolean) = transitioningFlag.update { oldState =>
		// May register or unregister the actor component from the actor handler
		if (oldState != newState)
		{
			if (newState)
				actorHandler += SizeUpdater
			else
				actorHandler -= SizeUpdater
		}
		newState
	}
	
	
	// IMPLEMENTED	--------------------
	
	override def component = panel.component
	
	override def children = super[SingleStackContainer].children
	
	override protected def wrapped = panel
	
	override def updateLayout() =
	{
		// Content is set to fill this container
		content.foreach { c => c.size = size }
		// panel.repaint()
	}
	
	override def stackSize =
	{
		// When transitioning, provides a stack size that is getting closer to the target
		if (transitioning)
		{
			val progress = curve(timePassed / transitionDuration)
			targetSize * progress + startSize * (1 - progress)
		}
		else if (isEmpty)
			StackSize.any
		else
			targetSize
	}
	
	override def resetCachedSize() =
	{
		// When stack size is reset while a transition is NOT in process, may start one
		if (!transitioning)
		{
			// Only starts animation if content stack size really changed
			content.foreach { c => newTarget(c.stackSize) }
		}
		// These events are ignored while transition is in progress, because this method is being called repeatedly
	}
	
	override val stackId = hashCode()
	
	override protected def add(component: C, index: Int) =
	{
		// Transitions only when already connected to the stack hierarchy
		if (isAttachedToMainHierarchy)
		{
			targetSize = StackSize.any(size)
			newTarget(component.stackSize)
		}
		else
			targetSize = component.stackSize
		panel.insert(component, index)
	}
	
	override protected def remove(component: C) =
	{
		panel -= component
		transitioning = false
		targetSize = StackSize.any
	}
	
	override def components = panel.components
	
	
	// OTHER	-------------------------
	
	private def newTarget(newTargetSize: StackSize) =
	{
		if (newTargetSize != targetSize)
		{
			// The starting size must still stay within new size bounds
			startSize = targetSize.map { (axis, length) =>
				val newTargetLength = newTargetSize.along(axis)
				length.within(newTargetLength.min, newTargetLength.max)
			}
			timePassed = Duration.Zero
			targetSize = newTargetSize
			transitioning = true
		}
	}
	
	
	// NESTED	-------------------------
	
	private object SizeUpdater extends Actor with Handleable
	{
		override def act(duration: FiniteDuration) =
		{
			// Advances, and may conclude process
			timePassed += duration
			if (timePassed >= transitionDuration)
			{
				transitioning = false
				revalidate()
			}
			// Has a limit on revalidation calls during transition.
			else if (timePassed >= nextRevalidationThreshold)
			{
				nextRevalidationThreshold = timePassed + maxRefreshRate.interval
				revalidate()
			}
		}
		
		// override def allowsHandlingFrom(handlerType: HandlerType) = isTransitioning
	}
}
