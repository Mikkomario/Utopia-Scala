package utopia.flow.datastructure.template

object Node
{
    def contentsOf[A](nodes: IterableOnce[Node[A]]) = nodes.iterator.map { node => node.content }
}

/**
 * Nodes are elements that contain data
 * @author Mikko Hilpinen
 * @since 28.10.2016
 */
trait Node[+A]
{
    /**
     * The contents of the node
     */
    def content: A
}