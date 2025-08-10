package utopia.flow.view.immutable.caching

import scala.annotation.unchecked.uncheckedVariance

object MergingLazy
{
	// OTHER    ----------------------
	
	/**
	 * @param left Left side source (lazy)
	 * @param right Right side source (lazy)
	 * @param merge A function that merges both sources
	 * @tparam L Type of left side values
	 * @tparam R Type of right side values
	 * @tparam A Type of merge results
	 * @return A lazy that becomes filled as soon as both sources get filled.
	 *         Caches the merge function results (i.e. won't ever reset).
	 */
	def apply[L, R, A](left: Lazy[L], right: Lazy[R])(merge: (L, R) => A) =
		new MergingLazy[L, R, A](left, right)(merge)
}

/**
 * A lazily initialized container that merges the results of two other lazy containers.
 * The merging function itself is not necessarily performed lazily.
 *
 * @author Mikko Hilpinen
 * @since 10.08.2025, v2.7
 */
class MergingLazy[-L, -R, +A](left: Lazy[L], right: Lazy[R])(f: (L, R) => A) extends Lazy[A]
{
	// ATTRIBUTES   ---------------------
	
	private var cached: Option[A @uncheckedVariance] = None
	
	
	// IMPLEMENTED  ---------------------
	
	// 'current' becomes available as soon as it's available in both sources
	override def current: Option[A] = cached.orElse {
		left.current.flatMap { leftValue =>
			right.current.map { rightValue =>
				val merged = f(leftValue, rightValue)
				cached = Some(merged)
				merged
			}
		}
	}
	override def value: A = cached.getOrElse {
		val result = f(left.value, right.value)
		cached = Some(result)
		result
	}
}
