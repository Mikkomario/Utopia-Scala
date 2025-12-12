package utopia.flow.collection.mutable.builder

import utopia.flow.collection.immutable.OptimizedIndexedSeq
import utopia.flow.util.MayHaveFailed.{AlwaysSuccess, WrapTry, WrapTryCatch}
import utopia.flow.util.{MayHaveFailed, TryCatch}

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

object TryCatchBuilder
{
	/**
	 * Creates a new TryCatch-builder
	 * @param alwaysSucceed Whether to always yield successful values
	 *                      (default = false = failures will be returned if all input was failed and not empty)
	 * @tparam A Type of the collected successful values
	 * @return A new builder
	 */
	def apply[A](alwaysSucceed: Boolean = false) =
		new TryCatchBuilder[A, IndexedSeq[A]](OptimizedIndexedSeq.newBuilder[A], alwaysSucceed)(_.isEmpty)
	
	/**
	 * Creates a new TryCatch-builder
	 * @param builder Builder that will be used for creating wrapped collection
	 * @tparam A Type of the collected successful values
	 * @tparam Coll Type of the resulting wrapped collection
	 * @return A new builder
	 */
	def wrap[A, Coll <: Iterable[_]](builder: mutable.Builder[A, Coll]) =
		new TryCatchBuilder[A, Coll](builder)(_.isEmpty)
	/**
	 * Creates a new TryCatch-builder which always yields successful values
	 * @param builder Builder that will be used for creating wrapped collection
	 * @tparam A Type of the collected successful values
	 * @tparam Coll Type of the resulting wrapped collection
	 * @return A new builder
	 */
	def wrapAlwaysSucceeding[A, Coll](builder: mutable.Builder[A, Coll]) =
		new TryCatchBuilder[A, Coll](builder, alwaysSucceed = true)(_ => false)
}

/**
 * A builder that generates collections wrapped in [[TryCatch]]
 * @author Mikko Hilpinen
 * @since 08.12.2025, v2.8
 */
class TryCatchBuilder[-A, +Coll](wrapped: mutable.Builder[A, Coll], alwaysSucceed: Boolean = false)
                                (testEmpty: Coll => Boolean)
	extends mutable.Builder[Try[A], TryCatch[Coll]]
{
	// ATTRIBUTES   -----------------------
	
	private val failuresBuilder = OptimizedIndexedSeq.newBuilder[Throwable]
	private var succeedFlag = alwaysSucceed
	
	
	// COMPUTED ---------------------------
	
	/**
	 * @return An interface to this builder which accepts successful results
	 */
	def fromSuccesses: mutable.Builder[A, TryCatch[Coll]] = Successes
	/**
	 * @return An interface to this builder which accepts instances of [[TryCatch]] instead of [[Try]]
	 */
	def catching: mutable.Builder[TryCatch[A], TryCatch[Coll]] = Catching
	/**
	 * @return An interface to this builder that accepts any instances that may have failed
	 */
	def generic: mutable.Builder[MayHaveFailed[A], TryCatch[Coll]] = Generic
	
	
	// IMPLEMENTED  -----------------------
	
	override def clear() = {
		wrapped.clear()
		failuresBuilder.clear()
		succeedFlag = alwaysSucceed
	}
	
	override def result(): TryCatch[Coll] = {
		val items = wrapped.result()
		val failures = failuresBuilder.result()
		
		if (succeedFlag || failures.isEmpty || !testEmpty(items))
			TryCatch.Success(items, failures)
		else
			TryCatch.Failure(failures.head)
	}
	
	override def addOne(elem: Try[A]) = {
		elem match {
			case Success(item) => wrapped += item
			case Failure(error) => failuresBuilder += error
		}
		this
	}
	
	
	// OTHER    -----------------------
	
	/**
	 * Adds an item to this builder
	 * @param item A success or a failure, or a partial failure
	 * @return This builder
	 */
	def +=(item: TryCatch[A]) = {
		item match {
			case TryCatch.Success(item, partialFailures) =>
				wrapped += item
				failuresBuilder ++= partialFailures
			
			case TryCatch.Failure(error) => failuresBuilder += error
		}
		this
	}
	/**
	 * Adds an item to this builder
	 * @param item A success or a failure, or a partial failure
	 * @return This builder
	 */
	def +=(item: MayHaveFailed[A]): TryCatchBuilder[A, Coll] = item match {
		case WrapTry(wrapped) => this += wrapped
		case WrapTryCatch(wrapped) => this += wrapped
		case AlwaysSuccess(value) =>
			wrapped += value
			this
	}
	/**
	 * Adds a failure to this builder
	 * @param failure Failure to add
	 * @return This builder
	 */
	def +=(failure: Throwable) = {
		failuresBuilder += failure
		this
	}
	
	/**
	 * Adds n items or a failure into this builder
	 * @param items Items to add to this builder, or a failure
	 * @return This builder
	 */
	def ++=(items: Try[IterableOnce[A]]) = {
		items match {
			case Success(items) =>
				succeedFlag = true
				wrapped ++= items
				
			case Failure(error) => failuresBuilder += error
		}
		this
	}
	/**
	 * Adds n items or a failure into this builder
	 * @param items Items to add to this builder, or a failure
	 * @return This builder
	 */
	def ++=(items: TryCatch[IterableOnce[A]]) = {
		items match {
			case TryCatch.Success(items, failures) =>
				succeedFlag = true
				wrapped ++= items
				failuresBuilder ++= failures
			
			case TryCatch.Failure(error) => failuresBuilder += error
		}
		this
	}
	/**
	 * Adds n items or a failure into this builder
	 * @param items Items to add to this builder, or a failure
	 * @return This builder
	 */
	def ++=(items: MayHaveFailed[IterableOnce[A]]): TryCatchBuilder[A, Coll] = items match {
		case WrapTry(wrapped) => this ++= wrapped
		case WrapTryCatch(wrapped) => this ++= wrapped
		case AlwaysSuccess(items) =>
			succeedFlag = true
			wrapped ++= items
			this
	}
	
	
	// NESTED   -----------------------
	
	private object Catching extends mutable.Builder[TryCatch[A], TryCatch[Coll]]
	{
		override def addOne(elem: TryCatch[A]) = {
			TryCatchBuilder.this += elem
			this
		}
		
		override def result() = TryCatchBuilder.this.result()
		override def clear() = TryCatchBuilder.this.clear()
	}
	
	private object Successes extends mutable.Builder[A, TryCatch[Coll]]
	{
		override def result(): TryCatch[Coll] = TryCatchBuilder.this.result()
		override def clear(): Unit = TryCatchBuilder.this.clear()
		
		override def addOne(elem: A): Successes.this.type = {
			wrapped += elem
			this
		}
	}
	
	private object Generic extends mutable.Builder[MayHaveFailed[A], TryCatch[Coll]]
	{
		override def addOne(elem: MayHaveFailed[A]): Generic.this.type = {
			TryCatchBuilder.this += elem
			this
		}
		
		override def result() = TryCatchBuilder.this.result()
		override def clear() = TryCatchBuilder.this.clear()
	}
}
