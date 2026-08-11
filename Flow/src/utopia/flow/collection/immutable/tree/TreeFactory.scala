package utopia.flow.collection.immutable.tree

import utopia.flow.collection.immutable.Single

/**
 * Common trait for factories which produce new tree instances
 * @author Mikko Hilpinen
 * @since 10.08.2026, v2.9
 */
trait TreeFactory[-N, +T]
{
	// ABSTRACT -----------------------
	
	/**
	 * @param children Children to assign
	 * @return A new tree node containing the specified child nodes
	 */
	def withChildren(children: IterableOnce[N]): T
	
	
	// OTHER    -----------------------
	
	def withChild(child: N) = withChildren(Single(child))
}
