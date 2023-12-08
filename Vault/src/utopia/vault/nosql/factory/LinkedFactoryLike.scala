package utopia.vault.nosql.factory

import utopia.vault.database.{Connection, References}
import utopia.vault.nosql.factory.row.FromRowFactory
import utopia.vault.sql.{Condition, Exists, JoinType}
import utopia.vault.sql.JoinType.Inner

/**
  * A common trait for linked factory classes
  * @author Mikko Hilpinen
  * @since 28.6.2021, v1.8
  */
trait LinkedFactoryLike[+Parent, +Child] extends FromResultFactory[Parent]
{
	// ABSTRACT -----------------------------
	
	/**
	  * @return Factory used for reading linked items from DB results
	  */
	def childFactory: FromRowFactory[Child]
	
	/**
	  * @return Whether there always exists at least one child item for each parent item
	  */
	def isAlwaysLinked: Boolean
	
	
	// COMPUTED -----------------------------
	
	/**
	  * @return The primary table used by the child instance
	  */
	def childTable = childFactory.table
	
	/**
	  * @return Index column to use in the child instance context
	  */
	def childIndex = childTable.primaryColumn.getOrElse {
		val referenceColumns = References.between(table, childTable).map { ref =>
			if (ref.from.table == table) ref.to.column else ref.from.column
		}
		referenceColumns.find { !_.allowsNull }.getOrElse { referenceColumns.head }
	}
	
	/**
	  * @return A condition that only accepts rows where there exists a linked child item
	  */
	def isLinkedCondition = if (isAlwaysLinked) Condition.alwaysTrue else childIndex.isNotNull
	/**
	  * @return A condition that only accepts rows where there doesn't exist a linked child item
	  */
	def notLinkedCondition = if (isAlwaysLinked) Condition.alwaysFalse else childIndex.isNull
	
	
	// IMPLEMENTED  -------------------------
	
	override def joinedTables = childFactory.tables
	
	override def joinType = if (isAlwaysLinked) Inner else JoinType.Left
	
	
	// OTHER    -----------------------------
	
	/**
	  * @param condition A search condition
	  * @param connection Implicit database connection
	  * @return Whether there exists a row where the specified condition is met and there exists a link
	  *         between a parent and a child at the same time
	  */
	def existsLinkWhere(condition: Condition)(implicit connection: Connection) = {
		if (isAlwaysLinked)
			Exists(target, condition)
		else
			Exists(target, condition && childIndex.isNotNull)
	}
	
	/**
	  * @param condition A search condition
	  * @param connection Implicit database connection
	  * @return Whether there exists a row where the specified condition is met and there doesn't
	  *         exist a link between a parent and a child
	  */
	def isWithoutLinkWhere(condition: Condition)(implicit connection: Connection) =
		!isAlwaysLinked || Exists(target, condition && childIndex.isNull)
}
