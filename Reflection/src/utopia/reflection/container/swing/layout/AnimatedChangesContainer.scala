package utopia.reflection.container.swing.layout

import utopia.flow.collection.VolatileList
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.TimeExtensions._
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.shape.Axis2D
import utopia.genesis.util.Fps
import utopia.reflection.component.context.AnimationContextLike
import utopia.reflection.component.swing.animation.AnimatedVisibility
import utopia.reflection.component.template.layout.stack.StackableWrapper
import utopia.reflection.container.stack.template.MultiStackContainer
import utopia.reflection.container.swing.layout.multi.Stack.AwtStackable
import utopia.reflection.container.template.{MultiContainer, WrappingContainer}
import utopia.reflection.event.Visibility.{Invisible, Visible}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

object AnimatedChangesContainer
{
	/**
	  * Creates a new animated changes container using contextual information
	  * @param container A container being wrapped
	  * @param transitionAxis Axis along which the items appear / disappear. None if transition should be applied on
	  *                       both axes (default)
	  * @param context Component creation context (implicit)
	  * @param exc Execution context (implicit)
	  * @tparam C Type of component in container
	  * @return A new container
	  */
	def contextual[C <: AwtStackable](container: MultiStackContainer[AnimatedVisibility[C]],
									  transitionAxis: Option[Axis2D] = None)
	                                 (implicit context: AnimationContextLike, exc: ExecutionContext) =
		new AnimatedChangesContainer[C, MultiStackContainer[AnimatedVisibility[C]]](container, context.actorHandler,
			transitionAxis, context.animationDuration, context.maxAnimationRefreshRate, context.useFadingInAnimations)
}

/**
  * This container is able to animated appearances and disappearances of its contents
  * @author Mikko Hilpinen
  * @since 20.4.2020, v1.2
  */
class AnimatedChangesContainer[C <: AwtStackable, Wrapped <: MultiStackContainer[AnimatedVisibility[C]]]
(protected val container: Wrapped, actorHandler: ActorHandler, transitionAxis: Option[Axis2D] = None,
 animationDuration: FiniteDuration = 0.25.seconds, maxRefreshRate: Fps = Fps(120), fadingIsEnabled: Boolean = true)
(implicit exc: ExecutionContext)
	extends WrappingContainer[C, AnimatedVisibility[C]] with StackableWrapper with MultiContainer[C]
{
	// ATTRIBUTES	-----------------------------
	
	private val wrappersList = VolatileList[(AnimatedVisibility[C], Boolean)]()
	
	
	// IMPLEMENTED	-----------------------------
	
	override protected def unwrap(wrapper: AnimatedVisibility[C]) = wrapper.display
	
	override protected def wrapped = container
	
	override protected def wrappers =
	{
		wrappersList.get.filterNot { _._2 }.map { _._1 }
		/*
		val notIncluded = waitingRemoval
		val wrappersInContainer = container.components
		if (notIncluded.isEmpty)
			wrappersInContainer.map { w => w -> w.display }
		else
			wrappersInContainer.filterNot(notIncluded.contains).map { w => w -> w.display }*/
	}
	
	override protected def removeWrapper(wrapper: AnimatedVisibility[C], index: Int) =
	{
		// Marks the wrapper as ready for removal
		wrappersList.update { old =>
			// Also starts the hiding process
			(wrapper.isShown = false).foreach { newState =>
				// If someone made the wrapper visible again, will not remove it
				if (newState.isNotVisible)
				{
					// Removes the wrapper from the container once animation has finished
					wrappersList.update { old =>
						container -= wrapper
						old.filterNot { case (w, removing) => removing && w == wrapper } }
				}
			}
			
			old.mapIndex(index) { case (w, _) => w -> true }
		}
	}
	
	override protected def add(component: C, index: Int) =
	{
		wrappersList.update { old =>
			// If the component was being removed from the container, cancels the removal
			// (may still need to reposition the wrapper)
			old.indexWhereOption { _._1.display == component } match
			{
				case Some(wrapperIndex) =>
					val (wrapper, wasRemoving) = old(wrapperIndex)
					if (wasRemoving)
						wrapper.isShown = true
					
					// Checks the "projected" index of the wrapper (index in system where removing components don't count)
					val removingCountBeforeWrapper = old.take(wrapperIndex).count { _._2 }
					val projectedWrapperIndex = wrapperIndex - removingCountBeforeWrapper
					
					// Case: Same index is kept -> may update removal status
					if (index == projectedWrapperIndex)
					{
						if (wasRemoving)
							old.updated(wrapperIndex, wrapper -> false)
						else
							old
					}
					// Case: Wrapper position was changed -> Needs to remove from and then add to container
					else
					{
						// Calculates the targeted index in the real system (where removing components are being counted)
						val newWrapperIndex = trueIndex(index, old)
						
						container -= wrapper
						container.insert(wrapper, newWrapperIndex)
						
						// Will have to take into account the wrapper's removal's effect on indexing
						val indexMod = if (wrapperIndex < newWrapperIndex) -1 else 0
						old.withoutIndex(wrapperIndex).inserted(wrapper -> false, newWrapperIndex + indexMod)
					}
				case None =>
					// Wraps the component in an animation
					val wrapper = new AnimatedVisibility[C](component, actorHandler, transitionAxis,
						duration = animationDuration, maxRefreshRate = maxRefreshRate, useFading = fadingIsEnabled)
					// Adds the wrapper to the container (needs to check indexing because there may still be wrappers waiting for removal)
					// Also, Starts the appearance animation
					val newWrapperIndex = trueIndex(index, old)
					container.insert(wrapper, newWrapperIndex)
					wrapper.isShown = true
					old.inserted(wrapper -> false, newWrapperIndex)
			}
		}
	}
	
	
	// OTHER	------------------------------
	
	/**
	  * Hides a component from view. Call show(...) to display it again
	  * @param component Component to hide from view.
	  * @param isAnimated Whether the hiding transition should be animated (default = true)
	  */
	def hide(component: C, isAnimated: Boolean = true) = container.components.find { _.display == component }
		.foreach { wrap => if (isAnimated) wrap.isShown = false else wrap.setStateWithoutTransition(Invisible) }
	
	/**
	  * Shows a previously hidden component. If the component is doesn't reside in this container, adds it
	  * @param component Component to show again
	  * @param isAnimated Whether transition should be animated (default = true)
	  */
	def show(component: C, isAnimated: Boolean = true): Unit = container.components.find { _.display == component } match
	{
		case Some(wrap) => if (isAnimated) wrap.isShown = true else wrap.setStateWithoutTransition(Visible)
		case None =>
			if (isAnimated)
				this += component
			else
				container += new AnimatedVisibility[C](component, actorHandler, transitionAxis, Visible, animationDuration,
					maxRefreshRate, fadingIsEnabled) // FIXME: Remove this once content tracking changes
	}
	
	private def trueIndex(projectedIndex: Int, data: Vector[(_, Boolean)]) =
	{
		val removeCountBeforeIndex = data.take(projectedIndex + 1).count { _._2 }
		// If there are removing items directly after the specified index, will move the index to the right
		val removeCountAfterIndex = data.drop(projectedIndex + removeCountBeforeIndex).takeWhile { _._2 }.size
		projectedIndex + removeCountBeforeIndex + removeCountAfterIndex
	}
}
