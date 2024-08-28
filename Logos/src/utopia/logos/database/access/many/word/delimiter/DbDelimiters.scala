package utopia.logos.database.access.many.word.delimiter

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.model.partial.word.DelimiterData
import utopia.vault.database.Connection
import utopia.vault.nosql.view.UnconditionalView

/**
  * The root access point when targeting multiple delimiters at a time
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
@deprecated("Replaced with a new version", "v0.3")
object DbDelimiters extends ManyDelimitersAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted delimiters
	  * @return An access point to delimiters with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbDelimitersSubset(ids)
	
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
			val existingDelimiterMap = matching(delimiters).toMap
			val newDelimiters = delimiters -- existingDelimiterMap.keySet
			if (newDelimiters.isEmpty)
				existingDelimiterMap
			else
				existingDelimiterMap ++
					model.insert(newDelimiters.toVector.map { DelimiterData(_) }).map { d => d.text -> d.id }
		}
	}
	
	
	// NESTED	--------------------
	
	class DbDelimitersSubset(targetIds: Set[Int]) extends ManyDelimitersAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(index in targetIds)
	}
}

