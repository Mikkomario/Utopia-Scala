package utopia.reflection.container.reach

import scala.language.implicitConversions

import utopia.reflection.component.reach.template.ReachComponentLike

object ComponentCreationResult
{
	// IMPLICIT	------------------------------
	
	implicit def tupleToResult[C <: ReachComponentLike, R](tuple: (C, R)): ComponentCreationResult[C, R] =
		new ComponentCreationResult[C, R](tuple._1, tuple._2)
	
	implicit def componentToResult[C <: ReachComponentLike](component: C): ComponentCreationResult[C, Unit] =
		new ComponentCreationResult[C, Unit](component, ())
}

/**
  * An object for wrapping a created component and an optional result
  * @author Mikko Hilpinen
  * @since 7.10.2020, v2
  */
class ComponentCreationResult[+C <: ReachComponentLike, +R](val component: C, val result: R)
{
	/**
	  * @param container Container that will hold this component
	  * @tparam P Type of parent container
	  * @return A component wrapping result
	  */
	def in[P](container: P) = ComponentWrapResult(container, component, result)
}
