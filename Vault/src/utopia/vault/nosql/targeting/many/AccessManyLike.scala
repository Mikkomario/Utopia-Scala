package utopia.vault.nosql.targeting.many

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.flow.generic.model.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.model.enumeration.SelectTarget
import utopia.vault.model.immutable.{Column, Result}
import utopia.vault.sql.{Condition, OrderBy, Select, SqlSegment, Update, Where}

/**
  * An interface used for accessing multiple items with each search query
  * @author Mikko Hilpinen
  * @since 16.05.2025, v1.21
  */
trait AccessManyLike[+A, +Repr] extends TargetingManyLike[A, Repr]
{
	// ABSTRACT ----------------------
	
	def selectTarget: SelectTarget
	
	def ordering: Option[OrderBy]
	
	// def alsoSelecting(additionalTarget: SelectTarget): Repr
	
	protected def finalizeStatement(statement: SqlSegment): SqlSegment
	
	protected def parse(result: Result): Seq[A]
	
	
	// COMPUTED ----------------------
	
	protected def appliedCondition = accessCondition.filterNot { _.isAlwaysTrue }
	
	protected def select = selectTarget.toSelect(target)
	
	protected def keys = {
		val selection = selectTarget
		target.tables.view.flatMap { _.primaryColumn }.filter(selection.contains).toOptimizedSeq
	}
	
	
	// IMPLEMENTED  ------------------
	
	override def pull(implicit connection: Connection): Seq[A] = pullManyWith[A](select)(parse)
	override def apply(column: Column, distinct: Boolean)(implicit connection: Connection) =
		pullManyWith(Select.distinctIf(target, column, distinct)) { _.rowValues }
	override def apply(columns: Seq[Column])(implicit connection: Connection) =
		pullManyWith(Select(target, columns)) { _.rows.map { row => columns.map(row.apply) } }
	
	override def update(column: Column, value: Value)(implicit connection: Connection): Boolean =
		connection(Update(target, column, value) + accessCondition.map { Where(_) }).updatedRows
		
	
	// OTHER    ---------------------------
	
	protected def pullWith[B](statement: SqlSegment, emptyResult: => B, condition: Option[Condition] = appliedCondition,
	                          ordering: Option[OrderBy] = this.ordering)
	                         (parse: Result => B)
	                         (implicit connection: Connection) =
	{
		if (condition.exists { _.isAlwaysFalse })
			emptyResult
		else
			parse(connection(completeStatement(statement, condition, ordering)))
	}
	protected def pullManyWith[B](statement: SqlSegment, condition: Option[Condition] = appliedCondition,
	                              ordering: Option[OrderBy] = this.ordering)
	                             (parse: Result => Seq[B])
	                             (implicit connection: Connection) =
		pullWith[Seq[B]](statement, Empty, condition, ordering)(parse)
	
	protected def completeStatement(statement: SqlSegment,
	                                condition: Option[Condition] = appliedCondition,
	                                ordering: Option[OrderBy] = this.ordering) =
		finalizeStatement(statement + condition.map(Where.apply) + ordering)
}
