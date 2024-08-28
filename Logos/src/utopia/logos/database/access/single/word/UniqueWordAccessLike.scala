package utopia.logos.database.access.single.word

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.logos.database.storable.word.WordModel
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

import java.time.Instant

/**
  * A common trait for access points which target individual words or similar items at a time
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
@deprecated("Replaced with a new version", "v0.3")
trait UniqueWordAccessLike[+A] 
	extends SingleModelAccess[A] with DistinctModelAccess[A, Option[A], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Text representation of this word. None if no word (or value) was found.
	  */
	def text(implicit connection: Connection) = pullColumn(model.text.column).getString
	
	/**
	  * Time when this word was added to the database. None if no word (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.created.column).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = WordModel
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the creation times of the targeted words
	  * @param newCreated A new created to assign
	  * @return Whether any word was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.created.column, newCreated)
	
	/**
	  * Updates the text of the targeted words
	  * @param newText A new text to assign
	  * @return Whether any word was affected
	  */
	def text_=(newText: String)(implicit connection: Connection) = putColumn(model.text.column, newText)
}

