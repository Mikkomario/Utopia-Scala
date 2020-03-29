package utopia.flow.test

import utopia.flow.datastructure.mutable.Tree

object TreeNodeTest extends App
{
    private def basicCheck(tree: utopia.flow.datastructure.template.TreeLike[_, _]) =
    {
        assert(tree.children.size == 2)
        assert(!tree.isEmpty)
        assert(tree.size == 5)
        assert(tree.depth == 3)
    }
    
    println("Running TreeNodeTest")
    
    // Creates tree
    val root = Tree(1)
    val bottomNode = Tree(4)
    root += Tree(2, Tree(3, bottomNode), Tree(5))
    val secondChild = Tree(6)
    root += secondChild
    
    // Performs tests on tree (not mutating)
    println(root)
    basicCheck(root)
    
    assert(root.nodesBelow.map { _.content }.toSet == Set(2, 3, 4, 5, 6))
    assert((root/2/3).children.size == 1)
    assert((root/5).children.isEmpty)
    assert((root/2).nodesBelow.map { _.content }.toSet == Set(3, 4, 5))
    assert(root.leaves.map { _.content } == Vector(4, 5, 6))
    assert(root.nodesBelow.contains(bottomNode))
    assert(root.contains(bottomNode))
    assert(root.contains(secondChild))
    assert(root.get(6).contains(secondChild))
    assert(root.find { _.content == 4 }.contains(bottomNode))
    
    // Creates an immutable copy of the tree
    val copy = root.immutableCopy
    
    // Performs tests on immutable tree
    basicCheck(copy)
    
    val increased = copy + 7
    
    assert(increased.find { _.content == 7 }.isDefined )
    assert(increased.find { _.content == 4 }.isDefined )
    
    val decreased = copy - 2
    
    assert(decreased.size == 1)
    assert(decreased.children.exists { _.content == 6 })
    
    // Mutates mutable tree & tests
    root.removeChild(secondChild)
    assert(root.children.size == 1)
    
    root -= bottomNode
    assert(root.depth == 2)
    
    root.clear()
    assert(root.isEmpty)
    
    println("Success")
}