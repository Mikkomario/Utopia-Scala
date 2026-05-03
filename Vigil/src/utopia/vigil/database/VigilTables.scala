package utopia.vigil.database

import utopia.vault.model.immutable.Table

/**
  * Used for accessing the database tables introduced in this project
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
object VigilTables
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Table that contains scopes (Used for limiting authorization to certain features or areas)
	  */
	lazy val scope = apply("scope")
	
	/**
	  * Table that contains tokens (Represents a token that may be used for authorizing certain actions)
	  */
	lazy val token = apply("token")
	/**
	  * Table that contains token grant rights (Used for allowing certain token types (templates) to 
	  * generate new tokens of other types)
	  */
	lazy val tokenGrantRight = apply("token_grant_right")
	/**
	  * Table that contains token scopes (Allows a token to be used in some scope)
	  */
	lazy val tokenScope = apply("token_scope")
	/**
	  * Table that contains token templates (A template or a mold for creating new tokens)
	  */
	lazy val tokenTemplate = apply("token_template")
	/**
	  * Table that contains token template scopes (Links a (granted) scope to a token template)
	  */
	lazy val tokenTemplateScope = apply("token_template_scope")
	
	
	// OTHER	--------------------
	
	private def apply(tableName: String): Table = VigilContext.table(tableName)
}

