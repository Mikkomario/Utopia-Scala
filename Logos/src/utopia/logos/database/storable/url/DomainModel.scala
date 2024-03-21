package utopia.logos.database.storable.url

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.logos.database.factory.url.DomainDbFactory
import utopia.logos.model.factory.url.DomainFactory
import utopia.logos.model.partial.url.DomainData
import utopia.logos.model.stored.url.Domain
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.model.template.FromIdFactory
import utopia.vault.nosql.storable.StorableFactory

import java.time.Instant

/**
  * Used for constructing DomainModel instances and for inserting domains to the database
  * @author Mikko Hilpinen
  * @since 20.03.2024, v1.0
  */
object DomainModel 
	extends StorableFactory[DomainModel, Domain, DomainData]
		with DomainFactory[DomainModel] with FromIdFactory[Int, DomainModel]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Property that contains ${ classToWrite.name.doc } ${ prop.name.doc }
	  */
	lazy val url = property("url")
	/**
	  * Property that contains ${ classToWrite.name.doc } ${ prop.name.doc }
	  */
	lazy val created = property("created")
	
	/**
	  * Name of the property that contains domain url
	  */
	@deprecated("Deprecated for removal", "v1.0")
	val urlAttName = "url"
	/**
	  * Name of the property that contains domain created
	  */
	@deprecated("Deprecated for removal", "v1.0")
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * The factory object used by this model type
	  */
	def factory = DomainDbFactory
	
	/**
	  * Column that contains domain url
	  */
	@deprecated("Deprecated for removal", "v1.0")
	def urlColumn = table(urlAttName)
	/**
	  * Column that contains domain created
	  */
	@deprecated("Deprecated for removal", "v1.0")
	def createdColumn = table(createdAttName)
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: DomainData) = apply(None, data.url, Some(data.created))
	
	/**
	  * @return A model with that id
	  */
	override def withId(id: Int) = apply(Some(id))
	
	override protected def complete(id: Value, data: DomainData) = Domain(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this domain was added to the database
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param url Full http(s) address of this domain in string format. Includes protocol, 
	  * domain name and possible port number.
	  * @return A model containing only the specified url
	  */
	def withUrl(url: String) = apply(url = url)
}

/**
  * Used for interacting with Domains in the database
  * @param id domain database id
  * @author Mikko Hilpinen
  * @since 20.03.2024, v1.0
  */
case class DomainModel(id: Option[Int] = None, url: String = "", created: Option[Instant] = None) 
	extends StorableWithFactory[Domain] with DomainFactory[DomainModel] with FromIdFactory[Int, DomainModel]
{
	// IMPLEMENTED	--------------------
	
	override def factory = DomainModel.factory
	
	override def valueProperties =
		Vector("id" -> id, DomainModel.url.name -> url, DomainModel.created.name -> created)
	
	override def withId(id: Int): DomainModel = copy(id = Some(id))
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this domain was added to the database
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param url Full http(s) address of this domain in string format. Includes protocol, 
	  * domain name and possible port number.
	  * @return A new copy of this model with the specified url
	  */
	def withUrl(url: String) = copy(url = url)
}

