package utopia.logos.database.access.many.text.word.placement

import utopia.vault.nosql.view.{UnconditionalView, ViewManyByIntIds}

/**
  * The root access point when targeting multiple word placements at a time
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
object DbWordPlacements 
	extends ManyWordPlacementsAccess with UnconditionalView with ViewManyByIntIds[ManyWordPlacementsAccess]

