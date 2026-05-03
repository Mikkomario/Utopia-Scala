package utopia.vigil.database.reader.scope

import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties
import utopia.vault.model.immutable.Table
import utopia.vigil.database.props.scope.ScopeRightDbProps
import utopia.vigil.model.partial.scope.ScopeRightData
import utopia.vigil.model.stored.scope.ScopeRight

import java.time.Instant

object ScopeRightDbReader
{
	// OTHER	--------------------
	
	/**
	  * @param table   Table from which data is read
	  * @param dbProps Database properties used when reading column data
	  * @return A factory used for parsing scope rights from database model data
	  */
	def apply(table: Table, dbProps: ScopeRightDbProps): ScopeRightDbReader = _ScopeRightDbReader(table, 
		dbProps)
	
	
	// NESTED	--------------------
	
	/**
	  * @param table   Table from which data is read
	  * @param dbProps Database properties used when reading column data
	  */
	private case class _ScopeRightDbReader(table: Table, 
		dbProps: ScopeRightDbProps) extends ScopeRightDbReader
	{
		// IMPLEMENTED	--------------------
		
		/**
		  * @param model   Model from which additional data may be read
		  * @param id      Id to assign to the read/parsed scope right
		  * @param scopeId scope id to assign to the new scope right
		  * @param created created to assign to the new scope right
		  * @param usable  usable to assign to the new scope right
		  */
		override protected def apply(model: HasProperties, id: Int, scopeId: Int, created: Instant, 
			usable: Boolean) = 
			ScopeRight(id, ScopeRightData(scopeId, created, usable))
	}
}

/**
  * Common trait for factories which parse scope right data from database-originated models
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
trait ScopeRightDbReader extends ScopeRightDbReaderLike[ScopeRight]

