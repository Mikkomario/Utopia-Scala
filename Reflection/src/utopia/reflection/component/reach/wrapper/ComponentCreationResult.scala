package utopia.reflection.component.reach.wrapper

import utopia.reflection.component.reach.template.ReachComponentLike

import scala.language.implicitConversions

object ComponentCreationResult
{
	// IMPLICIT	------------------------------
	
	implicit def tupleToResult[C, R](tuple: (C, R)): ComponentCreationResult[C, R] =
		new ComponentCreationResult[C, R](tuple._1, tuple._2)
	
	implicit def componentToResult[C <: ReachComponentLike](component: C): ComponentCreationResult[C, Unit] =
		new ComponentCreationResult[C, Unit](component, ())
	
	implicit def vectorToResult[C <: ReachComponentLike](components: Vector[C]): ComponentCreationResult[Vector[C], Unit] =
		new ComponentCreationResult[Vector[C], Unit](components, ())
	
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
}

/**
  * An object for wrapping a created component and an optional result
  * @author Mikko Hilpinen
  * @since 7.10.2020, v2
  */
class ComponentCreationResult[+C, +R](val component: C, val result: R)
{
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
