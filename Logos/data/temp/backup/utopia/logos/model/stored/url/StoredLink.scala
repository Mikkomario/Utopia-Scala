package utopia.logos.model.stored.url

import utopia.flow.generic.model.template.ModelLike.AnyModel
import utopia.logos.database.access.single.url.link.DbSingleLink
import utopia.logos.model.cached.Link
import utopia.logos.model.factory.url.LinkFactoryWrapper
import utopia.logos.model.partial.url.LinkData
import utopia.vault.model.template.{FromIdFactory, StoredFromModelFactory, StoredModelConvertible}

object StoredLink extends StoredFromModelFactory[LinkData, StoredLink]
{
	// COMPUTED ------------------------
	
	@deprecated("Moved to Link", "v0.3")
	def paramPartRegex = Link.paramPartRegex
	@deprecated("Moved to Link", "v0.3")
	def regex = Link.regex
	
	
	// IMPLEMENTED	--------------------
	
	override def dataFactory = LinkData
	
	override protected def complete(model: AnyModel, data: LinkData) = model("id").tryInt.map { apply(_, data) }
}

/**
  * Represents a link that has already been stored in the database
  * @param id id of this link in the database
  * @param data Wrapped link data
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
case class StoredLink(id: Int, data: LinkData)
	extends StoredModelConvertible[LinkData] with FromIdFactory[Int, StoredLink]
		with LinkFactoryWrapper[LinkData, StoredLink]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this link in the database
	  */
	def access = DbSingleLink(id)
	
	
	// IMPLEMENTED	--------------------
	
	override protected def wrappedFactory = data
	
	override def withId(id: Int) = copy(id = id)
	
	override protected def wrap(data: LinkData) = copy(data = data)
}

