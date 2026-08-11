package utopia.flow.collection.immutable.tree

import utopia.flow.collection.immutable.OptimizedIndexedSeq
import utopia.flow.collection.template

object Tree
{
	// OTHER    ---------------------------
	
	/**
	 * @param children Children to assign to this tree
	 * @return A tree with the specified child nodes
	 */
	def apply(children: Seq[Tree]): Tree = _Tree(children)
	
	
	// NESTED   ---------------------------
	
	private case class _Tree(children: Seq[Tree]) extends Tree
	{
		override def self: Tree = this
		
		override def withChildren(newChildren: IterableOnce[Tree]): Tree = Tree(OptimizedIndexedSeq.from(newChildren))
	}
}

/**
 * Common trait for immutable trees.
 * Based on [[CopyableTreeLike]], but removes the generic Repr type.
 * @author Mikko Hilpinen
 * @since 10.08.2026, v2.9
 */
trait Tree extends template.tree.Tree with CopyableTreeLike[Tree]
