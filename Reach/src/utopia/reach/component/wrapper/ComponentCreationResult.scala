package utopia.reach.component.wrapper

import utopia.flow.event.ChangingLike
import utopia.reach.component.template.ReachComponentLike

import scala.language.implicitConversions

object ComponentCreationResult
{
	// TYPES	------------------------------
	
	/**
	  * Component creation result without additional result value
	  */
	type CreationWrapper[C] = ComponentCreationResult[C, Unit]
	
	/**
	  * A wrapper that wraps multiple creation results, containing an additional result of its own
	  */
	type CreationsResult[C, CR, R] = ComponentCreationResult[IterableOnce[ComponentCreationResult[C, CR]], R]
	
	/**
	  * A wrapper that wraps multiple creation results with no additional value
	  */
	type CreationsWrapper[C, CR] = CreationsResult[C, CR, Unit]
	
	/**
	  * Component creation result wrapping multiple components that have individual visibility states
	  */
	type SwitchableCreations[C, R] = CreationsResult[C, Option[ChangingLike[Boolean]], R]
	
	
	// IMPLICIT	------------------------------
	
	implicit def tupleToResult[C, R](tuple: (C, R)): ComponentCreationResult[C, R] =
		new ComponentCreationResult[C, R](tuple._1, tuple._2)
	
	implicit def componentToResult[C <: ReachComponentLike](component: C): CreationWrapper[C] =
		new ComponentCreationResult[C, Unit](component, ())
	
	/*
	implicit def vectorToResult[C](components: Vector[C]): ComponentCreationResult[Vector[C], Unit] =
		new ComponentCreationResult[Vector[C], Unit](components, ())
	*/
	
	implicit def componentVectorToResult[C <: ReachComponentLike](components: Vector[C]): CreationWrapper[Vector[C]] =
		new ComponentCreationResult[Vector[C], Unit](components, ())
	
	implicit def componentAndVisibilityPointersToResult[C <: ReachComponentLike]
	(components: IterableOnce[(C, Option[ChangingLike[Boolean]])]): SwitchableCreations[C, Unit] =
		apply(components.iterator.map { case (c, p) => apply(c, p) })
	
	implicit def containerVectorToResult[P](containers: Vector[ComponentWrapResult[P, _, _]]): CreationWrapper[Vector[P]] =
		ComponentCreationResult[Vector[P]](containers.map { _.parent })
	
	implicit def wrapToResult[P, R](wrapResult: ComponentWrapResult[P, _, R]): ComponentCreationResult[P, R] =
		new ComponentCreationResult[P, R](wrapResult.parent, wrapResult.result)
	
	
	// OTHER	------------------------------
	
	/**
	  * Creates a new component creation result with additional data
	  * @param component Created component
	  * @param result Additional data
	  * @tparam C Type of the component
	  * @tparam R Type of additional data
	  * @return A new component creation result
	  */
	def apply[C, R](component: C, result: R) = new ComponentCreationResult[C, R](component, result)
	
	/**
	  * Wraps a component
	  * @param component Component to wrap
	  * @tparam C Type of the component
	  * @return A new component creation result
	  */
	def apply[C](component: C) = new ComponentCreationResult[C, Unit](component, ())
	
	/**
	 * @param items Multiple created items
	 * @param result Additional creation result
	 * @tparam C Type of individual items
	 * @tparam R Type of additional result
	 * @return A new component creation result
	 */
	def many[C, R](items: Vector[C], result: R) = new ComponentCreationResult(items, result)
}

/**
  * An object for wrapping a created component and an optional result
  * @author Mikko Hilpinen
  * @since 7.10.2020, v2
  */
class ComponentCreationResult[+C, +R](val component: C, val result: R)
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
	def in[P](container: P) = ComponentWrapResult(container, component, result)
	
	/**
	  * @param component A new component
	  * @tparam C2 Type of the new component
	  * @return A new component creation result with new component and same additional result
	  */
	def withComponent[C2](component: C2) = new ComponentCreationResult(component, result)
	
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
	def withResult[R2](result: R2) = new ComponentCreationResult(component, result)
	
	/**
	  * @param f A mapping function for the result part
	  * @tparam R2 Type of the new result part
	  * @return A copy of this creation result with mapped additional value
	  */
	def mapResult[R2](f: R => R2) = withResult(f(result))
}
