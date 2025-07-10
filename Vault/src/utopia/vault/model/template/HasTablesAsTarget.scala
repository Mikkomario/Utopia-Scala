package utopia.vault.model.template

import utopia.vault.model.immutable.Table
import utopia.vault.sql.JoinType

/**
  * Common trait for classes which specify an SQL target by joining tables
  * @author Mikko Hilpinen
  * @since 10.07.2025, v1.22
  */
trait HasTablesAsTarget extends HasTable with HasTarget
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
	  * @return The table(s) used by this class (never empty)
	  */
	def tables = table +: joinedTables
	/**
	  * @return Whether this class targets a single table only
	  */
	def targetsSingleTable = joinedTables.isEmpty
	
	
	// IMPLEMENTED  --------------------
	
	def target = table.join(joinedTables, joinType)
}
