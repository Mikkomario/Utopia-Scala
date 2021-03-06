package utopia.citadel.database.model.description

import java.time.Instant
import utopia.citadel.database.factory.description.DescriptionLinkFactory
import utopia.citadel.database.model.DeprecatableAfter
import utopia.flow.time.Now
import utopia.metropolis.model.partial.description.{DescriptionData, DescriptionLinkData}
import utopia.metropolis.model.partial.description.DescriptionLinkData.PartialDescriptionLinkData
import utopia.metropolis.model.stored.description.DescriptionLink
import utopia.vault.database.Connection
import utopia.vault.model.immutable.{Storable, Table}
import utopia.vault.sql.Insert

object DescriptionLinkModelFactory
{
	// OTHER	------------------------------
	
	/**
	  * @param table Targeted table
	  * @param targetIdAttName Name of the attribute that contains a link to the targeted item
	  * @return A new model factory for that type of links
	  */
	def apply(table: Table, targetIdAttName: String): DescriptionLinkModelFactory[DescriptionLinkModel[
		DescriptionLink, DescriptionLinkFactory[DescriptionLink]]] =
		DescriptionLinkModelFactoryImplementation(table, targetIdAttName)
	
	
	// NESTED	------------------------------
	
	private case class DescriptionLinkModelFactoryImplementation(table: Table, targetIdAttName: String)
		extends DescriptionLinkModelFactory[DescriptionLinkModel[DescriptionLink, DescriptionLinkFactory[DescriptionLink]]]
	{
		// ATTRIBUTES	----------------------
		
		private lazy val factory = DescriptionLinkFactory(this)
		
		def apply(id: Option[Int] = None, targetId: Option[Int] = None, descriptionId: Option[Int] = None,
		          created: Option[Instant] = None,
		          deprecatedAfter: Option[Instant] = None): DescriptionLinkModel[DescriptionLink, DescriptionLinkFactory[DescriptionLink]] =
			DescriptionLinkModelImplementation(id, targetId, descriptionId, created, deprecatedAfter)
		
		
		// NESTED	--------------------------
		
		private case class DescriptionLinkModelImplementation(id: Option[Int] = None, targetId: Option[Int] = None,
		                                                      descriptionId: Option[Int] = None,
		                                                      created: Option[Instant] = None,
		                                                      deprecatedAfter: Option[Instant] = None)
			extends DescriptionLinkModel[DescriptionLink, DescriptionLinkFactory[DescriptionLink]]
		{
			override def factory =
				DescriptionLinkModelFactoryImplementation.this.factory
		}
	}
}

/**
  * A common trait for description link model companion objects
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1.0
  */
trait DescriptionLinkModelFactory[+M <: Storable] extends DeprecatableAfter[M]
{
	// ABSTRACT	----------------------------------
	
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
	  * @return Column that contains description link id
	  */
	def linkIdColumn = table.primaryColumn.get
	/**
	  * @return Column that refers to described items/targets
	  */
	def targetIdColumn = table(targetIdAttName)
	
	
	// IMPLEMENTED  -------------------------------
	
	/**
	 * @param deprecationTime Deprecation time for this description link
	 * @return A model with only deprecation time set
	 */
	override def withDeprecatedAfter(deprecationTime: Instant) = apply(deprecatedAfter = Some(deprecationTime))
	
	
	// OTHER	-----------------------------------
	
	/**
	  * @param linkId A description link id
	  * @return A model with that id set
	  */
	def withId(linkId: Int) = apply(id = Some(linkId))
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
	  * Inserts a new description link to DB
	  * @param data Data to insert
	  * @param connection DB Connection (implicit)
	  * @return Newly inserted description link
	  */
	def insert(data: PartialDescriptionLinkData)(implicit connection: Connection): DescriptionLink = insert(
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
		DescriptionLink(linkId, DescriptionLinkData(targetId, newDescription, created))
	}
	/**
	  * Inserts a number of new descriptions & description links to the DB
	  * @param data A sequence of description target id + description data pairs
	  * @param connection Implicit database connection
	  * @return Generated description link ids, each paired with the matching description instance
	  */
	def insert(data: Seq[(Int, DescriptionData)])(implicit connection: Connection) =
	{
		val descriptions = DescriptionModel.insert(data.map { _._2 })
		val linkTime = Now.toInstant
		Insert(table, data.zip(descriptions)
			.map { case ((targetId, _), description) =>
				apply(None, Some(targetId), Some(description.id), Some(linkTime)).toModel })
			.generatedIntKeys.zip(descriptions)
	}
}
