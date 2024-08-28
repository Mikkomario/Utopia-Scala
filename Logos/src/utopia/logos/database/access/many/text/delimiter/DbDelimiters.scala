package utopia.logos.database.access.many.text.delimiter

import utopia.logos.model.partial.text.DelimiterData
import utopia.vault.database.Connection
import utopia.vault.nosql.view.{UnconditionalView, ViewManyByIntIds}

/**
  * The root access point when targeting multiple delimiters at a time
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
object DbDelimiters 
	extends ManyDelimitersAccess with UnconditionalView with ViewManyByIntIds[ManyDelimitersAccess]
{
	/**
	  * Stores the specified delimiters to the database. Avoids inserting duplicates.
	  * @param delimiters Delimiters to insert
	  * @param connection Implicit DB connection
	  * @return Map where keys are the specified delimiters and values are their matching ids
	  */
	def store(delimiters: Set[String])(implicit connection: Connection) = {
		if (delimiters.isEmpty)
			Map[String, Int]()
		else {
			val existingDelimiterMap = matchingDelimiters(delimiters).toMap
			val newDelimiters = delimiters -- existingDelimiterMap.keySet
			if (newDelimiters.isEmpty)
				existingDelimiterMap
			else
				existingDelimiterMap ++
					model.insert(newDelimiters.toVector.map { DelimiterData(_) }).map { d => d.text -> d.id }
		}
	}
}