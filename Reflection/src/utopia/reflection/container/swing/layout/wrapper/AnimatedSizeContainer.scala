package utopia.reflection.container.swing.layout.wrapper

import utopia.firmament.context.{AnimationContext, ComponentCreationDefaults}
import utopia.firmament.model.stack.StackSize
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.async.VolatileFlag
import utopia.flow.view.template.eventful.Flag
import utopia.genesis.handling.action.{Actor, ActorHandler}
import utopia.genesis.util.Fps
import utopia.paradigm.path.ProjectilePath
import utopia.reflection.component.swing.template.AwtComponentWrapperWrapper
import utopia.reflection.container.stack.template.SingleStackContainer
import utopia.reflection.container.swing.layout.multi.Stack.AwtStackable
import utopia.reflection.container.swing.{AwtContainerRelated, Panel}

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
		new AnimatedSizeContainer[C](actorHandler, component, transitionDuration, maxRefreshRate)
	
	/**
	  * Creates a new animated size container with contextual information
	  * @param component A component being wrapped in this container
	  * @param context Component creation context
	  * @tparam C Type of component being wrapped
	  * @return A new animated changes container
	  */
	def contextual[C <: AwtStackable](component: C)(implicit context: AnimationContext) =
		apply(component, context.actorHandler, context.animationDuration, context.maxAnimationRefreshRate)
}

/**
  * A container which wraps a single component and provides animations for it. <b>Remember to add this container
  * to a working ActorHandler</b>
  * @author Mikko Hilpinen
  * @since 18.4.2020, v1.2
  */
class AnimatedSizeContainer[C <: AwtStackable](actorHandler: ActorHandler, initialContent: C,
                                               transitionDuration: FiniteDuration = ComponentCreationDefaults.transitionDuration,
                                               maxRefreshRate: Fps = ComponentCreationDefaults.maxAnimationRefreshRate)
	extends SingleStackContainer[C] with AwtComponentWrapperWrapper with AwtContainerRelated
{
	// ATTRIBUTES	--------------------
	
	private var _content = initialContent
	
	private val panel = new Panel[C]
	private val curve = ProjectilePath()
	
	private var startSize = StackSize.any
	private var targetSize = StackSize.any
	private var timePassed = Duration.Zero
	private var nextRevalidationThreshold = Duration.Zero
	
	private val transitioningFlag = new VolatileFlag()
	
	
	// INITIAL CODE	--------------------
	
	panel += initialContent
	addResizeListener { _ => updateLayout() }
	
	
	// COMPUTED	------------------------
	
	private def transitioning = transitioningFlag.value
	
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
	
	override def children = components
	
	override protected def wrapped = panel
	
	override def updateLayout() =
	{
		// Content is set to fill this container
		content.size = size
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
		else if (components.isEmpty)
			StackSize.any
		else
			targetSize
	}
	
	override def resetCachedSize() =
	{
		// When stack size is reset while a transition is NOT in process, may start one
		if (!transitioning) {
			// Only starts animation if content stack size really changed
			newTarget(content.stackSize)
		}
		// These events are ignored while transition is in progress, because this method is being called repeatedly
	}
	
	override val stackId = hashCode()
	
	override protected def _set(content: C): Unit = {
		panel -= _content
		_content = content
		// Transitions only when already connected to the stack hierarchy
		if (isAttachedToMainHierarchy) {
			targetSize = StackSize.any(size)
			newTarget(content.stackSize)
		}
		else
			targetSize = content.stackSize
		panel.insert(content, 0)
	}
	
	override protected def content: C = _content
	
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
	
	private object SizeUpdater extends Actor
	{
		override def handleCondition: Flag = AlwaysTrue
		
		override def act(duration: FiniteDuration) = {
			// Advances, and may conclude process
			timePassed += duration
			if (timePassed >= transitionDuration) {
				transitioning = false
				revalidate()
			}
			// Has a limit on revalidation calls during transition.
			else if (timePassed >= nextRevalidationThreshold) {
				nextRevalidationThreshold = timePassed + maxRefreshRate.interval
				revalidate()
			}
		}
		
		// override def allowsHandlingFrom(handlerType: HandlerType) = isTransitioning
	}
}
