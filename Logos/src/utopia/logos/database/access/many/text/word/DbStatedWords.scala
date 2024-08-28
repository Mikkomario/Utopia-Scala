package utopia.logos.database.access.many.text.word

import utopia.vault.nosql.view.{UnconditionalView, ViewManyByIntIds}

/**
  * The root access point when targeting multiple stated words at a time
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
object DbStatedWords 
	extends ManyStatedWordsAccess with UnconditionalView with ViewManyByIntIds[ManyStatedWordsAccess]

