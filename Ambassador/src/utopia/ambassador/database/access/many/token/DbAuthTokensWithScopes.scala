package utopia.ambassador.database.access.many.token

import utopia.ambassador.model.combined.token.AuthTokenWithScopes
import utopia.vault.nosql.view.NonDeprecatedView

/**
  * Used for accessing multiple authentication tokens at a time, including their scopes
  * @author Mikko Hilpinen
  * @since 19.7.2021, v1.0
  */
object DbAuthTokensWithScopes extends ManyAuthTokensWithScopesAccess with NonDeprecatedView[AuthTokenWithScopes]