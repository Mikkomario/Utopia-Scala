package utopia.flow.collection.template.tree

import utopia.flow.collection.immutable.Empty

object Tree
{
	// OTHER    -------------------------
	
	/**
	 * @param children Children to include. Default = empty.
	 * @return A new tree node.
	 */
	def apply(children: Seq[Tree] = Empty): Tree = new _Tree(children)
	
	
	// NESTED   -------------------------
	
	private class _Tree(override val children: Seq[Tree]) extends Tree
	{
		override def self: Tree = this
	}
}

/**
 * Common trait for tree implementations. Removes the generic Repr type from [[TreeLike2]].
 * @author Mikko Hilpinen
 * @since 05.06.2026, v2.9
 */
trait Tree extends TreeLike2[Tree]
