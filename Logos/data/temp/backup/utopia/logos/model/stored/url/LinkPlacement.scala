package utopia.logos.model.stored.url

import utopia.flow.generic.model.template.ModelLike.AnyModel
import utopia.logos.database.access.single.url.link.placement.DbSingleLinkPlacement
import utopia.logos.model.factory.url.{LinkPlacementFactory, LinkPlacementFactoryWrapper}
import utopia.logos.model.partial.text.TextPlacementData
import utopia.logos.model.partial.url.LinkPlacementData
import utopia.logos.model.stored.text.StoredTextPlacementLike
import utopia.logos.model.template.StoredPlaced
import utopia.vault.model.template.{FromIdFactory, StoredFromModelFactory, StoredModelConvertible}

object LinkPlacement extends StoredFromModelFactory[LinkPlacementData, LinkPlacement]
{
	// IMPLEMENTED	--------------------
	
	override def dataFactory = LinkPlacementData
	
	override protected def complete(model: AnyModel, data: LinkPlacementData) = 
		model("id").tryInt.map { apply(_, data) }
}

/**
  * Represents a link placement that has already been stored in the database
  * @param id id of this link placement in the database
  * @param data Wrapped link placement data
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
case class LinkPlacement(id: Int, data: LinkPlacementData) 
	extends LinkPlacementFactoryWrapper[LinkPlacementData, LinkPlacement] with TextPlacementData
		with StoredTextPlacementLike[LinkPlacementData, LinkPlacement]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this link placement in the database
	  */
	def access = DbSingleLinkPlacement(id)
	
	
	// IMPLEMENTED	--------------------
	
	override def withId(id: Int) = copy(id = id)
	
	override protected def wrap(data: LinkPlacementData) = copy(data = data)
}

