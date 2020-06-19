package utopia.exodus.database.access.many

import utopia.exodus.database.factory.language.LanguageFactory
import utopia.exodus.database.model.language.LanguageModel
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.stored.language.Language
import utopia.vault.database.Connection
import utopia.vault.nosql.access.ManyModelAccess
import utopia.vault.sql.Extensions._

/**
  * Used for accessing multiple languages at a time
  * @author Mikko Hilpinen
  * @since 10.5.2020, v1
  */
object DbLanguages extends ManyModelAccess[Language]
{
	// IMPLEMENTED	----------------------------
	
	override def factory = LanguageFactory
	
	override def globalCondition = None
	
	
	// COMPUTED	--------------------------------
	
	private def model = LanguageModel
	
	
	// OTHER	--------------------------------
	
	/**
	  * @param codes ISO-standard language codes
	  * @param connection DB Connection (implicit)
	  * @return Languages that match those codes
	  */
	def forIsoCodes(codes: Set[String])(implicit connection: Connection) = read(Some(
		model.isoCodeColumn.in(codes)))
}
