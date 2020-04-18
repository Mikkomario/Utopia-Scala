package utopia.reflection.container.swing

import utopia.flow.util.TimeExtensions._
import utopia.genesis.handling.Actor
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.shape.path.ProjectilePath
import utopia.inception.handling.HandlerType
import utopia.reflection.component.swing.AwtComponentWrapperWrapper
import utopia.reflection.container.stack.SingleStackContainer
import utopia.reflection.container.swing.Stack.AwtStackable
import utopia.reflection.shape.StackSize

import scala.concurrent.duration.{Duration, FiniteDuration}

object AnimatedSizeContainer
{
	/**
	  * Creates a new animated size container that is ready to be used
	  * @param component Component placed inside the container
	  * @param actorHandler Actor handler that will deliver action events for this container
	  * @param transitionDuration Duration it takes to transition sizes (default = 0.25 seconds)
	  * @tparam C Type of container content
	  * @return Newly created container
	  */
	def apply[C <: AwtStackable](component: C, actorHandler: ActorHandler, transitionDuration: FiniteDuration = 0.25.seconds) =
	{
		val container = new AnimatedSizeContainer[C](transitionDuration)
		container.set(component)
		actorHandler += container
		container
	}
}

/**
  * A container which wraps a single component and provides animations for it. <b>Remember to add this container
  * to a working ActorHandler</b>
  * @author Mikko Hilpinen
  * @since 18.4.2020, v1.2
  */
class AnimatedSizeContainer[C <: AwtStackable](transitionDuration: FiniteDuration = 0.25.seconds) extends SingleStackContainer[C]
	with AwtComponentWrapperWrapper with Actor with AwtContainerRelated
{
	// ATTRIBUTES	--------------------
	
	private val panel = new Panel[C]
	private val curve = ProjectilePath()
	
	private var startSize = StackSize.any
	private var targetSize = StackSize.any
	private var timePassed = Duration.Zero
	private var isTransitioning = false
	
	
	// INITIAL CODE	--------------------
	
	addResizeListener { _ => updateLayout() }
	
	
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
		if (isTransitioning)
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
		if (!isTransitioning)
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
		isTransitioning = false
		targetSize = StackSize.any
	}
	
	override def components = panel.components
	
	override def act(duration: FiniteDuration) =
	{
		// Advances, and may conclude process
		timePassed += duration
		if (timePassed >= transitionDuration)
			isTransitioning = false
		revalidate()
	}
	
	override def allowsHandlingFrom(handlerType: HandlerType) = isTransitioning
	
	
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
			isTransitioning = true
			targetSize = newTargetSize
		}
	}
}
