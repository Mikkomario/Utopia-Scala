package utopia.citadel.database.model.description

import java.time.Instant
import utopia.citadel.database.factory.description.DescriptionRoleFactory
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.partial.description.DescriptionRoleData
import utopia.metropolis.model.stored.description.DescriptionRole
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

/**
  * Used for constructing DescriptionRoleModel instances and for inserting DescriptionRoles to the database
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DescriptionRoleModel extends DataInserter[DescriptionRoleModel, DescriptionRole, DescriptionRoleData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains DescriptionRole jsonKeySingular
	  */
	val jsonKeySingularAttName = "jsonKeySingular"
	
	/**
	  * Name of the property that contains DescriptionRole jsonKeyPlural
	  */
	val jsonKeyPluralAttName = "jsonKeyPlural"
	
	/**
	  * Name of the property that contains DescriptionRole created
	  */
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains DescriptionRole jsonKeySingular
	  */
	def jsonKeySingularColumn = table(jsonKeySingularAttName)
	
	/**
	  * Column that contains DescriptionRole jsonKeyPlural
	  */
	def jsonKeyPluralColumn = table(jsonKeyPluralAttName)
	
	/**
	  * Column that contains DescriptionRole created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = DescriptionRoleFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: DescriptionRoleData) = 
		apply(None, Some(data.jsonKeySingular), Some(data.jsonKeyPlural), Some(data.created))
	
	override def complete(id: Value, data: DescriptionRoleData) = DescriptionRole(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this DescriptionRole was first created
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param id A DescriptionRole id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param jsonKeyPlural Key used in json documents for multiple values (array) of this description role
	  * @return A model containing only the specified jsonKeyPlural
	  */
	def withJsonKeyPlural(jsonKeyPlural: String) = apply(jsonKeyPlural = Some(jsonKeyPlural))
	
	/**
	  * @param jsonKeySingular Key used in json documents for a singular value (string) of this description role
	  * @return A model containing only the specified jsonKeySingular
	  */
	def withJsonKeySingular(jsonKeySingular: String) = apply(jsonKeySingular = Some(jsonKeySingular))
}

/**
  * Used for interacting with DescriptionRoles in the database
  * @param id DescriptionRole database id
  * @param jsonKeySingular Key used in json documents for a singular value (string) of this description role
  * @param jsonKeyPlural Key used in json documents for multiple values (array) of this description role
  * @param created Time when this DescriptionRole was first created
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class DescriptionRoleModel(id: Option[Int] = None, jsonKeySingular: Option[String] = None, 
	jsonKeyPlural: Option[String] = None, created: Option[Instant] = None) 
	extends StorableWithFactory[DescriptionRole]
{
	// IMPLEMENTED	--------------------
	
	override def factory = DescriptionRoleModel.factory
	
	override def valueProperties = 
	{
		import DescriptionRoleModel._
		Vector("id" -> id, jsonKeySingularAttName -> jsonKeySingular, jsonKeyPluralAttName -> jsonKeyPlural, 
			createdAttName -> created)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param jsonKeyPlural A new jsonKeyPlural
	  * @return A new copy of this model with the specified jsonKeyPlural
	  */
	def withJsonKeyPlural(jsonKeyPlural: String) = copy(jsonKeyPlural = Some(jsonKeyPlural))
	
	/**
	  * @param jsonKeySingular A new jsonKeySingular
	  * @return A new copy of this model with the specified jsonKeySingular
	  */
	def withJsonKeySingular(jsonKeySingular: String) = copy(jsonKeySingular = Some(jsonKeySingular))
}

