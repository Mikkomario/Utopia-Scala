package utopia.vigil.model.stored.token

import utopia.vault.store.{FromIdFactory, StandardStoredFactory, StoredModelConvertible}
import utopia.vigil.database.access.token.template.AccessTokenTemplate
import utopia.vigil.model.factory.token.TokenTemplateFactoryWrapper
import utopia.vigil.model.partial.token.TokenTemplateData

object TokenTemplate extends StandardStoredFactory[TokenTemplateData, TokenTemplate]
{
	// ATTRIBUTES	--------------------
	
	override val dataFactory = TokenTemplateData
}

/**
  * Represents a token template that has already been stored in the database. 
  * A template or a mold for creating new tokens
  * @param id   ID of this token template in the database
  * @param data Wrapped token template data
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class TokenTemplate(id: Int, data: TokenTemplateData) 
	extends StoredModelConvertible[TokenTemplateData] with FromIdFactory[Int, TokenTemplate] 
		with TokenTemplateFactoryWrapper[TokenTemplateData, TokenTemplate]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this token template in the database
	  */
	def access = AccessTokenTemplate(id)
	
	
	// IMPLEMENTED	--------------------
	
	override protected def wrappedFactory = data
	
	override def withId(id: Int) = copy(id = id)
	
	override protected def wrap(data: TokenTemplateData) = copy(data = data)
}

