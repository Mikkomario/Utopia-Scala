package utopia.logos.database.access.many.text.word

import utopia.logos.database.CachingVolatileMapStore
import utopia.logos.model.partial.text.WordData
import utopia.vault.database.Connection
import utopia.vault.nosql.view.{UnconditionalView, ViewManyByIntIds}

/**
  * The root access point when targeting multiple words at a time
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
object DbWords
	extends CachingVolatileMapStore[String, String, Int] with ManyWordsAccess with UnconditionalView
		with ViewManyByIntIds[ManyWordsAccess]
{
	override protected def standardize(value: String): String = value
	override protected def diff(proposed: Set[String], existing: Set[String]): Set[String] = proposed -- existing
	
	override protected def pullMatchMap(values: Set[String])(implicit connection: Connection): Map[String, Int] =
		matchingWords(values).toMap
	override protected def insertAndMap(values: Seq[String])(implicit connection: Connection): Map[String, Int] =
		model.insert(values.sorted.map { WordData(_) }).view.map { w => w.text -> w.id }.toMap
}