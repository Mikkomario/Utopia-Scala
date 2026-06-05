package utopia.flow.collection.immutable.graph

/**
  * Represents an advancement in a graph search process.
  * May represent a completed or an ongoing search.
  * @param node Last visited node. If 'isDestination' is true, this is a searched node.
  * @param routes The currently discovered cheapest routes to that node. Contains 1 or more entries.
  *               If 'isConfirmedAsOptimal' is true, these are the actual cheapest routes to this node.
  * @param cost The cost of traversing these routes
  * @param isDestination Whether this represents a search destination.
  *                      False if this represents potential progress in the node-search process instead.
  * @param isConfirmedAsOptimal True if 'routes' has been confirmed to be the optimal / the cheapest routes to 'node'.
  *                             If false, another route or routes may be discovered,
  *                             which are at least as valid.
  *
  *                             Note: 'routes' may still be the optimal set or routes, even if this value is false,
  *                             but that wouldn't be for certain.
  * @tparam Node Type of nodes searched
  * @tparam Edge Type of edges traversed
  * @tparam C Type of cost used
  */
case class NodeTravelStage[+Node, +Edge, +C](node: Node, routes: Seq[Seq[Edge]], cost: C,
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
