package utopia.citadel.database.access.many.user

import utopia.citadel.database.access.many.user.ManyFullUserLanguagesAccess.SubAccess
import utopia.citadel.database.factory.user.FullUserLanguageFactory
import utopia.metropolis.model.combined.user.FullUserLanguage
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition

object ManyFullUserLanguagesAccess
{
	private class SubAccess(override val parent: ManyModelAccess[FullUserLanguage],
	                        override val filterCondition: Condition)
		extends ManyFullUserLanguagesAccess with SubView
}

/**
  * Common trait for access points which return multiple full user language sets at once
  * @author Mikko Hilpinen
  * @since 24.10.2021, v2.0
  */
trait ManyFullUserLanguagesAccess extends ManyUserLanguageLinksAccessLike[FullUserLanguage, ManyFullUserLanguagesAccess]
{
	// IMPLEMENTED  ----------------------------
	
	override def factory = FullUserLanguageFactory
	override protected def defaultOrdering = Some(factory.defaultOrder)
	
	override protected def _filter(condition: Condition): ManyFullUserLanguagesAccess =
		new SubAccess(this, condition)
}
