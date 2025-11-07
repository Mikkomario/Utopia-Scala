package utopia.nexus.controller.api.node.scalable

import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy
import utopia.nexus.controller.api.node.ApiNode
import utopia.nexus.model.api.PathFollowResult
import utopia.nexus.model.api.PathFollowResult.Follow

import scala.language.implicitConversions

object FollowImplementation
{
	// IMPLICIT --------------------------------
	
	/**
	 * @param f A function to use as a FollowImplementation
	 * @tparam C Type of the required request context
	 * @return A follow implementation wrapping the specified function
	 */
	implicit def apply[C](f: String => Option[PathFollowResult[C]]): FollowImplementation[C] =
		new _FollowImplementation[C](f)
	
	
	// OTHER    --------------------------------
	
	/**
	 * Creates a new follow implementation based on a static child node
	 * @param child A child API node (called lazily)
	 * @tparam C Type of expected request context
	 * @return A new follow implementation that returns the specified child when the request path
	 *         matches the name of that child (case-insensitive)
	 */
	def withChild[C](child: => ApiNode[C]): FollowImplementation[C] = withChild(Lazy(child))
	/**
	 * Creates a new follow implementation based on a child node view
	 * @param childView A view into the child API node
	 * @tparam C Type of expected request context
	 * @return A new follow implementation that returns the specified child when the request path
	 *         matches the name of that child (case-insensitive)
	 */
	def withChild[C](childView: View[ApiNode[C]]): FollowImplementation[C] = new ChildFollowImplementation[C](childView)
	
	
	// NESTED   --------------------------------
	
	private class ChildFollowImplementation[-C](childView: View[ApiNode[C]])
		extends FollowImplementation[C]
	{
		// IMPLEMENTED  ------------------------
		
		override def apply(step: String): Option[PathFollowResult[C]] = {
			val child = childView.value
			if (child.name ~== step) Some(Follow(child)) else None
		}
	}
	
	private class _FollowImplementation[-C](f: String => Option[PathFollowResult[C]])
		extends FollowImplementation[C]
	{
		override def apply(step: String): Option[PathFollowResult[C]] = f(step)
	}
}

/**
 * A common trait for external API node follow-function implementations
 * @author Mikko Hilpinen
 * @since 17.6.2021, v1.6
 */
trait FollowImplementation[-C]
{
	/**
	 * Attempts to follow using this implementation
	 * @param step Name of the next element on the request path
	 * @return Action to be taken based on that path. None if this implementation doesn't cover this case.
	 */
	def apply(step: String): Option[PathFollowResult[C]]
}
