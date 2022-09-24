package utopia.reflection.container.stack

import utopia.flow.async.process.LoopingProcess
import utopia.flow.collection.immutable.GraphEdge
import utopia.flow.collection.mutable.iterator.Counter
import utopia.flow.collection.mutable.{GraphNode, VolatileList}
import utopia.flow.util.logging.Logger
import utopia.genesis.util.Fps
import utopia.reflection.component.swing.template.AwtComponentRelated
import utopia.reflection.component.template.layout.stack.Stackable
import utopia.reflection.util.AwtEventThread

import scala.collection.immutable.VectorBuilder
import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/**
  * Stack hierarchy manager tracks stack component hierarchies and updates the components when necessary
  * @author Mikko Hilpinen
  * @since 15.4.2019, v0.1+
  */
object StackHierarchyManager
{
	// TYPES	-------------------------
	
	private type Node = GraphNode[Stackable, Int]
	private type Edge = GraphEdge[Stackable, Int, Node]
	
	
	// ATTRIBUTES	---------------------
	
	private val indexCounter = new Counter(1)
	// Stackable -> id, used for finding parents
	private val ids = mutable.HashMap[Int, StackId]()
	// Id -> Node -> Children, used for finding children
	private val graph = mutable.HashMap[Int, Node]()
	
	private val validationQueue = VolatileList[Stackable]()
	
	private var validationLoop: Option[RevalidateLoop] = None
	
	
	// COMPUTED	-------------------------
	
	/**
	  * @return Whether this hierarchy manager expects to be revalidated in the near future
	  */
	def waitsRevalidation = validationQueue.nonEmpty
	
	/**
	  * @return A string description of the stack hierarchy
	  */
	def description = s"[${graph.values.map(nodeToString).mkString(", ")}]"
	
	/**
	  * @return Pulls an awt component from the registered components, provided that any are available
	  */
	def anyComponent = graph.valuesIterator.find { _.content.isInstanceOf[AwtComponentRelated] }
		.map { _.asInstanceOf[AwtComponentRelated].component }
	
	private def nodeToString(node: Node): String =
	{
		val children = node.endNodes
		if (children.isEmpty)
			node.content.toString
		else if (children.size == 1)
			s"${node.content.toString} -> ${nodeToString(children.head)}"
		else
			s"${node.content.toString} -> [${children.map(nodeToString).mkString(", ")}]"
	}
	
	
	// IMPLEMENTED	----------------------
	
	override def toString = description
	
	
	// OTHER	-------------------------
	
	/**
	  * Finds the component hierarchy above the specified component (inclusive)
	  * @param component A component
	  * @return All components that are above the specified component in this stack hierarchy, from the highest
	  *         to the lowest. Ends with the specified component. Not empty.
	  */
	def upperHierarchyOf(component: Stackable) = ids.get(component.stackId) match
	{
		case Some(id) =>
			// Case: Master component
			if (id.isMasterId)
				graph.get(id.masterId).map { _.content }.toVector
			// Case: Child component
			else
				graph.get(id.masterId) match
				{
					case Some(masterNode) =>
						// Collects the whole component path
						val builder = new VectorBuilder[Stackable]()
						builder += masterNode.content
						var lastNode = masterNode
						var remainingIds = id.parts.drop(1)
						while (remainingIds.nonEmpty)
						{
							(lastNode/remainingIds.head).headOption match
							{
								case Some(nextNode) =>
									builder += nextNode.content
									lastNode = nextNode
									remainingIds = remainingIds.drop(1)
								// Case: Path was invalid / broken => adds the component to the end and finishes
								case None =>
									builder += component
									remainingIds = Vector()
							}
						}
						builder.result()
					case None => Vector(component)
				}
		// Case: Unregistered component => treats it as an individual master component
		case None => Vector(component)
	}
	
	/**
	  * Requests validation for the specified item
	  * @param item An item
	  */
	def requestValidationFor(item: Stackable) =
	{
		// Queues the item
		validationQueue :+= item
		
		// Informs validation loop
		validationLoop.foreach { _.runAsync() }
	}
	
	/**
	  * Requests validation for the specified item
	  * @param items Items to revalidate
	  */
	def requestValidationFor(items: Iterable[Stackable]) =
	{
		// Queues the item
		validationQueue ++= items
		
		// Informs validation loop
		if (items.nonEmpty)
			validationLoop.foreach { _.runAsync() }
	}
	
	/**
	  * Starts automatic revalidation in a background thread
	  * @param vps The maximum validations per second value (default = 30)
	  * @param context The asynchronous execution context
	  */
	def startRevalidationLoop(vps: Fps = Fps(30))(implicit context: ExecutionContext, logger: Logger) =
	{
		if (validationLoop.isEmpty) {
			val loop = new RevalidateLoop(vps.interval)
			validationLoop = Some(loop)
			if (waitsRevalidation)
				loop.runAsync()
		}
	}
	
	/**
	  * Performs a single revalidation on this stack hierarchy manager. This method needn't be called if automatic
	  * revalidation is used
	  */
	def revalidate(): Unit =
	{
		// Only revalidates if necessary
		if (waitsRevalidation)
		{
			val items = validationQueue.getAndSet(Vector()).toSet
			val itemIds = items.map { _.stackId }.flatMap(ids.get)
			
			// First resets stack sizes for all revalidating hierarchies
			resetStackSizesFor(itemIds)
			
			// Validates the necessary master items
			val masterNodes = itemIds.map { _.masterId }.flatMap { index => graph.get(index) }
			masterNodes.foreach { _.content.updateLayout() }
			
			// Validates the deeper levels
			val nextIds = itemIds.flatMap { _.tail }
			revalidate(nextIds, masterNodes)
		}
	}
	
	@scala.annotation.tailrec
	private def revalidate(remainingIds: Set[StackId], nodes: Set[Node]): Unit =
	{
		// Finds the next set of nodes to validate
		val currentLevelIds = remainingIds.map { _.head }
		val nextNodes = nodes.flatMap { _.leavingEdges.filter { e => currentLevelIds.contains(e.content) }.map { _.end } }
		
		if (nextNodes.nonEmpty)
		{
			// Validates the items
			nextNodes.foreach { _.content.updateLayout() }
			
			// Traverses to the next level, if necessary
			val nextIds = remainingIds.flatMap { _.tail }
			if (nextIds.nonEmpty)
				revalidate(nextIds, nextNodes)
		}
	}
	
	@scala.annotation.tailrec
	private def resetStackSizesFor(remainingIds: Set[StackId]): Unit =
	{
		if (remainingIds.nonEmpty)
		{
			// Handles the items from bottom to the top (longest ids are treated first and shortened)
			val maxIdLength = remainingIds.map { _.length }.max
			val groups = remainingIds.groupBy { _.length == maxIdLength }
			
			val longest = groups.getOrElse(true, Set())
			longest.foreach { nodeOptionForId(_).foreach { _.content.resetCachedSize() } }
			
			// Shortened ids are treated on another recursive round
			val nextIds = longest.flatMap { _.parentId } ++ groups.getOrElse(false, Set())
			resetStackSizesFor(nextIds)
		}
	}
	
	/**
	  * Disconnects the specified stackable and all its children from this stack hierarchy. If you ever re-add the
	  * item to this stack hierarchy, you will also need to reattach all its child components.
	  * @param item The stackable item to be removed from this hierarchy
	  */
	def unregister(item: Stackable) =
	{
		// Removes the provided item and each child from both ids and graph
		ids.get(item.stackId).foreach { itemId =>
			// Finds correct id and node
			nodeOptionForId(itemId) match
			{
				case Some(node) =>
					// Removes the node from graph
					if (itemId.isMasterId)
						graph -= itemId.masterId
					else
						graphForId(itemId).disconnectAll(node)
					
					// Removes any child nodes
					node.foreach { ids -= _.content.stackId }
				case None =>
					// If, for some reason, the node was already removed, makes sure the id is removed as well
					ids -= item.stackId
			}
		}
	}
	
	/**
	  * Registers an individual component to this stack hierarchy. If the component was already registered, removes
	  * it from under another item. This method should be used only for items that user their own stack hierarchy
	  * @param component A component
	  */
	def registerIndividual(component: Stackable): Unit =
	{
		// Only registers the component if it hasn't been registered yet
		if (!ids.contains(component.stackId))
			addRoot(component)
		// If the component was already registered, detaches it from under its parent
		else
			_detach(component)
	}
	
	/**
	  * Registers a parent-child combo to this hierarchy
	  * @param parent A parent element
	  * @param child A child element
	  */
	def registerConnection(parent: Stackable, child: Stackable) =
	{
		// If the child already had a parent, makes the child a master (top level component) first
		ids.get(child.stackId).foreach { childId =>
			childId.parentId.foreach { parentId =>
				// Disconnects the child from the parent, also updates all id numbers
				nodeOptionForId(parentId).foreach { parentNode =>
					val childIndex = childId.last
					(parentNode/childIndex).headOption.foreach { childNode =>
						// Disconnects the child node
						parentNode.disconnectDirect(childNode)
						// Updates the ids of grandchildren (and their children) to not include the removed old parent's id
						childNode.foreach { c =>
							val grandChildStackId = c.content.stackId
							ids.get(grandChildStackId).foreach { grandChildId =>
								ids(grandChildStackId) = grandChildId.dropUntil(childIndex)
							}
						}
						
						// Makes the child a master
						graph(childIndex) = childNode
					}
				}
			}
		}
		
		// Makes sure that the parent is already registered
		val newParentId = parentId(parent)
		// If, for some reason, the parent's node was removed before or during this method call, will not finish
		// the node creation
		nodeOptionForId(newParentId).foreach { newParentNode =>
			// If the child (master) already exists, attaches it to the new parent
			ids.get(child.stackId) match
			{
				case Some(oldChildId) =>
					val childIndex = oldChildId.last
					// If the child node was removed from this hierarchy before or during this operation,
					// will not modify or insert it
					graph.get(childIndex).foreach { childNode =>
						// Updates all child ids
						childNode.foreach { n => ids(n.content.stackId) = newParentId + ids(n.content.stackId) }
						// Removes the child from master nodes and attaches it to the new parent
						graph -= childIndex
						newParentNode.connect(childNode, childIndex)
					}
				case None =>
					// Otherwise adds the child as a new id + node
					val newChildId = newParentId + indexCounter.next()
					ids(child.stackId) = newChildId
					newParentNode.connect(new Node(child), newChildId.last)
			}
		}
	}
	
	private def graphForId(id: StackId) = graphOptionForId(id).get
	
	private def graphOptionForId(id: StackId) = graph.get(id.masterId)
	
	// FIXME: Throws at times
	@deprecated("Use nodeOptionForId instead since this may trow", "v1.2")
	private def nodeForId(id: StackId) = nodeOptionForId(id).get
	
	private def nodeOptionForId(id: StackId) =
	{
		if (id.isMasterId)
			graphOptionForId(id)
		else
			graphOptionForId(id).flatMap { n => (n / id.parts.drop(1)).headOption }
	}
	
	private def parentId(item: Stackable) = ids.get(item.stackId) match
	{
		case Some(existing) => existing
		case None => addRoot(item)
	}
	
	private def _detach(item: Stackable): Unit =
	{
		ids.get(item.stackId).foreach { childId =>
			childId.parentId.foreach { parentId =>
				// Disconnects the child from the parent, also updates all id numbers
				nodeOptionForId(parentId).foreach { parentNode =>
					val childIndex = childId.last
					(parentNode / childIndex).headOption.foreach { childNode =>
						parentNode.disconnectDirect(childNode)
						childNode.foreach { c => ids(c.content.stackId) = ids(c.content.stackId).dropUntil(childIndex) }
						
						// Makes the child a master
						graph(childIndex) = childNode
					}
				}
			}
		}
	}
	
	private def addRoot(item: Stackable) =
	{
		// Creates a new id
		val newId = StackId.root(indexCounter.next())
		// Adds the new id to id map as well as graph
		ids += (item.stackId -> newId)
		graph(newId.masterId) = new Node(item)
		
		newId
	}
	
	
	// NESTED   --------------------------------
	
	private object StackId
	{
		def root(index: Int) = StackId(Vector(index))
	}
	
	private case class StackId(parts: Vector[Int])
	{
		def length = parts.size
		
		def head = parts.head
		
		def last = parts.last
		
		def tail = if (parts.size < 2) None else Some(StackId(parts.tail))
		
		def isMasterId = parts.size < 2
		
		def masterId = parts.head
		
		def parentId = if (parts.size == 1) None else Some(StackId(parts.dropRight(1)))
		
		def apply(index: Int) = parts(index)
		
		def +(index: Int) = StackId(parts :+ index)
		
		def +(other: StackId) = StackId(parts ++ other.parts)
		
		def isChildOf(other: StackId) =
		{
			if (parts.size <= other.parts.size)
				false
			else
				other.parts.indices.forall { i => apply(i) == other(i) }
		}
		
		def dropUntil(index: Int) = StackId(parts.dropWhile { _ != index })
		
		override def toString = parts.mkString(":")
	}
	
	private class RevalidateLoop(validationInterval: FiniteDuration)(implicit exc: ExecutionContext, logger: Logger)
		extends LoopingProcess
	{
		override protected def isRestartable = true
		
		override def iteration() =
		{
			// Performs the validation in Swing graphics thread
			AwtEventThread.blocking { StackHierarchyManager.revalidate() }
			
			// Only continues as long as more items need revalidation
			if (StackHierarchyManager.waitsRevalidation)
				Some(validationInterval)
			else
				None
		}
	}
}