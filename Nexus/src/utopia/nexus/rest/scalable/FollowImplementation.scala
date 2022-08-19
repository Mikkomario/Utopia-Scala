package utopia.nexus.rest.scalable

import utopia.flow.operator.EqualsExtensions._
import utopia.nexus.http.Path
import utopia.nexus.rest.ResourceSearchResult.Follow
import utopia.nexus.rest.{Context, Resource, ResourceSearchResult}

import scala.language.implicitConversions

object FollowImplementation
{
	// IMPLICIT --------------------------------
	
	implicit def functionToImplementation[C <: Context]
	(f: Path => Option[ResourceSearchResult[C]]): FollowImplementation[C] = apply[C](f)
	
	
	// OTHER    --------------------------------
	
	/**
	 * @param f A function for handling request paths (returns Some on covered case and None when a case is not covered)
	 * @tparam C Type of expected request context
	 * @return A new follow implementation
	 */
	def apply[C <: Context](f: Path => Option[ResourceSearchResult[C]]): FollowImplementation[C] =
		new FunctionalFollowImplementation[C](f)
	
	/**
	 * Creates a new follow implementation based on a static child resource
	 * @param childResource Child resource (lazily initialized)
	 * @tparam C Type of expected request context
	 * @return A new follow implementation that returns the specified child when the request path
	 *         matches the name of that child (case-insensitive)
	 */
	def withChild[C <: Context](childResource: => Resource[C]): FollowImplementation[C] =
		new ChildFollowImplementation[C](childResource)
	
	
	// NESTED   --------------------------------
	
	private class FunctionalFollowImplementation[-C <: Context](f: Path => Option[ResourceSearchResult[C]])
		extends FollowImplementation[C]
	{
		override def apply(path: Path) = f(path)
	}
	
	private class ChildFollowImplementation[-C <: Context](getChild: => Resource[C])
		extends FollowImplementation[C]
	{
		// ATTRIBUTES   ------------------------
		
		private lazy val child: Resource[C] = getChild
		
		
		// IMPLEMENTED  ------------------------
		
		override def apply(path: Path) =
			if (child.name ~== path.head) Some(Follow(child, path.tail)) else None
	}
}

/**
 * A common trait for rest node request path handling implementations
 * @author Mikko Hilpinen
 * @since 17.6.2021, v1.6
 */
trait FollowImplementation[-C <: Context]
{
	/**
	 * Attempts to follow with this implementation
	 * @param path Request path after this resource
	 * @return Action to be taken based on that path. None if this implementation doesn't cover that case.
	 */
	def apply(path: Path): Option[ResourceSearchResult[C]]
}
