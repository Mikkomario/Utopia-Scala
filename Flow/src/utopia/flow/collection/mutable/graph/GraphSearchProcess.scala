package utopia.flow.collection.mutable.graph

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.graph.{GraphTravelResults, NodeTravelStage}
import utopia.flow.collection.immutable.{Empty, Single}

import scala.annotation.unchecked.uncheckedVariance
import scala.math.Ordered.orderingToOrdered

/**
  * A mutable interface for finding graph search results
  * @param progressIterator An iterator that yields the next set of travel results
  * @param startNode Node to start the search from
  * @param startCost Starting cost to apply
  * @param includeStartAsResult Whether to include 'startNode' as a result (default = false)
  * @param autocomplete Whether to immediately consider the search as completed (default = false)
  * @tparam Node Type of searched nodes
  * @tparam Edge Type of traversed edges
  * @tparam C Type of the accumulated cost
  */
class GraphSearchProcess[+Node, +Edge, C](progressIterator: Iterator[GraphTravelResults[Node, Edge, C]],
                                        startNode: Node, startCost: C,
                                        includeStartAsResult: Boolean = false, autocomplete: Boolean = false)
	extends Iterator[GraphTravelResults[Node, Edge, C]]
{
	// ATTRIBUTES   --------------------------
	
	private val startStage = NodeTravelStage[Node, Edge, C](startNode, Single(Empty), startCost,
		isDestination = includeStartAsResult, isConfirmedAsOptimal = true)
	private var _latestResult: GraphTravelResults[Node @uncheckedVariance, Edge @uncheckedVariance, C] =
		GraphTravelResults(Single(startStage), startCost, foundResults = includeStartAsResult,
			foundAllResults = autocomplete)
	
	
	// COMPUTED ------------------------------
	
	/**
	  * @return The latest acquired result value
	  */
	def current = _latestResult
	
	/**
	  * @return An iterator that first returns the latest acquired result
	  *         and then proceeds to acquire further results, advancing the search process.
	  */
	def resultsIterator = Iterator.single(current) ++ this
	
	/**
	  * May advance this search in order to find a successful search result.
	  * @return The first available successful search result.
	  *         None if this search didn't yield any results.
	  *
	  *         Note: This result might not be complete nor optimal.
	  */
	def anySuccess =
		resultsIterator.findMap { _.successes.headOption }
	/**
	  * May advance this search in order to find successful search results.
	  * @return The first available result which contains all targeted items.
	  *         None if this search didn't yield complete results.
	  *
	  *         Note: This result might not be complete nor optimal.
	  */
	def fullSuccess = resultsIterator.find { _.foundAllResults }
	
	/**
	  * May advance this search in order to find one successful search result
	  * for which an optimal route has been found.
	  * @return The first available search result where the optimal route has been identified.
	  *         None if this search didn't yield any results.
	  */
	def anyOptimalSuccess =
		resultsIterator.findMap { _.successes.find { _.isConfirmedAsOptimal } }
	/**
	  * May complete this search in order to find the optimal results.
	  * @return Optimal search results, if successful. None if partial or unsuccessful.
	  */
	def fullOptimalSuccess =
		resultsIterator.find { r => r.foundAllResults && r.isConfirmedAsOptimal }
	
	
	// IMPLEMENTED  --------------------------
	
	override def hasNext: Boolean = progressIterator.hasNext
	
	override def next(): GraphTravelResults[Node, Edge, C] = {
		// Proceeds with the iterator
		val r = progressIterator.next()
		// Stores the iteration result locally, also
		// May need to add the start completion to the iteration results
		if (includeStartAsResult) {
			val merged = r.copy(stages = startStage +: r.stages, foundResults = true,
				foundAllResults = r.foundAllResults || autocomplete)
			_latestResult = merged
			merged
		}
		else {
			_latestResult = r
			r
		}
	}
	
	
	// OTHER    ---------------------------
	
	/**
	  * Finishes this search process
	  * @return The final search results
	  */
	def finish() = {
		while (hasNext) next()
		current
	}
	
	/**
	  * Finds a search result which is cheaper than the specified cost threshold.
	  * Terminates the search if it becomes impossible to achieve the specified threshold,
	  * therefore making this function more cost-effective than a full search.
	  *
	  * A requirement for finding the optimal results may also be applied.
	  * In this case, this search will continue until the identified search result has been optimized.
	  * Otherwise, this search completes as soon as small-enough cost has been achieved.
	  *
	  * @param costThreshold A cost threshold, under which the result must fall (exclusive).
	  * @param optimize Whether the acquired results should be optimized before returning.
	  *                 If false (default), this search will not attempt to find optimal routes but will
	  *                 accept any route that is cheaper than the specified cost threshold.
	  *                 If true, optimization will be performed after the initial search has succeeded.
	  * @param ord Implicit ordering to use when comparing cost values.
	  *
	  * @return A search result cheaper than the specified cost threshold. None if no such result could be found.
	  */
	def findOneCheaperThan(costThreshold: C, optimize: Boolean = false)(implicit ord: Ordering[C]) =
		findCheaperThan(costThreshold, acceptPartialResults = true, optimize = optimize).flatMap { result =>
			val successView = result.successes.view
			val optionsView = if (optimize) successView.filter { _.isConfirmedAsOptimal } else successView
			optionsView.filter { _.cost < costThreshold }.minByOption { _.cost }
		}
	
	/**
	  * Finds results which are cheaper than the specified cost threshold.
	  * Terminates the search if it becomes impossible to achieve the specified threshold,
	  * therefore making this function more cost-effective than a full search.
	  *
	  * This search may be applied as full (i.e. requiring all search results to be identified)
	  * or as partial (i.e. requiring only that a single cheap-enough result has been identified).
	  * Please note that when performing a full search (default), the result is considered successful even if
	  * it contains too expensive values, as long as they are optimal values
	  * and there exists one or more cheaper values.
	  *
	  * A requirement for finding the optimal results may also be applied.
	  * In this case, this search will continue until the identified search result or results have been optimized.
	  * Otherwise, this search completes as soon as small-enough cost has been achieved.
	  *
	  * @param costThreshold A cost threshold, under which the result must fall (exclusive).
	  * @param acceptPartialResults Whether partial results should be accepted.
	  *                             If true, this search may complete
	  *                             as soon as the first search result has been found.
	  *                             If false (default), all results must be found before this search is completed.
	  * @param optimize Whether the acquired results should be optimized before returning.
	  *                 If false (default), this search will not attempt to find optimal routes but will
	  *                 accept any route that is cheaper than the specified cost threshold.
	  *                 If true, optimization will be performed after the initial search has succeeded.
	  * @param ord Implicit ordering to use when comparing cost values.
	  *
	  * @return Search results which fulfilled the specified conditions.
	  *         None if no such results could be acquired.
	  *
	  *         Note: Even if this function returns None, [[current]] may still contain a semi-successful value
	  *         (just not one which fulfills all the specified conditions).
	  */
	def findCheaperThan(costThreshold: C, acceptPartialResults: Boolean = false,
	                    optimize: Boolean = false)
	                   (implicit ord: Ordering[C]): Option[GraphTravelResults[Node, Edge, C]] =
	{
		// Identifies the first successful result that is cheap enough.
		// If one can't be found, this search terminates.
		resultsIterator.takeTo { _.minFutureCost >= costThreshold }
			.find { _.successes.exists { _.cost < costThreshold } }
			// Applies additional conditions & searches, where appropriate
			.flatMap { preliminary =>
				// Case: Any successful result will do
				if (acceptPartialResults) {
					// Case: The first search is unoptimized and optimization is required
					//       => Continues the search until an optimal result has been acquired
					if (optimize && !preliminary.successes
						.exists { r => r.isConfirmedAsOptimal && r.cost < costThreshold })
						find { _.successes.exists { r => r.isConfirmedAsOptimal && r.cost < costThreshold } }
					// Case: Found an optimal result or sub-optimal results are accepted => Returns this result
					else
						Some(preliminary)
				}
				// Full results are required => Advances the search until all search results have been found
				else
					(Iterator.single(preliminary) ++ this).find { _.foundAllResults }.map { preliminary =>
						// Case: Optimal results are required
						//       => Optimizes the search and returns the optimal results
						if (optimize && !preliminary.isConfirmedAsOptimal)
							find { _.isConfirmedAsOptimal }.getOrElse(current)
						// Case: Optimal results were found or suboptimal results are accepted => Returns
						else
							preliminary
					}
			}
	}
}
