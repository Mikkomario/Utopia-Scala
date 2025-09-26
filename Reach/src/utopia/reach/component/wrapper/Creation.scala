package utopia.reach.component.wrapper

import utopia.flow.collection.immutable.Pair
import utopia.flow.view.template.eventful.Flag
import utopia.reach.component.template.ReachComponent
import utopia.reach.component.wrapper.Open.OpenSwitchables
import utopia.reach.container.layered.LayerPositioning

import scala.language.implicitConversions

object Creation
{
	// TYPES	------------------------------
	
	/**
	  * Represents a component [[Creation]]. Has no additional result.
	  */
	type Created[+C] = Creation[C, Unit]
	@deprecated("Replaced with Created", "v1.7")
	type CreationWrapper[+C] = Created[C]
	/**
	 * Represents the [[Creation]] of a single [[ReachComponent]], including an additional result.
	 */
	type CreationOfComponent[+R] = Creation[ReachComponent, R]
	/**
	 * Represents the [[Creation]] of a single [[ReachComponent]]
	 */
	type NewComponent = CreationOfComponent[Unit]
	
	/**
	 * Represents the [[Creation]] of 0-n components / elements; Includes an additional result.
	 */
	type CreationOfMany[+C, +R] = Creation[Seq[C], R]
	/**
	  * Component creation result that wraps multiple components at once
	  */
	@deprecated("Renamed to CreationOfMany", "v1.7")
	type ComponentsResult[+C, +R] = CreationOfMany[C, R]
	/**
	 * Represents the [[Creation]] of 0-n elements, without any additional result.
	 */
	type CreatedGroup[+C] = CreationOfMany[C, Unit]
	/**
	 * Represents the [[Creation]] of 0-n [[ReachComponent]]s; Includes an additional result.
	 */
	type CreationOfComponents[+R] = CreationOfMany[ReachComponent, R]
	/**
	 * Represents the [[Creation]] of 0-n [[ReachComponent]]s, with no additional result included.
	 */
	type NewComponents = CreationOfComponents[Unit]
	
	/**
	 * Represents the [[Creation]] of a single primary component (M), 0-n layers (C), with an additional result (R)
	 */
	type CreationOfLayers[+M, +C, +R] = Creation[(M, Seq[(C, LayerPositioning)]), R]
	/**
	  * Component creation result that wraps a primary component (of type M),
	  * plus potentially multiple layers (of type C), with an additional result (of type R)
	  */
	@deprecated("Renamed to CreationOfLayers", "v1.7")
	type LayersResult[+M, +C, +R] = CreationOfLayers[M, C, R]
	/**
	 * Represents the [[Creation]] of a single main component, 0-n additional layers,
	 * with no additional creation result.
	 */
	type NewLayers = CreationOfLayers[ReachComponent, ReachComponent, Unit]
	
	/**
	 * Represents the [[Creation]] of 0-n creations, including an additional result (R)
	 */
	type CreationOfCreations[+C, +CR, +R] = Creation[IterableOnce[Creation[C, CR]], R]
	/**
	  * A wrapper that wraps multiple creation results, containing an additional result of its own
	  */
	@deprecated("Renamed to CreationOfCreations", "v1.7")
	type CreationsResult[+C, +CR, +R] = CreationOfCreations[C, CR, R]
	/**
	 * Represents a [[Creation]] of 0-n creations, not including an additional result.
	 */
	type Creations[+C, +CR] = CreationOfCreations[C, CR, Unit]
	/**
	  * A wrapper that wraps multiple creation results with no additional value
	  */
	@deprecated("Renamed to Creations", "v1.7")
	type CreationsWrapper[+C, +CR] = Creations[C, CR]
	
	/**
	 * Represents the [[Creation]] of 0-n elements that may be switched on or off; Includes an additional result.
	 */
	type CreationOfSwitchables[+C, +R] = CreationOfCreations[C, Flag, R]
	/**
	 * Represents the [[Creation]] of 0-n open components that may be switched on or off; Includes an additional result.
	 */
	type CreationOfOpenSwitchables[+C, +R] = Creation[OpenSwitchables[C], R]
	/**
	 * Represents the [[Creation]] of 0-n elements, which may be switched on or off;
	 * Doesn't include an additional result.
	 */
	type SwitchableCreations[+C] = Creations[C, Flag]
	/**
	 * Represents the [[Creation]] of 0-n conditionally appearing components, including an additional result.
	 */
	type CreationOfSwitchableComponents[+R] = CreationOfSwitchables[ReachComponent, R]
	/**
	 * Represents the [[Creation]] of 0-n conditionally appearing components, without any additional result.
	 */
	type SwitchableComponents = CreationOfSwitchableComponents[Unit]
	
	
	// IMPLICIT	------------------------------
	
	implicit def autoAccess[C](result: Creation[C, _]): C = result.component
	
	implicit def componentsToCreations[C, R](components: CreationOfMany[C, R]): CreationOfCreations[C, Unit, R] =
		components.mapComponent { _.map { c => Creation(c) } }
	
	implicit def tupleToCreation[C, R](tuple: (C, R)): Creation[C, R] =
		new Creation[C, R](tuple._1, tuple._2)
	
	implicit def componentToCreation[C <: ReachComponent](component: C): Created[C] =
		new Creation[C, Unit](component, ())
	
	implicit def componentPairToCreation[C <: ReachComponent](componentPair: Pair[C]): Created[Pair[C]] =
		new Creation[Pair[C], Unit](componentPair, ())
	
	implicit def componentsToCreation[C <: ReachComponent](components: Seq[C]): CreatedGroup[C] =
		new Creation[Seq[C], Unit](components, ())
	
	implicit def switchableComponentsToCreation[C <: ReachComponent](components: Seq[(C, Flag)]): SwitchableCreations[C] =
		apply(components.map { case (component, flag) => Creation(component, flag) })
	
	implicit def switchableTupleToCreation[C <: ReachComponent, R](tuple: (Seq[(C, Flag)], R)): CreationOfSwitchables[C, R] =
		new Creation[Seq[Creation[C, Flag]], R](tuple._1.map { case (component, flag) => new Creation(component, flag) },
			tuple._2)
	
	implicit def autoWrapOpenSwitchables[C](c: OpenSwitchables[C]): CreationOfOpenSwitchables[C, Unit] =
		Creation(c)
	
	implicit def containersToCreation[P](containers: Seq[ContainerCreation[P, _, _]]): CreatedGroup[P] =
		Creation[Seq[P]](containers.map { _.parent })
	
	implicit def containerToCreation[P, R](container: ContainerCreation[P, _, R]): Creation[P, R] =
		new Creation[P, R](container.parent, container.result)
	
	
	// OTHER	------------------------------
	
	/**
	  * Wraps a component
	  * @param component Component to wrap
	  * @tparam C Type of the component
	  * @return A new component creation result
	  */
	def apply[C](component: C) = new Creation[C, Unit](component, ())
	
	/**
	 * @param items Multiple created items
	 * @param result Additional creation result
	 * @tparam C Type of individual items
	 * @tparam R Type of additional result
	 * @return A new component creation result
	 */
	def many[C, R](items: Seq[C], result: R) = new Creation(items, result)
	
	/**
	  * Creates a component creation result for building layered views
	  * @param main The main component
	  * @param layers The layer components
	  * @param result Additional creation result
	  * @tparam M Type of the main component
	  * @tparam C Type of the layer components
	  * @tparam R Type of the creation result
	  * @return A new component creation result
	  */
	def layers[M, C, R](main: M, layers: Seq[(C, LayerPositioning)], result: R): CreationOfLayers[M, C, R] =
		Creation(main -> layers, result)
	/**
	  * Creates a component creation result for building layered views (without using an additional creation result)
	  * @param main The main component
	  * @param layers The layer components
	  * @tparam M Type of the main component
	  * @tparam C Type of the layer components
	  * @return A new component creation result
	  */
	def layers[M, C](main: M, layers: Seq[(C, LayerPositioning)]): CreationOfLayers[M, C, Unit] =
		this.layers(main, layers, ())
}

/**
  * An object for wrapping a created component (or components) and an (optional) result
  * @tparam C Type of wrapped component
  * @tparam R Type of additional result
  *
  * @constructor Wraps the specified component and attaches additional data
  * @param component Created component
  * @param result    Additional data
  *
  * @author Mikko Hilpinen
  * @since 7.10.2020, v0.1
  */
case class Creation[+C, +R](component: C, result: R)
{
	// COMPUTED	-----------------------------
	
	/**
	  * @return Wrapped component, then additional result
	  */
	def toTuple = component -> result
	
	
	// OTHER	-----------------------------
	
	/**
	  * @param container Container that will hold this component
	  * @tparam P Type of parent container
	  * @return A component wrapping result
	  */
	def in[P](container: P) = ContainerCreation(container, component, result)
	
	/**
	  * @param component A new component
	  * @tparam C2 Type of the new component
	  * @return A new component creation result with new component and same additional result
	  */
	def withComponent[C2](component: C2) = new Creation(component, result)
	/**
	  * @param f A component mapping function
	  * @tparam C2 Type of new component
	  * @return A new component creation result with mapped component and same additional result
	  */
	def mapComponent[C2](f: C => C2) = withComponent(f(component))
	
	/**
	  * @param result A new additional result
	  * @tparam R2 Type of the new result
	  * @return A copy of this creation result with new additional value
	  */
	def withResult[R2](result: R2) = new Creation(component, result)
	/**
	  * @param f A mapping function for the result part
	  * @tparam R2 Type of the new result part
	  * @return A copy of this creation result with mapped additional value
	  */
	def mapResult[R2](f: R => R2) = withResult(f(result))
}
