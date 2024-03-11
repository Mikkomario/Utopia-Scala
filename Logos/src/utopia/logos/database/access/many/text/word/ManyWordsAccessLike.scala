package utopia.logos.database.access.many.text.word

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView
import utopia.vault.sql.Condition
import utopia.logos.database.model.text.WordModel

import java.time.Instant

/**
  * A common trait for access points which target multiple words or similar instances at a time
  * @author Mikko Hilpinen
  * @since 12.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
trait ManyWordsAccessLike[+A, +Repr] extends ManyModelAccess[A] with Indexed with FilterableView[Repr]
{
	// COMPUTED	--------------------
	
	/**
	  * text of the accessible words
	  */
	def text(implicit connection: Connection) = pullColumn(model.textColumn).flatMap { _.string }
	
	/**
	  * creation times of the accessible words
	  */
	def creationTimes(implicit connection: Connection) = pullColumn(model.createdColumn)
		.map { v => v.getInstant }
	
	def ids(implicit connection: Connection) = pullColumn(index).map { v => v.getInt }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = WordModel
	
	
	// OTHER	--------------------
	
	/**
	 * @param words Searched words
	 * @return Access to those words
	 */
	def matching(words: Iterable[String]) = filter(model.textColumn.in(words))
	
	/**
	 * @param word A searched word or a string
	 * @return Access to words that contain the specified string
	 */
	def containing(word: String) = filter(model.textColumn.contains(word))
	/**
	 * @param words Searched words or strings
	 * @return Access to words that contain any of the specified strings
	 */
	def like(words: Seq[String]) = filter(Condition.or(words.map { model.textColumn.contains }))
	
	/**
	  * Updates the creation times of the targeted words
	  * @param newCreated A new created to assign
	  * @return Whether any word was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the text of the targeted words
	  * @param newText A new text to assign
	  * @return Whether any word was affected
	  */
	def text_=(newText: String)(implicit connection: Connection) = putColumn(model.textColumn, newText)
}

