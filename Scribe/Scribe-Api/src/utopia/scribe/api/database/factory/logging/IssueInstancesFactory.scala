package utopia.scribe.api.database.factory.logging

import utopia.flow.collection.CollectionExtensions._
import utopia.scribe.core.model.combined.logging.IssueInstances
import utopia.vault.model.immutable.{Result, Table}
import utopia.vault.nosql.factory.FromResultFactory
import utopia.vault.sql.JoinType
import utopia.vault.sql.JoinType.Inner
import utopia.vault.util.ErrorHandling

/**
  * Used for reading issue data, along with its variants and occurrences
  * @author Mikko Hilpinen
  * @since 25.5.2023, v0.1
  */
object IssueInstancesFactory extends FromResultFactory[IssueInstances]
{
	// COMPUTED ----------------------------
	
	private def parentFactory = IssueFactory
	private def childFactory = IssueVariantInstancesFactory
	
	
	// IMPLEMENTED  ------------------------
	
	override def table: Table = parentFactory.table
	override def joinedTables: Seq[Table] = childFactory.tables
	override def joinType: JoinType = Inner
	
	override def defaultOrdering = None
	
	override def apply(result: Result): Vector[IssueInstances] = result.grouped(table, childFactory.table).iterator
		.map { case (_, (row, variantData)) =>
			parentFactory(row).map { issue =>
				val variants = childFactory(Result(variantData))
				IssueInstances(issue, variants)
			}
		}.toTry.getOrMap { error => ErrorHandling.modelParsePrinciple.handle(error); Vector() }
}
