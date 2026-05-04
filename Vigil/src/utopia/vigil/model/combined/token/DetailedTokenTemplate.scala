package utopia.vigil.model.combined.token

import utopia.vigil.model.factory.token.TokenTemplateFactoryWrapper
import utopia.vigil.model.partial.token.TokenTemplateData
import utopia.vigil.model.stored.token.{TokenGrantRight, TokenTemplate, TokenTemplateScope}

/**
 * Includes scope & grant right information with a token template
 * @author Mikko Hilpinen
 * @since 04.05.2026, v0.1
 */
case class DetailedTokenTemplate(id: Int, data: TokenTemplateData, scopeLinks: Seq[TokenTemplateScope],
                                 grantRights: Seq[TokenGrantRight])
	extends TokenTemplate with TokenTemplateFactoryWrapper[TokenTemplateData, DetailedTokenTemplate]
{
	override def withId(id: Int): TokenTemplate = copy(id = id)
	override protected def wrap(factory: TokenTemplateData): DetailedTokenTemplate = copy(data = factory)
}
