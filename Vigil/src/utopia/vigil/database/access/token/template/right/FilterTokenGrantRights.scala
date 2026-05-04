package utopia.vigil.database.access.token.template.right

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.template.Filterable
import utopia.vault.sql.Condition
import utopia.vigil.database.storable.token.TokenGrantRightDbModel

/**
  * Common trait for access points which may be filtered based on token grant right properties
  * @author Mikko Hilpinen
  * @since 04.05.2026, v0.1
  */
trait FilterTokenGrantRights[+Repr] extends Filterable[Repr]
{
	// COMPUTED	--------------------
	
	/**
	  * Model that defines token grant right database properties
	  */
	def model = TokenGrantRightDbModel
	
	
	// OTHER	--------------------
	
	/**
	  * @param ownerTemplateId owner template id to target
	  * @return Copy of this access point that only includes token grant rights with the specified owner 
	  * template id
	  */
	def ofTemplate(ownerTemplateId: Int) = filter(model.ownerTemplateId.column <=> ownerTemplateId)
	
	/**
	  * @param ownerTemplateIds Targeted owner template ids
	  * @return Copy of this access point that only includes token grant rights where owner template id is 
	  * within the specified value set
	  */
	def ofTemplates(ownerTemplateIds: IterableOnce[Int]) = 
		filter(Condition.indexIn(model.ownerTemplateId, ownerTemplateIds))
	
	/**
	  * @param grantedTemplateId granted template id to target
	  * @return Copy of this access point that only includes token grant rights with the specified granted 
	  * template id
	  */
	def toUseTemplate(grantedTemplateId: Int) = filter(model.grantedTemplateId.column <=> grantedTemplateId)
	
	/**
	  * @param grantedTemplateIds Targeted granted template ids
	  * @return Copy of this access point that only includes token grant rights where granted template id is 
	  * within the specified value set
	  */
	def toUseTemplates(grantedTemplateIds: IterableOnce[Int]) = 
		filter(Condition.indexIn(model.grantedTemplateId, grantedTemplateIds))
}

