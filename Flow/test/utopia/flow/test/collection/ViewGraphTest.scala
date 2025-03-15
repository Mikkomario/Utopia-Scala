package utopia.flow.test.collection

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, ViewGraphNode}
import utopia.flow.util.RangeExtensions._
import utopia.flow.view.immutable.caching.Lazy

/**
 * Tests certain graph functions
 * @author Mikko Hilpinen
 * @since 14.03.2025, v2.6
 */
object ViewGraphTest extends App
{
	private var edgesCreated = 0
	private val valueIterator = Iterator.iterate(1) { _ + 1 }
	/*
	 * 1 -> 2 -> 3 -> 5
	 *             -> 6
	 *        -> 4 -> 7
	 *             -> 8
	 */
	private val g1 = ViewGraphNode.iterate { i: Int =>
		val start = ((i - 1) * 2 + 1) max (i + 1)
		if (start > 8)
			Empty
		else {
			val span = start spanTo ((i * 2) min 8)
			println(s"$i => $span")
			span.iterator.map { i2 =>
				edgesCreated += 1
				Lazy { valueIterator.next() } -> Lazy.initialized(i2)
			}
		}
	}(1)
	
	assert(edgesCreated == 0)
	
	// 1 -> 2 -> 4 -> 8
	private val g2 = g1.flatMapNodes { i => if (i <= 2 || i % 2 == 0) Some(i) else None }.only.get.value
	
	assert(edgesCreated == 0)
	assert(g2.value == 1)
	
	// 2 -> 4 -> 8
	private val g22 = g2.leavingEdges.head.end
	
	assert(g22.value == 2)
	assert(edgesCreated == 1)
	
	// 4 -> 8
	private val g23 = g22.leavingEdges.head.end
	
	assert(g23.value == 4)
	assert(edgesCreated == 3)
	
	// 8
	private val g24 = g23.leavingEdges.head.end
	
	assert(g24.value == 8, g24.value)
	assert(edgesCreated == 5)
	assert(g24.leavingEdges.isEmpty)
	assert(g23.leavingEdges.size == 1)
	
	/*
	   2 -> 3 -> 5
			  -> 6
         -> 4 -> 7
              -> 8
	 */
	private val g12 = g1.leavingEdges.only.get.end
	
	assert(g12.value == 2)
	assert(edgesCreated == 5)
	assert(g12.leavingEdges.size == 2)
	assert(edgesCreated == 5)
	
	/*
	   3 -> 5
		 -> 6
	 */
	private val g13 = g12.leavingEdges.head.end
	
	assert(g13.value == 3)
	assert(edgesCreated == 5)
	
	// 5
	private val g14 = g13.leavingEdges.head.end
	
	assert(g14.value == 5)
	assert(edgesCreated == 6)
	assert(g14.leavingEdges.isEmpty)
	assert(g13.leavingEdges.size == 2)
	assert(edgesCreated == 7)
	
	println("Success!")
}
