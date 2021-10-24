package utopia.citadel.database.access.many.organization

import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing multiple organization deletions at a time, including their cancellations
  * @author Mikko Hilpinen
  * @since 23.10.2021, v2.0
  */
object DbOrganizationDeletionsWithCancellations
	extends ManyOrganizationDeletionsWithCancellationsAccess with UnconditionalView