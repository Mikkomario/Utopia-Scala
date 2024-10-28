package utopia.vault.util.console

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Single, TreeLike, ViewGraphNode}
import utopia.flow.operator.equality.EqualsFunction
import utopia.flow.operator.ordering.CombinedOrdering
import utopia.flow.util.StringExtensions._
import utopia.flow.util.console.{ArgumentSchema, Command}
import utopia.vault.context.VaultContext
import utopia.vault.database.References
import utopia.vault.model.immutable.{Reference, Table}

import scala.collection.mutable
import scala.util.{Success, Try}

private object ConsoleCommands
{
	// ATTRIBUTES   -----------------------
	
	private implicit val tableOrdering: Ordering[(Table, Int)] = CombinedOrdering[(Table, Int)](
		Ordering.by[(Table, Int), Int] { _._2 }.reverse,
		Ordering.by { _._1.name })
	private implicit val nodeOrdering: Ordering[ViewGraphNode[(Table, Int), (Reference, Boolean)]] = Ordering.by { _.value }
	
	
	// NESTED   ---------------------------
	
	private object HierarchicalTable
	{
		// Expects a bidirectional reference graph
		def build(graph: ViewGraphNode[(Table, Int), (Reference, Boolean)], from: Option[Table] = None): HierarchicalTable = {
			val table = graph.value._1
			
			val parents = graph.leavingEdges.view.filter { _._2 }.map { _.end }
				.toOptimizedSeq.sorted.map { _._1.name }
			val primaryParent = parents.headOption.filterNot { p => from.exists { _.name == p } }
			val otherParents = parents.filterNot { p => from.exists { _.name == p } || primaryParent.contains(p) }
			
			val children = {
				// Case: Primary parent defined elsewhere => Children won't be listed under this node
				if (primaryParent.isEmpty)
					Empty
				else
					graph.leavingEdges.view
						.filterNot { _._2 }.filterNot { edge => from.contains(edge._1.to.table) }
						.toOptimizedSeq.sortBy { _.end }
						.map { edge => build(edge.end, Some(table)) }
			}
			
			HierarchicalTable(table.name, otherParents, primaryParent, children)
		}
	}
	private case class HierarchicalTable(name: String, otherParents: Seq[String] = Empty,
	                                     primaryParent: Option[String] = None, children: Seq[HierarchicalTable] = Empty)
		extends TreeLike[String, HierarchicalTable]
	{
		// IMPLEMENTED  ------------------------
		
		override def self = this
		override def nav = name
		
		override implicit def navEquals: EqualsFunction[String] = EqualsFunction.default
		
		override protected def createCopy(nav: String, children: Seq[HierarchicalTable]) =
			copy(name = nav, children = children)
		override protected def newNode(content: String) = HierarchicalTable(content)
		
		
		// OTHER    --------------------------
		
		def print(indentation: Int = 0): Unit = {
			val otherParentsNote = if (otherParents.isEmpty) "" else s"also depends from ${ otherParents.mkString(", ") }"
			val parentsNote = primaryParent match {
				case Some(primary) => s" (see hierarchy under $primary${ otherParentsNote.prependIfNotEmpty(", ") })"
				case None => otherParentsNote.mapIfNotEmpty { n => s" ($n)" }
			}
			println(s"${ "\t" * indentation }- $name$parentsNote")
			children.foreach { _.print(indentation + 1) }
		}
	}
}

/**
  * An interface for creating utility commands for command line consoles interacting with a database
  * @author Mikko Hilpinen
  * @since 28.10.2024, v1.20.1
  */
class ConsoleCommands(implicit context: VaultContext)
{
	import ConsoleCommands._
	
	// ATTRIBUTES   -----------------------
	
	/**
	  * Lists tables in the targeted database.
	  * Supports optional filtering.
	  */
	val listTables = Command("list-tables", "tables", "Lists database tables")(
		ArgumentSchema("filter",
			help = "A filter applied to the listed table names. \nCase-insensitive, and doesn't need to include special characters (e.g. _). \nOptional. If omitted, all tables will be listed.")) {
		args =>
			val filter: Table => Boolean = args("filter").string match {
				case Some(f) =>
					val modified = f.filter { _.isLetterOrDigit }.toLowerCase
					_.name.filter { _.isLetterOrDigit }.toLowerCase.contains(modified)
				case None => _ => true
			}
			val tables = context.tables.all(context.databaseName).view.filter(filter).map { _.name }.toVector.sorted
			if (tables.isEmpty)
				println("No tables found")
			else
				tables.foreach(println)
	}
	
	/**
	  * A command which lists dependencies between tables.
	  * Can be targeted, or used to describe references concerning a single table.
	  */
	val listDependencies = Command("list-dependencies", "dependencies",
		"Shows which tables depend from which (via foreign keys)")(
		ArgumentSchema("table", help = "The primary table used as the \"origin\" of this search. \nOptional. If omitted, full table hierarchy is displayed instead.")) {
		args =>
			args("table").string match {
				case Some(tableName) =>
					Try { context.table(tableName) } match {
						case Success(table) =>
							// Writes the parent tables
							val parents = References.parentsTree(table).map { _.name }
							if (parents.children.nonEmpty) {
								println("Parents:")
								parents.children.foreach { printTree(_) }
							}
								
							// Writes the table hierarchy downwards
							val tableReferenceCounts = References.referenceTree(table).allNodesIterator
								.map { n => n.nav -> n.size }.toMap
							val graph = References.toBiDirectionalLinkGraphFrom(table)
								.mapValues { t => t -> tableReferenceCounts(t) }
							println()
							HierarchicalTable.build(graph).print()
						
						case _ => println(s"Table $tableName doesn't exist in database ${ context.databaseName }")
					}
				case None =>
					// Counts the number of dependencies for each table
					val tables = context.tables.all(context.databaseName)
						.map { table =>
							val referenceCount = References.referenceTree(table).size
							table -> referenceCount
						}
						.toVector.sorted
					val tableCounts = tables.toMap
					
					// Writes hierarchies until all tables have been mentioned
					val writtenTables = mutable.Set[String]()
					tables.foreach { case (table, _) =>
						if (!writtenTables.contains(table.name)) {
							val graph = References.toBiDirectionalLinkGraphFrom(table)
								.mapValues { t => t -> tableCounts(t) }
							val hierarchy = HierarchicalTable.build(graph)
							println()
							hierarchy.print()
							
							writtenTables ++= hierarchy.allNavsIterator
						}
					}
			}
	}
	
	
	// OTHER    --------------------------
	
	private def printTree[T <: TreeLike[_, T]](tree: T, indentation: Int = 0): Unit = {
		val linear = linearPathFrom(tree)
		println(s"${ "\t" * indentation }- ${ linear.view.map { _.nav }.mkString(" -> ") }")
		linear.last.children.foreach { printTree(_, indentation + 1) }
	}
	
	private def linearPathFrom[T <: TreeLike[_, T]](tree: T): Seq[T] = {
		if (tree.children.hasSize(1))
			tree +: linearPathFrom(tree.children.head)
		else
			Single(tree)
	}
}
