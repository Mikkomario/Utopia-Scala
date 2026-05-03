package utopia.vigil.database.reader.token

import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties
import utopia.vigil.database.reader.scope.ScopeRightDbReaderLike
import utopia.vigil.database.storable.token.TokenTemplateScopeDbModel
import utopia.vigil.model.partial.token.TokenTemplateScopeData
import utopia.vigil.model.stored.token.TokenTemplateScope

import java.time.Instant

/**
  * Used for reading token template scope data from the DB
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
object TokenTemplateScopeDbReader extends ScopeRightDbReaderLike[TokenTemplateScope]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Model that specifies how the data is read
	  */
	override val dbProps = TokenTemplateScopeDbModel
	
	
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
		TokenTemplateScope(id, TokenTemplateScopeData(scopeId = scopeId, 
			templateId = model(dbProps.templateId.name).getInt, created = created, usable = usable))
}

