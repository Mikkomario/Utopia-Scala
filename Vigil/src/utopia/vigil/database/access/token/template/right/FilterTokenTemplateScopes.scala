package utopia.vigil.database.access.token.template.right

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.sql.Condition
import utopia.vigil.database.access.scope.right.FilterScopeRights
import utopia.vigil.database.storable.token.TokenTemplateScopeDbModel

/**
  * Common trait for access points which may be filtered based on token template scope properties
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
trait FilterTokenTemplateScopes[+Repr] extends FilterScopeRights[Repr]
{
	// COMPUTED	--------------------
	
	/**
	  * Model that defines token template scope database properties
	  */
	def model = TokenTemplateScopeDbModel
	
	
	// OTHER	--------------------
	
	/**
	  * @param templateId template id to target
	  * @return Copy of this access point that only includes token template scopes with the specified 
	  * template id
	  */
	def ofTemplate(templateId: Int) = filter(model.templateId.column <=> templateId)
	
	/**
	  * @param templateIds Targeted template ids
	  * @return Copy of this access point that only includes token template scopes where template id is 
	  * within the specified value set
	  */
	def ofTemplates(templateIds: IterableOnce[Int]) = filter(Condition.indexIn(model.templateId, templateIds))
}

