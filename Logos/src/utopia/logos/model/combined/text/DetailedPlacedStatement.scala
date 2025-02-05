package utopia.logos.model.combined.text

import utopia.flow.collection.immutable.Empty
import utopia.logos.model.combined.url.DetailedLinkPlacement
import utopia.logos.model.stored.text.{Delimiter, StatementPlacement, StoredStatement}

object DetailedPlacedStatement
{
	// OTHER    -----------------------------
	
	/**
	 * @param statement A statement to wrap
	 * @param placement Placement of that statement
	 * @return Combination of this information
	 */
	def apply(statement: DetailedStatement, placement: StatementPlacement): DetailedPlacedStatement =
		apply(statement.statement, placement, statement.words, statement.links, statement.delimiter)
	
	/**
	 * @param statement Statement to wrap
	 * @param placement Placement of that statement
	 * @param words Words that appear within that statement
	 * @param links Links that appear within that statement
	 * @param delimiter Delimiter ending the statement
	 * @return Combination of this information
	 */
	def apply(statement: StoredStatement, placement: StatementPlacement, words: Seq[StatedWord] = Empty,
	          links: Seq[DetailedLinkPlacement] = Empty, delimiter: Option[Delimiter] = None): DetailedPlacedStatement =
		_DetailedPlacedStatement(statement, words, links, delimiter, placement)
		
	
	// NESTED   -----------------------------
	
	private case class _DetailedPlacedStatement(statement: StoredStatement, words: Seq[StatedWord],
	                                            links: Seq[DetailedLinkPlacement], delimiter: Option[Delimiter],
	                                            placement: StatementPlacement)
		extends DetailedPlacedStatement
	{
		override protected def wrap(factory: StoredStatement): DetailedPlacedStatement = copy(statement = factory)
	}
}

/**
 * Represents a detailed statement appearing within a specific textual context
 * @author Mikko Hilpinen
 * @since 05.02.2025, v0.5
 */
trait DetailedPlacedStatement
	extends DetailedStatement with PlacedStatement with CombinedStatement[DetailedPlacedStatement]