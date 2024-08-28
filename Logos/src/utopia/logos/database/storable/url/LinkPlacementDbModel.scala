package utopia.logos.database.storable.url

import utopia.flow.generic.model.immutable.Value
import utopia.logos.database.LogosTables
import utopia.logos.database.props.text.TextPlacementDbProps
import utopia.logos.database.storable.text.{TextPlacementDbModel, TextPlacementDbModelFactoryLike, TextPlacementDbModelLike}
import utopia.logos.model.factory.url.LinkPlacementFactory
import utopia.logos.model.partial.url.LinkPlacementData
import utopia.logos.model.stored.url.LinkPlacement
import utopia.vault.model.immutable.DbPropertyDeclaration

/**
  * Used for constructing LinkPlacementDbModel instances and for inserting link placements to the database
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
object LinkPlacementDbModel 
	extends TextPlacementDbModelFactoryLike[LinkPlacementDbModel, LinkPlacement, LinkPlacementData] 
		with LinkPlacementFactory[LinkPlacementDbModel] with TextPlacementDbProps
{
	// ATTRIBUTES	--------------------
	
	override lazy val id = DbPropertyDeclaration("id", index)
	
	/**
	  * Database property used for interacting with statement ids
	  */
	lazy val statementId = property("statementId")
	
	/**
	  * Database property used for interacting with link ids
	  */
	lazy val linkId = property("linkId")
	
	/**
	  * Database property used for interacting with order indices
	  */
	override lazy val orderIndex = property("orderIndex")
	
	
	// IMPLEMENTED	--------------------
	
	override def parentId = statementId
	
	override def placedId = linkId
	
	override def table = LogosTables.linkPlacement
	
	override def apply(data: LinkPlacementData) = 
		apply(None, Some(data.statementId), Some(data.linkId), Some(data.orderIndex))
	
	override def withId(id: Int) = apply(id = Some(id))
	
	/**
	  * @param linkId Referenced / placed link
	  * @return A model containing only the specified link id
	  */
	override def withLinkId(linkId: Int) = apply(linkId = Some(linkId))
	
	/**
	  * @param orderIndex 0-based index that indicates the specific location of the placed text
	  * @return A model containing only the specified order index
	  */
	override def withOrderIndex(orderIndex: Int) = apply(orderIndex = Some(orderIndex))
	
	/**
	  * @param statementId Id of the statement where the specified link is referenced
	  * @return A model containing only the specified statement id
	  */
	override def withStatementId(statementId: Int) = apply(statementId = Some(statementId))
	
	override protected def complete(id: Value, data: LinkPlacementData) = LinkPlacement(id.getInt, data)
}

/**
  * Used for interacting with LinkPlacements in the database
  * @param id link placement database id
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
case class LinkPlacementDbModel(id: Option[Int] = None, statementId: Option[Int] = None, 
	linkId: Option[Int] = None, orderIndex: Option[Int] = None) 
	extends TextPlacementDbModel with TextPlacementDbModelLike[LinkPlacementDbModel] 
		with LinkPlacementFactory[LinkPlacementDbModel]
{
	// IMPLEMENTED	--------------------
	
	override def dbProps = LinkPlacementDbModel
	
	override def parentId = statementId
	
	override def placedId = linkId
	
	override def table = LinkPlacementDbModel.table
	
	/**
	  * @param id Id to assign to the new model (default = currently assigned id)
	  * @param parentId parent id to assign to the new model (default = currently assigned value)
	  * @param placedId placed id to assign to the new model (default = currently assigned value)
	  * @param orderIndex order index to assign to the new model (default = currently assigned value)
	  */
	override def copyTextPlacement(id: Option[Int] = id, parentId: Option[Int] = parentId, 
		placedId: Option[Int] = placedId, orderIndex: Option[Int] = orderIndex) = 
		copy(id = id, statementId = parentId, linkId = placedId, orderIndex = orderIndex)
	
	/**
	  * @param linkId Referenced / placed link
	  * @return A new copy of this model with the specified link id
	  */
	override def withLinkId(linkId: Int) = copy(linkId = Some(linkId))
	
	/**
	  * @param statementId Id of the statement where the specified link is referenced
	  * @return A new copy of this model with the specified statement id
	  */
	override def withStatementId(statementId: Int) = copy(statementId = Some(statementId))
}

