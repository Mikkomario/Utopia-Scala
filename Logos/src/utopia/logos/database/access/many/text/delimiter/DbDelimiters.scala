package utopia.logos.database.access.many.text.delimiter

import utopia.logos.database.CachingVolatileMapStore
import utopia.logos.model.partial.text.DelimiterData
import utopia.vault.database.Connection
import utopia.vault.nosql.view.{UnconditionalView, ViewManyByIntIds}

/**
  * The root access point when targeting multiple delimiters at a time
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
object DbDelimiters 
	extends CachingVolatileMapStore[String, String, Int] with ManyDelimitersAccess with UnconditionalView
		with ViewManyByIntIds[ManyDelimitersAccess]
{
	// IMPLEMENTED  -------------------------
	
	override protected def standardize(value: String): String = value
	override protected def diff(proposed: Set[String], existing: Set[String]): Set[String] = proposed -- existing
	
	override protected def pullMatchMap(values: Set[String])(implicit connection: Connection): Map[String, Int] =
		matchingDelimiters(values).toMap
	override protected def insertAndMap(values: Seq[String])(implicit connection: Connection): Map[String, Int] =
		model.insert(values.sorted.map { DelimiterData(_) }).view.map { d => d.text -> d.id }.toMap
	
	override protected def idOf(value: Int): Int = value
}