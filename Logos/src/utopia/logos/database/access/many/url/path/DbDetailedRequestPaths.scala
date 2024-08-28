package utopia.logos.database.access.many.url.path

import utopia.vault.nosql.view.{UnconditionalView, ViewManyByIntIds}

/**
  * The root access point when targeting multiple detailed request paths at a time
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
object DbDetailedRequestPaths 
	extends ManyDetailedRequestPathsAccess with UnconditionalView 
		with ViewManyByIntIds[ManyDetailedRequestPathsAccess]

