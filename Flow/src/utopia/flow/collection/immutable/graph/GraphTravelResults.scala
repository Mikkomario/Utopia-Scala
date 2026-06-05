package utopia.flow.collection.immutable.graph

import utopia.flow.collection.immutable.Empty
import utopia.flow.operator.MaybeEmpty

/**
  * Represents a result (either preliminary or final) in a graph search process.
  * @param stages A sequence which contains both acquired results (which might not be optimal),
  *               and stateful information concerning the progress in traversing through the graph.
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
case class GraphTravelResults[+Node, +Edge, C] private(stages: Seq[NodeTravelStage[Node, Edge, C]],
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
	  * @return Whether this result may still contain suboptimal routes
	  */
	def mayBeSuboptimal = !isConfirmedAsOptimal
	
	/**
	  * @return Any successful result found. Same as calling [[successes]].headOption
	  */
	def any = successes.headOption
	
	/**
	  * @return Routes to any successfully identified search result.
	  *         Empty if no successful results were available.
	  */
	def anyRoutes = any match {
		case Some(success) => success.routes
		case None => Empty
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
	def toRouteMap[N2 >: Node]: Map[N2, (Seq[Seq[Edge]], C)] =
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
