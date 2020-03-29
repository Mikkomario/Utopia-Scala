package utopia.flow.util

import java.util
import java.util.stream.Collector

import scala.collection.immutable.VectorBuilder

/**
 * Collects items to a vector. May be used when you want to collect java stream items to a scala vector
 * @author Mikko Hilpinen
 * @since 17.11.2019, v1.6.1+
 */
class VectorCollector[A] extends Collector[A, VectorBuilder[A], Vector[A]]
{
	override def supplier() = () => new VectorBuilder[A]
	
	override def accumulator() = (buffer, item) => buffer += item
	
	override def combiner() = (a, b) => { a ++= b.result() }
	
	override def finisher() = buffer => buffer.result()
	
	override def characteristics() = new util.HashSet[Collector.Characteristics]()
}
