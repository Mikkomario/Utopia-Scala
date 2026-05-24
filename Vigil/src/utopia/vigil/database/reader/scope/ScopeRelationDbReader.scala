package utopia.vigil.database.reader.scope

import utopia.flow.generic.model.immutable.Model
import utopia.vault.model.template.HasTableAsTarget
import utopia.vault.nosql.read.DbRowReader
import utopia.vault.nosql.read.parse.ParseTableModel
import utopia.vigil.database.storable.scope.ScopeRelationDbModel
import utopia.vigil.model.partial.scope.ScopeRelationData
import utopia.vigil.model.stored.scope.ScopeRelation

import scala.util.Success

/**
  * Used for reading scope relation data from the DB
  * @author Mikko Hilpinen
  * @since 24.05.2026, v0.1
  */
object ScopeRelationDbReader 
	extends DbRowReader[ScopeRelation] with ParseTableModel[ScopeRelation] with HasTableAsTarget
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Model that specifies how the data is read
	  */
	val model = ScopeRelationDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def table = model.table
	
	override def fromValid(valid: Model) = 
		Success(ScopeRelation(valid(this.model.id.name).getInt, 
			ScopeRelationData(parentScopeId = valid(this.model.parentScopeId.name).getInt, 
			grantedScopeId = valid(this.model.grantedScopeId.name).getInt)))
}

