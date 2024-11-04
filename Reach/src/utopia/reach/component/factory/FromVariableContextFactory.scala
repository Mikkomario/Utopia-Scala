package utopia.reach.component.factory

import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing

import scala.language.implicitConversions

@deprecated("With the introduction of variable context classes, this trait is no longer needed", "v1.4")
object FromVariableContextFactory
{
	// IMPLICIT    -----------------------
	
	/**
	  * @param f A function that acts as a factory
	  * @tparam N Type of accepted contexts
	  * @tparam A Type of resulting items
	  * @return A new factory based on the specified function
	  */
	implicit def apply[N, A](f: Changing[N] => A): FromVariableContextFactory[N, A] =
		new _FromVariableContextFactory[N, A](f)
	
	
	// NESTED   -----------------------
	
	private class _FromVariableContextFactory[-N, +A](f: Changing[N] => A) extends FromVariableContextFactory[N, A]
	{
		override def withContextPointer(p: Changing[N]): A = f(p)
	}
}

/**
  * Common trait for factories that accept a variable context and yield another object
  * @author Mikko Hilpinen
  * @since 31.5.2023, v1.1
  */
@deprecated("With the introduction of variable context classes, this trait is no longer needed", "v1.4")
trait FromVariableContextFactory[-N, +A] extends FromContextFactory[N, A]
{
	// ABSTRACT -------------------------
	
	/**
	  * @param p A variable context pointer
	  * @return A new item based on the specified (variable) context
	  */
	def withContextPointer(p: Changing[N]): A
	
	
	// IMPLEMENTED    -------------------
	
	override def withContext(context: N): A = withContextPointer(Fixed(context))
}
