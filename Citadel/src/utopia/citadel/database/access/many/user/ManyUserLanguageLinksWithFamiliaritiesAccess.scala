package utopia.citadel.database.access.many.user

import utopia.citadel.database.access.many.user.ManyUserLanguageLinksWithFamiliaritiesAccess.SubAccess
import utopia.citadel.database.factory.user.UserLanguageLinkWithFamiliarityFactory
import utopia.metropolis.model.combined.user.UserLanguageLinkWithFamiliarity
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition

object ManyUserLanguageLinksWithFamiliaritiesAccess
{
	private class SubAccess(override val parent: ManyRowModelAccess[UserLanguageLinkWithFamiliarity],
	                        override val filterCondition: Condition)
		extends ManyUserLanguageLinksWithFamiliaritiesAccess with SubView
}

/**
  * A common trait for access points which retrieve multiple user language links at a time and include language
  * familiarity information
  * @author Mikko Hilpinen
  * @since 24.10.2021, v2.0
  */
trait ManyUserLanguageLinksWithFamiliaritiesAccess
	extends ManyUserLanguageLinksAccessLike[UserLanguageLinkWithFamiliarity,
		ManyUserLanguageLinksWithFamiliaritiesAccess]
{
	override protected def self = this
	
	override def factory = UserLanguageLinkWithFamiliarityFactory
	
	override protected def _filter(condition: Condition): ManyUserLanguageLinksWithFamiliaritiesAccess =
		new SubAccess(this, condition)
}
