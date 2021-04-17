package utopia.trove.model.enumeration

/**
  * Enumeration for different sql file purposes
  * @author Mikko Hilpinen
  * @since 19.9.2020, v1
  */
sealed trait SqlFileType

object SqlFileType
{
	// ATTRIBUTES	--------------------
	
	/**
	  * All values of this enumeration
	  */
	val values = Vector[SqlFileType](Full, Changes)
	
	
	// OTHER	------------------------
	
	/**
	  * Interprets a sql file type string
	  * @param typeString A sql file type string
	  * @return File type that best matches specified string
	  */
	def forString(typeString: String) =
	{
		val lower = typeString.toLowerCase
		
		if (lower.contains("full") || lower.contains("all"))
			Full
		else if (lower.contains("changes") || lower.contains("update"))
			Changes
		else
			Full
	}
	
	
	// NESTED	------------------------
	
	/**
	  * Used when a file contains the whole database structure
	  */
	object Full extends SqlFileType
	{
		override def toString = "Full"
	}
	
	/**
	  * Used when a file contains changes between database versions
	  */
	object Changes extends SqlFileType
	{
		override def toString = "Changes"
	}
}