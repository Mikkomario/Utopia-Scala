package utopia.annex.schrodinger

import utopia.annex.model.manifest.SchrodingerState
import utopia.annex.model.manifest.SchrodingerState.{Alive, Dead}
import utopia.annex.model.response.RequestResult
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.operator.Identity
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object PullManySchrodinger
{
	// ATTRIBUTES   ----------------------------
	
	/**
	  * A schrödinger that has failed because it was empty
	  */
	lazy val failedAsEmpty = wrap(Fixed(Vector(), Failure(new IllegalStateException("Empty results")), Dead))
	
	
	// CONSTRUCTOR  ----------------------------
	
	/**
	  * Wraps a pointer
	  * @param pointer A pointer that always contains 3 elements:
	  *                 1) Current items (may be empty)
	  *                 2) Pull result (placeholder or final), may be failure
	  *                 3) State (flux or final)
	  * @tparam L The locally stored item variants (spirits)
	  * @tparam R The remotely store item variants (instances)
	  * @return A new schrödinger that wraps the specified pointer
	  */
	def wrap[L, R](pointer: Changing[(Vector[L], Try[Vector[R]], SchrodingerState)]) =
		new PullManySchrodinger[L, R](pointer)
	/**
	  * Creates a schrödinger that has already resolved into a success or a failure
	  * @param results Pull results
	  * @param requireNonEmpty Whether the results should be required (and tested) not to be empty.
	  *                        Default = false = empty results are a success (Alive).
	  * @tparam A Type of individual pulled items
	  * @return A schrödinger with static state (dead or alive)
	  */
	def resolved[A](results: Try[Vector[A]], requireNonEmpty: Boolean = false) =
		wrap(Fixed(Schrodinger2.testEmptyState(Vector[A](), results, requireNonEmpty)(Identity)))
	/**
	  * Creates a schrödinger that has already resolved into a success or a failure
	  * @param items         Pulled items
	  * @param requireNonEmpty Whether the result should be required and tested not to be empty.
	  *                        If true, this schrödinger will be a failure (Dead) if the results are empty.
	  * @tparam A Type of individual pulled items
	  * @return A schrödinger with static state (dead or alive)
	  */
	def resolved[A](items: Vector[A], requireNonEmpty: Boolean): PullManySchrodinger[A, A] =
		resolved(Success(items), requireNonEmpty)
	
	/**
	  * Creates a resolved, successful schrödinger
	  * @param items Pulled results
	  * @tparam A Type of individual items
	  * @return A successfully resolved schrödinger
	  */
	def successful[A](items: Vector[A]) = wrap(Fixed((items, Success(items), Alive)))
	/**
	  * @param cause Cause of failure
	  * @tparam A Type of items, if this schrödinger were successful
	  * @return A resolved, failed schrödinger
	  */
	def failed[A](cause: Throwable) = wrap[A, A](Fixed((Vector(), Failure(cause), Dead)))
	
	/**
	  * Creates a schrodinger that contains 0-n locally cached values until server results arrive,
	  * from which are parsed a new set of items, if possible.
	  * Typically used when updating locally cached data.
	  * @param local        A result based on local data pull (0-n items as a Vector)
	  * @param resultFuture A future that will contain the server response, if successful
	  * @param parser       A parser used for parsing items from a server response
	  * @param emptyIsDead  Whether an empty result (0 items) should be considered Dead and not Alive.
	  *                     Default = false = Result is alive as long as it resolves successfully.
	  * @param localize     A function that converts a remotely pulled instance into its local variant
	  * @param exc          Implicit execution context
	  * @tparam L The locally stored item variants (spirits)
	  * @tparam R The remotely store item variants (instances)
	  * @return A new schrödinger that contains local pull results
	  *         and updates itself once the server-side results arrive.
	  */
	def apply[L, R](local: Vector[L], resultFuture: Future[RequestResult], parser: FromModelFactory[R],
	                emptyIsDead: Boolean = false)(localize: R => L)(implicit exc: ExecutionContext) =
		wrap(Schrodinger2.getPointer(local, Vector[R](), resultFuture, emptyIsDead) { _.parseMany(parser) } {
			_.map(localize) })
	
	/**
	  * Creates a schrödinger that wraps a remote pull request that yields 0-n items if successful.
	  * This is typically used in situations where no proper local data is available.
	  * @param resultFuture  A future that will yield the server response, provided one is acquired
	  * @param parser        A parser for reading items from the response
	  * @param emptyIsDead   Whether 0 items found is considered a failure (Dead).
	  *                      Default = false = Successful request always yields Alive (provided parsing succeeds, also)
	  * @param expectFailure Whether the request is expected to fail (default = false = expected to succeed)
	  * @param exc           Implicit execution context
	  * @tparam A Type of items parsed
	  * @return A new schrödinger that will contain an empty vector until server results arrive
	  */
	def remote[A](resultFuture: Future[RequestResult], parser: FromModelFactory[A],
	              emptyIsDead: Boolean = false, expectFailure: Boolean = false)
	             (implicit exc: ExecutionContext) =
		wrap(Schrodinger2.remoteGetPointer[Vector[A]](Vector(), resultFuture, emptyIsDead,
			expectFailure) { _.parseMany(parser) })
}

/**
  * A schrödinger that represents an attempt to read 0-n items from local and/or remote data.
  * Read items are stored in a Vector. Remote pull is either a success or a failure.
  * @author Mikko Hilpinen
  * @since 23.12.2022, v1.4
  * @tparam L The locally stored item variants (spirits)
  * @tparam R The remotely store item variants (instances)
  */
class PullManySchrodinger[+L, +R](pointer: Changing[(Vector[L], Try[Vector[R]], SchrodingerState)])
	extends Schrodinger2[Vector[L], Try[Vector[R]]](pointer)
