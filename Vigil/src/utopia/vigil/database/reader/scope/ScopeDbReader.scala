package utopia.vigil.database.reader.scope

import utopia.flow.generic.model.immutable.Model
import utopia.vault.model.template.HasTableAsTarget
import utopia.vault.nosql.read.DbRowReader
import utopia.vault.nosql.read.parse.ParseTableModel
import utopia.vigil.database.storable.scope.ScopeDbModel
import utopia.vigil.model.partial.scope.ScopeData
import utopia.vigil.model.stored.scope.Scope

import scala.util.Success

/**
  * Used for reading scope data from the DB
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
object ScopeDbReader extends DbRowReader[Scope] with ParseTableModel[Scope] with HasTableAsTarget
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Model that specifies how the data is read
	  */
	val model = ScopeDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def table = model.table
	
	override def fromValid(valid: Model) = 
		Success(Scope(valid(this.model.id.name).getInt, ScopeData(key = valid(this.model.key.name).getString, 
			parentId = valid(this.model.parentId.name).int)))
}

