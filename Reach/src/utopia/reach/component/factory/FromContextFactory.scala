package utopia.reach.component.factory

import scala.language.implicitConversions

object FromContextFactory
{
	// OTHER    --------------------------
	
	/**
	  * @param f A function to convert into a FromContextFactory
	  * @tparam N Type of accepted context
	  * @tparam A Type of generated values
	  * @return A new factory that uses the specified function
	  */
	implicit def apply[N, A](f: N => A): FromContextFactory[N, A] = new _FromContextFactory[N, A](f)
	
	implicit def fromGeneric[N, F[_ <: N]](f: FromGenericContextFactory[N, F]): FromContextFactory[N, F[N]] =
		apply { f.withContext }
	
	
	// NESTED   --------------------------
	
	private class _FromContextFactory[-N, +A](f: N => A) extends FromContextFactory[N, A]
	{
		override def withContext(context: N): A = f(context)
	}
}

/**
  * Common trait for factory classes that accept a context and yield other factories that utilize that context.
  * This trait is intended to be used with static context types and context limitations.
  * If you wish to use generic context types (e.g. for sharing the more specific context type forward),
  * please use [[FromGenericContextFactory]]
  *
  * @tparam N Type of accepted context
  * @tparam A Type of generated items
  *
  * @author Mikko Hilpinen
  * @since 17.4.2023, v1.0
  */
trait FromContextFactory[-N, +A] extends Any
{
	// ABSTRACT -----------------------
	
	/**
	  * @param context A context
	  * @return A contextual copy of this factory
	  */
	def withContext(context: N): A
	
	
	// COMPUTED ----------------------
	
	/**
	  * @param context Implicit context to use
	  * @return A contextual copy of this factory
	  */
	def contextual(implicit context: N) = withContext(context)
	
	
	// OTHER    ---------------------
	
	/**
	  * @param context A context
	  * @param f       A mapping function for the context
	  * @tparam N2 Type of initial context
	  * @return A copy of this factory that uses a mapped copy of the specified context
	  */
	def withMappedContext[N2](context: N2)(f: N2 => N) = withContext(f(context))
}
