package utopia.citadel.database.model.language

import java.time.Instant
import utopia.citadel.database.factory.language.LanguageFamiliarityFactory
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.partial.language.LanguageFamiliarityData
import utopia.metropolis.model.stored.language.LanguageFamiliarity
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

/**
  * Used for constructing LanguageFamiliarityModel instances and for inserting LanguageFamiliaritys to the database
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object LanguageFamiliarityModel 
	extends DataInserter[LanguageFamiliarityModel, LanguageFamiliarity, LanguageFamiliarityData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains LanguageFamiliarity orderIndex
	  */
	val orderIndexAttName = "orderIndex"
	
	/**
	  * Name of the property that contains LanguageFamiliarity created
	  */
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains LanguageFamiliarity orderIndex
	  */
	def orderIndexColumn = table(orderIndexAttName)
	
	/**
	  * Column that contains LanguageFamiliarity created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = LanguageFamiliarityFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: LanguageFamiliarityData) = apply(None, Some(data.orderIndex), Some(data.created))
	
	override def complete(id: Value, data: LanguageFamiliarityData) = LanguageFamiliarity(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this LanguageFamiliarity was first created
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param id A LanguageFamiliarity id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param orderIndex Index used for ordering between language familiarities, 
		where lower values mean higher familiarity
	  * @return A model containing only the specified orderIndex
	  */
	def withOrderIndex(orderIndex: Int) = apply(orderIndex = Some(orderIndex))
}

/**
  * Used for interacting with LanguageFamiliarities in the database
  * @param id LanguageFamiliarity database id
  * @param orderIndex Index used for ordering between language familiarities, 
	where lower values mean higher familiarity
  * @param created Time when this LanguageFamiliarity was first created
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class LanguageFamiliarityModel(id: Option[Int] = None, orderIndex: Option[Int] = None, 
	created: Option[Instant] = None) 
	extends StorableWithFactory[LanguageFamiliarity]
{
	// IMPLEMENTED	--------------------
	
	override def factory = LanguageFamiliarityModel.factory
	
	override def valueProperties = 
	{
		import LanguageFamiliarityModel._
		Vector("id" -> id, orderIndexAttName -> orderIndex, createdAttName -> created)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param orderIndex A new orderIndex
	  * @return A new copy of this model with the specified orderIndex
	  */
	def withOrderIndex(orderIndex: Int) = copy(orderIndex = Some(orderIndex))
}

