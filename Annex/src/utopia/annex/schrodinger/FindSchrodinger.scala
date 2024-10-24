package utopia.annex.schrodinger

import utopia.annex.model.manifest.SchrodingerState
import utopia.annex.model.manifest.SchrodingerState.{Alive, Final}
import utopia.annex.model.response.RequestResult
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.Value
import utopia.flow.operator.Identity
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object FindSchrodinger
{
	// ATTRIBUTES   -----------------------
	
	/**
	  * A schrödinger that has successfully resolved into no results
	  */
	lazy val notFound = successful(None)
	
	
	// OTHER    --------------------------
	
	/**
	  * @param pointer A pointer that always contains 3 elements:
	  *                 1) Local search results,
	  *                 2) Remote search results or a failure (Success(None) while results are pending)
	  *                 3) State (final or flux)
	  * @tparam L Type of local search results
	  * @tparam R Type of remote search results
	  * @return A new schrödinger
	  */
	def wrap[L, R](pointer: Changing[(Option[L], Try[Option[R]], SchrodingerState)]) =
		new FindSchrodinger[L, R](pointer)
	
	/**
	  * Creates a schrödinger that has already resolved
	  * @param result Search result, which may be a failure
	  * @tparam A Type of search results when found
	  * @return A new schrödinger
	  */
	def resolved[A](result: Try[Option[A]]) =
		wrap(Fixed((result.toOption.flatten, result, Final(result.isSuccess))))
	/**
	  * Creates a schrödinger that has successfully resolved into found or not found
	  * @param result Results that were found. None if no results were found.
	  * @tparam A Type of results when found
	  * @return A new schrödinger
	  */
	def successful[A](result: Option[A]) = wrap(Fixed((result, Success(result), Alive)))
	/**
	  * Creates a schrödinger that has successfully found the specified item
	  * @param result The item that was found
	  * @tparam A Type of that item
	  * @return A new schrödinger
	  */
	def found[A](result: A) = successful(Some(result))
	/**
	  * Creates a schrödinger where the search failed
	  * @param cause Cause of failure
	  * @tparam A Type of search results, if they were successful and found
	  * @return A new schrödinger
	  */
	def failed[A](cause: Throwable) = resolved[A](Failure(cause))
	
	/**
	  * Creates a schrödinger that contains 0-1 locally cached values until server results arrive,
	  * from which a new item is parsed, if present and possible.
	  * Typically used when updating locally cached data.
	  * @param local        A result based on local data pull (0-n items as a Vector)
	  * @param resultFuture A future that will contain the server response, if successful
	  * @param emptyIsDead  Whether an empty result (0 items) should be considered Dead and not Alive.
	  *                     Default = false = Result is alive as long as it resolves successfully.
	  * @param localize     A function for converting a remote search result into its local counterpart
	  * @param exc          Implicit execution context
	  * @param log Implicit logging implementation for handling certain unexpected failures.
	  *            For example those thrown by listeners attached to this Schrödinger's pointers.
	  * @tparam L Type of local search results
	  * @tparam R Type of remote search results
	  * @return A new schrödinger that contains local pull results
	  *         and updates itself once the server-side results arrive.
	  */
	def apply[L, R](local: Option[L], resultFuture: Future[RequestResult[Option[R]]], emptyIsDead: Boolean = false)
	               (localize: R => L)(implicit exc: ExecutionContext, log: Logger) =
		wrap(Schrodinger.getPointer[Option[L], Option[R]](local, None, resultFuture, emptyIsDead) { _.map(localize) })
	
	/**
	  * Creates a schrödinger that contains 0-1 locally cached values until server results arrive,
	  * from which a new item is parsed, if present and possible.
	  * Typically used when updating locally cached data.
	  * @param local        A result based on local data pull (0-n items as a Vector)
	  * @param resultFuture A future that will contain the server response, if successful
	  * @param parser       A parser used for parsing items from a server response
	  * @param emptyIsDead  Whether an empty result (0 items) should be considered Dead and not Alive.
	  *                     Default = false = Result is alive as long as it resolves successfully.
	  * @param localize     A function for converting a remote search result into its local counterpart
	  * @param exc          Implicit execution context
	  * @param log Implicit logging implementation for handling certain unexpected failures.
	  *            For example those thrown by listeners attached to this Schrödinger's pointers.
	  * @tparam L Type of local search results
	  * @tparam R Type of remote search results
	  * @return A new schrödinger that contains local pull results
	  *         and updates itself once the server-side results arrive.
	  */
	def findAndParse[L, R](local: Option[L], resultFuture: Future[RequestResult[Value]],
	                       parser: FromModelFactory[R], emptyIsDead: Boolean = false)
	                      (localize: R => L)(implicit exc: ExecutionContext, log: Logger) =
		apply[L, R](local, resultFuture.map { _.parsingOptionWith(parser) }, emptyIsDead)(localize)
	
	@deprecated("Deprecated for removal. Please use .findAndParse(...) instead", "v1.8")
	def apply[L, R](local: Option[L], resultFuture: Future[RequestResult[Value]], parser: FromModelFactory[R])
	               (localize: R => L)(implicit exc: ExecutionContext, log: Logger): FindSchrodinger[L, R] =
		findAndParse(local, resultFuture, parser)(localize)
	
	/**
	  * Creates a schrödinger that either **permanently** contains the local search result, if found, or
	  * performs a remote search and contains the server results once they arrive.
	  * In other words, remote search is only performed if no local results are found.
	  * This type of schrödinger is typically used when the local data is expected to be accurate, when present.
	  * @param local       Local search results
	  * @param emptyIsDead Whether an empty result (0 items) should be considered Dead and not Alive.
	  *                    Default = false = Result is alive as long as it resolves successfully.
	  * @param makeRequest A function for starting a remote search.
	  *                    Yields a future that contains the result of this request.
	  * @param exc         Implicit execution context
	  * @param log Implicit logging implementation for handling certain unexpected failures.
	  *            For example those thrown by listeners attached to this Schrödinger's pointers.
	  * @tparam A Type of search results, when found
	  * @return A new schrödinger, either based on local results (final) or remote search (flux)
	  */
	def findAny[A](local: Option[A], emptyIsDead: Boolean = false)(makeRequest: => Future[RequestResult[Option[A]]])
	              (implicit exc: ExecutionContext, log: Logger) =
	{
		// Case: Local results found => Skips the remote search
		if (local.isDefined)
			successful(local)
		// Case: No local results => Performs the remote search
		else
			apply(local, makeRequest, emptyIsDead)(Identity)
	}
	
	/**
	  * Creates a schrödinger that either **permanently** contains the local search result, if found, or
	  * performs a remote search and contains the server results once they arrive.
	  * In other words, remote search is only performed if no local results are found.
	  * This type of schrödinger is typically used when the local data is expected to be accurate, when present.
	  * @param local       Local search results
	  * @param parser      A parser used for handling server-side results
	  * @param emptyIsDead Whether an empty result (0 items) should be considered Dead and not Alive.
	  *                    Default = false = Result is alive as long as it resolves successfully.
	  * @param makeRequest A function for starting a remote search.
	  *                    Yields a future that contains the result of this request.
	  * @param exc         Implicit execution context
	  * @param log Implicit logging implementation for handling certain unexpected failures.
	  *            For example those thrown by listeners attached to this Schrödinger's pointers.
	  * @tparam A Type of search results, when found
	  * @return A new schrödinger, either based on local results (final) or remote search (flux)
	  */
	def findAndParseAny[A](local: Option[A], parser: FromModelFactory[A], emptyIsDead: Boolean = false)
	                      (makeRequest: => Future[RequestResult[Value]])
	                      (implicit exc: ExecutionContext, log: Logger) =
		findAny[A](local, emptyIsDead) { makeRequest.map { _.parsingOptionWith(parser) } }
	
	@deprecated("Deprecated for removal. Please use .findAndParseAny(...) instead", "v1.8")
	def findAny[A](local: Option[A], parser: FromModelFactory[A])(makeRequest: => Future[RequestResult[Value]])
	              (implicit exc: ExecutionContext, log: Logger): FindSchrodinger[A, A] =
		findAndParseAny(local, parser)(makeRequest)
	
	/**
	  * Creates a schrödinger that wraps a remote search request which yields 0-1 items when successful.
	  * This is typically used when no appropriate local data is available.
	  * @param resultFuture  A future that will contain the server response, if successful
	  * @param emptyIsDead   Whether an empty result (0 items) should be considered Dead and not Alive.
	  *                      Default = false = Result is alive as long as it resolves successfully.
	  * @param expectFailure Whether the request is expected to fail (default = false = expected to succeed)
	  * @param exc           Implicit execution context
	  * @param log Implicit logging implementation for handling certain unexpected failures.
	  *            For example those thrown by listeners attached to this Schrödinger's pointers.
	  * @tparam A Type of found item
	  * @return A new schrödinger that will contain None until server results arrive
	  */
	def findRemote[A](resultFuture: Future[RequestResult[Option[A]]], emptyIsDead: Boolean = false,
	                  expectFailure: Boolean = false)
	                 (implicit exc: ExecutionContext, log: Logger) =
		wrap(Schrodinger.remoteGetPointer[Option[A]](None, resultFuture, emptyIsDead, expectFailure))
	
	/**
	  * Creates a schrödinger that wraps a remote search request which yields 0-1 items when successful.
	  * This is typically used when no appropriate local data is available.
	  * @param resultFuture  A future that will contain the server response, if successful
	  * @param parser        A parser used for parsing items from a server response
	  * @param emptyIsDead   Whether an empty result (0 items) should be considered Dead and not Alive.
	  *                      Default = false = Result is alive as long as it resolves successfully.
	  * @param expectFailure Whether the request is expected to fail (default = false = expected to succeed)
	  * @param exc           Implicit execution context
	  * @param log Implicit logging implementation for handling certain unexpected failures.
	  *            For example those thrown by listeners attached to this Schrödinger's pointers.
	  * @tparam A Type of found item
	  * @return A new schrödinger that will contain None until server results arrive
	  */
	def findAndParseRemote[A](resultFuture: Future[RequestResult[Value]], parser: FromModelFactory[A],
	                          emptyIsDead: Boolean = false, expectFailure: Boolean = false)
	                         (implicit exc: ExecutionContext, log: Logger) =
		findRemote[A](resultFuture.map { _.parsingOptionWith(parser) }, emptyIsDead, expectFailure)
	
	@deprecated("Deprecated for removal. Please use .findAndParseRemote(...) instead", "v1.8")
	def findRemote[A](resultFuture: Future[RequestResult[Value]], parser: FromModelFactory[A])
	                 (implicit exc: ExecutionContext, log: Logger): FindSchrodinger[A, A] =
		findAndParseRemote(resultFuture, parser)
}

/**
  * A schrödinger that wraps an attempt to find an item from local and/or remote data.
  * @author Mikko Hilpinen
  * @since 24.12.2022, v1.4
  * @tparam L Type of local (manifest) search results, when found
  * @tparam R Type of remote (result) search results, when found
  */
class FindSchrodinger[+L, +R](pointer: Changing[(Option[L], Try[Option[R]], SchrodingerState)])
	extends Schrodinger[Option[L], Try[Option[R]]](pointer)
