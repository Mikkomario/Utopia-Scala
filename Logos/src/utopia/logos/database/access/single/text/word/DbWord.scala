package utopia.logos.database.access.single.text.word

import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{SubView, UnconditionalView, View}
import utopia.vault.sql.Condition
import utopia.logos.database.factory.text.WordFactory
import utopia.logos.database.model.text.WordModel
import utopia.logos.model.stored.text.Word

/**
  * Used for accessing individual words
  * @author Mikko Hilpinen
  * @since 12.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
object DbWord extends SingleRowModelAccess[Word] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = WordModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = WordFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted word
	  * @return An access point to that word
	  */
	def apply(id: Int) = DbSingleWord(id)
	
	/**
	 * @param word Targeted word
	 * @return Access to that word's information in the database
	 */
	def apply(word: String) = new DbSpecificWord(word)
	
	/**
	  * @param condition Filter condition to apply in addition to this root view's condition. Should yield
	  *  unique words.
	  * @return An access point to the word that satisfies the specified condition
	  */
	protected def filterDistinct(condition: Condition) = UniqueWordAccess(mergeCondition(condition))
	
	
	// NESTED   --------------------
	
	class DbSpecificWord(word: String) extends UniqueWordAccess with SubView
	{
		// IMPLEMENTED  ------------
		
		override protected def parent: View = DbWord
		override def filterCondition: Condition = this.model.withText(word).toCondition
		
		
		// OTHER    ----------------
		
		/**
		 * Acquires an id for this word. If this word doesn't yet exist in the database, it is inserted.
		 * @param connection Implicit DB Connection
		 * @return Either an existing word id (right), or a newly inserted id (left)
		 */
		def pullOrInsertId()(implicit connection: Connection) =
			id.toRight { this.model.withText(word).insert().getInt }
	}
}

