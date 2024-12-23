package utopia.logos.database.access.many.text.word

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.storable.text.WordDbModel
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView
import utopia.vault.sql.Condition

/**
  * A common trait for access points which target multiple words or similar instances at a time
  * @tparam A Type of read (words -like) instances
  * @tparam Repr Type of this access point
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait ManyWordsAccessLike[+A, +Repr] extends ManyModelAccess[A] with Indexed with FilterableView[Repr]
{
	// COMPUTED	--------------------
	
	/**
	  * text of the accessible words
	  */
	def text(implicit connection: Connection) = pullColumn(model.text.column).flatMap { _.string }
	/**
	  * creation times of the accessible words
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.created.column).map { v => v.getInstant }
	/**
	  * Unique ids of the accessible words
	  */
	def ids(implicit connection: Connection) = pullColumn(index).map { v => v.getInt }
	
	/**
	  * All accessible word ids mapped to their string values
	  * @param connection Implicit DB Connection
	  */
	def toMap(implicit connection: Connection) =
		pullColumnMap(model.text.column, index).map { case (text, id) => text.getString -> id.getInt }
	
	/**
	  * Model which contains the primary database properties interacted with in this access point
	  */
	protected def model = WordDbModel
	
	
	// OTHER	--------------------
	
	/**
	  * @param text text to target
	  * @return Copy of this access point that only includes words with the specified text
	  */
	def matching(text: String) = filter(model.text.column <=> text)
	/**
	  * @param text Targeted text
	  * @return Copy of this access point that only includes words where text is within the specified value set
	  */
	// FIXME: Fails when emojis are involved
	def matchingWords(text: Iterable[String]) = filter(model.text.column.in(text))
	
	/**
	  * @param words Searched words or strings
	  * @return Access to words that contain any of the specified strings
	  */
	def like(words: Seq[String]) = filter(Condition.or(words.map { model.text.column.contains }))
	/**
	  * @param word A searched word or a string
	  * @return Access to words that contain the specified string
	  */
	def containing(word: String) = filter(model.text.column.contains(word))
}

