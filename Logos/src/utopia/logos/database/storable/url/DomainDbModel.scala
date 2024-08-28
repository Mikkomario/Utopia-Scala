package utopia.logos.database.storable.url

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.logos.database.LogosTables
import utopia.logos.model.factory.url.DomainFactory
import utopia.logos.model.partial.url.DomainData
import utopia.logos.model.stored.url.Domain
import utopia.vault.model.immutable.{DbPropertyDeclaration, Storable}
import utopia.vault.model.template.{FromIdFactory, HasId, HasIdProperty}
import utopia.vault.nosql.storable.StorableFactory

import java.time.Instant

/**
  * Used for constructing DomainDbModel instances and for inserting domains to the database
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
object DomainDbModel 
	extends StorableFactory[DomainDbModel, Domain, DomainData] with FromIdFactory[Int, DomainDbModel] 
		with HasIdProperty with DomainFactory[DomainDbModel]
{
	// ATTRIBUTES	--------------------
	
	override lazy val id = DbPropertyDeclaration("id", index)
	
	/**
	  * Database property used for interacting with urls
	  */
	lazy val url = property("url")
	
	/**
	  * Database property used for interacting with creation times
	  */
	lazy val created = property("created")
	
	
	// IMPLEMENTED	--------------------
	
	override def table = LogosTables.domain
	
	override def apply(data: DomainData) = apply(None, data.url, Some(data.created))
	
	/**
	  * @param created Time when this domain was added to the database
	  * @return A model containing only the specified created
	  */
	override def withCreated(created: Instant) = apply(created = Some(created))
	
	override def withId(id: Int) = apply(id = Some(id))
	
	/**
	  * @param url Full http(s) address of this domain in string format. Includes protocol, 
	  * domain name and possible port number.
	  * @return A model containing only the specified url
	  */
	override def withUrl(url: String) = apply(url = url)
	
	override protected def complete(id: Value, data: DomainData) = Domain(id.getInt, data)
}

/**
  * Used for interacting with Domains in the database
  * @param id domain database id
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
case class DomainDbModel(id: Option[Int] = None, url: String = "", created: Option[Instant] = None) 
	extends Storable with HasId[Option[Int]] with FromIdFactory[Int, DomainDbModel] 
		with DomainFactory[DomainDbModel]
{
	// IMPLEMENTED	--------------------
	
	override def table = DomainDbModel.table
	
	override def valueProperties = 
		Vector(DomainDbModel.id.name -> id, DomainDbModel.url.name -> url, 
			DomainDbModel.created.name -> created)
	
	/**
	  * @param created Time when this domain was added to the database
	  * @return A new copy of this model with the specified created
	  */
	override def withCreated(created: Instant) = copy(created = Some(created))
	
	override def withId(id: Int) = copy(id = Some(id))
	
	/**
	  * @param url Full http(s) address of this domain in string format. Includes protocol, 
	  * domain name and possible port number.
	  * @return A new copy of this model with the specified url
	  */
	override def withUrl(url: String) = copy(url = url)
}

