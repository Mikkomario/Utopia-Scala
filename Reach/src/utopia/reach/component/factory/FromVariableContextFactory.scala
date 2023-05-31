package utopia.reach.component.factory

import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing

import scala.language.implicitConversions

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
		override def withContext(context: Changing[N]): A = f(context)
	}
}

/**
  * Common trait for factories that accept a variable context (i.e. a context pointer) and yield another object
  * @author Mikko Hilpinen
  * @since 31.5.2023, v1.1
  */
trait FromVariableContextFactory[-N, +A] extends FromContextFactory[Changing[N], A]
{
	// OTHER    -------------------------
	
	def withStaticContext(context: N): A = withContext(Fixed(context))
}
