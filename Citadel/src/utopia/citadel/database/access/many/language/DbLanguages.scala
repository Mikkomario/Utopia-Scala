package utopia.citadel.database.access.many.language

import utopia.citadel.database.factory.language.LanguageFactory
import utopia.citadel.database.model.language.LanguageModel
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.stored.language.Language
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.sql.SqlExtensions._

/**
  * Used for accessing multiple languages at a time
  * @author Mikko Hilpinen
  * @since 10.5.2020, v1.0
  */
object DbLanguages extends ManyModelAccess[Language]
{
	// IMPLEMENTED	----------------------------
	
	override def factory = LanguageFactory
	
	override def globalCondition = None
	
	override protected def defaultOrdering = None
	
	
	// COMPUTED	--------------------------------
	
	private def model = LanguageModel
	
	
	// OTHER	--------------------------------
	
	/**
	  * @param codes      ISO-standard language codes
	  * @param connection DB Connection (implicit)
	  * @return Languages that match those codes
	  */
	def forIsoCodes(codes: Set[String])(implicit connection: Connection) = read(Some(
		model.isoCodeColumn.in(codes)))
}
