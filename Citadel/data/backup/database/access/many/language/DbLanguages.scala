package utopia.citadel.database.access.many.language

import utopia.citadel.database.access.many.description.{DbLanguageDescriptions, ManyDescribedAccess, ManyDescribedAccessByIds}
import utopia.citadel.database.factory.language.LanguageFactory
import utopia.citadel.database.model.language.LanguageModel
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.combined.language.DescribedLanguage
import utopia.metropolis.model.stored.language.Language
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.{RowFactoryView, UnconditionalView}
import utopia.vault.sql.SqlExtensions._

/**
  * Used for accessing multiple languages at a time
  * @author Mikko Hilpinen
  * @since 10.5.2020, v1.0
  */
object DbLanguages
	extends ManyRowModelAccess[Language] with ManyDescribedAccess[Language, DescribedLanguage] with UnconditionalView
{
	// IMPLEMENTED	----------------------------
	
	override def factory = LanguageFactory
	
	override protected def defaultOrdering = None
	
	override protected def manyDescriptionsAccess = DbLanguageDescriptions
	
	override protected def describedFactory = DescribedLanguage

	override protected def idOf(item: Language) = item.id
	
	
	// COMPUTED	--------------------------------
	
	private def model = LanguageModel
	
	
	// OTHER	--------------------------------
	
	/**
	 * @param ids A set of language ids
	 * @return An access point to those languages
	 */
	def apply(ids: Set[Int]) = new DbLanguagesWithIds(ids)
	
	/**
	  * @param codes      ISO-standard language codes
	  * @param connection DB Connection (implicit)
	  * @return Languages that match those codes
	  */
	def forIsoCodes(codes: Set[String])(implicit connection: Connection) = read(Some(
		model.isoCodeColumn.in(codes)))
	
	
	// NESTED   --------------------------------
	
	class DbLanguagesWithIds(override val ids: Set[Int])
		extends ManyDescribedAccessByIds[Language, DescribedLanguage] with RowFactoryView[Language]
	{
		// COMPUTED ----------------------------
		
		/**
		 * @param connection Implicit DB Connection
		 * @return ISO-codes of these languages
		 */
		def isoCodes(implicit connection: Connection) =
			pullAttribute(model.isoCodeAttName).flatMap { _.string }
		
		
		// IMPLEMENTED  ------------------------
		
		override def factory = DbLanguages.factory
		override protected def defaultOrdering = None
		
		override protected def manyDescriptionsAccess = DbLanguageDescriptions
		override protected def describedFactory = DescribedLanguage
		
		override protected def idOf(item: Language) = item.id
	}
}
