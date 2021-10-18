package utopia.exodus.rest.resource.description

import utopia.citadel.database.access.many.language.DbLanguages
import utopia.exodus.rest.util.AuthorizedContext
import utopia.metropolis.model.cached.LanguageIds
import utopia.metropolis.model.combined.language.DescribedLanguage
import utopia.metropolis.model.enumeration.ModelStyle.Simple
import utopia.metropolis.model.stored.language.Language
import utopia.nexus.result.Result
import utopia.vault.database.Connection

object LanguagesNode extends PublicDescriptionsNodeFactory[LanguagesNode]
{
	override def apply(authorization: (AuthorizedContext, => Result, Connection) => Result) =
		new LanguagesNode(authorization)
}

/**
  * Used for accessing all specified languages
  * @author Mikko Hilpinen
  * @since 20.5.2020, v1
  */
class LanguagesNode(authorization: (AuthorizedContext, => Result, Connection) => Result)
	extends PublicDescriptionsNode[Language, DescribedLanguage]
{
	// IMPLEMENTED	--------------------------------
	
	override val name = "languages"
	
	override def defaultModelStyle = Simple
	
	override protected def authorize(onAuthorized: => Result)(implicit context: AuthorizedContext, connection: Connection) =
		authorization(context, onAuthorized, connection)
	
	override protected def describedItems(implicit connection: Connection, languageIds: LanguageIds) =
		DbLanguages.described
}
