package utopia.nexus.controller.api.node.scalable

import utopia.access.model.enumeration.Method
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.caching.iterable.CachingSeq
import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq, Single}
import utopia.nexus.model.response.RequestResult

import scala.collection.mutable

object ExtendableApiNodeFactory
{
	// TODO: Continue
	
	// NESTED   ------------------------
	
	private class CachingFactory[Param, RC, LC, N]
	(f: (Param, Seq[FollowImplementation[RC]], Map[Method, Seq[UseCaseImplementation[RC, LC]]]) => N)
		extends ExtendableApiNodeFactory[Param, RC, LC, N]
	{
		// ATTRIBUTES   ----------------
		
		private val cache = mutable.Map[Param, N]()
		
		
		// IMPLEMENTED  ----------------
		
		override protected def apply(param: Param, follow: Seq[FollowImplementation[RC]],
		                             execute: Map[Method, Seq[UseCaseImplementation[RC, LC]]]): N =
			cache.getOrElseUpdate(param, f(param, follow, execute))
		
		override def followWith(implementation: FollowImplementation[RC]): Unit = {
			cache.clear()
			super.followWith(implementation)
		}
		override def ++=(followImplementations: Iterable[FollowImplementation[RC]]): Unit = {
			cache.clear()
			super.++=(followImplementations)
		}
		override def addImplementation(method: Method, implementation: UseCaseImplementation[RC, LC]): Unit = {
			cache.clear()
			super.addImplementation(method, implementation)
		}
		override def update(method: Method)(implementation: (LC, RC, Seq[String]) => RequestResult): Unit = {
			cache.clear()
			super.update(method)(implementation)
		}
		
		override def followWith(follow: Param => FollowImplementation[RC]): Unit = {
			cache.clear()
			super.followWith(follow)
		}
		override def addDynamicImplementation(method: Method)
		                                     (implementation: Param => UseCaseImplementation[RC, LC]): Unit =
		{
			cache.clear()
			super.addDynamicImplementation(method)(implementation)
		}
	}
	
	private class _ExtendableApiNodeFactory[Param, RC, LC, +N]
	(f: (Param, Seq[FollowImplementation[RC]], Map[Method, Seq[UseCaseImplementation[RC, LC]]]) => N)
		extends ExtendableApiNodeFactory[Param, RC, LC, N]
	{
		override protected def apply(param: Param, follow: Seq[FollowImplementation[RC]],
		                             execute: Map[Method, Seq[UseCaseImplementation[RC, LC]]]): N =
			f(param, follow, execute)
	}
}

/**
 * A common trait for resource factory classes that can be extended with custom functionality
 * @tparam Param Accepted construction parameter
 * @tparam RC Type of required request context
 * @tparam LC Type of locally API node context
 * @tparam N Type of the generated API nodes
 * @author Mikko Hilpinen
 * @since 25.6.2021, v1.6
 */
abstract class ExtendableApiNodeFactory[Param, RC, LC, +N] extends Extendable[RC, LC]
{
	// ATTRIBUTES   --------------------
	
	private var staticFollow: Seq[FollowImplementation[RC]] = Empty
	private var staticExecute: Map[Method, Seq[UseCaseImplementation[RC, LC]]] = Map()
	
	private var dynamicFollow: Seq[Param => FollowImplementation[RC]] = Empty
	private var dynamicExecute = Map[Method, Seq[Param => UseCaseImplementation[RC, LC]]]()
	
	
	// ABSTRACT ------------------------
	
	/**
	 * @param param A node construction parameter
	 * @param follow Follow implementations to add to the node
	 * @param execute Method implementations to add to the node
	 * @return A new API node
	 */
	protected def apply(param: Param, follow: Seq[FollowImplementation[RC]],
	                    execute: Map[Method, Seq[UseCaseImplementation[RC, LC]]]): N
	
	
	// IMPLEMENTED  --------------------
	
	override def followWith(implementation: FollowImplementation[RC]): Unit =
		staticFollow = implementation +: staticFollow
	override def ++=(followImplementations: Iterable[FollowImplementation[RC]]): Unit =
		staticFollow = OptimizedIndexedSeq.concat(followImplementations, staticFollow)
	
	override def addImplementation(method: Method, implementation: UseCaseImplementation[RC, LC]): Unit =
		staticExecute += (method -> (staticExecute.get(method) match {
			case Some(existing) => implementation +: existing
			case None => Single(implementation)
		}))
	override def update(method: Method)(implementation: (LC, RC, Seq[String]) => RequestResult): Unit = {
		dynamicExecute -= method
		staticExecute += (method -> Single(UseCaseImplementation.default.usingNodeContext(implementation)))
	}
	
	
	// OTHER    ------------------------
	
	/**
	 * @param param Node construction input
	 * @return API node based on the specified input
	 */
	def apply(param: Param): N = {
		// Combines the static & dynamic follow implementations
		val follow = {
			if (dynamicFollow.isEmpty)
				staticFollow
			else {
				val computedView = dynamicFollow.view.map { _(param) }
				if (staticFollow.isEmpty)
					computedView.caching
				else
					CachingSeq.concat(staticFollow, computedView)
			}
		}
		// Combines the static & dynamic method implementations
		val execute = {
			if (dynamicExecute.isEmpty)
				staticExecute
			else if (staticExecute.isEmpty)
				dynamicExecute.view.mapValues { _.view.map { _(param) }.caching }.toMap
			else
				(staticExecute.keySet ++ dynamicExecute.keySet).iterator
					.map { method =>
						val implementations = dynamicExecute.get(method) match {
							case Some(dynamic) =>
								val computedView = dynamic.view.map { _(param) }
								staticExecute.get(method) match {
									case Some(static) => CachingSeq.concat(static, computedView)
									case None => computedView.caching
								}
							case None => staticExecute(method)
						}
						method -> implementations
					}
					.toMap
		}
		// Constructs the API node
		apply(param, follow, execute)
	}
	
	/**
	 * Adds a new dynamic follow implementation
	 * @param follow A function that accepts a custom parameter
	 *               and yields a follow implementation to add to the generated nodes
	 */
	def followWith(follow: Param => FollowImplementation[RC]) = dynamicFollow :+= follow
	/**
	 * Adds a dynamic method implementation
	 * @param method The method being executed
	 * @param implementation A function that accepts a custom parameter and
	 *                       yields an implementation for the specified method
	 */
	def addDynamicImplementation(method: Method)(implementation: Param => UseCaseImplementation[RC, LC]) =
		dynamicExecute += (method -> (implementation +: dynamicExecute.getOrElse(method, Empty)))
	
	/**
	 * Adds a new use case to all resources that will be generated from this factory
	 * @param useCase A function that creates use cases based on the specified parameters
	 */
	@deprecated("Replaced with .addDymanicImplementation(...)", "v2.0")
	def addUseCase(method: Method, useCase: Param => UseCaseImplementation[RC, LC]) =
		addDynamicImplementation(method)(useCase)
	/**
	 * Adds a new follow implementation to all resources that will be generated from this factory
	 * @param follow A function that creates follow implementations based on the specified parameters
	 */
	@deprecated("Renamed to .followWith(...)", "v2.0")
	def addFollow(follow: Param => FollowImplementation[RC]) = followWith(follow)
}
