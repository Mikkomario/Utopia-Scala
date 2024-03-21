package utopia.logos.database

import utopia.vault.model.immutable.Table

/**
  * Used for accessing the database tables introduced in this project
  * @author Mikko Hilpinen
  * @since 20.03.2024, v1.0
  */
object LogosTables
{
	// COMPUTED	--------------------
	
	/**
	  * 
		Table that contains delimiters (Represents a character sequence used to separate two statements or parts
	  *  of a statement)
	  */
	def delimiter = apply("delimiter")
	
	/**
	  * Table that contains domains (Represents the address of an internet service)
	  */
	def domain = apply("domain")
	
	/**
	  * Table that contains links (Represents a link for a specific http(s) request)
	  */
	def link = apply("link")
	
	/**
	  * Table that contains link placements (Places a link within a statement)
	  */
	def linkPlacement = apply("link_placement")
	
	/**
	  * Table that contains request paths (Represents a specific http(s) request url, 
	  * not including any query parameters)
	  */
	def requestPath = apply("request_path")
	
	/**
	  * Table that contains statements (Represents an individual statement made within some text.
	  *  Consecutive statements form whole texts.)
	  */
	def statement = apply("statement")
	
	/**
	  * Table that contains words (Represents an individual word used in a text document. Case-sensitive.)
	  */
	def word = apply("word")
	
	/**
	  * Table that contains word placements (Records when a word is used in a statement)
	  */
	def wordPlacement = apply("word_placement")
	
	
	// OTHER	--------------------
	
	private def apply(tableName: String): Table = {
		// TODO: Refer to a tables instance of your choice
		// If you're using the Citadel module, import utopia.citadel.database.Tables
		// Tables(tableName)
		???
	}
}

