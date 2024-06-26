package utopia.flow.collection.template

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.caching.LazyTree
import utopia.flow.collection.immutable.caching.iterable.CachingSeq
import utopia.flow.collection.immutable.{Empty, Graph, Pair, Single}
import utopia.flow.collection.mutable.iterator.OrderedDepthIterator
import utopia.flow.collection.template.GraphNode.{AnyNode, GraphSearchProcess, PathsFinder}
import utopia.flow.operator.{Identity, MaybeEmpty}
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy

import scala.collection.mutable
import scala.math.Ordered.orderingToOrdered

object GraphNode
{
	// TYPES    ------------------------
	
	type AnyNode = GraphNode[_, _, _, _]
	type AnyEdge = GraphEdge[_, _]
	
	
	// NESTED   ------------------------
	
	/**
	  * Represents an advancement in a graph search process.
	  * May represent a completed or an ongoing search.
	  * @param node Last visited node. If 'isDestination' is true, this is a searched node.
	  * @param routes The currently discovered cheapest routes to that node. Contains 1 or more entries.
	  *               If 'isConfirmedAsOptimal' is true, these are the actual cheapest routes to this node.
	  * @param cost The cost of traversing these routes
	  * @param isDestination Whether this represents a search destination.
	  *                      False if this represents potential progress in the node-search process instead.
	  * @param isConfirmedAsOptimal True if 'routes' has been confirmed to be the optimal / cheapest routes to 'node'.
	  *                             If false, another route or routes may be discovered,
	  *                             which are at least equally as valid.
	  *
	  *                             Note: 'routes' may still be the optimal set or routes, even if this value is false,
	  *                             but that wouldn't be for certain.
	  * @tparam Node Type of nodes searched
	  * @tparam Edge Type of edges traversed
	  * @tparam C Type of cost used
	  */
	case class NodeTravelStage[+Node, Edge, +C](node: Node, routes: Set[Seq[Edge]], cost: C,
	                                             isDestination: Boolean = false, isConfirmedAsOptimal: Boolean = false)
	{
		// COMPUTED -------------------------
		
		/**
		  * @return Whether this represents a temporary searching state, not a search result
		  */
		def isSearching = !isDestination
		/**
		  * @return Whether the routes to this node may still be improved upon
		  */
		def mayBeSuboptimal = !isConfirmedAsOptimal
		
		/**
		  * @return Any of the discovered routes to the targeted node
		  */
		def anyRoute = routes.head
		
		
		// IMPLEMENTED  -------------------
		
		override def toString = {
			if (isDestination) {
				if (isConfirmedAsOptimal)
					s"Found ${routes.size} optimal route(s) to $node (cost $cost)"
				else
					s"Found ${ routes.size } potential routes(s) to $node (cost $cost)"
			}
			else
				s"Arrived to $node from ${ routes.size } route(s) with cost $cost"
		}
	}
	
	/**
	  * Represents a result (either preliminary or final) in a graph search process.
	  * @param stages A sequence which contains both acquired results (which might not be optimal),
	  *               as well as stateful information concerning the progress in traversing through the graph.
	  *               The first values listed represent actual successful results.
	  *               After that are listed values which represent temporary / preliminary search progress.
	  * @param minFutureCost Smallest cost that may be acquired for any future result.
	  *                                     It is not necessarily possible to acquire this result,
	  *                                     but it is certain that there won't be any additional results where the
	  *                                     cost would be less than this
	  *                                     (assuming that the cost function always returns a non-negative value,
	  *                                     and that the cost function can technically return a zero value).
	  * @param foundResults Whether ANY search results have been identified yet.
	  *                     For searches that target a singular node, this indicates
	  *                     whether some routes to that node has been found.
	  *                     Note, however, that the acquired route might not be the cheapest / optimal route,
	  *                     unless 'isConfirmedAsOptimal' is also set to true
	  *                     in either this instance or within the result itself.
	  * @param foundAllResults Whether ALL search results have been identified.
	  *                        Has different meanings between different search styles:
	  *                             - When exclusive search mode is used (i.e. one result per one search),
	  *                             contains true when and only when a result has been found
	  *                             for each defined search function.
	  *                             - When the search is inclusive
	  *                             (i.e. allowing multiple results for each search function),
	  *                             contains true only once the whole graph has been traversed.
	  *
	  *                        Please note that even when this is true, more optimal routes may still be discovered,
	  *                        unless otherwise indicated by 'isConfirmedAsOptimal' in this instance or within the
	  *                        results themselves.
	  * @param isConfirmedAsOptimal Whether this result has been confirmed to be the optimal and final state.
	  *                             Contains false as long as it is technically possible to find more optimal routes
	  *                             to the targeted nodes.
	  * @tparam Node Type of searched graph nodes
	  * @tparam Edge Type of traversed edges
	  * @tparam C Type of cost used
	  */
	// NB: Assumes that successful results are always listed before the "traversing" data
	case class GraphTravelResults[+Node, Edge, C] private(stages: Seq[NodeTravelStage[Node, Edge, C]],
	                                                      minFutureCost: C, foundResults: Boolean = false,
	                                                      foundAllResults: Boolean = false,
	                                                      isConfirmedAsOptimal: Boolean = false)
		extends MaybeEmpty[GraphTravelResults[Node, Edge, C]]
	{
		// ATTRIBUTES   ------------------------
		
		/**
		  * Results which identify a searched node
		  */
		lazy val successes = if (foundResults) stages.takeWhile { _.isDestination } else Empty
		/**
		  * Results which identify a searched node and have identified the optimal route/routes to that node.
		  */
		lazy val optimalSuccesses = successes.filter { _.isConfirmedAsOptimal }
		
		/**
		  * @return Stages which represent temporary progress through the traversed graph
		  */
		lazy val temporaryStages = if (foundResults) stages.dropWhile { _.isDestination } else stages
		
		
		// COMPUTED ----------------------------
		
		/**
		  * @return Whether this result is still partial.
		  *         I.e. whether this result is still possibly missing some search results.
		  */
		def isPartial = !foundAllResults
		/**
		  * @return Whether this result may still contain sub-optimal routes
		  */
		def mayBeSuboptimal = !isConfirmedAsOptimal
		
		/**
		  * @return Any successful result found. Same as calling [[successes]].headOption
		  */
		def any = successes.headOption
		
		/**
		  * @return Routes to any successfully identified search result.
		  *         Empty set if no successful results were available.
		  */
		def anyRoutes = any match {
			case Some(success) => success.routes
			case None => Set.empty[Seq[Edge]]
		}
		/**
		  * @return Any successfully identified route to any identified search result.
		  *         None if no search results were found.
		  */
		def anyRoute = any.map { _.routes.head }
		
		/**
		  * @tparam N2 Type of returned keys
		  * @return A map which contains an entry for each successful search result.
		  *         Map keys are identified nodes. Map values consist of 2 parts:
		  *             1) Cheapest identified routes to the targeted node and,
		  *             2) The cost of these routes
		  */
		def toRouteMap[N2 >: Node]: Map[N2, (Set[Seq[Edge]], C)] =
			successes.view.map { r => r.node -> (r.routes -> r.cost) }.toMap
		
		/**
		  * @param ord Implicit cost-value ordering to use
		  * @return Successful result which contained the cheapest route.
		  *         None if no successes were available.
		  */
		def cheapest(implicit ord: Ordering[C]) =
			successes.minByOption { _.cost }
		
		
		// IMPLEMENTED  -----------------------
		
		override def self: GraphTravelResults[Node, Edge, C] = this
		
		override def isEmpty: Boolean = !foundResults
		
		override def toString = {
			if (foundAllResults) {
				if (isConfirmedAsOptimal)
					s"Optimal results found: [${ successes.mkString(", ") }]"
				else
					s"Optimizing results: [${ successes.mkString(", ") }], traversing: [${
						temporaryStages.mkString(", ") }]"
			}
			else if (foundResults) {
				if (isConfirmedAsOptimal)
					s"Found partial results: [${ successes.mkString(", ") }]"
				else
					s"Found ${ successes.size } result(s): [${ successes.mkString(", ") }], searching with: [${
						temporaryStages.mkString(", ") }]"
			}
			else if (isConfirmedAsOptimal)
				"Search finished without results"
			else
				s"Searching with [${ temporaryStages.mkString(", ") }]"
		}
	}
	
	class GraphSearchProcess[Node, Edge, C](progressIterator: Iterator[GraphTravelResults[Node, Edge, C]],
	                                        startNode: Node, startCost: C,
	                                        includeStartAsResult: Boolean = false, autocomplete: Boolean = false)
		extends Iterator[GraphTravelResults[Node, Edge, C]]
	{
		// ATTRIBUTES   --------------------------
		
		private val startStage = NodeTravelStage[Node, Edge, C](startNode, Set(Empty), startCost,
			isDestination = includeStartAsResult, isConfirmedAsOptimal = true)
		private var _latestResult = GraphTravelResults(Single(startStage), startCost,
			foundResults = includeStartAsResult, foundAllResults = autocomplete)
		
		
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
		  * Otherwise this search completes as soon as small-enough cost has been achieved.
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
		  * Otherwise this search completes as soon as small-enough cost has been achieved.
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
							// Case: Optimal results were found or sub-optimal results are accepted => Returns
							else
								preliminary
						}
				}
		}
	}
	
	private class PathsFinder[N, E, GNode <: GraphNode[N, E, GNode, Edge], Edge <: GraphEdge[E, GNode], C]
	(start: GNode, destinations: Iterable[GNode => Boolean], startCost: C, exclusive: Boolean = true)
	(costOf: Edge => C)(sumOf: (C, C) => C)
	(implicit ord: Ordering[C])
	{
		// COMPUTED --------------------
		
		def iterator: GraphSearchProcess[GNode, Edge, C] = {
			if (destinations.isEmpty)
				new GraphSearchProcess(Iterator.empty, start, startCost, includeStartAsResult = false,
					autocomplete = true)
			else {
				val (searchedDestinations, addStartResult) = {
					// Case: Performing an exclusive search (i.e. single result per search)
					//       => Excludes searches if they match the starting node
					if (exclusive) {
						val incompleteDestinations = destinations.filterNot { _(start) }
						incompleteDestinations -> (incompleteDestinations.hasSize < destinations)
					}
					// Case: Performing an inclusive search => May still include the starting node in the results
					else
						destinations -> destinations.exists { _(start) }
				}
				val iter = {
					// Case: All destinations are already reached => No need to perform any search
					if (searchedDestinations.isEmpty)
						Iterator.empty
					// Case: An actual search is required
					else
						new FinderIterator(searchedDestinations, exclusive)
				}
				new GraphSearchProcess(iter, start, startCost, includeStartAsResult = addStartResult,
					autocomplete = searchedDestinations.isEmpty)
			}
		}
		
		
		// NESTED   -------------------------
		
		private class PathFinder(val currentNode: GNode, val currentCost: C, val pathHistory: Set[Seq[Edge]])
		{
			def next(blockedNodes: scala.collection.Set[AnyNode]) = {
				currentNode.leavingEdges.view.filterNot { e => blockedNodes.contains(e.end) }.map { edge =>
					new PathFinder(edge.end, sumOf(currentCost, costOf(edge)), pathHistory.map { _ :+ edge })
				}
			}
		}
		
		private class FinderIterator(destinations: Iterable[GNode => Boolean], isExclusive: Boolean)
			extends Iterator[GraphTravelResults[GNode, Edge, C]]
		{
			// ATTRIBUTES   ----------------
			
			// Contains nodes from which a path finder has LEFT
			private val blockedNodesBuffer = mutable.Set[AnyNode]()
			// Contains an entry for each encountered search result. The contained values may not be the final results.
			private val resultsBuffer = mutable.Map[GNode, (Set[Seq[Edge]], C)]()
			
			// Prepared path finders for the next iteration
			private var nextOrigins: Map[GNode, PathFinder] = Map(start -> new PathFinder(start, startCost, Set(Empty)))
			// Smallest achievable cost for the next iteration,
			// assuming that cost function always returns a positive (> 0) value
			private var nextMinCost = startCost
			// Destination search functions for which no nodes have been found
			// In 'inclusive' mode, this will remain as 'destinations' throughout the whole process
			private var remainingDestinations = destinations
			// Destination search functions, discovered routes, achieved cost values, plus found node,
			// Listed for cases where a node has been found, but where a better result may still be achieved
			// Only filled in 'exclusive' mode
			private var unprovenDestinations: Iterable[(GNode => Boolean, Set[Seq[Edge]], C, GNode)] = Empty
			private val provenDestinationsBuffer = mutable.Set[AnyNode]()
			
			// Contains true once the search process has completed
			private var completed = false
			
			
			// IMPLEMENTED  ----------------
			
			override def hasNext: Boolean = !completed
			
			override def next() = {
				// Selects the next iteration origins - Nodes with the lowest current cost
				val (delayedOrigins, iterationOrigins) = nextOrigins
					.divideBy { case (_, finder) => ord.equiv(finder.currentCost, nextMinCost) }.toTuple
				
				// Will not allow arriving to a node from which we left already (now or in the past),
				// because that can only increase the route cost
				blockedNodesBuffer ++= iterationOrigins.keys
				
				// Takes the next step and merges routes that arrived to the same node
				// Will reject new arrivals to nodes visited earlier with a better cost
				val newFinders = iterationOrigins.values.flatMap { _.next(blockedNodesBuffer) }
					.groupBy { _.currentNode }
					.flatMap { case (location, finders) =>
						val bestNewCost = finders.map { _.currentCost }.min
						// Only recognizes the new results if they were better than some previously encountered origins
						// In which case either merges or discards the previous origins
						val earlierResults = nextOrigins.get(location)
						// Case: Found better or at least as good routes to the already discovered nodes
						if (earlierResults.forall { _.currentCost >= bestNewCost }) {
							// Includes the previously found "origins" in the merging process
							val bestFinders = (finders ++ earlierResults)
								.filter { f => ord.equiv(f.currentCost, bestNewCost) }
							// Case: Only one finder arrived to this location => picks it as it is
							if (bestFinders hasSize 1)
								Some(bestFinders.head)
							// Case: Merging => Takes the finders of smallest cost and combines them
							else
								Some(bestFinders.reduce { (a, b) =>
									new PathFinder(location, a.currentCost, a.pathHistory ++ b.pathHistory)
								})
						}
						// Case: The new routes were more expensive than the ones found before => Discards them
						else
							None
					}
				
				
				// Updates the next origins and the minimum cost value
				nextOrigins = delayedOrigins ++ newFinders.map { f => f.currentNode -> f }
				if (nextOrigins.nonEmpty)
					nextMinCost = nextOrigins.valuesIterator.map { _.currentCost }.min
				
				// Case: Iteration yielded new routes => updates results and destinations, etc.
				if (newFinders.nonEmpty) {
					// Checks whether already arrived to some or all destinations
					// Found destinations contains 4 parts:
					//      1) The original destination function
					//      2) Destination node
					//      3) Routes
					//      4) Cost
					val (nextDestinations, foundDestinations) = remainingDestinations.flatDivideWith { destination =>
						val arrived = newFinders.filter { o => destination(o.currentNode) }
						// Case: No finder arrived to this destination yet => Keeps it as a remaining destination
						if (arrived.isEmpty)
							Single(Left(destination))
						// Case: One or more finders arrived to this destination
						//       => Creates a summary of the (currently) best results
						else if (isExclusive) {
							val bestResults = arrived.filterMinBy { _.currentCost }.toSeq
							val bestResult = bestResults.head
							val routes = (bestResults.tail.filter { _.currentNode == bestResult.currentNode } :+
								bestResult)
								.view.flatMap { _.pathHistory }.toSet
							// The latest result may still be improved upon
							Single(Right((destination, routes, bestResult.currentCost, bestResult.currentNode)))
						}
						// Case: One or more finders arrived to a node identified by this destination
						//       => Determines the best result for each encountered node
						else
							arrived.groupBy { _.currentNode }.map { case (node, arrived) =>
								// WET WET
								val bestResults = arrived.filterMinBy { _.currentCost }.toSeq
								val bestResult = bestResults.head
								val routes = bestResults.view.flatMap { _.pathHistory }.toSet
								Right((destination, routes, bestResult.currentCost, node))
							}
					}
					// Checks whether it's possible to get a better result than one already found
					// Results are considered "unproven"
					// until the acquired minimum cost exceeds that found for the destination
					// Note: Only used in "exclusive" mode where each destination corresponds with a single node
					if (exclusive) {
						val (newlyProvenDestinations, remainsUnprovenDestinations) = unprovenDestinations
							.divideBy { nextMinCost <= _._3 }.toTuple
						// Remembers the proven destinations in order to prevent their removal
						provenDestinationsBuffer ++= newlyProvenDestinations.view.map { _._4 }
						// Each entry contains 4 values:
						//      1) New result node
						//      2) New result routes
						//      3) New best cost
						//      4) Node to possibly remove from the results
						val updatedUnprovenResults = remainsUnprovenDestinations
							.map { case (destination, previousRoutes, previousMinCost, previousNode) =>
								// Finds new search results which are better or as good as the results found before
								val arrived = newFinders
									.filter { o => destination(o.currentNode) }
									.filter { _.currentCost <= previousMinCost }
								// Case: New competing results found
								//       => Merges them to the previous results or overrides previous results with them
								if (arrived.nonEmpty) {
									val bestResults = arrived.filterMinBy { _.currentCost }
									val bestResult = bestResults.find { _.currentNode == previousNode }
										.getOrElse(bestResults.head)
									val newNode = bestResult.currentNode
									val discoveredRoutes = bestResults
										.filter { _.currentNode == newNode }.flatMap { _.pathHistory }
									
									// Case: The new results are better than those found before
									//       => Replaces the old results
									if (bestResult.currentCost < previousMinCost) {
										// If targeted different nodes, may remove the other node from the results
										(destination, newNode, discoveredRoutes.toSet, bestResult.currentCost,
											Some(previousNode).filterNot { _ == newNode })
									}
									// Case: The new results are equally as good as the previous
									//       => Merges them if they're for the same node, otherwise ignores them
									else if (newNode == previousNode)
										(destination, previousNode, previousRoutes ++ discoveredRoutes,
											previousMinCost, None)
									else
										(destination, previousNode, previousRoutes, previousMinCost, None)
								}
								// Case: No competing results found => Keeps the previous entry
								else
									(destination, previousNode, previousRoutes, previousMinCost, None)
							}
						
						if (updatedUnprovenResults.nonEmpty) {
							// Performs result-removal first, if appropriate
							resultsBuffer --= updatedUnprovenResults.view.flatMap { _._5 }.filterNot { node =>
								provenDestinationsBuffer.contains(node) || updatedUnprovenResults.exists { _._2 == node }
							}
							// Next adds the updated results to the results buffer
							resultsBuffer ++= updatedUnprovenResults
								.view.map { case (_, node, routes, cost, _) => node -> (routes -> cost) }
						}
						
						// Updates the remaining destinations
						remainingDestinations = nextDestinations
						unprovenDestinations = updatedUnprovenResults
							.map { case (destination, node, routes, cost, _) => (destination, routes, cost, node) } ++
							foundDestinations
					}
					
					// Adds new results to the buffer
					if (foundDestinations.nonEmpty)
						resultsBuffer ++= foundDestinations
							.map { case (_, routes, cost, node) => node -> (routes -> cost) }
				}
				
				// Checks for completion
				// Completes if either:
				//      1) There are no more nodes to travel through, OR
				//      2) All destinations have been identified, AND
				//          3) It is impossible to achieve an equal or better cost for any destination
				// Tests condition #1
				completed = nextOrigins.isEmpty ||
					// Tests for #2
					(remainingDestinations.isEmpty &&
						// Tests for #3
						unprovenDestinations.view.map { _._3 }.maxOption.forall { _ <= nextMinCost })
				
				// Converts the search results into graph travel results
				// Result-based stages are processed immediately because the results collection is mutable
				val resultStages = resultsBuffer.view
					.map { case (node, (routes, cost)) =>
						NodeTravelStage(node, routes, cost, isDestination = true,
							isConfirmedAsOptimal = completed || cost <= nextMinCost)
					}
					.toVector
				// Other stages are added only when requested
				val pathFinderStagesIterator = nextOrigins.valuesIterator.map { pf =>
					NodeTravelStage(pf.currentNode, pf.pathHistory, pf.currentCost,
						isConfirmedAsOptimal = completed || pf.currentCost <= nextMinCost)
				}
				
				GraphTravelResults(
					stages = CachingSeq(pathFinderStagesIterator, resultStages),
					minFutureCost = nextMinCost,
					foundResults = resultsBuffer.nonEmpty,
					foundAllResults = if (isExclusive) remainingDestinations.isEmpty else completed,
					isConfirmedAsOptimal = completed)
			}
		}
	}
}

/**
 * Graph nodes contain content and are connected to other graph nodes via edges
 * @author Mikko Hilpinen
 * @since 10.4.2019
 */
trait GraphNode[N, E, GNode <: GraphNode[N, E, GNode, Edge], Edge <: GraphEdge[E, GNode]]
	extends View[N]
{
    // TYPES    --------------------
	
    type Route = Seq[Edge]
    
    
    // ABSTRACT --------------------
	
	/**
	  * @return The edges leaving this node.
	  */
    def leavingEdges: Iterable[Edge]
	
	/**
	  * @return This node
	  */
	def self: GNode
    
    
    // COMPUTED PROPERTIES    -------
	
	/**
	  * The nodes accessible from this node
	  */
	def endNodes = leavingEdges.map { _.end }.distinctBy(Identity)
	
	/**
	  * @return An iterator that returns all nodes within this graph, starting with this one.
	  *         The iterator is not specifically ordered, but iterates by traversing through this graph.
	  *         See: .orderedAllNodesIterator if you want an ordered iterator.
	  */
	def allNodesIterator = {
		val visitedNodes = mutable.Set[Any](this)
		_allNodesIterator(visitedNodes)
	}
	private def _allNodesIterator(visitedNodes: mutable.Set[Any]): Iterator[GNode] = {
		Iterator.single(self) ++ leavingEdges.iterator.flatMap { edge =>
			val node = edge.end
			if (visitedNodes.contains(node))
				None
			else {
				visitedNodes += node
				node._allNodesIterator(visitedNodes)
			}
		}
	}
	/**
	  * @return An iterator that returns all nodes within this graph, starting with this one.
	  *         The iterator is ordered in a way that the nodes are returned from closest to furthest.
	  */
	def orderedAllNodesIterator = {
		val visitedNodes = mutable.Set[Any](this)
		OrderedDepthIterator(Iterator.single(self)) { start =>
			start.leavingEdges.iterator.flatMap { edge =>
				val node = edge.end
				if (visitedNodes.contains(node))
					None
				else {
					visitedNodes += node
					Some(node)
				}
			}
		}
	}
	
	/**
	  * @return An iterator that returns all shortest routes that appear within this node.
	  *         In situations where there would be multiple equally short routes,
	  *         only the first encountered route is returned.
	  *
	  *         The resulting routes / nodes are listed in order of length;
	  *         Empty route to this node is returned first,
	  *         then routes of length 1 to this node's siblings, then routes of length 2 and so on.
	  *
	  *         The values returned by the returned iterator consist of two parts:
	  *         1) Route to the node in question as a sequence of edges
	  *         2) The node at the end of that route
	  */
	def shortestRoutesIterator = {
		val visitedNodes = mutable.Set[Any](this)
		OrderedDepthIterator(Iterator.single[(Seq[Edge], GNode)](Empty -> self)) { case (route, lastNode) =>
			lastNode.leavingEdges.iterator.flatMap { edge =>
				val node = edge.end
				if (visitedNodes.contains(node))
					None
				else {
					visitedNodes += node
					Some((route :+ edge) -> node)
				}
			}
		}
	}
	
	/**
	  * @return An iterator that returns all edges that appear within this graph.
	  *         The edges are not returned in any specific order, except that all edges belonging to a single node
	  *         are returned in sequence.
	  */
    def allEdgesIterator = allNodesIterator.flatMap { _.leavingEdges }
	/**
	  * @return An iterator that returns all values within the nodes in this graph.
	  *         The values are not returned in any specific order, except that the value of this node is returned first.
	  */
	def allValuesIterator = allNodesIterator.map { _.value }
	/**
	  * @return An iterator that returns all values within the nodes in this graph.
	  *         The iterator is ordered so that it returns first the values of nodes closest to this node,
	  *         starting from this node itself. The iterator then moves further one layer at a time.
	  */
	def orderedAllValuesIterator = orderedAllNodesIterator.map { _.value }
	
	/**
	  * @return All nodes that appear within this graph, including this node
	  */
	def allNodes = allNodesIterator.toSet
	/**
	  * @return All edges that appear within this graph
	  */
	def allEdges = allEdgesIterator.toSet
	/**
	  * @return All distinct values that appear within this graph
	  */
	def allValues = allValuesIterator.toSet
	/**
	 * @return Content of all nodes linked with this node, including the contents of this node
	 */
	@deprecated("Replaced with .allValues", "< v2.3")
	def allNodeContent = allValues
	
	/**
	 * Converts this node to a graph
	 * @return A graph based on this node's connections
	 */
	def toGraph =
		Graph(allNodesIterator
			.flatMap { node => node.leavingEdges.map { edge => (node.value, edge.value, edge.end.value) } }.toSet)
	
	/**
	  * @return A lazily initialized tree based on this graph.
	  *         This node will appear as the root of the tree.
	  *         Other nodes may appear in multiple locations, but never twice in a single branch.
	  *         Each tree node contains a reference to a graph node.
	  *
	  *         For example, if node A connects to nodes B and C, which both connect to node D,
	  *         which then connects to node E, the resulting branches would be:
	  *         A -> B -> D -> E,
	  *         A -> C -> D -> E.
	  *         Notice how D and E appear twice.
	  *
	  *         The resulting tree may be considered to consist of unique paths within this graph that all start
	  *         from this node and never traverse one node twice.
	  *
	  *         Please note that the resulting tree will be very large for graphs with a large number of edges.
	  */
	def toTree = _toTree(Set(this))
	private def _toTree(traversedNodes: Set[Any]): LazyTree[GNode] = {
		// Remembers which nodes have been visited (branch-specific)
		val newTraversed = traversedNodes + this
		// Creates the tree lazily
		LazyTree(Lazy(self), leavingEdges.iterator.flatMap { edge =>
			val node = edge.end
			// Case: A node would be a parent of this node in the tree => ends
			if (newTraversed.contains(node))
				None
			// Case: Unique node within this branch => Converts it to a tree lazily, also
			else
				Some(node._toTree(newTraversed))
		}.caching)
	}
	
	/**
	 * @return Finds all circular routes from this node to itself without traversing trough any other node more than once
	 */
	def routesToSelf = routesTo(this)
	
	/**
	  * @return An interactive search process which targets all nodes in this graph
	  */
	def searchShortestRoutesToAll = searchAllNodes[Int] { _: Edge => 1 }
	
	
	// IMPLEMENTED  ----------------------
	
	override def toString = s"Node($value)"


	// OTHER	-----------------
	
	/**
	  * Traverses edges from this node once
	  * @param edgeType The content of the traversed edge(s)
	  * @return The node(s) at the end of the edge(s)
	  */
	def /(edgeType: E) = leavingEdges.filter { _.value == edgeType }.map { _.end }
	/**
	  * Traverses a deep path that consists of edges between nodes
	  * @param path The content of the edges to travel in sequence, starting from edges of this node.
	  *             An empty path is considered to point to this node.
	  * @return The node(s) at the end of the path
	  */
	def /(path: Seq[E]): Set[GNode] = {
		if (path.isEmpty)
			Set(self)
		else {
			var current = /(path.head).toSet
			path.drop(1).foreach { p => current = current.flatMap { _/p } }
			current
		}
	}
	/**
	  * Traverses a deep path that consists of edges between nodes
	  * @param first The first edge to traverse
	  * @param second The second edge to traverse
	  * @param more More edges
	  * @return The node(s) at the end of the path
	  */
	def /(first: E, second: E, more: E*): Set[GNode] = this / (Pair(first, second) ++ more)
	
	/**
	  * @param other Another node
	  * @return Whether this node contains a direct connection to the specified node
	  */
	def isDirectlyConnectedTo(other: AnyNode) = leavingEdges.exists { _.end == other }
	
    /**
     * Finds an edge pointing to another node, if there is one
     * @param other The node this node may be connected to
     * @return an edge connecting the two nodes, if there is one. If there are multiple edges
     * pointing toward the specified node, returns the first one encountered.
     * @see edgesTo(GraphNode[_, _])
     */
    def edgeTo(other: AnyNode) = leavingEdges.find { _.end == other }
    /**
     * Finds all edges pointing from this node to the specified node.
     * @param other another node
     * @return The edges pointing towards the provided node from this node
     */
    def edgesTo(other: AnyNode) = leavingEdges.filter { _.end == other }
	
	/**
	  * Starts a graph search which targets 0-n nodes.
	  * Throughout this interactive process, discovers these nodes and the cheapest route or routes to them.
	  * This process may be advanced manually, through the returned interface.
	  * @param destinations A set of find functions which identify the targeted graph nodes.
	  *                     Will yield 0-1 results for each of these functions.
	  * @param startCost Initial assigned to all routes.
	  * @param costOf A function for determining the cost of a single edge-traversal.
	  * @param sumOf A function for computing the sum of two cost values.
	  * @param ord Implicit ordering to use for cost values.
	  * @tparam C Type of cost values used.
	  * @return An interface for advancing the search process and for accessing the results,
	  *         including preliminary search results.
	  */
	def customSearchForEach[C](destinations: Iterable[GNode => Boolean], startCost: C)
	                          (costOf: Edge => C)(sumOf: (C, C) => C)
	                          (implicit ord: Ordering[C]) =
		_searchForEach(destinations, startCost, exclusive = true)(costOf)(sumOf)
	/**
	  * Starts a graph search which targets 0-n nodes.
	  * Throughout this interactive process, discovers these nodes and the cheapest route or routes to them.
	  * This process may be advanced manually, through the returned interface.
	  * @param destinations A set of find functions which identify the targeted graph nodes.
	  *                     Will yield 0-1 results for each of these functions.
	  * @param costOf A function for determining the cost of a single edge-traversal.
	  * @param n Numeric implementation for the cost values
	  * @tparam C Type of cost values used.
	  * @return An interface for advancing the search process and for accessing the results,
	  *         including preliminary search results.
	  */
	def searchForEach[C](destinations: Iterable[GNode => Boolean])(costOf: Edge => C)
	                    (implicit n: Numeric[C]): GraphSearchProcess[GNode, Edge, C] =
		customSearchForEach(destinations, n.zero)(costOf)(n.plus)
	
	/**
	  * Starts a graph search which targets 0-n nodes and looks for shortest routes (in terms of the number of edges).
	  * @param destinations A set of find functions which identify the targeted graph nodes.
	  *                     Will yield 0-1 results for each of these functions.
	  * @return An interface for advancing the search process and for accessing the results,
	  *         including preliminary search results.
	  */
	def searchShortestRouteForEach(destinations: Iterable[GNode => Boolean]): GraphSearchProcess[GNode, Edge, Int] =
		searchForEach(destinations) { _ => 1 }
	
	/**
	  * Starts a graph search which target an individual node.
	  * Throughout this interactive process, discovers this node and the cheapest route or routes to it.
	  * This process may be advanced manually, through the returned interface.
	  * @param startCost Initial assigned to all routes.
	  * @param find A function for identifying the targeted node
	  * @param costOf A function for determining the cost of a single edge-traversal.
	  * @param sumOf A function for computing the sum of two cost values.
	  * @param ord Implicit ordering to use for cost values.
	  * @tparam C Type of cost values used.
	  * @return An interface for advancing the search process and for accessing the results,
	  *         including preliminary search results.
	  */
	def customSearchForOne[C](startCost: C)(find: GNode => Boolean)(costOf: Edge => C)(sumOf: (C, C) => C)
	                         (implicit ord: Ordering[C]) =
		customSearchForEach(Single(find), startCost)(costOf)(sumOf)
	/**
	  * Starts a graph search which target an individual node.
	  * Throughout this interactive process, discovers this node and the cheapest route or routes to it.
	  * This process may be advanced manually, through the returned interface.
	  * @param find A function for identifying the targeted node
	  * @param costOf A function for determining the cost of a single edge-traversal.
	  * @param n Numeric implementation for the cost values
	  * @tparam C Type of cost values used.
	  * @return An interface for advancing the search process and for accessing the results,
	  *         including preliminary search results.
	  */
	def searchForOne[C](find: GNode => Boolean)(costOf: Edge => C)(implicit n: Numeric[C]) =
		searchForEach(Single(find))(costOf)
	
	/**
	  * Starts a graph search which targets 1 node and looks for shortest routes (in terms of the number of edges).
	  * @param find A function for identifying the targeted node
	  * @return An interface for advancing the search process and for accessing the results,
	  *         including preliminary search results.
	  */
	def searchShortestRouteToOne(find: GNode => Boolean) =
		searchForOne(find) { _ => 1 }
	
	/**
	  * Starts a graph search.
	  * Throughout this interactive process, discovers ALL nodes accepted by the specified filter function.
	  * Also discovers the cheapest route or routes to each of these nodes.
	  * This process may be advanced manually, through the returned interface.
	  * @param startCost Initial assigned to all routes.
	  * @param filter A function for identifying the targeted nodes
	  * @param costOf A function for determining the cost of a single edge-traversal.
	  * @param sumOf A function for computing the sum of two cost values.
	  * @param ord Implicit ordering to use for cost values.
	  * @tparam C Type of cost values used.
	  * @return An interface for advancing the search process and for accessing the results,
	  *         including preliminary search results.
	  */
	def customSearch[C](startCost: C)(filter: GNode => Boolean)(costOf: Edge => C)(sumOf: (C, C) => C)
	                   (implicit ord: Ordering[C]) =
		_searchForEach(Single(filter), startCost, exclusive = false)(costOf)(sumOf)
	/**
	  * Starts a graph search.
	  * Throughout this interactive process, discovers ALL nodes accepted by the specified filter function.
	  * Also discovers the cheapest route or routes to each of these nodes.
	  * This process may be advanced manually, through the returned interface.
	  * @param filter A function for identifying the targeted nodes
	  * @param costOf A function for determining the cost of a single edge-traversal.
	  * @param n Numeric implementation for the cost values
	  * @tparam C Type of cost values used.
	  * @return An interface for advancing the search process and for accessing the results,
	  *         including preliminary search results.
	  */
	def search[C](filter: GNode => Boolean)(costOf: Edge => C)
	             (implicit n: Numeric[C]): GraphSearchProcess[GNode, Edge, C] =
		customSearch(n.zero)(filter)(costOf)(n.plus)
	
	/**
	  * Starts a graph search which looks for shortest routes (in terms of the number of edges).
	  * @param filter A function for identifying the targeted nodes
	  * @return An interface for advancing the search process and for accessing the results,
	  *         including preliminary search results.
	  */
	def searchShortestRoutesTo(filter: GNode => Boolean) =
		search(filter) { _ => 1 }
	
	/**
	  * Starts a graph search which targets 0-n nodes.
	  * Throughout this interactive process, discovers the cheapest route or routes to these nodes.
	  * This process may be advanced manually, through the returned interface.
	  * @param nodes Targeted route end nodes
	  * @param startCost Initial assigned to all routes.
	  * @param costOf A function for determining the cost of a single edge-traversal.
	  * @param sumOf A function for computing the sum of two cost values.
	  * @param ord Implicit ordering to use for cost values.
	  * @tparam C Type of cost values used.
	  * @return An interface for advancing the search process and for accessing the results,
	  *         including preliminary search results.
	  */
	def customSearchForNodes[C](nodes: Iterable[AnyNode], startCost: C)(costOf: Edge => C)(sumOf: (C, C) => C)
	                           (implicit ord: Ordering[C]) =
		customSearchForEach(nodes.map { node => _ == node }, startCost)(costOf)(sumOf)
	/**
	  * Starts a graph search which targets 0-n nodes.
	  * Throughout this interactive process, discovers the cheapest route or routes to these nodes.
	  * This process may be advanced manually, through the returned interface.
	  * @param nodes Targeted route end nodes
	  * @param costOf A function for determining the cost of a single edge-traversal.
	  * @param n Numeric implementation for the cost values
	  * @tparam C Type of cost values used.
	  * @return An interface for advancing the search process and for accessing the results,
	  *         including preliminary search results.
	  */
	def searchForNodes[C](nodes: Iterable[AnyNode])(costOf: Edge => C)
	                     (implicit n: Numeric[C]): GraphSearchProcess[GNode, Edge, C] =
		customSearchForNodes[C](nodes, n.zero)(costOf)(n.plus)
	
	/**
	  * Starts a graph search which looks for shortest routes to the specified nodes (in terms of the number of edges).
	  * @param nodes Targeted route end nodes
	  * @return An interface for advancing the search process and for accessing the results,
	  *         including preliminary search results.
	  */
	def searchShortestRoutesToNodes(nodes: Iterable[AnyNode]) =
		searchForNodes(nodes) { _ => 1 }
	
	/**
	  * Starts a graph search which targets a single node.
	  * Throughout this interactive process, discovers the cheapest route or routes to that node.
	  * This process may be advanced manually, through the returned interface.
	  * @param node Targeted route end node
	  * @param startCost Initial assigned to all routes.
	  * @param costOf A function for determining the cost of a single edge-traversal.
	  * @param sumOf A function for computing the sum of two cost values.
	  * @param ord Implicit ordering to use for cost values.
	  * @tparam C Type of cost values used.
	  * @return An interface for advancing the search process and for accessing the results,
	  *         including preliminary search results.
	  */
	def customSearchForNode[C](node: AnyNode, startCost: C)(costOf: Edge => C)(sumOf: (C, C) => C)
	                          (implicit ord: Ordering[C]) =
		customSearchForNodes(Single(node), startCost)(costOf)(sumOf)
	/**
	  * Starts a graph search which targets a single node.
	  * Throughout this interactive process, discovers the cheapest route or routes to that node.
	  * This process may be advanced manually, through the returned interface.
	  * @param node Targeted route end node
	  * @param costOf A function for determining the cost of a single edge-traversal.
	  * @param n Numeric implementation for the cost values
	  * @tparam C Type of cost values used.
	  * @return An interface for advancing the search process and for accessing the results,
	  *         including preliminary search results.
	  */
	def searchForNode[C](node: AnyNode)(costOf: Edge => C)(implicit n: Numeric[C]) =
		searchForNodes(Single(node))(costOf)
	
	/**
	  * Starts a graph search which looks for shortest route to the specified node (in terms of the number of edges).
	  * @param node Targeted route end node
	  * @return An interface for advancing the search process and for accessing the results,
	  *         including preliminary search results.
	  */
	def searchShortestRouteToNode(node: AnyNode) = searchForNode(node) { _ => 1 }
	
	/**
	  * Starts a graph search which targets all nodes in this graph.
	  * Throughout this interactive process, discovers the cheapest route or routes to these nodes.
	  * This process may be advanced manually, through the returned interface.
	  * @param startCost Initial assigned to all routes.
	  * @param costOf A function for determining the cost of a single edge-traversal.
	  * @param sumOf A function for computing the sum of two cost values.
	  * @param ord Implicit ordering to use for cost values.
	  * @tparam C Type of cost values used.
	  * @return An interface for advancing the search process and for accessing the results,
	  *         including preliminary search results.
	  */
	def customSearchAllNodes[C](startCost: C)(costOf: Edge => C)(sumOf: (C, C) => C)(implicit ord: Ordering[C]) =
		customSearch(startCost) { _ => true }(costOf)(sumOf)
	/**
	  * Starts a graph search which targets all nodes in this graph.
	  * Throughout this interactive process, discovers the cheapest route or routes to these nodes.
	  * This process may be advanced manually, through the returned interface.
	  * @param costOf A function for determining the cost of a single edge-traversal.
	  * @param n Numeric implementation for the cost values
	  * @tparam C Type of cost values used.
	  * @return An interface for advancing the search process and for accessing the results,
	  *         including preliminary search results.
	  */
	def searchAllNodes[C](costOf: Edge => C)(implicit n: Numeric[C]): GraphSearchProcess[GNode, Edge, C] =
		customSearchAllNodes(n.zero)(costOf)(n.plus)
	
	/**
	  * Finds the cheapest routes to each of the targeted nodes.
	  * @param destinations Collection of search function, each of which will be used for matching to a single node.
	  * @param costOf A function for determining the cost of a single edge-traversal.
	  * @param n Numeric implementation for the cost values
	  * @tparam C Type of cost values used.
	  * @return Search results
	  */
	def cheapestRoutesToEach[C](destinations: Iterable[GNode => Boolean])(costOf: Edge => C)(implicit n: Numeric[C]) =
		searchForEach(destinations)(costOf).finish()
	/**
	  * Finds the shortest routes to each of the targeted nodes.
	  * @param destinations Collection of search function, each of which will be used for matching to a single node.
	  * @return Search results
	  */
	def shortestRoutesToEach(destinations: Iterable[GNode => Boolean]) =
		cheapestRoutesToEach(destinations) { _ => 1 }
	
	/**
	  * Finds the cheapest routes to a single node.
	  * @param find A function used for identifying the targeted node.
	  * @param costOf A function for determining the cost of a single edge-traversal.
	  * @param n Numeric implementation for the cost values
	  * @tparam C Type of cost values used.
	  * @return Search results, if successful. None if unsuccessful.
	  */
	def cheapestRoutesToOne[C](find: GNode => Boolean)(costOf: Edge => C)(implicit n: Numeric[C]) =
		searchForOne(find)(costOf).finish().any
	/**
	  * Finds the shortest routes to a single node.
	  * @param find A function used for identifying the targeted node.
	  * @return Search results, if successful. None if unsuccessful.
	  */
	def shortestRoutesToOne(find: GNode => Boolean) =
		cheapestRoutesToOne(find) { _ => 1 }
	
	/**
	  * Finds the cheapest routes to a certain sub-group of nodes within this graph
	  * @param filter A function used for identifying, which nodes are targeted and which are not.
	  * @param costOf A function for determining the cost of a single edge-traversal.
	  * @param n Numeric implementation for the cost values
	  * @tparam C Type of cost values used.
	  * @return Search results
	  */
	def cheapestRoutesTo[C](filter: GNode => Boolean)(costOf: Edge => C)(implicit n: Numeric[C]) =
		search(filter)(costOf).finish()
	/**
	  * Finds the shortest routes to a certain sub-group of nodes within this graph
	  * @param filter A function used for identifying, which nodes are targeted and which are not.
	  * @return Search results
	  */
	def shortestRoutesTo(filter: GNode => Boolean) =
		cheapestRoutesTo(filter) { _ => 1 }
	
	/**
	  * Finds the cheapest routes to a certain group of nodes
	  * @param nodes Searched nodes
	  * @param costOf A function for determining the cost of a single edge-traversal.
	  * @param n Numeric implementation for the cost values
	  * @tparam C Type of cost values used.
	  * @return Search results
	  */
	def cheapestRoutesToNodes[C](nodes: Iterable[AnyNode])(costOf: Edge => C)(implicit n: Numeric[C]) =
		searchForNodes(nodes)(costOf).finish()
	/**
	  * Finds the shortest routes to a certain group of nodes
	  * @param nodes Searched nodes
	  * @return Search results
	  */
	def shortestRoutesToNodes(nodes: Iterable[AnyNode]) =
		cheapestRoutesToNodes(nodes) { _ => 1 }
	
	/**
	  * Finds the cheapest routes to a single node.
	  * @param node The targeted node
	  * @param costOf A function for determining the cost of a single edge-traversal.
	  * @param n Numeric implementation for the cost values
	  * @tparam C Type of cost values used.
	  * @return Search results, if successful. None if unsuccessful.
	  */
	def cheapestRoutesToNode[C](node: AnyNode)(costOf: Edge => C)(implicit n: Numeric[C]) =
		searchForNode(node)(costOf).finish().any
	/**
	  * Finds the shortest routes to a single node.
	  * @param node The targeted node
	  * @return Search results, if successful. None if unsuccessful.
	  */
	def shortestRoutesToNode(node: AnyNode) =
		cheapestRoutesToNode(node) { _ => 1 }
	
	// TODO: Add findAny version of filter
	
	/**
	  * Finds the cheapest routes to multiple destination searches at once
	  * @param destinations Destinations to find, where each is a node search function
	  * @param startCost The initial (zero) route cost
	  * @param costOf A function for calculating the cost of a single route step (edge)
	  * @param sumOf A function for combining two costs together
	  * @param ord Implicit ordering for comparing costs
	  * @tparam C Type of calculated cost
	  * @return A map containing an entry for each <b>found</b> destination. Values of the map contain 2 parts:
	  *         1) All cheapest routes o that node and 2) the cost of those routes
	  */
	@deprecated("Please use .searchForEach(...).finish().toRouteMap instead", "v2.4")
	def findCheapestRoutesTo[C](destinations: Set[GNode => Boolean], startCost: C)
	                           (costOf: Edge => C)
	                           (sumOf: (C, C) => C)
	                           (implicit ord: Ordering[C]): Map[GNode, (Set[Seq[Edge]], C)] =
		customSearchForEach(destinations, startCost)(costOf)(sumOf).finish().toRouteMap
	/**
	  * Finds the cheapest routes to multiple destination searches at once
	  * @param destinations Destinations to find, where each is a node search function
	  * @param costOf A function for calculating the cost of a single route step (edge)
	  * @param n Implicit numeric functions for the cost type
	  * @tparam C Type of calculated cost
	  * @return A map containing an entry for each <b>found</b> destination. Values of the map contain 2 parts:
	  *         1) All cheapest routes o that node and 2) the cost of those routes
	  */
	@deprecated("Please use .cheapestRoutesToEach(...).toRouteMap instead", "v2.4")
	def findCheapestRoutesTo[C](destinations: Set[GNode => Boolean])
	                           (costOf: Edge => C)
	                           (implicit n: Numeric[C]): Map[GNode, (Set[Seq[Edge]], C)] =
		findCheapestRoutesTo[C](destinations, n.zero)(costOf)(n.plus)
	/**
	  * Finds the cheapest routes to a node found with a find function
	  * @param startCost The initial (zero) route cost
	  * @param find A find function that determines the destination node
	  * @param costOf A function for calculating the cost of a single route step (edge)
	  * @param sumOf A function for combining two costs together
	  * @param ord Implicit ordering for comparing costs
	  * @tparam C Type of calculated cost
	  * @return None if no route / destination was found.
	  *         Otherwise 1) the destination node, 2) cheapest routes to that node and 3) cost of those routes
	  */
	@deprecated("Please use .searchForOne(...).finish().any instead (notice the different return type)", "v2.4")
	def findCheapestRoutes[C](startCost: C)(find: GNode => Boolean)(costOf: Edge => C)(sumOf: (C, C) => C)
	                           (implicit ord: Ordering[C]): Option[(GNode, Set[Seq[Edge]], C)] =
		customSearchForOne(startCost)(find)(costOf)(sumOf).finish().any.map { r => (r.node, r.routes, r.cost) }
	/**
	  * Finds the cheapest routes to a node found with a find function
	  * @param find A find function that determines the destination node
	  * @param costOf A function for calculating the cost of a single route step (edge)
	  * @param n Implicit numeric functions for the cost type
	  * @tparam C Type of calculated cost
	  * @return None if no route / destination was found.
	  *         Otherwise 1) the destination node, 2) cheapest routes to that node and 3) cost of those routes
	  */
	@deprecated("Please use .cheapestRoutesToOne(...) instead (notice the different return type)", "v2.4")
	def findCheapestRoutes[C](find: GNode => Boolean)(costOf: Edge => C)
	                         (implicit n: Numeric[C]): Option[(GNode, Set[Seq[Edge]], C)] =
		findCheapestRoutes[C](n.zero)(find)(costOf)(n.plus)
	
	/**
	  * Finds the cheapest routes to multiple nodes at once
	  * @param destinations Nodes to find
	  * @param startCost The initial (zero) route cost
	  * @param costOf A function for calculating the cost of a single route step (edge)
	  * @param sumOf A function for combining two costs together
	  * @param ord Implicit ordering for comparing costs
	  * @tparam C Type of calculated cost
	  * @return A map containing an entry for each <b>found</b> destination. Values of the map contain 2 parts:
	  *         1) All cheapest routes o that node and 2) the cost of those routes
	  */
	@deprecated("Please use .searchForNodes(...).finish().toRouteMap instead", "v2.4")
	def cheapestRoutesToMany[C](destinations: Set[AnyNode], startCost: C)(costOf: Edge => C)(sumOf: (C, C) => C)
	                       (implicit ord: Ordering[C]): Map[GNode, (Set[Seq[Edge]], C)] =
		findCheapestRoutesTo[C](destinations.map { d => _ == d }, startCost)(costOf)(sumOf)
	/**
	  * Finds the cheapest routes to multiple nodes at once
	  * @param destinations Nodes to find
	  * @param costOf A function for calculating the cost of a single route step (edge)
	  * @param n Implicit numeric functions for the cost type
	  * @tparam C Type of calculated cost
	  * @return A map containing an entry for each <b>found</b> destination. Values of the map contain 2 parts:
	  *         1) All cheapest routes o that node and 2) the cost of those routes
	  */
	@deprecated("Please use .cheapestRoutesToNodes(...).toRouteMap instead", "v2.4")
	def cheapestRoutesToMany[C](destinations: Set[AnyNode])(costOf: Edge => C)
	                       (implicit n: Numeric[C]): Map[GNode, (Set[Seq[Edge]], C)] =
		cheapestRoutesToMany[C](destinations, n.zero)(costOf)(n.plus)
	/**
	  * Finds the cheapest routes to another node
	  * @param destination The node to reach
	  * @param startCost The initial (zero) route cost
	  * @param costOf A function for calculating the cost of a single route step (edge)
	  * @param sumOf A function for combining two costs together
	  * @param ord Implicit ordering for comparing costs
	  * @tparam C Type of calculated cost
	  * @return 1) Routes found to to the targeted node (may be empty),
	  *         2) Cost of those routes (zero if no routes were found)
	  */
	@deprecated("Please use .searchForNode(...).finish().any instead (notice the different return type)", "v2.4")
	def cheapestRoutesTo[C](destination: AnyNode, startCost: C)(costOf: Edge => C)(sumOf: (C, C) => C)
	                       (implicit ord: Ordering[C]): (Set[Seq[Edge]], C) =
		findCheapestRoutes(startCost) { _ == destination }(costOf)(sumOf) match {
			case Some((_, routes, cost)) => routes -> cost
			case None => Set[Seq[Edge]]() -> startCost
		}
	/**
	  * Finds the cheapest routes to another node
	  * @param destination The node to reach
	  * @param costOf A function for calculating the cost of a single route step (edge)
	  * @param n Implicit numeric functions for the cost type
	  * @tparam C Type of calculated cost
	  * @return 1) Routes found to to the targeted node (may be empty),
	  *         2) Cost of those routes (zero if no routes were found)
	  */
	@deprecated("Please use .cheapestRoutesToNode(...) instead (notice the different return type)", "v2.4")
	def cheapestRoutesTo[C](destination: AnyNode)(costOf: Edge => C)(implicit n: Numeric[C]): (Set[Seq[Edge]], C) =
		cheapestRoutesTo[C](destination, n.zero)(costOf)(n.plus)
	
	/**
	  * Finds one cheapest route to a node found with a search function
	  * @param find A search function for the destination node
	  * @param costOf A function for calculating the cost of a single route step (edge)
	  * @param n Implicit numeric functions for the cost type
	  * @tparam C Type of cost used
	  * @return None if no destination or route was found. Otherwise 1) The destination node and 2) cheapest route to that node
	  */
	@deprecated("Please use .cheapestRoutesToOne(...) instead (notice the different return type)", "v2.4")
	def findCheapestRoute[C](find: GNode => Boolean)(costOf: Edge => C)(implicit n: Numeric[C]) =
		findCheapestRoutes(find)(costOf).map { case (node, routes, _) => node -> routes.head }
	
	/**
	  * Finds one cheapest route to a node
	  * @param destination The destination node
	  * @param costOf A function for calculating the cost of a single route step (edge)
	  * @param n Implicit numeric functions for the cost type
	  * @tparam C Type of cost used
	  * @return None if no route was found. Otherwise the cheapest route found.
	  */
	@deprecated("Please use .cheapestRoutesToNode(...) instead (notice the different return type)", "v2.4")
	def cheapestRouteTo[C](destination: AnyNode)(costOf: Edge => C)(implicit n: Numeric[C]) =
		findCheapestRoute { _ == destination }(costOf).map { _._2 }
	
	/**
	  * Finds the shortest routes to multiple destination searches at once
	  * @param destinations Destinations to find, where each is a node search function
	  * @return A map containing an entry for each <b>found</b> destination. Values of the map contain all shortest
	  *         routes to that node
	  */
	@deprecated("Please use .shortestRoutesToEach(...).toRouteMap instead", "v2.4")
	def findShortestRoutesTo(destinations: Set[GNode => Boolean]) =
		searchShortestRouteForEach(destinations).finish().successes.view.map { r => r.node -> r.routes }.toMap
	/**
	  * Finds the shortest routes to a destination found with a search function
	  * @param find A search function for the destination node
	  * @return None if no destination / route was found.
	  *         Otherwise the node that was found and the shortest routes to that node.
	  */
	@deprecated("Please use .shortestRoutesToOne(...) instead (notice the different return type)", "v2.4")
	def findShortestRoutes(find: GNode => Boolean) =
		searchShortestRouteToOne(find).finish().successes.headOption.map { r => r.node -> r.routes }
	/**
	  * Finds one shortest route to a destination found with a search function
	  * @param find A search function for the destination node
	  * @return None if no destination / route was found.
	  *         Otherwise the node that was found and the shortest route to that node.
	  */
	@deprecated("Please use .shortestRoutesToOne(...) instead (notice the different return type)", "v2.4")
	def findShortestRoute(find: GNode => Boolean) = findCheapestRoute[Int](find) { _ => 1 }
	
	/**
	  * Finds the shortest routes to multiple destinations at once
	  * @param destinations Destinations nodes
	  * @return A map containing an entry for each <b>found</b> destination. Values of the map contain all shortest
	  *         routes to that node
	  */
	@deprecated("Please use .shortestRoutesToNodes(...).toRouteMap instead", "v2.4")
	def shortestRoutesToMany(destinations: Set[AnyNode]) =
		findShortestRoutesTo(destinations.map { d => _ == d })
	/**
	  * Finds the shortest routes to another node
	  * @param destination the destination node
	  * @return Shortest routes to that node. Empty if no routes were found.
	  */
	@deprecated("Please use .shortestRoutesToNode(...) instead (notice the different return type)", "v2.4")
	def shortestRoutesTo(destination: AnyNode) = cheapestRoutesTo[Int](destination) { _ => 1 }._1
	/**
	  * Finds one shortest route to another node
	  * @param destination the destination node
	  * @return Shortest route to that node. None if no routes were found.
	  */
	@deprecated("Please use .shortestRoutesToNode(...) instead (notice the different return type)", "v2.4")
	def shortestRouteTo(destination: AnyNode) = cheapestRouteTo[Int](destination) { _ => 1 }
    
    /**
     * Finds all routes (edge combinations) that connect this node to the provided node. Routes
     * can't contain the same node multiple times so no looping routes are included. An exception to this is the case
	 * where this node is targeted. In that case, the resulting routes start and end at this node.
     * @param node The node this node may be connected to
     * @return All possible routes to the provided node. In case this node is the searched node,
     * however, a single empty route will be returned. The end node will always be at the end of
     * each route and nowhere else. If there are no connecting routes, an empty array is returned.
     */
    def routesTo(node: AnyNode): Iterable[Route] = {
		// If trying to find routes to self, will have to handle limitations a bit differently
		if (node == this) {
			leavingEdges.find { _.end == this } match {
				case Some(zeroRoute) => Single(Single(zeroRoute))
				case None => leavingEdges.flatMap { e => e.end.routesTo(this, Set()).map { route => e +: route } }
			}
		}
		else
			routesTo(node, Set())
	}
    // Uses recursion
    private def routesTo(node: AnyNode, visitedNodes: Set[AnyNode]): Iterable[Route] = {
		// Tries to find the destination from each connected edge that leads to a new node
		val newVisitedNodes = visitedNodes + this
	
		// Checks whether there exist edges to the final node
		val availableEdges = leavingEdges.filterNot { e => newVisitedNodes.contains(e.end) }
		availableEdges.find { _.end == node } match {
			case Some(directRoute) => Single(Single(directRoute))
			case None =>
				// If there didn't exist a direct path, tries to find an indirect one
				// Attaches this element at the beginning of each returned route (if there were any returned)
				availableEdges.flatMap { e => e.end.routesTo(node, newVisitedNodes).map { route => e +: route } }
		}
    }
	
	private def _searchForEach[C](destinations: Iterable[GNode => Boolean], startCost: C, exclusive: Boolean)
	                            (costOf: Edge => C)(sumOf: (C, C) => C)
	                            (implicit ord: Ordering[C]) =
		new PathsFinder[N, E, GNode, Edge, C](self, destinations, startCost, exclusive)(costOf)(sumOf).iterator
	
	/**
	  * Performs a recursive check and looks whether this node is at all connected to the specified node
	  * @param other Another node
	  * @return Whether this node is at all connected to the specified node
	  */
	def isConnectedTo(other: AnyNode) = allNodesIterator.contains(other)
}