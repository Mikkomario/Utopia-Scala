package utopia.vigil.database.reader.token

import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties
import utopia.vigil.database.reader.scope.ScopeRightDbReaderLike
import utopia.vigil.database.storable.token.TokenScopeDbModel
import utopia.vigil.model.partial.token.TokenScopeData
import utopia.vigil.model.stored.token.TokenScope

import java.time.Instant

/**
  * Used for reading token scope data from the DB
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
object TokenScopeDbReader extends ScopeRightDbReaderLike[TokenScope]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Model that specifies how the data is read
	  */
	override val dbProps = TokenScopeDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def table = dbProps.table
	
	/**
	  * @param model   Model from which additional data may be read
	  * @param id      Id to assign to the read/parsed scope right
	  * @param scopeId scope id to assign to the new scope right
	  * @param created created to assign to the new scope right
	  * @param usable  usable to assign to the new scope right
	  */
	override protected def apply(model: HasProperties, id: Int, scopeId: Int, created: Instant, 
		usable: Boolean) = 
		TokenScope(id, TokenScopeData(scopeId = scopeId, tokenId = model(dbProps.tokenId.name).getInt, 
			created = created, usable = usable))
}

