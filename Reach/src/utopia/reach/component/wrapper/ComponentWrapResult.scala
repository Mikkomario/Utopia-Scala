package utopia.reach.component.wrapper

import scala.language.implicitConversions

object ComponentWrapResult
{
	// IMPLICIT	------------------------
	
	// Results can implicitly be converted to supply the parent component when requested
	implicit def autoAccessParent[P](result: ComponentWrapResult[P, _, _]): P = result.parent
	
	
	// OTHER	------------------------
	
	/**
	  * Creates a new component wrap result without additional function result
	  * @param parent Parent component
	  * @param child Child component
	  * @tparam P Type of parent component
	  * @tparam C Type of child component
	  * @return A component wrap result that contains both components
	  */
	def apply[P, C](parent: P, child: C): ComponentWrapResult[P, C, Unit] = apply(parent, child, ())
	
	/**
	  * @param parent Parent component
	  * @param child Child component
	  * @param result Additional creation result (optional)
	  * @tparam P Type of parent component
	  * @tparam C Type of child component
	  * @tparam R Type of additional result value
	  * @return A component wrap result that contains both components and the additional result
	  */
	def apply[P, C, R](parent: P, child: C, result: R) = new ComponentWrapResult[P, C, R](parent, child, result)
}

/**
  * A result returned by functions that create a component and then wrap it. These results contain and provide easy
  * access to both the wrapping container and the wrapped component
  * @author Mikko Hilpinen
  * @since 7.10.2020, v0.1
  */
class ComponentWrapResult[+P, +C, +R](val parent: P, val child: C, val result: R)
{
	// COMPUTED	----------------------------
	
	/**
	  * @return This wrap result as a tuple with both parent and child components
	  */
	def toTuple = parent -> child
	
	/**
	  * @return This wrap result as a tuple with parent, child and result
	  */
	def toTriple = (parent, child, result)
	
	/**
	  * @return This wrap result as a tuple with parent and result
	  */
	def parentAndResult = parent -> result
	
	/**
	  * @return A component creation result based on this wrap result
	  */
	def toCreationResult = ComponentCreationResult(parent, result)
	
	
	// OTHER	----------------------------
	
	/**
	  * @param newResult New additional result
	  * @tparam R2 Type of the new result
	  * @return A copy of this wrap result with specified additional value
	  */
	def withResult[R2](newResult: R2) = new ComponentWrapResult(parent, child, newResult)
	
	/**
	  * @param f A mapping function for the additional result
	  * @tparam R2 Type of the mapped result
	  * @return A copy of this wrap result with mapped additional result
	  */
	def mapResult[R2](f: R => R2) = withResult(f(result))
}
