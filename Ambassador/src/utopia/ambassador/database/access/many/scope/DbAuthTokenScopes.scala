package utopia.ambassador.database.access.many.scope

import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing multiple scopes at a time, including authentication token linking
  * @author Mikko Hilpinen
  * @since 27.10.2021, v2.0
  */
object DbAuthTokenScopes extends ManyAuthTokenScopesAccess with UnconditionalView
