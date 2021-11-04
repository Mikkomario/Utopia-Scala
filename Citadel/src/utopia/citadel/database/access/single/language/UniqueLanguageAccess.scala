package utopia.citadel.database.access.single.language

import java.time.Instant
import utopia.citadel.database.factory.language.LanguageFactory
import utopia.citadel.database.model.language.LanguageModel
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.stored.language.Language
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct Languages.
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
trait UniqueLanguageAccess 
	extends SingleRowModelAccess[Language] with DistinctModelAccess[Language, Option[Language], Value] 
		with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * 2 letter ISO-standard code for this language. None if no instance (or value) was found.
	  */
	def isoCode(implicit connection: Connection) = pullColumn(model.isoCodeColumn).string
	
	/**
	  * Time when this Language was first created. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = LanguageModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = LanguageFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted Language instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any Language instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the isoCode of the targeted Language instance(s)
	  * @param newIsoCode A new isoCode to assign
	  * @return Whether any Language instance was affected
	  */
	def isoCode_=(newIsoCode: String)(implicit connection: Connection) = 
		putColumn(model.isoCodeColumn, newIsoCode)
}

