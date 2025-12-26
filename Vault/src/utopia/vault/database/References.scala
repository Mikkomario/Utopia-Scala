package utopia.vault.database

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.caching.cache.Cache
import utopia.flow.collection.immutable.caching.iterable.CachingSeq
import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq, Pair, ViewGraphNode}
import utopia.flow.error.EnvironmentNotSetupException
import utopia.flow.generic.model.immutable.Value
import utopia.flow.view.immutable.View
import utopia.flow.view.mutable.Pointer
import utopia.vault.context.VaultContext.log
import utopia.vault.model.error.NoReferenceFoundException
import utopia.vault.model.immutable.{Column, Reference, ReferencePoint, Table, TableColumn}
import utopia.vault.sql.{Update, Where}

import scala.collection.immutable.HashSet

/**
  * The references object keeps track of all references between different tables in a multiple
  * databases. The object is used for accessing the reference data. The actual data must be set
  * into the object before it can be properly used
  * @author Mikko Hilpinen
  * @since 28.5.2017
  */
object References
{
	// ATTRIBUTES    ---------------------
	
	/**
	  * A pointer that contains all initialized references, mapped by database name
	  */
	private val referencesP = Pointer.eventful(Map[String, Set[Reference]]())
	/**
	  * A deep cache with the following keys:
	  *     1. Database name
	  *     1. Table name
	  *     1. Column (property) name
	  *
	  * Values are individual references originating from the specified column
	  */
	private val fromIndex = Cache.clearable { dbName: String =>
		referencesP.value.get(dbName) match {
			case Some(references) =>
				references.groupToSeqsBy { _.from.tableName }
					.view.mapValues { _.iterator.map { ref => ref.from.name -> ref }.toMap }.toMap
			
			// FIXME: Somehow we arrive here sometimes at program startup
			//  (although we shouldn't have any Table instances before the setup function is called)
			case None => throw EnvironmentNotSetupException(s"References for database '$dbName' haven't been specified")
		}
	}
	/**
	  * A deep cache with the following keys:
	  *     1. Database name
	  *     1. Table name
	  *     1. Column (property) name
	  *
	  * Values are references made to that column
	  */
	private val toIndex = Cache.clearable { dbName: String =>
		referencesP.value.get(dbName) match {
			case Some(references) =>
				references.groupToSeqsBy { _.to.tableName }
					.view.mapValues { _.groupBy { _.to.name }.withDefaultValue(Empty) }.toMap
					.withDefaultValue(Map[String, Seq[Reference]]().withDefaultValue(Empty))
				
			case None => throw EnvironmentNotSetupException(s"References for database '$dbName' haven't been specified")
		}
	}
	
	
	// INITIAL CODE ----------------------
	
	// Resets indices when references are updated or removed
	referencesP.addContinuousListener { refs =>
		refs.oldValue.filterNot { case (dbName, oldRefs) => refs.newValue.get(dbName).contains(oldRefs) }.keys
			.foreach { dbName =>
				fromIndex.clear(dbName)
				toIndex.clear(dbName)
			}
	}
	
	
	// COMPUTED --------------------------
	
	/**
	  * Creates a new reference graph that only contains direct links from the origin table to the target table.
	  * I.e. the edges point the same direction as the table references.
	  * @return A function that accepts a table and yields a reference graph node representing the specified table
	  *         (lazily initialized)
	  */
	def linkGraph = ViewGraphNode
		.iterate { table: Table => from(table).map { ref => View.fixed(ref) -> View.fixed(ref.to.table) } }
	/**
	  * Creates a new reference graph where leaving edges are the references coming **to** the node table.
	  * I.e. all the references are associated with the tables they point towards, not where they originate from.
	  * @return A function that accepts a table and yields a reference graph node representing the specified table
	  *         (lazily initialized)
	  */
	def reverseLinkGraph = ViewGraphNode
		.iterate { table: Table => to(table).map { ref => View.fixed(ref) -> View.fixed(ref.from.table) } }
	/**
	  * Creates a new reference graph that contains each reference twice:
	  * Once in the table from which the reference originates and once in the table to which the reference points to.
	  * In other words, all the edges in the resulting graph go both ways.
	  * @return A function that accepts a table and yields a reference graph
	  *         node representing the specified table (lazily initialized).
	  *         Edges containing true as the second value are pointing in the same direction as the reference,
	  *         and those with false to the opposite.
	  */
	def biDirectionalLinkGraph = ViewGraphNode.iterate { table: Table =>
		from(table).map { ref => View.fixed(ref -> true) -> View.fixed(ref.to.table) } ++
			to(table).map { ref => View.fixed(ref -> false) -> View.fixed(ref.from.table) }
	}
	
	
	// IMPLEMENTED  ----------------------
	
	override def toString = {
		val refs = referencesP.value
		if (refs.isEmpty)
			"No references recorded"
		else
			refs.view.map { case (dbName, refs) => s"$dbName: [${ refs.mkString(", ") }]" }.mkString("\n")
	}
	
	
	// OTHER METHODS    ------------------
	
	/**
	  * Sets up reference data for a single database. Existing data will be preserved.
	  * @param databaseName the name of the database
	  * @param references a set of references in the database
	  */
	def setup(databaseName: String, references: IterableOnce[Reference]) = referencesP.update { current =>
		current.updatedWith(databaseName) {
			case Some(existing) => Some(existing ++ references)
			case None => Some(Set.from(references))
		}
	}
	/**
	  * Sets up reference data for a single database. Each pair should contain 4 elements:
	  * 1) referencing table, 2) name of the referencing property, 3) referenced table,
	  * 4) name of the referenced property.
	  */
	@deprecated("Deprecated for removal", "v1.22")
	def setup(sets: IterableOnce[(Table, String, Table, String)]): Unit = {
		// Converts the tuple data into a reference set
		val references = sets.iterator.flatMap { case (table1, name1, table2, name2) =>
			Reference(table1, name1, table2, name2) }.toSet
		references.groupBy { _.from.table.databaseName }.foreach { case (dbName, refs) => setup(dbName, refs) }
	}
	/**
	  * Sets up reference data for a single database. Each pair should contain 4 elements:
	  * 1) referencing table, 2) name of the referencing property, 3) referenced table,
	  * 4) name of the referenced property.
	  */
	@deprecated("Deprecated for removal", "v1.22")
	def setup(firstSet: (Table, String, Table, String), more: (Table, String, Table, String)*): Unit =
		setup(HashSet(firstSet) ++ more)
	
	/**
	  * @param column A column from which a reference is acquired
	  * @throws NoReferenceFoundException If the specified column didn't refer to another column
	  * @return Reference from the specified column.
	  */
	@throws[NoReferenceFoundException]("If the specified column didn't refer to another column")
	def from(column: TableColumn) =
		findFrom(column).getOrElse {
			throw new NoReferenceFoundException(s"${ column.sqlName } doesn't refer to any other column")
		}
	@deprecated("Deprecated for removal", "v1.22")
	def from(table: Table, column: Column): Reference = from(TableColumn(table, column))
	@deprecated("Deprecated for removal", "v1.22")
	def from(table: Table, columnName: String): Option[TableColumn] =
		ReferencePoint(table, columnName).flatMap(findFrom).map { _.to }
	/**
	  * @param table Table from which references are made
	  * @return All references originating from the specified table
	  */
	def from(table: Table) = fromIndex(table.databaseName).get(table.name) match {
		case Some(refs) => refs.values
		case None => Empty
	}
	/**
	  * @param column A column that possibly references another column
	  * @return Reference made from that column. None if the specified column didn't refer to another column.
	  */
	def findFrom(column: TableColumn) =
		fromIndex(column.table.databaseName).get(column.table.name).flatMap { _.get(column.name) }
	
	/**
	  * @param column A column
	  * @throws NoReferenceFoundException If the specified column didn't refer to another column
	  * @return Column referenced from the specified column
	  */
	@throws[NoReferenceFoundException]("If the specified column didn't refer to another column")
	def referencedFrom(column: TableColumn) = from(column).to
	/**
	  * @param table A table
	  * @return All columns referenced from the specified table
	  */
	def referencedFrom(table: Table) = from(table).map { _.to }
	/**
	  * Finds all tables referenced from a certain table
	  */
	def tablesReferencedFrom(table: Table) = from(table).map { _.to.table }
	/**
	  * @param column A column that possibly references another column
	  * @return The column referred from the specified column.
	  *         None if the specified column didn't refer to another column.
	  */
	def findReferencedFrom(column: TableColumn) = findFrom(column).map { _.to }
	
	/**
	  * @param column A column
	  * @return All references that refer to the specified column
	  */
	def to(column: TableColumn) = toIndex(column.table.databaseName)(column.table.name)(column.name)
	/**
	  * Finds all places where the provided reference point is referenced
	  * @param table the table that contains the column
	  * @param column the referenced column
	  * @return All reference points that target the specified reference point
	  */
	@deprecated("Deprecated for removal", "v1.22")
	def to(table: Table, column: Column): Seq[Reference] = to(TableColumn(table, column))
	/**
	  * Finds all places where the provided reference point is referenced
	  * @param table the table that contains the column
	  * @param columnName the name of the referenced column
	  * @return All reference points that target the specified reference point
	  */
	@deprecated("Deprecated for removal", "v1.22")
	def to(table: Table, columnName: String): Seq[Reference] =
		table.find(columnName) match {
			case Some(point) => to(point)
			case None => Empty
		}
	/**
	  * Finds all references made into a specific table
	  */
	def to(table: Table) = toIndex(table.databaseName)(table.name).valuesIterator.flatten.caching
	/**
	  * @param column A column
	  * @return Columns which refer to the specified column
	  */
	def referencing(column: TableColumn) = to(column).map { _.from }
	/**
	  * Finds all tables that contain references to the specified table
	  */
	def tablesReferencing(table: Table) = to(table).map { _.from.table }
	
	/**
	  * Finds all references between the two tables. The results contain pairings of left side
	  * columns matched with right side columns. The references may go either way
	  */
	def columnsBetween(left: Table, right: Table): CachingSeq[Pair[TableColumn]] =
		collection.View.concat(fromTo(left, right), fromTo(right, left).view.map { _.reverse })
			.iterator.map { ref => ref.ends }.caching
	/**
	  * Finds all references between the two tables. The results contain pairings of left side
	  * columns matched with right side columns. The references may go either way
	  */
	def columnsBetween(tables: Pair[Table]): CachingSeq[Pair[TableColumn]] =
		columnsBetween(tables.first, tables.second)
	/**
	  * Finds a single connection between the two tables
	  * @param tables Left & right table
	  * @return Left side column -> right side column. None if there wasn't a connection between the two tables
	  */
	def connectionBetween(tables: Pair[Table]): Option[Pair[TableColumn]] =
		connectionBetween(tables.first, tables.second)
	/**
	  * Finds a single connection between the two tables
	  * @param left Left side table
	  * @param right Right side table
	  * @return Left side column -> right side column. None if there wasn't a connection between the two tables
	  */
	def connectionBetween(left: Table, right: Table): Option[Pair[TableColumn]] = columnsBetween(left, right).headOption
	
	/**
	  * Finds all references from the left table to the right table. Only one-sided references are included.
	  */
	def fromTo(left: Table, right: Table) = from(left).filter { _.to.table == right }
	/**
	  * Finds all references between the two tables. The reference(s) may point to either direction
	  */
	def between(a: Table, b: Table) = OptimizedIndexedSeq.concat(fromTo(a, b), fromTo(b, a))
	/**
	  * Finds all references between the two tables. The reference(s) may point to either direction
	  */
	def between(tables: Pair[Table]): IndexedSeq[Reference] = between(tables.first, tables.second)
	/**
	  * Finds one reference between two tables
	  * @param a First table
	  * @param b Second table
	  * @return A reference between these two tables. This reference may go either way.
	  */
	def findBetween(a: Table, b: Table) = fromTo(a, b).headOption.orElse { fromTo(b, a).headOption }
	
	/**
	  * Lists all tables that either directly or indirectly refer to the specified table.
	  * Will not include the specified table itself, even if it refers to itself.
	  * @param table Targeted table
	  * @return All tables directly or indirectly referencing the specified table
	  */
	def tablesAffectedBy(table: Table): CachingSeq[Table] =
		toReverseLinkGraphFrom(table).allNodesIterator.drop(1).map { _.value }.caching
	
	/**
	  * Creates a new reference graph that only contains direct links from the origin table to the target table.
	  * I.e. the edges point the same direction as the table references.
	  * @param table The origin node table
	  * @return A reference graph node representing the specified table (lazily initialized)
	  */
	def toLinkGraphFrom(table: Table) = linkGraph(table)
	/**
	  * Creates a new reference graph where leaving edges are the references coming **to** the node table.
	  * I.e. all the references are associated with the tables they point towards, not where they originate from.
	  * @param table The origin node table
	  * @return A reference graph node representing the specified table (lazily initialized)
	  */
	def toReverseLinkGraphFrom(table: Table) = reverseLinkGraph(table)
	/**
	  * Creates a new reference graph that contains each reference twice:
	  * Once in the table from which the reference originates and once in the table to which the reference points to.
	  * In other words, all the edges in the resulting graph go both ways.
	  * @param table The origin node table
	  * @return A reference graph node representing the specified table (lazily initialized).
	  *         Edges containing true as the second value are pointing in the same direction as the reference,
	  *         and those with false to the opposite.
	  */
	def toBiDirectionalLinkGraphFrom(table: Table) =
		biDirectionalLinkGraph(table)
	
	/**
	  * Forms a tree based on table references where the root is the specified table and node children are based on
	  * references. No table is added twice to a single branch, although a table may exist in multiple branches
	  * at the same time.
	  * @param root Table that will form the reference tree root
	  * @return A reference tree where the specified table is the root and tables referencing that table are below it.
	  *         The references in the result point from tree leaves towards the root of the tree.
	  */
	def referenceTree(root: Table) = toReverseLinkGraphFrom(root).toTree.map { _.value }
	/**
	  * Forms a tree based on table references where the root is the specified table and node children are tables
	  * referenced from the higher tables in the tree.
	  * No table is added twice to a single branch, although a table may exist in multiple branches at the same time.
	  * @param root Table that will form the reference tree root
	  * @return A reference tree where the specified table is the root and tables referenced from that table are below it.
	  *         The references in the result point from the root towards the leaves of the tree.
	  */
	def parentsTree(root: Table) = toLinkGraphFrom(root).toTree.map { _.value }
	
	/**
	  * Clears all cached reference data concerning a single database
	  * @param databaseName Name of the database whose references should be cleared
	  */
	def clear(databaseName: String) = referencesP.update { _ - databaseName }
	
	/**
	  * Replaces all references to an individual row to point to another row
	  * @param table Targeted table
	  * @param targetIndex Targeted row index within the targeted table
	  * @param withIndex Replacing row index within the targeted table
	  * @param connection Implicit DB connection
	  * @return Number of references that were updated
	  */
	def replace(table: Table, targetIndex: Value, withIndex: Value)(implicit connection: Connection) = {
		table.primaryColumn match {
			case Some(index) =>
				to(index).iterator
					.map { reference =>
						connection(Update(reference.from, withIndex) + Where(reference.to <=> targetIndex))
							.updatedRowCount
					}
					.sum
			case None => 0
		}
	}
}
