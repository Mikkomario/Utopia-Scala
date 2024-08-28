package utopia.logos.database.access.many.url.link.placement

import utopia.vault.nosql.view.{UnconditionalView, ViewManyByIntIds}

/**
  * The root access point when targeting multiple link placements at a time
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
object DbLinkPlacements 
	extends ManyLinkPlacementsAccess with UnconditionalView with ViewManyByIntIds[ManyLinkPlacementsAccess]

