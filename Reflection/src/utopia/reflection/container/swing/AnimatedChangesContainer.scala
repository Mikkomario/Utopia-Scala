package utopia.reflection.container.swing

import utopia.flow.util.TimeExtensions._
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.shape.Axis2D
import utopia.reflection.component.stack.StackableWrapper
import utopia.reflection.component.swing.AnimatedVisibility
import utopia.reflection.container.{MappingContainer, MultiContainer}
import utopia.reflection.container.stack.MultiStackContainer
import utopia.reflection.container.swing.Stack.AwtStackable

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

object AnimatedChangesContainer
{
	// OTHER	---------------------------
	
	/**
	  * Wraps a container and gives it animations
	  * @param container Container to wrap (must support AnimatedVisibility)
	  * @param actorHandler ActorHandler that will deliver action events for animations
	  * @param transitionAxis Axis along which the appearance & disappearance animations are made
	  * @param animationDuration Duration of a single appearance / disappearance animation (default = 0.25 seconds)
	  * @param fadingIsEnabled Whether components should alter their transparency as they appear or disappear (default = true)
	  * @param exc Implicit execution context
	  * @tparam C Type of wrapped component
	  * @return A new container that uses animations for component appearance & disappearance
	  */
	def wrap[C <: AwtStackable](container: MultiStackContainer[AnimatedVisibility[C]], actorHandler: ActorHandler,
								transitionAxis: Axis2D, animationDuration: FiniteDuration = 0.25.seconds,
								fadingIsEnabled: Boolean = true)
							   (implicit exc: ExecutionContext): AnimatedChangesContainer[C, MultiStackContainer[AnimatedVisibility[C]]] =
		new SimpleContainer(container, actorHandler, transitionAxis, animationDuration, fadingIsEnabled)
	
	
	// NESTED	---------------------------
	
	private class SimpleContainer[C <: AwtStackable, Wrapped <: MultiStackContainer[AnimatedVisibility[C]]]
	(override val container: Wrapped, override val actorHandler: ActorHandler, override val transitionAxis: Axis2D,
	 override val animationDuration: FiniteDuration = 0.25.seconds, override val fadingIsEnabled: Boolean = true)
	(implicit val executionContext: ExecutionContext) extends AnimatedChangesContainer[C, Wrapped]
	{
		// ATTRIBUTES	-------------------
		
		override protected var waitingRemoval = Vector[AnimatedVisibility[C]]()
	}
}

/**
  * This container is able to animated appearances and disappearances of its contents
  * @author Mikko Hilpinen
  * @since 20.4.2020, v1.2
  */
trait AnimatedChangesContainer[C <: AwtStackable, Wrapped <: MultiStackContainer[AnimatedVisibility[C]]]
	extends MappingContainer[C, AnimatedVisibility[C], Wrapped] with StackableWrapper with MultiContainer[C]
{
	// ABSTRACT	---------------------------------
	
	/**
	  * @return Wrappers currently waiting to be removed from the managed container
	  */
	protected def waitingRemoval: Vector[AnimatedVisibility[C]]
	/**
	  * Updates the waiting wrappers list
	  * @param newWaiting New set of wrappers waiting for removal
	  */
	protected def waitingRemoval_=(newWaiting: Vector[AnimatedVisibility[C]]): Unit
	
	/**
	  * @return Execution context (implicit)
	  */
	protected implicit def executionContext: ExecutionContext
	
	/**
	  * @return ActorHandler that delivers action events for the animations
	  */
	protected def actorHandler: ActorHandler
	
	/**
	  * @return Axis along which the appearance & disappearance animations are made
	  */
	protected def transitionAxis: Axis2D
	
	/**
	  * @return Duration of a single appearance / disappearance animation
	  */
	protected def animationDuration: FiniteDuration
	
	/**
	  * @return Whether components should alter their transparency as they appear or disappear
	  */
	protected def fadingIsEnabled: Boolean
	
	
	// IMPLEMENTED	-----------------------------
	
	override protected def wrappers =
	{
		val notIncluded = waitingRemoval
		val wrappersInContainer = container.components
		if (notIncluded.isEmpty)
			wrappersInContainer.map { w => w -> w.display }
		else
			wrappersInContainer.filterNot(notIncluded.contains).map { w => w -> w.display }
	}
	
	override protected def removeWrapper(wrapper: AnimatedVisibility[C], component: C) =
	{
		waitingRemoval :+= wrapper
		(wrapper.isShown = false).foreach { _ =>
			// Removes the wrapper from the container once animation has finished. Until that, keeps the
			// wrapper in a separate vector
			container -= wrapper
			waitingRemoval = waitingRemoval.filterNot { _ == wrapper }
		}
	}
	
	override protected def add(component: C, index: Int) =
	{
		// Wraps the component in an animation
		val wrapper = new AnimatedVisibility[C](component, actorHandler, transitionAxis, animationDuration, fadingIsEnabled)
		// Adds the wrapper to the container and starts the animation
		container.insert(wrapper, index)
		wrapper.isShown = true
	}
	
	
	// OTHER	------------------------------
	
	/**
	  * Hides a component from view. Call show(...) to display it again
	  * @param component Component to hide from view.
	  * @param isAnimated Whether the hiding transition should be animated (default = true)
	  */
	def hide(component: C, isAnimated: Boolean = true) = container.components.find { _.display == component }
		.foreach { wrap => if (isAnimated) wrap.isShown = false else wrap.setStateWithoutTransition(false) }
	
	/**
	  * Shows a previously hidden component. If the component is doesn't reside in this container, adds it
	  * @param component Component to show again
	  * @param isAnimated Whether transition should be animated (default = true)
	  */
	def show(component: C, isAnimated: Boolean = true): Unit = container.components.find { _.display == component } match
	{
		case Some(wrap) => if (isAnimated) wrap.isShown = true else wrap.setStateWithoutTransition(true)
		case None =>
			if (isAnimated)
				this += component
			else
				container += new AnimatedVisibility[C](component, actorHandler, transitionAxis, animationDuration,
					fadingIsEnabled, isShownInitially = true)
	}
}
