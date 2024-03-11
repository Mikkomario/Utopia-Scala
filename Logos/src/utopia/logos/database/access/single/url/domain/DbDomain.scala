package utopia.logos.database.access.single.url.domain

import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.Condition
import utopia.logos.database.factory.url.DomainFactory
import utopia.logos.database.model.url.DomainModel
import utopia.logos.model.stored.url.Domain

/**
  * Used for accessing individual domains
  * @author Mikko Hilpinen
  * @since 16.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
object DbDomain extends SingleRowModelAccess[Domain] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = DomainModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = DomainFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted domain
	  * @return An access point to that domain
	  */
	def apply(id: Int) = DbSingleDomain(id)
	
	/**
	  * @param condition Filter condition to apply in addition to this root view's condition. Should yield
	  *  unique domains.
	  * @return An access point to the domain that satisfies the specified condition
	  */
	protected def filterDistinct(condition: Condition) = UniqueDomainAccess(mergeCondition(condition))
}

