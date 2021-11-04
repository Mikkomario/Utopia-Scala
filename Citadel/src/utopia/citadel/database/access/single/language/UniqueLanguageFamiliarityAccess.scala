package utopia.citadel.database.access.single.language

import java.time.Instant
import utopia.citadel.database.factory.language.LanguageFamiliarityFactory
import utopia.citadel.database.model.language.LanguageFamiliarityModel
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.stored.language.LanguageFamiliarity
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct LanguageFamiliarities.
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
trait UniqueLanguageFamiliarityAccess 
	extends SingleRowModelAccess[LanguageFamiliarity] 
		with DistinctModelAccess[LanguageFamiliarity, Option[LanguageFamiliarity], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Index used for ordering between language familiarities, 
		where lower values mean higher familiarity. None if no instance (or value) was found.
	  */
	def orderIndex(implicit connection: Connection) = pullColumn(model.orderIndexColumn).int
	
	/**
	  * Time when this LanguageFamiliarity was first created. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = LanguageFamiliarityModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = LanguageFamiliarityFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted LanguageFamiliarity instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any LanguageFamiliarity instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the orderIndex of the targeted LanguageFamiliarity instance(s)
	  * @param newOrderIndex A new orderIndex to assign
	  * @return Whether any LanguageFamiliarity instance was affected
	  */
	def orderIndex_=(newOrderIndex: Int)(implicit connection: Connection) = 
		putColumn(model.orderIndexColumn, newOrderIndex)
}

