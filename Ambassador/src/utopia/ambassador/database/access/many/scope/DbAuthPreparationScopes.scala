package utopia.ambassador.database.access.many.scope

import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing multiple authentication preparation scopes at a time
  * @author Mikko Hilpinen
  * @since 26.10.2021, v2.0
  */
object DbAuthPreparationScopes extends ManyAuthPreparationScopesAccess with UnconditionalView
