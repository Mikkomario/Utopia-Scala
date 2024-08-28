package utopia.logos.database.storable.url

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.logos.database.factory.url.LinkPlacementDbFactory
import utopia.logos.database.storable.word.StatementLinkedModel
import utopia.logos.model.factory.url.LinkPlacementFactory
import utopia.logos.model.partial.url.LinkPlacementData
import utopia.logos.model.stored.url.LinkPlacement
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.model.template.FromIdFactory
import utopia.vault.nosql.storable.StorableFactory

/**
  * Used for constructing LinkPlacementModel instances and for inserting link placements to the database
  * @author Mikko Hilpinen
  * @since 20.03.2024, v1.0
  */
@deprecated("Replaced with LinkPlacementDbModel", "v0.3")
object LinkPlacementModel 
	extends StorableFactory[LinkPlacementModel, LinkPlacement, LinkPlacementData] 
		with LinkPlacementFactory[LinkPlacementModel] with FromIdFactory[Int, LinkPlacementModel]
		with StatementLinkedModel
{
	// ATTRIBUTES	--------------------
	
	lazy val statementId = property("statementId")
	lazy val linkId = property("linkId")
	lazy val orderIndex = property("orderIndex")
	
	/**
	  * Name of the property that contains link placement link id
	  */
	@deprecated("Deprecated for removal", "v1.0")
	val linkIdAttName = "linkId"
	
	
	// COMPUTED	--------------------
	
	/**
	  * The factory object used by this model type
	  */
	def factory = LinkPlacementDbFactory
	
	/**
	  * Column that contains link placement link id
	  */
	@deprecated("Deprecated for removal", "v1.0")
	def linkIdColumn = table(linkIdAttName)
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	/**
	  * Name of the property that contains link placement statement id
	  */
	override def statementIdAttName = statementId.name
	/**
	  * Name of the property that contains link placement order index
	  */
	override def orderIndexAttName = orderIndex.name
	
	override def apply(data: LinkPlacementData) = 
		apply(None, Some(data.statementId), Some(data.linkId), Some(data.orderIndex))
	
	/**
	  * @return A model with that id
	  */
	override def withId(id: Int) = apply(Some(id))
	
	override protected def complete(id: Value, data: LinkPlacementData) = LinkPlacement(id.getInt, data)
	
	
	// OTHER	--------------------
	
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
  * @since 20.03.2024, v1.0
  */
@deprecated("Replaced with LinkPlacementDbModel", "v0.3")
case class LinkPlacementModel(id: Option[Int] = None, statementId: Option[Int] = None, 
	linkId: Option[Int] = None, orderIndex: Option[Int] = None) 
	extends StorableWithFactory[LinkPlacement] with LinkPlacementFactory[LinkPlacementModel]
		with FromIdFactory[Int, LinkPlacementModel]
{
	// IMPLEMENTED	--------------------
	
	override def factory = LinkPlacementModel.factory
	
	override def valueProperties =
		Vector("id" -> id, LinkPlacementModel.statementId.name -> statementId, 
			LinkPlacementModel.linkId.name -> linkId, LinkPlacementModel.orderIndex.name -> orderIndex)
	
	override def withId(id: Int): LinkPlacementModel = copy(id = Some(id))
	
	
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

