package utopia.flow.collection.immutable.tree

object TreeFromNodesFactory
{
	// NESTED   ------------------------
	
	private class AppendingFactory[Node](delegate: TreeFromNodesFactory[Node], existing: Iterable[Node])
		extends TreeFromNodesFactory[Node]
	{
		override def withChildren(children: IterableOnce[Node]): Node = ???
	}
}

/**
 * Common trait for Tree factories that accept completed child nodes
 * @author Mikko Hilpinen
 * @since 19.08.2026, v2.9
 */
trait TreeFromNodesFactory[Node] extends TreeFactory[Node, Node]
{
	
}
