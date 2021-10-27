package utopia.ambassador.database.access.many.scope

import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing multiple task-linked scopes at a time
  * @author Mikko Hilpinen
  * @since 27.10.2021, v2.0
  */
object DbTaskScopes extends ManyTaskScopesAccess with UnconditionalView
