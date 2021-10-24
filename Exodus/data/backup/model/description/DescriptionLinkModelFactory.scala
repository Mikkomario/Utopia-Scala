package utopia.exodus.database.model.description

import java.time.Instant
import utopia.exodus.database.factory.description.DescriptionLinkFactory
import utopia.flow.time.Now
import utopia.metropolis.model.partial.description.{DescriptionData, DescriptionLinkDataOld}
import utopia.metropolis.model.partial.description.DescriptionLinkDataOld.PartialDescriptionLinkData
import utopia.metropolis.model.stored.description.DescriptionLinkOld
import utopia.vault.database.Connection
import utopia.vault.model.immutable.{Storable, Table}

/**
  * A common trait for description link model companion objects
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1
  */
@deprecated("Please use the Citadel version instead", "v2.0")
trait DescriptionLinkModelFactory[+M <: Storable]
{
	// ABSTRACT	----------------------------------
	
	/**
	  * @return table used by this model type
	  */
	def table: Table
	
	/**
	  * @return Name of the attribute which contains the targeted item id
	  */
	def targetIdAttName: String
	
	/**
	  * Creates a new storable model based on specified attributes
	  * @param id Link id (optional)
	  * @param targetId Description target id (optional)
	  * @param descriptionId Description id (optional)
	  * @param created Creation time of this link (optional)
	  * @param deprecatedAfter Deprecation / invalidation time of this link (optional)
	  * @return A new database model with specified attributes
	  */
	def apply(id: Option[Int] = None, targetId: Option[Int] = None, descriptionId: Option[Int] = None,
			  created: Option[Instant] = None, deprecatedAfter: Option[Instant] = None): M
	
	
	// COMPUTED	-----------------------------------
	
	/**
	  * @return Column that refers to described items/targets
	  */
	def targetIdColumn = table(targetIdAttName)
	
	/**
	  * @return A model that has just been marked as deprecated
	  */
	def nowDeprecated = withDeprecatedAfter(Now)
	
	
	// OTHER	-----------------------------------
	
	/**
	  * @param targetId Id of description target
	  * @return A model with only target id set
	  */
	def withTargetId(targetId: Int) = apply(targetId = Some(targetId))
	
	/**
	  * @param creationTime Row creation time
	  * @return A model with only creation time set
	  */
	def withCreationTime(creationTime: Instant) = apply(created = Some(creationTime))
	
	/**
	  * @param deprecationTime Deprecation time for this description link
	  * @return A model with only deprecation time set
	  */
	def withDeprecatedAfter(deprecationTime: Instant) = apply(deprecatedAfter = Some(deprecationTime))
	
	/**
	  * Inserts a new description link to DB
	  * @param data Data to insert
	  * @param connection DB Connection (implicit)
	  * @return Newly inserted description link
	  */
	def insert(data: PartialDescriptionLinkData)(implicit connection: Connection): DescriptionLinkOld = insert(
		data.targetId, data.description, data.created)
	
	/**
	  * Inserts a new description link to DB
	  * @param targetId Id of described item
	  * @param data Description to insert
	  * @param created Creation time of this description link (default = current time)
	  * @param connection DB Connection (implicit)
	  * @return Newly inserted description link
	  */
	def insert(targetId: Int, data: DescriptionData, created: Instant = Now)
			  (implicit connection: Connection) =
	{
		// Inserts the description
		val newDescription = DescriptionModel.insert(data)
		// Inserts the link between description and target
		val linkId = apply(None, Some(targetId), Some(newDescription.id), Some(created)).insert().getInt
		DescriptionLinkOld(linkId, DescriptionLinkDataOld(targetId, newDescription, created))
	}
}

object DescriptionLinkModelFactory
{
	// OTHER	------------------------------
	
	/**
	  * @param table Targeted table
	  * @param targetIdAttName Name of the attribute that contains a link to the targeted item
	  * @return A new model factory for that type of links
	  */
	def apply(table: Table, targetIdAttName: String): DescriptionLinkModelFactory[DescriptionLinkModel[
		DescriptionLinkOld, DescriptionLinkFactory[DescriptionLinkOld]]] =
		DescriptionLinkModelFactoryImplementation(table, targetIdAttName)
	
	
	// NESTED	------------------------------
	
	private case class DescriptionLinkModelFactoryImplementation(table: Table, targetIdAttName: String)
		extends DescriptionLinkModelFactory[DescriptionLinkModel[DescriptionLinkOld, DescriptionLinkFactory[DescriptionLinkOld]]]
	{
		// ATTRIBUTES	----------------------
		
		private lazy val factory = DescriptionLinkFactory(this)
		
		def apply(id: Option[Int] = None, targetId: Option[Int] = None, descriptionId: Option[Int] = None,
				  created: Option[Instant] = None,
				  deprecatedAfter: Option[Instant] = None): DescriptionLinkModel[DescriptionLinkOld, DescriptionLinkFactory[DescriptionLinkOld]] =
			DescriptionLinkModelImplementation(id, targetId, descriptionId, created, deprecatedAfter)
		
		
		// NESTED	--------------------------
		
		private case class DescriptionLinkModelImplementation(id: Option[Int] = None, targetId: Option[Int] = None,
															  descriptionId: Option[Int] = None,
															  created: Option[Instant] = None,
															  deprecatedAfter: Option[Instant] = None)
			extends DescriptionLinkModel[DescriptionLinkOld, DescriptionLinkFactory[DescriptionLinkOld]]
		{
			override def factory =
				DescriptionLinkModelFactoryImplementation.this.factory
		}
	}
}
