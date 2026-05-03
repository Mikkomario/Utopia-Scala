package utopia.vigil.database.storable.scope

import utopia.vault.model.immutable.Table
import utopia.vigil.database.props.scope.ScopeRightDbProps

object ScopeRightDbModel
{
	// OTHER	--------------------
	
	/**
	  * @param table The primarily targeted table
	  * @param props Targeted database properties
	  * @return A factory used for constructing scope right models using the specified configuration
	  */
	def factory(table: Table, props: ScopeRightDbProps) = ScopeRightDbModelFactory(table, props)
}

/**
  * Common trait for database interaction models dealing with scope rights
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
trait ScopeRightDbModel extends ScopeRightDbModelLike[ScopeRightDbModel]

