package utopia.citadel.database.access.single.language

import utopia.citadel.database.factory.language.LanguageFactory
import utopia.citadel.database.model.language.LanguageModel
import utopia.metropolis.model.partial.language.LanguageData
import utopia.metropolis.model.stored.language.Language
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing individual Languages
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbLanguage extends SingleRowModelAccess[Language] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = LanguageModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = LanguageFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted Language instance
	  * @return An access point to that Language
	  */
	def apply(id: Int) = DbSingleLanguage(id)
	
	/**
	  * @param isoCode An 2-character ISO-code of the targeted language
	  * @return An access point to that language
	  */
	def forIsoCode(isoCode: String) = new DbLanguageForIsoCode(isoCode)
	
	
	// NESTED   ---------------------
	
	class DbLanguageForIsoCode(val isoCode: String) extends UniqueLanguageAccess
	{
		// IMPLEMENTED  -------------
		
		override def globalCondition = Some(this.model.withIsoCode(isoCode).toCondition)
		
		
		// OTHER    ------------------
		
		/**
		  * @param connection Implicit DB Connection
		  * @return This language, or a new language with this ISO code
		  */
		def getOrInsert()(implicit connection: Connection) =
			pull.getOrElse { this.model.insert(LanguageData(isoCode)) }
	}
}

