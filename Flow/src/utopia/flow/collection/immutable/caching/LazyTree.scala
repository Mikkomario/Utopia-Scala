package utopia.flow.collection.immutable.caching

import utopia.flow.collection.immutable.TreeLike
import utopia.flow.collection.immutable.caching.iterable.CachingSeq
import utopia.flow.collection.mutable.iterator.LazyInitIterator
import utopia.flow.operator.EqualsFunction
import utopia.flow.view.immutable.caching.Lazy

object LazyTree
{
	/**
	  * Creates a new empty lazy tree node
	  * @param nav The navigational element of this node (call-by-name)
	  * @tparam A Type of nav element used by this tree
	  * @return A new tree node without any children
	  */
	def empty[A](nav: => A) = apply(Lazy(nav), CachingSeq.empty)
	/**
	  * Creates a new empty tree node
	  * @param nav The navigational element of this node (already initialized)
	  * @tparam A Type of nav-elements used by this tree
	  * @return A new tree node without any children
	  */
	def initializedEmpty[A](nav: A) = apply(Lazy.initialized(nav), CachingSeq.empty)
	
	/**
	  * Creates a new lazily initialized tree node
	  * @param nav The navigational element of this node (lazy)
	  * @param children The children under this node (lazy)
	  * @tparam A Type of navigational elements used by this tree
	  * @return A new lazily initialized tree
	  */
	def apply[A](nav: Lazy[A], children: CachingSeq[LazyTree[A]]) = new LazyTree[A](nav, children)
	/**
	  * Creates a new lazily initialized tree
	  * @param nav The navigational element of this node (call-by-name)
	  * @param children The children under this node (will be iterated lazily)
	  * @tparam A Type of navigational elements used by this tree
	  * @return A lazily initialized tree
	  */
	def lazily[A](nav: => A, children: IterableOnce[LazyTree[A]]): LazyTree[A] =
		apply(Lazy(nav), CachingSeq.from(children))
	
	/**
	  * Creates a new lazily initialized tree based on a recursive function
	  * @param root The root node nav-element (lazy)
	  * @param goDeeper A function that accepts a nav-element and returns the (lazily initialized) nav-elements
	  *                 of the children that are to appear under that node.
	  *                 The function is called only when necessary, and results are iterated and opened lazily.
	  *
	  *                 At some point the function should return empty collections, otherwise the resulting tree
	  *                 structure will be of infinite size.
	  *
	  * @tparam A Type of navigational elements used by this tree structure.
	  * @return A new lazily initialized tree
	  */
	def iterate[A](root: Lazy[A])(goDeeper: A => IterableOnce[Lazy[A]]): LazyTree[A] = {
		apply(root, CachingSeq(LazyInitIterator { goDeeper(root.value) }.map { nav => iterate(nav)(goDeeper) }))
	}
}

/**
  * A lazily initialized tree
  * @author Mikko Hilpinen
  * @since 28.9.2022, v2.0
  */
class LazyTree[A](lazyNav: Lazy[A], override val children: CachingSeq[LazyTree[A]] = CachingSeq.empty)
                 (override implicit val navEquals: EqualsFunction[A] = EqualsFunction.default)
	extends TreeLike[A, LazyTree[A]]
{
	// IMPLEMENTED  ---------------------------
	
	override def nav = lazyNav.value
	override def self = this
	
	override protected def createCopy(nav: A, children: Seq[LazyTree[A]]) =
		new LazyTree[A](Lazy.initialized(nav), CachingSeq.from(children))
	
	override protected def newNode(content: A) = new LazyTree[A](Lazy.initialized(content))
	
	
	// OTHER    ------------------------------
	
	/**
	  * Lazily maps the contents / navs of this tree
	  * @param f A mapping function for the nav elements of this tree
	  * @tparam B Type of mapping results
	  * @return Lazily mapped tree
	  */
	def map[B](f: A => B): LazyTree[B] = new LazyTree[B](lazyNav.map(f), children.map { _.map(f) })
}
