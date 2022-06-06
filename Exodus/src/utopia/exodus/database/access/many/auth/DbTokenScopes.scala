package utopia.exodus.database.access.many.auth

import utopia.vault.nosql.view.UnconditionalView

/**
  * A root access point for scopes, including token link data
  * @author Mikko Hilpinen
  * @since 19.2.2022, v4.0
  */
object DbTokenScopes extends ManyTokenScopesAccess with UnconditionalView
