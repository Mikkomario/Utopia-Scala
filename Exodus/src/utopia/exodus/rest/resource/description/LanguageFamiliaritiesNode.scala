package utopia.exodus.rest.resource.description

import utopia.exodus.database.access.many.{DbDescriptions, DbLanguageFamiliarities}
import utopia.exodus.rest.util.AuthorizedContext
import utopia.metropolis.model.combined.language.DescribedLanguageFamiliarity
import utopia.metropolis.model.stored.description.DescriptionLink
import utopia.metropolis.model.stored.language.LanguageFamiliarity
import utopia.nexus.result.Result
import utopia.vault.database.Connection

object LanguageFamiliaritiesNode extends PublicDescriptionsNodeFactory[LanguageFamiliaritiesNode]
{
	override def apply(authorization: (AuthorizedContext, => Result, Connection) => Result) =
		new LanguageFamiliaritiesNode(authorization)
}

/**
  * Used for accessing language familiarity levels and their descriptions
  * @author Mikko Hilpinen
  * @since 25.7.2020, v1
  */
class LanguageFamiliaritiesNode(authorization: (AuthorizedContext, => Result, Connection) => Result)
	extends PublicDescriptionsNode[LanguageFamiliarity, DescribedLanguageFamiliarity]
{
	override protected def authorize(onAuthorized: => Result)(implicit context: AuthorizedContext, connection: Connection) =
		authorization(context, onAuthorized, connection)
	
	override protected def items(implicit connection: Connection) = DbLanguageFamiliarities.all
	
	override protected def descriptionsAccess = DbDescriptions.ofAllLanguageFamiliarities
	
	override protected def idOf(item: LanguageFamiliarity) = item.id
	
	override protected def combine(item: LanguageFamiliarity, descriptions: Set[DescriptionLink]) =
		DescribedLanguageFamiliarity(item, descriptions)
	
	override def name = "language-familiarities"
}
