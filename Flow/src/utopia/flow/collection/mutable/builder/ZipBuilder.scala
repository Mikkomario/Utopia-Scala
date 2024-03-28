package utopia.flow.collection.mutable.builder

import utopia.flow.collection.immutable.Pair
import utopia.flow.view.mutable.Pointer

import scala.annotation.unchecked.uncheckedVariance
import scala.collection.immutable.VectorBuilder
import scala.collection.mutable

object ZipBuilder
{
	/**
	 * Creates a new builder
	 * @param merge A function for merging the two sides of this builder, once a value is available on both
	 * @tparam A Type of the left side input
	 * @tparam B Type of the right side input
	 * @tparam R Type of output / merge result
	 * @return A new builder that merges items from two sides
	 */
	def apply[A, B, R](merge: (A, B) => R) = new ZipBuilder[A, B, R](merge)
	
	/**
	 * Creates a new builder that forms tuples from collected items
	 * @tparam A Type of the left side input / items
	 * @tparam B Type of the right side input / items
	 * @return A new builder that accepts items from two sides
	 */
	def zip[A, B]() = apply[A, B, (A, B)]((l, r) => (l, r))
	/**
	 * Creates a new builder that forms [[Pair]]s from the collected items
	 * @tparam A Type of the items collected
	 * @return A new builder that builds pairs from two sides of input
	 */
	def pair[A]() = apply[A, A, Pair[A]](Pair.apply)
}

/**
 * A builder with two separate inputs. Joins the inputs together, as pairs are formed.
 * @tparam A Type of the left side input
 * @tparam B Type of the right side input
 * @tparam R Type of output / merge result
 * @author Mikko Hilpinen
 * @since 23/03/2024, v2.4
 */
class ZipBuilder[-A, -B, +R](merge: (A, B) => R) extends mutable.Builder[(A, B), Vector[R]]
{
	// ATTRIBUTES   -----------------------
	
	// Stores merge results
	private val resultBuilder: VectorBuilder[R @uncheckedVariance] = new VectorBuilder[R]()
	
	// Stores unmerged left & right side inputs separately
	private val leftBufferPointer: Pointer[List[A @uncheckedVariance]] = Pointer(List[A]())
	private val rightBufferPointer: Pointer[List[B @uncheckedVariance]] = Pointer(List[B]())
	
	/**
	 * An interface for pushing items on the left side
	 */
	val left = new Input[A](new Buffer(leftBufferPointer, rightBufferPointer)(merge))
	/**
	 * An interface for pushing items on the right side
	 */
	val right = new Input[B](new Buffer[B, A](rightBufferPointer, leftBufferPointer)((r, l) => merge(l, r)))
	
	
	// IMPLEMENTED  -----------------------
	
	override def addOne(elem: (A, B)) = {
		left.addOne(elem._1)
		right.addOne(elem._2)
		this
	}
	
	override def result() = resultBuilder.result()
	
	override def clear() = {
		leftBufferPointer.value = List()
		rightBufferPointer.value = List()
		resultBuilder.clear()
	}
	
	
	// OTHER    ---------------------------
	
	/**
	 * Removes and returns all zipped items so far.
	 * Unlike in [[clear]](), however, the unzipped queued items will be preserved.
	 * @return Collected zipped elements
	 */
	def popResult() = {
		val res = result()
		resultBuilder.clear()
		res
	}
	
	
	// NESTED   ---------------------------
	
	/**
	 * Provides an interface for pushing items to one side of this builder
	 * @tparam I Type of the added items
	 */
	class Input[-I] private[ZipBuilder](buffer: Buffer[I, _]) extends mutable.Growable[I]
	{
		override def addOne(elem: I) = {
			buffer.addOne(elem)
			this
		}
		override def addAll(xs: IterableOnce[I]) = {
			buffer.addAll(xs)
			this
		}
		
		override def clear() = ZipBuilder.this.clear()
	}
	
	private class Buffer[I, O](buffer: Pointer[List[I]], opposite: Pointer[List[O]])(merge: (I, O) => R)
		extends mutable.Growable[I]
	{
		override def addOne(elem: I) = {
			opposite.update { buffered =>
				buffered.headOption match {
					// Case: Other side had a buffered item => Extracts that and merges it with this item
					case Some(other) =>
						resultBuilder += merge(elem, other)
						buffered.tail
					// Case: Other side was still empty => Buffers this item
					case None =>
						buffer.update { _ :+ elem }
						buffered
				}
			}
			this
		}
		override def addAll(xs: IterableOnce[I]) = {
			val items = Iterable.from(xs)
			if (items.nonEmpty) {
				// Merges as many of the items for which an opposing side has been buffered
				val keepInput = opposite.mutate { buffered =>
					val (mergeOther, keepOther) = buffered.splitAt(items.size)
					val (mergeInput, keepInput) = items.splitAt(mergeOther.size)
					resultBuilder ++= mergeInput.iterator.zip(mergeOther).map { case (i, o) => merge(i, o) }
					// Keeps the remaining items in one of the buffers
					keepInput -> keepOther
				}
				if (keepInput.nonEmpty)
					buffer.update { _ ++ keepInput }
			}
			this
		}
		
		override def clear() = ZipBuilder.this.clear()
	}
}
