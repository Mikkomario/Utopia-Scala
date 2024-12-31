package utopia.logos.model.combined.text

import utopia.flow.collection.immutable.Empty
import utopia.logos.model.combined.url.DetailedLinkPlacement
import utopia.logos.model.stored.text.{Delimiter, StoredStatement}
import utopia.logos.model.template.Placed

/**
 * Contains full statement information:
 *      - Included words and their placements
 *      - Included links and their placements
 *      - Included delimiter
 *
 * @author Mikko Hilpinen
 * @since 17.12.2024, v0.4
 */
case class DetailedStatement(statement: StoredStatement, words: Seq[StatedWord] = Empty,
                             links: Seq[DetailedLinkPlacement] = Empty, delimiter: Option[Delimiter] = None)
	extends CombinedStatement[DetailedStatement]
{
	// IMPLEMENTED  ------------------------
	
	override def toString = {
		val delimiterStr = delimiter match {
			case Some(d) => d.text
			case None => ""
		}
		val textPart = (words ++[Placed] links).sorted.mkString(" ")
		s"$textPart$delimiterStr"
	}
	
	override protected def wrap(factory: StoredStatement): DetailedStatement = copy(statement = factory)
	
	
	// OTHER    --------------------------
	
	/**
	 * @param wordId Id of the targeted word
	 * @return Whether this statement contains that word
	 */
	def containsWord(wordId: Int) = words.exists { _.id == wordId }
}