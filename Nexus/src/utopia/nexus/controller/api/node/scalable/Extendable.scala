package utopia.nexus.controller.api.node.scalable

import utopia.access.model.enumeration.Method
import utopia.flow.collection.immutable.caching.iterable.LazySeq
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy
import utopia.nexus.controller.api.node.ApiNode
import utopia.nexus.model.response.RequestResult

/**
 * Common trait for classes which allow the inclusion of new follow- and method execution implementations
 * @tparam C Type of the required request context
 * @tparam P Type of the locally generated context
 * @author Mikko Hilpinen
 * @since 07.11.2025, v2.0
 */
trait Extendable[+C, +P]
{
	// ABSTRACT ------------------------
	
	/**
	 * Adds a new follow implementation to this node
	 * @param implementation Implementation to add
	 */
	def followWith(implementation: FollowImplementation[C]): Unit
	/**
	 * Adds new follow implementations to this node
	 * @param followImplementations New follow implementations to add
	 */
	def ++=(followImplementations: Iterable[FollowImplementation[C]]): Unit
	
	/**
	 * Adds a new implementation for method execution
	 * @param method The method being executed
	 * @param implementation Implementation for that method's execution
	 */
	def addImplementation(method: Method, implementation: UseCaseImplementation[C, P]): Unit
	/**
	 * Replaces a method's execution implementation with a new function
	 * @param method The method to implement
	 * @param implementation Implementation for that method's execution.
	 *                       Receives 3 values:
	 *                          1. The local context of this node
	 *                          1. The applicable request context
	 *                          1. Request path remaining after this node, if applicable
	 *
	 *                       Yields the result to send to the client
	 */
	def update(method: Method)(implementation: (P, C, Seq[String]) => RequestResult): Unit
	
	
	// OTHER    ------------------------
	
	/**
	 * Alias for [[followWith]]
	 */
	def +=(followImplementation: FollowImplementation[C]) = followWith(followImplementation)
	/**
	 * Adds a new API node under this one
	 * @param child API node accessible via this node
	 */
	def +=(child: ApiNode[C]) = addChild(View.fixed(child))
	
	/**
	 * Adds a child node under this one
	 * @param child A child node to add under this node
	 */
	def addChild(child: => ApiNode[C]): Unit = addChild(Lazy(child))
	/**
	 * Adds a child node under this one
	 * @param childView A view into the child node to add under this node
	 */
	def addChild(childView: View[ApiNode[C]]): Unit = this += FollowImplementation.withChild(childView)
	/**
	 * Adds new child nodes under this node
	 * @param children child nodes to add
	 */
	def addChildren(children: Iterable[ApiNode[C]]) = children match {
		case lazyColl: LazySeq[ApiNode[C]] =>
			this ++= lazyColl.lazyContents.view.map { FollowImplementation.withChild[C](_) }
		case i => this ++= i.view.map { FollowImplementation.withChild[C](_) }
	}
	
	/**
	 * Adds a new implementation for method execution
	 * @param implementation Implemented method, plus the implementation for that method's execution
	 */
	def +=(implementation: (Method, UseCaseImplementation[C, P])) =
		addImplementation(implementation._1, implementation._2)
	
	/**
	 * Adds a new implementation for a method's execution
	 * @param method The method being executed
	 * @param implementation A function for implementing that method.
	 *                       Receives 4 values:
	 *                          1. This node's local context
	 *                          1. The applicable request context
	 *                          1. Request path remaining after this node, if applicable
	 *                          1. A lazy container that yields a result from a lower implementation
	 *                             (or a failure, if no implementations remain)
	 * @see [[addGeneralImplementationFor]], if you don't need the local context parameter
	 */
	def addImplementationFor(method: Method)(implementation: (P, C, Seq[String], Lazy[RequestResult]) => RequestResult) =
		addImplementation(method, UseCaseImplementation.usingNodeContext(implementation))
	/**
	 * Adds a new implementation for a method's execution
	 * @param method The method being executed
	 * @param implementation A function for implementing that method.
	 *                       Receives 3 values:
	 *                          1. The applicable request context
	 *                          1. Request path remaining after this node, if applicable
	 *                          1. A lazy container that yields a result from a lower implementation
	 *                             (or a failure, if no implementations remain)
	 */
	def addGeneralImplementationFor(method: Method)
	                               (implementation: (C, Seq[String], Lazy[RequestResult]) => RequestResult) =
		addImplementation(method, UseCaseImplementation(implementation))
}
