package utopia.reach.container.multi

import utopia.firmament.context.ComponentCreationDefaults
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.caching.cache.Cache
import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq}
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.eventful.{CopyOnDemand, EventfulPointer, LockablePointer}
import utopia.flow.view.template.eventful.Changing
import utopia.reach.component.hierarchy.SeedHierarchyBlock
import utopia.reach.component.template.ReachComponent
import utopia.reach.component.wrapper.ComponentWrapResult.SwitchableComponentsWrapResult
import utopia.reach.component.wrapper.{ComponentWrapResult, OpenComponent}
import utopia.reach.component.wrapper.OpenComponent.{SeparateOpenComponents, SwitchableOpenComponents}
import utopia.reach.container.ContainerFactory

import scala.collection.mutable

/**
  * Common trait for initialized container factories that create containers that switch some of their content on or off
  * @author Mikko Hilpinen
  * @since 5.5.2023, v1.1
  */
trait ViewContainerFactory[+Container <: ReachComponent, -Top]
	extends ContainerFactory[Container, Top, SwitchableOpenComponents, SwitchableComponentsWrapResult]
{
	// ABSTRACT ------------------------
	
	/**
	  * Creates a new container by wrapping a content pointer.
	  * The content attachment logic is handled outside of this function.
	  * @param contentPointer A pointer that contains the currently displayed content.
	  * @return A new container
	  */
	protected def _apply(contentPointer: Changing[Seq[Top]]): Container
	
	
	// IMPLEMENTED  --------------------
	
	override def apply[C <: Top, R](content: SwitchableOpenComponents[C, R]): SwitchableComponentsWrapResult[Container, C, R] = {
		val container = fromVisibilityFlags(content)
		ComponentWrapResult(container, content.map { _.componentAndResult }, content.result)
	}
	
	
	// OTHER    ------------------------
	
	/**
	  * Creates a new container that changes its contents dynamically, based on a content pointer
	  * @param content A pointer that contains the currently displayed content.
	  * @return A new container
	  */
	def pointer(content: Changing[SeparateOpenComponents[Top, _]]): Container = {
		// Creates the container
		val componentsP = content.map { _.map { _.component } }
		val container = _apply(componentsP)
		
		content.fixedValue match {
			// Case: Content is static => Permanently attaches the components to the container
			case Some(staticContent) => staticContent.foreach { _.hierarchy.complete(container) }
			// Case: Content is dynamic => Adds attachment management
			case None =>
				// Tracks visible components as hashcodes
				val visibleHashesP = componentsP.map { _.view.map { _.hashCode() }.toSet }
				
				// When new components are introduced, makes sure they get attached to this stack
				// For each encountered component (hashcode), creates a new link pointer
				val trackedHashes = mutable.Set[Int]()
				content.addListenerWhileAndSimulateEvent(hierarchy.linkedFlag, Empty) { change =>
					change.newValue.foreach { open =>
						val hash = open.component.hashCode()
						// Case: Not previously attached => Creates a new link pointer and attaches the component
						if (!trackedHashes.contains(hash)) {
							trackedHashes += hash
							open.attachTo(container, visibleHashesP.map { _.contains(hash) })
						}
					}
				}
		}
		
		container
	}
	
	/**
	  * Builds a new container which reflects the contents of a multi-value pointer.
	  * This is best used in combination with view-based components.
	  * @param pointer Pointer whose value is reflected in the container contents
	  * @param construct A function that accepts:
	  *                     1. A pointer that contains the value to display on that component
	  *                     1. Index of this component
	  *
	  *                  and yields a new open component based on that pointer
	  * @tparam A Type of values displayed on individual components
	  * @return A new container
	  */
	protected def _mapPointer[A](pointer: Changing[Seq[A]])(construct: (Changing[A], Int) => OpenComponent[Top, _]): Container =
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
			construct(valueP.readOnly, index).withResult(valueP -> lockableP)
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
	
	/**
	  * Creates a new container from a set of component-visibility-flag -pairs
	  * @param content Content to display
	  * @return A newly created container
	  */
	protected def fromVisibilityFlags(content: SwitchableOpenComponents[Top, _]) = {
		// Checks which components have dynamic linking and which have static linking
		val (staticComponents, dynamicComponents) = content.zipWithIndex.divideBy { _._1.result.mayChange }.toTuple
		val alwaysVisibleComponents = staticComponents.view.filter { _._1.result.value }.toOptimizedSeq
		
		// Case: No dynamic linking applied => Creates a container with fixed content
		if (dynamicComponents.isEmpty) {
			val container = _apply(Fixed(alwaysVisibleComponents.map { _._1.component }))
			joinUnconditionalHierarchiesTo(alwaysVisibleComponents.map { _._1.hierarchy }, container)
			
			container
		}
		// Case: Dynamic linking applied => Creates a dynamic container
		else {
			implicit val log: Logger = ComponentCreationDefaults.componentLogger
			// A manually updated pointer that contains all components that are visible at that time
			val visibleContentP = {
				// Case: All components are dynamic => No extra sorting is required
				if (alwaysVisibleComponents.isEmpty) {
					val displayedComponents = dynamicComponents.map { _._1 }
					CopyOnDemand {
						displayedComponents.view.filter { _.result.value }.map { _.component }.toOptimizedSeq
					}
				}
				// Case: A mixture of static and dynamic components
				//       => Applies custom sorting in order to ensure that the ordering is preserved
				else {
					lazy val justAlwaysVisibleComponents = alwaysVisibleComponents.map { _._1.component }
					CopyOnDemand {
						val visibleDynamicComponents = dynamicComponents.filter { _._1.result.value }
						// Case: Only static components visible at this time
						if (visibleDynamicComponents.isEmpty)
							justAlwaysVisibleComponents
						else
							OptimizedIndexedSeq
								.concat(alwaysVisibleComponents, visibleDynamicComponents)
								.sortBy { _._2 }.map { _._1.component }
					}
				}
			}
			// Updates this pointer whenever one of the component visibility pointers changes
			dynamicComponents.foreach { case (component, _) =>
				component.result.addListenerWhile(hierarchy.linkedFlag) { _ => visibleContentP.update() }
			}
			
			// Creates the container and attaches the components to it
			val container = _apply(visibleContentP)
			joinUnconditionalHierarchiesTo(alwaysVisibleComponents.map { _._1.hierarchy }, container)
			dynamicComponents.foreach { case (component, _) => component.attachTo(container, component.result) }
			
			container
		}
	}
	
	/**
	  * Merges and joins n component hierarchies to a single container
	  * @param hierarchies Hierarchies to merge & attach
	  * @param container Container to which these hierarchies will be attached
	  */
	protected def joinUnconditionalHierarchiesTo(hierarchies: Iterable[SeedHierarchyBlock], container: ReachComponent) = {
		if (hierarchies.hasSize > 1) {
			val combinedHierarchy = hierarchies.head
			hierarchies.view.tail.foreach { _.replaceWith(combinedHierarchy) }
			combinedHierarchy.complete(container)
		}
		else
			hierarchies.foreach { _.complete(container) }
	}
}