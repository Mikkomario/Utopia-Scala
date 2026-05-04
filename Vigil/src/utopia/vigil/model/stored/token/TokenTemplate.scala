package utopia.vigil.model.stored.token

import utopia.vault.store.{FromIdFactory, StandardStoredFactory, StoredModelConvertible}
import utopia.vigil.database.access.token.template.AccessTokenTemplate
import utopia.vigil.model.factory.token.TokenTemplateFactoryWrapper
import utopia.vigil.model.partial.token.TokenTemplateData

object TokenTemplate extends StandardStoredFactory[TokenTemplateData, TokenTemplate]
{
	// ATTRIBUTES	--------------------
	
	override val dataFactory = TokenTemplateData
	
	
	// OTHER	--------------------
	
	/**
	  * Creates a new token template
	  * @param id   ID of this token template in the database
	  * @param data Wrapped token template data
	  * @return token template with the specified id and wrapped data
	  */
	def apply(id: Int, data: TokenTemplateData): TokenTemplate = _TokenTemplate(id, data)
	
	
	// NESTED	--------------------
	
	/**
	  * Concrete implementation of the token template trait
	  * @param id   ID of this token template in the database
	  * @param data Wrapped token template data
	  * @author Mikko Hilpinen
	  * @since 04.05.2026
	  */
	private case class _TokenTemplate(id: Int, data: TokenTemplateData) extends TokenTemplate
	{
		// IMPLEMENTED	--------------------
		
		override def withId(id: Int) = copy(id = id)
		
		override protected def wrap(data: TokenTemplateData) = copy(data = data)
	}
}

/**
  * Represents a token template that has already been stored in the database. 
  * A template or a mold for creating new tokens
  * @author Mikko Hilpinen
  * @since 04.05.2026, v0.1
  */
trait TokenTemplate 
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
}

