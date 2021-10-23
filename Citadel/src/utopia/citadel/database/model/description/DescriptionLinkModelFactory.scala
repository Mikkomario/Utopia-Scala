package utopia.citadel.database.model.description

import utopia.citadel.model.cached.DescriptionLinkTable
import utopia.flow.datastructure.immutable.Value
import utopia.metropolis.model.combined.description.LinkedDescription
import utopia.metropolis.model.partial.description.{DescriptionData, DescriptionLinkData}
import utopia.metropolis.model.stored.description.DescriptionLink
import utopia.vault.database.Connection
import utopia.vault.nosql.storable.DataInserter
import utopia.vault.nosql.template.Indexed
import utopia.vault.sql.Insert

/**
  * Used for constructing description link database interaction models
  * @author Mikko Hilpinen
  * @since 23.10.2021, v2.0
  */
case class DescriptionLinkModelFactory(linkTable: DescriptionLinkTable)
	extends DataInserter[DescriptionLinkModel, DescriptionLink, DescriptionLinkData] with Indexed
{
	// COMPUTED	-----------------------------------
	
	/**
	  * @return Column that contains description link id
	  */
	def linkIdColumn = index
	/**
	  * @return Column that refers to described items/targets
	  */
	def targetIdColumn = linkTable.targetLinkColumn
	/**
	  * @return Column that refers to linked descriptions
	  */
	def descriptionIdColumn = linkTable.descriptionLinkColumn
	
	
	// IMPLEMENTED  -------------------------------
	
	override protected def complete(id: Value, data: DescriptionLinkData) = DescriptionLink(id.getInt, data)
	
	override def table = linkTable.wrapped
	
	
	// OTHER	-----------------------------------
	
	/**
	  * Creates a new description link model based on specified attributes
	  * @param id Link id (optional)
	  * @param targetId Description target id (optional)
	  * @param descriptionId Description id (optional)
	  * @return A new database model with specified attributes
	  */
	def apply(id: Option[Int] = None, targetId: Option[Int] = None, descriptionId: Option[Int] = None) =
		DescriptionLinkModel(linkTable, id, targetId, descriptionId)
	
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
	  * @param descriptionId Id of the linked description
	  * @return A model with that description id
	  */
	def withDescriptionId(descriptionId: Int) = apply(descriptionId = Some(descriptionId))
	
	/**
	  * Inserts a new description and its link to the DB
	  * @param targetId Id of described item
	  * @param data Description to insert
	  * @param connection DB Connection (implicit)
	  * @return Newly inserted description link
	  */
	def insert(targetId: Int, data: DescriptionData)(implicit connection: Connection): LinkedDescription =
	{
		// Inserts the description
		val description = DescriptionModel.insert(data)
		// Inserts the link between description and target
		val link = insert(DescriptionLinkData(targetId, description.id))
		LinkedDescription(description, link.id, targetId)
	}
	/**
	  * Inserts a number of new descriptions & description links to the DB
	  * @param data A sequence of description target id + description data pairs
	  * @param connection Implicit database connection
	  * @return Inserted description links
	  */
	def insertDescriptions(data: Seq[(Int, DescriptionData)])(implicit connection: Connection) =
	{
		val descriptions = DescriptionModel.insert(data.map { _._2 })
		val dataToInsert = data.zip(descriptions).map { case ((targetId, _), description) =>
			apply(None, Some(targetId), Some(description.id)).toModel
		}
		val generatedLinkIds = Insert(table, dataToInsert).generatedIntKeys
		generatedLinkIds.indices
			.map { i => LinkedDescription(descriptions(i), generatedLinkIds(i), data(i)._1) }.toVector
	}
	/**
	 * Inserts a number of new descriptions for a single item to the database
	 * @param targetId Id of the described item
	 * @param data Description data for that item
	 * @param connection Implicit DB Connection
	 * @return Inserted description links
	 */
	def insert(targetId: Int, data: Seq[DescriptionData])(implicit connection: Connection): Vector[LinkedDescription] =
	{
		val descriptions = DescriptionModel.insert(data)
		val linkIds = Insert(table, descriptions.map { d => apply(None, Some(targetId), Some(d.id)).toModel })
			.generatedIntKeys
		descriptions.zip(linkIds).map { case (d, linkId) => LinkedDescription(d, linkId, targetId) }
	}
}
