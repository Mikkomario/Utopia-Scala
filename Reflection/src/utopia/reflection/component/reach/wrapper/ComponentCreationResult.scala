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
	
	
	// OTHER	------------------------------
	
	def apply[C, R](component: C, result: R) = new ComponentCreationResult[C, R](component, result)
	
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
}
