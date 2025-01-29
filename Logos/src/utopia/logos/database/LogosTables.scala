package utopia.logos.database

import utopia.vault.model.immutable.Table

/**
  * Used for accessing the database tables introduced in this project
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
object LogosTables
{
	// COMPUTED	--------------------
	
	/**
	  * Table that contains domains (Represents the address of an internet service)
	  */
	lazy val domain = apply("domain")
	/**
	  * Table that contains links (Represents a link for a specific http(s) request)
	  */
	lazy val link = apply("link")
	/**
	  * Table that contains link placements (Places a link within a statement)
	  */
	lazy val linkPlacement = apply("link_placement")
	/**
	  * Table that contains request paths (Represents a specific http(s) request url, 
	  * not including any query parameters)
	  */
	lazy val requestPath = apply("request_path")
	
	/**
	  * Table that contains statements (Represents an individual statement made within some text.
	  *  Consecutive statements form whole texts.)
	  */
	lazy val statement = apply("statement")
	/**
	  * Table that contains words (Represents an individual word used in a text document. Case-sensitive.)
	  */
	lazy val word = apply("word")
	/**
	  * Table that contains word placements (Records when a word is used in a statement)
	  */
	lazy val wordPlacement = apply("word_placement")
	/**
	  * Table that contains delimiters (Represents a character sequence used to separate two statements or parts
	  *  of a statement)
	  */
	lazy val delimiter = apply("delimiter")
	
	
	// OTHER	--------------------
	
	private def apply(tableName: String): Table = LogosContext.table(tableName)
}

