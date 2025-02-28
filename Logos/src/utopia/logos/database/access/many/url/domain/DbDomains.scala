package utopia.logos.database.access.many.url.domain

import utopia.logos.database.CachingVolatileMapStore
import utopia.logos.model.partial.url.DomainData
import utopia.logos.model.stored.url.Domain
import utopia.vault.database.Connection
import utopia.vault.nosql.view.{UnconditionalView, ViewManyByIntIds}

/**
  * The root access point when targeting multiple domains at a time
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
object DbDomains
	extends CachingVolatileMapStore[String, String, Domain] with ManyDomainsAccess with UnconditionalView
		with ViewManyByIntIds[ManyDomainsAccess]
{
	override protected def standardize(value: String): String = value.toLowerCase
	override protected def diff(proposed: Set[String], existing: Set[String]): Set[String] =
		proposed.filterNot { d => existing.contains(d.toLowerCase) }
	
	override protected def pullMatchMap(values: Set[String])(implicit connection: Connection): Map[String, Domain] =
		matching(values).toMapBy { _.url.toLowerCase }
	override protected def insertAndMap(values: Seq[String])(implicit connection: Connection): Map[String, Domain] =
		model.insert(values.sorted.map { DomainData(_) }).view.map { d => d.url.toLowerCase -> d }.toMap
}

