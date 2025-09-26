package utopia.reach.component.wrapper

import utopia.flow.view.template.eventful.{Changing, Flag}

import scala.language.implicitConversions

object ContainerCreation
{
	// TYPES    ------------------------
	
	/**
	 * Represents the creation of a container that contains 0-n components
	 */
	type MultiContainerCreation[+P, +C, +R] = ContainerCreation[P, Seq[C], R]
	/**
	  * Type of component wrap result that wraps multiple components
	  */
	@deprecated("Renamed to MultiContainerCreation", "v1.7")
	type ComponentsWrapResult[+P, +C, +R] = MultiContainerCreation[P, C, R]
	
	/**
	 * Represents the creation of a container where the contents are conditionally displayed
	 */
	type ViewContainerCreation[+P, +C, +R] = MultiContainerCreation[P, Creation[C, Flag], R]
	/**
	  * Type of component wrap result that wraps multiple switchable components and
	  * includes their visibility pointers
	  */
	@deprecated("Replaced with ViewContainerCreation", "v1.7")
	type SwitchableComponentsWrapResult[+P, +C, +R] = ContainerCreation[P, Seq[(C, Changing[Boolean])], R]
	
	
	// IMPLICIT	------------------------
	
	// Results can implicitly be converted to supply the parent component when requested
	implicit def autoAccessParent[P](result: ContainerCreation[P, _, _]): P = result.parent
	
	
	// OTHER	------------------------
	
	/**
	  * Creates a new component wrap result without additional function result
	  * @param parent Parent component
	  * @param child Child component
	  * @tparam P Type of parent component
	  * @tparam C Type of child component
	  * @return A component wrap result that contains both components
	  */
	def apply[P, C](parent: P, child: C): ContainerCreation[P, C, Unit] = apply(parent, child, ())
}

/**
  * A result returned by functions that create a component and then wrap it. These results contain and provide easy
  * access to both the wrapping container and the wrapped component
  * @tparam P Type of parent component
  * @tparam C Type of child component
  * @tparam R Type of additional result value
  *
  * @constructor Wraps a parent container, a child component and an additional result
  * @param parent Parent component
  * @param child  Child component
  * @param result Additional creation result (optional)
  *
  * @author Mikko Hilpinen
  * @since 7.10.2020, v0.1
  */
case class ContainerCreation[+P, +C, +R](parent: P, child: C, result: R)
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
	def toCreationResult = Creation(parent, result)
	
	
	// OTHER	----------------------------
	
	/**
	 * @param newParent New parent / container to assign to this result
	 * @tparam P2 Type of the new parent
	 * @return Copy of this creation with the specified parent
	 */
	def withParent[P2](newParent: P2) = copy(parent = newParent)
	/**
	 * @param f A mapping function applied to the parent container
	 * @tparam P2 Type of the mapping result
	 * @return A copy of this creation with a mapped container
	 */
	def mapParent[P2](f: P => P2) = withParent(f(parent))
	
	/**
	  * @param newChild New child to assign to this result
	  * @tparam C2 Type of the new child component
	  * @return A copy of this result with the specified child component
	  */
	def withChild[C2](newChild: C2) = copy(child = newChild)
	/**
	  * @param f A mapping function for the wrapped child components
	  * @tparam C2 Type of mapping result
	  * @return A copy of this result with mapped children
	  */
	def mapChild[C2](f: C => C2) = copy(child = f(child))
	
	/**
	  * @param newResult New additional result
	  * @tparam R2 Type of the new result
	  * @return A copy of this wrap result with specified additional value
	  */
	def withResult[R2](newResult: R2) = new ContainerCreation(parent, child, newResult)
	/**
	  * @param f A mapping function for the additional result
	  * @tparam R2 Type of the mapped result
	  * @return A copy of this wrap result with mapped additional result
	  */
	def mapResult[R2](f: R => R2) = withResult(f(result))
}
