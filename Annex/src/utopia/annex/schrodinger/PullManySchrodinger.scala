package utopia.annex.schrodinger

import utopia.annex.model.manifest.SchrodingerState
import utopia.annex.model.manifest.SchrodingerState.{Alive, Dead}
import utopia.annex.model.response.RequestResult2
import utopia.flow.collection.immutable.Empty
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.Value
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
	lazy val failedAsEmpty = wrap(Fixed(Empty, Failure(new IllegalStateException("Empty results")), Dead))
	
	
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
	def wrap[L, R](pointer: Changing[(Seq[L], Try[Seq[R]], SchrodingerState)]) =
		new PullManySchrodinger[L, R](pointer)
	/**
	  * Creates a schrödinger that has already resolved into a success or a failure
	  * @param results Pull results
	  * @param requireNonEmpty Whether the results should be required (and tested) not to be empty.
	  *                        Default = false = empty results are a success (Alive).
	  * @tparam A Type of individual pulled items
	  * @return A schrödinger with static state (dead or alive)
	  */
	def resolved[A](results: Try[Seq[A]], requireNonEmpty: Boolean = false) =
		wrap(Fixed(Schrodinger.testEmptyState(Empty: Seq[A], results, requireNonEmpty)(Identity)))
	/**
	  * Creates a schrödinger that has already resolved into a success or a failure
	  * @param items         Pulled items
	  * @param requireNonEmpty Whether the result should be required and tested not to be empty.
	  *                        If true, this schrödinger will be a failure (Dead) if the results are empty.
	  * @tparam A Type of individual pulled items
	  * @return A schrödinger with static state (dead or alive)
	  */
	def resolved[A](items: Seq[A], requireNonEmpty: Boolean): PullManySchrodinger[A, A] =
		resolved(Success(items), requireNonEmpty)
	
	/**
	  * Creates a resolved, successful schrödinger
	  * @param items Pulled results
	  * @tparam A Type of individual items
	  * @return A successfully resolved schrödinger
	  */
	def successful[A](items: Seq[A]) = wrap(Fixed((items, Success(items), Alive)))
	/**
	  * @param cause Cause of failure
	  * @tparam A Type of items, if this schrödinger were successful
	  * @return A resolved, failed schrödinger
	  */
	def failed[A](cause: Throwable) = wrap[A, A](Fixed((Empty, Failure(cause), Dead)))
	
	/**
	  * Creates a schrodinger that contains 0-n locally cached values until server results arrive,
	  * from which are parsed a new set of items, if possible.
	  * Typically used when updating locally cached data.
	  * @param local        A result based on local data pull (0-n items as a Seq)
	  * @param resultFuture A future that will contain the server response, if successful
	  * @param emptyIsDead  Whether an empty result (0 items) should be considered Dead and not Alive.
	  *                     Default = false = Result is alive as long as it resolves successfully.
	  * @param localize     A function that converts a remotely pulled instance into its local variant
	  * @param exc          Implicit execution context
	  * @tparam L The locally stored item variants (spirits)
	  * @tparam R The remotely store item variants (instances)
	  * @return A new schrödinger that contains local pull results
	  *         and updates itself once the server-side results arrive.
	  */
	def apply[L, R](local: Seq[L], resultFuture: Future[RequestResult2[Seq[R]]], emptyIsDead: Boolean = false)
	               (localize: R => L)(implicit exc: ExecutionContext) =
		wrap(Schrodinger.getPointer(local, Empty: Seq[R], resultFuture, emptyIsDead) { _.map(localize) })
	
	/**
	  * Creates a schrodinger that contains 0-n locally cached values until server results arrive,
	  * from which are parsed a new set of items, if possible.
	  * Typically used when updating locally cached data.
	  * @param local        A result based on local data pull (0-n items as a Seq)
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
	def pullAndParse[L, R](local: Seq[L], resultFuture: Future[RequestResult2[Value]], parser: FromModelFactory[R],
	                       emptyIsDead: Boolean = false)(localize: R => L)(implicit exc: ExecutionContext) =
		apply(local, resultFuture.map { _.parsingManyWith(parser) }, emptyIsDead)(localize)
	
	@deprecated("Deprecated for removal. Please use .pullAndParse(...) instead", "v1.8")
	def apply[L, R](local: Seq[L], resultFuture: Future[RequestResult2[Value]], parser: FromModelFactory[R])
	               (localize: R => L)(implicit exc: ExecutionContext): PullManySchrodinger[L, R] =
		pullAndParse(local, resultFuture, parser)(localize)
	
	/**
	  * Creates a schrödinger that wraps a remote pull request that yields 0-n items if successful.
	  * This is typically used in situations where no proper local data is available.
	  * @param resultFuture  A future that will yield the server response, provided one is acquired
	  * @param emptyIsDead   Whether 0 items found is considered a failure (Dead).
	  *                      Default = false = Successful request always yields Alive (provided parsing succeeds, also)
	  * @param expectFailure Whether the request is expected to fail (default = false = expected to succeed)
	  * @param exc           Implicit execution context
	  * @tparam A Type of items parsed
	  * @return A new schrödinger that will contain an empty vector until server results arrive
	  */
	def remote[A](resultFuture: Future[RequestResult2[Seq[A]]],
	              emptyIsDead: Boolean = false, expectFailure: Boolean = false)
	             (implicit exc: ExecutionContext) =
		wrap(Schrodinger.remoteGetPointer[Seq[A]](Empty, resultFuture, emptyIsDead, expectFailure))
	
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
	def parseRemote[A](resultFuture: Future[RequestResult2[Value]], parser: FromModelFactory[A],
	                   emptyIsDead: Boolean = false, expectFailure: Boolean = false)
	                  (implicit exc: ExecutionContext) =
		remote[A](resultFuture.map { _.parsingManyWith(parser) }, emptyIsDead, expectFailure)
	
	@deprecated("Deprecated for removal. Please use .parseRemote(...) instead", "v1.8")
	def remote[A](resultFuture: Future[RequestResult2[Value]], parser: FromModelFactory[A])
	             (implicit exc: ExecutionContext): PullManySchrodinger[A, A] =
		parseRemote(resultFuture, parser)
}

/**
  * A schrödinger that represents an attempt to read 0-n items from local and/or remote data.
  * Read items are stored in a Seq. Remote pull is either a success or a failure.
  * @author Mikko Hilpinen
  * @since 23.12.2022, v1.4
  * @tparam L The locally stored item variants (spirits)
  * @tparam R The remotely store item variants (instances)
  */
class PullManySchrodinger[+L, +R](pointer: Changing[(Seq[L], Try[Seq[R]], SchrodingerState)])
	extends Schrodinger[Seq[L], Try[Seq[R]]](pointer)
