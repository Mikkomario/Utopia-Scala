package utopia.vault.model.template

import utopia.vault.model.immutable.Table
import utopia.vault.sql.JoinType

/**
  * Common trait for classes which specify an SQL target by joining tables
  * @author Mikko Hilpinen
  * @since 10.07.2025, v1.22
  */
trait HasTablesAsTarget extends HasTable with HasTables with HasTarget
{
	// ABSTRACT -------------------------
	
	/**
	  * @return The tables that are joined to form the complete [[target]]
	  */
	def joinedTables: Seq[Table]
	/**
	  * @return Joining style used
	  */
	def joinType: JoinType
	
	
	// COMPUTED ------------------------
	
	
	/**
	  * @return Whether this class targets a single table only
	  */
	def targetsSingleTable = joinedTables.isEmpty
	
	
	// IMPLEMENTED  --------------------
	
	/**
	 * @return The table(s) used by this class (never empty)
	 */
	override def tables = table +: joinedTables
	override def target = table.join(joinedTables, joinType)
}
