package utopia.logos.database.model.url

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.logos.database.factory.url.LinkPlacementFactory
import utopia.logos.database.model.text.StatementLinkModel
import utopia.logos.model.partial.url.LinkPlacementData
import utopia.logos.model.stored.url.LinkPlacement
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

/**
  * Used for constructing LinkPlacementModel instances and for inserting link placements to the database
  * @author Mikko Hilpinen
  * @since 16.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
object LinkPlacementModel
	extends DataInserter[LinkPlacementModel, LinkPlacement, LinkPlacementData] with StatementLinkModel
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains link placement statement id
	  */
	override val statementIdAttName = "statementId"
	/**
	  * Name of the property that contains link placement link id
	  */
	val linkIdAttName = "linkId"
	/**
	  * Name of the property that contains link placement order index
	  */
	override val orderIndexAttName = "orderIndex"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains link placement link id
	  */
	def linkIdColumn = table(linkIdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = LinkPlacementFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: LinkPlacementData) = 
		apply(None, Some(data.statementId), Some(data.linkId), Some(data.orderIndex))
	
	override protected def complete(id: Value, data: LinkPlacementData) = LinkPlacement(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param id A link placement id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param linkId Referenced link
	  * @return A model containing only the specified link id
	  */
	def withLinkId(linkId: Int) = apply(linkId = Some(linkId))
	
	/**
	  * @param orderIndex Index where the link appears in the statement (0-based)
	  * @return A model containing only the specified order index
	  */
	def withOrderIndex(orderIndex: Int) = apply(orderIndex = Some(orderIndex))
	
	/**
	  * @param statementId Id of the statement where the specified link is referenced
	  * @return A model containing only the specified statement id
	  */
	def withStatementId(statementId: Int) = apply(statementId = Some(statementId))
}

/**
  * Used for interacting with LinkPlacements in the database
  * @param id link placement database id
  * @author Mikko Hilpinen
  * @since 16.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
case class LinkPlacementModel(id: Option[Int] = None, statementId: Option[Int] = None, 
	linkId: Option[Int] = None, orderIndex: Option[Int] = None) 
	extends StorableWithFactory[LinkPlacement]
{
	// IMPLEMENTED	--------------------
	
	override def factory = LinkPlacementModel.factory
	
	override def valueProperties = {
		import LinkPlacementModel._
		Vector("id" -> id, statementIdAttName -> statementId, linkIdAttName -> linkId, 
			orderIndexAttName -> orderIndex)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param linkId Referenced link
	  * @return A new copy of this model with the specified link id
	  */
	def withLinkId(linkId: Int) = copy(linkId = Some(linkId))
	
	/**
	  * @param orderIndex Index where the link appears in the statement (0-based)
	  * @return A new copy of this model with the specified order index
	  */
	def withOrderIndex(orderIndex: Int) = copy(orderIndex = Some(orderIndex))
	
	/**
	  * @param statementId Id of the statement where the specified link is referenced
	  * @return A new copy of this model with the specified statement id
	  */
	def withStatementId(statementId: Int) = copy(statementId = Some(statementId))
}

