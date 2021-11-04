package utopia.citadel.database.access.many.language

import utopia.citadel.database.access.many.description.ManyDescribedAccessByIds
import utopia.metropolis.model.combined.language.DescribedLanguageFamiliarity
import utopia.metropolis.model.stored.language.LanguageFamiliarity
import utopia.vault.nosql.view.UnconditionalView

/**
  * The root access point when targeting multiple LanguageFamiliarities at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbLanguageFamiliarities extends ManyLanguageFamiliaritiesAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted LanguageFamiliarities
	  * @return An access point to LanguageFamiliarities with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbLanguageFamiliaritiesSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbLanguageFamiliaritiesSubset(override val ids: Set[Int]) 
		extends ManyLanguageFamiliaritiesAccess 
			with ManyDescribedAccessByIds[LanguageFamiliarity, DescribedLanguageFamiliarity]
}

