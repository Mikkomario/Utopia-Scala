package utopia.logos.model.combined.text

import utopia.flow.collection.immutable.Empty
import utopia.logos.model.combined.url.DetailedLinkPlacement
import utopia.logos.model.stored.text.{Delimiter, StoredStatement}
import utopia.logos.model.template.Placed

object DetailedStatement
{
	// OTHER    ---------------------------
	
	/**
	 * @param statement Statement to wrap
	 * @param words Words that appear within that statement
	 * @param links Links that appear within that statement
	 * @param delimiter Delimiter that ends the statement
	 * @return A combination of this information
	 */
	def apply(statement: StoredStatement, words: Seq[StatedWord] = Empty,
	          links: Seq[DetailedLinkPlacement] = Empty, delimiter: Option[Delimiter] = None): DetailedStatement =
		_DetailedStatement(statement, words, links, delimiter)
	
	
	// NESTED   ---------------------------
	
	private case class _DetailedStatement(statement: StoredStatement, words: Seq[StatedWord],
	                                      links: Seq[DetailedLinkPlacement], delimiter: Option[Delimiter])
		extends DetailedStatement
	{
		// IMPLEMENTED  ------------------------
		
		override protected def wrap(factory: StoredStatement): DetailedStatement = copy(statement = factory)
	}
}

/**
 * Contains full statement information:
 *      - Included words and their placements
 *      - Included links and their placements
 *      - Included delimiter
 *
 * @author Mikko Hilpinen
 * @since 17.12.2024, v0.4
 */
trait DetailedStatement extends CombinedStatement[DetailedStatement]
{
	// ABSTRACT ----------------------------
	
	/**
	 * @return Words that appear within this statement
	 */
	def words: Seq[StatedWord]
	/**
	 * @return Links that appear within this statement
	 */
	def links: Seq[DetailedLinkPlacement]
	/**
	 * @return Delimiter that ends this statement
	 */
	def delimiter: Option[Delimiter]
	
	
	// IMPLEMENTED  ------------------------
	
	override def toString = {
		val delimiterStr = delimiter match {
			case Some(d) => d.text
			case None => ""
		}
		val textPart = (words ++[Placed] links).sorted.mkString(" ")
		s"$textPart$delimiterStr"
	}
	
	
	// OTHER    --------------------------
	
	/**
	 * @param wordId Id of the targeted word
	 * @return Whether this statement contains that word
	 */
	def containsWord(wordId: Int) = words.exists { _.id == wordId }
}