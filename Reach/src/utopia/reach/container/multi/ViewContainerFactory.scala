package utopia.reach.container.multi

import utopia.flow.collection.immutable.caching.cache.Cache
import utopia.flow.view.mutable.eventful.{EventfulPointer, LockablePointer}
import utopia.flow.view.template.eventful.Changing
import utopia.reach.component.wrapper.ComponentWrapResult.SwitchableComponentsWrapResult
import utopia.reach.component.wrapper.OpenComponent.{SeparateOpenComponents, SwitchableOpenComponents}
import utopia.reach.component.wrapper.{ComponentCreationResult, OpenComponent}
import utopia.reach.container.ContainerFactory

/**
  * Common trait for initialized container factories that create containers that switch some of their content on or off
  * @author Mikko Hilpinen
  * @since 5.5.2023, v1.1
  */
trait ViewContainerFactory[+Container, -Top]
	extends ContainerFactory[Container, Top, SwitchableOpenComponents, SwitchableComponentsWrapResult]
{
	/**
	  * Creates a new container that changes its contents dynamically, based on a content pointer
	  * @param content A pointer that contains the currently displayed content.
	  * @return A new container
	  */
	def pointer(content: Changing[SeparateOpenComponents[Top, _]]): Container
	
	/**
	  * Builds a new container which reflects the contents of a multi-value pointer.
	  * This is best used in combination with view-based components.
	  * @param pointer Pointer whose value is reflected in the container contents
	  * @param construct A function that accepts a pointer that contains the value to display on that component
	  *                  and yields a new open component based on that pointer
	  * @tparam A Type of values displayed on individual components
	  * @return A new container
	  */
	protected def _mapPointer[A](pointer: Changing[Seq[A]])(construct: Changing[A] => OpenComponent[Top, _]): Container =
	{
		// Creates one component for each slot in the display values, adding more components as needed
		val componentCache = Cache.clearable { index: Int =>
			val initialValue = pointer.value(index)
			// If the source pointer supports sealing (i.e. may stop changing), utilizes lockable value pointers
			// in order to forward that information
			val (valueP, lockableP) = {
				import utopia.firmament.context.ComponentCreationDefaults.componentLogger
				if (pointer.destiny.isPossibleToSeal) {
					val p = LockablePointer(initialValue)
					p -> Some(p)
				}
				else
					EventfulPointer(initialValue) -> None
			}
			// Creates the new (open) component and remembers its pointer
			construct(valueP.readOnly).withResult(valueP -> lockableP)
		}
		// The components themselves are added or removed only when the number of displayed values changes
		// In other situations, their individual value pointers are updated instead
		val componentsP = pointer.mapWhile(linkedFlag) { _.indices.map(componentCache.apply) }
		val container = this.pointer(componentsP)
		
		// Updates the individual value pointers whenever the mapped pointer updates
		pointer.addListenerWhile(linkedFlag) { e =>
			e.newValue.view.zipWithIndex.foreach { case (value, index) => componentCache(index).result._1.value = value }
		}
		// Once/if the mapped pointer stops changing,
		// locks all value pointers and clears cached info (since it is not needed anymore)
		pointer.onceChangingStops {
			componentCache.cachedValues.foreach { _.result._2.foreach { _.lock() } }
			componentCache.clear()
		}
		
		container
	}
}