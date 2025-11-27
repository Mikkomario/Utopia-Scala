package utopia.logos.database.access.text.statement.placement

import utopia.logos.database.LogosTables
import utopia.logos.database.access.text.statement.{AccessStatementValues, FilterByStatement}
import utopia.logos.database.access.text.word.placement.{AccessWordPlacementValues, FilterByWordPlacement}
import utopia.logos.database.access.text.word.{AccessWordValues, FilterByWord}
import utopia.logos.model.partial.text.HasTextPlacementProps
import utopia.vault.database.Connection
import utopia.vault.nosql.targeting.many.TargetingManyLike

/**
 * Common trait for access points which yield some form of statement-placements
 *
 * @tparam A Type of pulled items
 * @tparam Repr Type of this access point
 * @tparam One Type of the access point which targets only one item at a time
 * @author Mikko Hilpinen
 * @since 11.07.2025, v0.6
 */
trait AccessStatementPlacements[+A, +Repr <: TargetingManyLike[_, Repr, _], +One]
	extends TargetingManyLike[A, Repr, One] with FilterStatementPlacements[Repr]
{
	// ATTRIBUTES   ------------------------
	
	lazy val joinedToStatements = join(LogosTables.statement)
	lazy val whereStatements = FilterByStatement(joinedToStatements)
	lazy val statements = AccessStatementValues(joinedToStatements)
	
	lazy val joinedToWordPlacements = joinedToStatements.join(LogosTables.wordPlacement)
	lazy val whereWordPlacements = FilterByWordPlacement(joinedToWordPlacements)
	lazy val wordPlacements = AccessWordPlacementValues(joinedToWordPlacements)
	
	lazy val joinedToWords = joinedToWordPlacements.join(LogosTables.word)
	lazy val whereWords = FilterByWord(joinedToWords)
	lazy val words = AccessWordValues(joinedToWords)
}

object AccessStatementPlacementRows
{
	// EXTENSIONS   -----------------------
	
	implicit class RichAccessStatementPlacementRows(val a: AccessStatementPlacementRows[HasTextPlacementProps, _, _])
		extends AnyVal
	{
		/**
		 * Pulls all accessible text. Intended to be used with access points limited to a single text's statements.
		 * @param connection Implicit DB connection
		 * @return All accessible text as a single string, including full text, delimiters and links.
		 */
		def text(implicit connection: Connection) =
			a.pullWithText.sortBy { _._1.orderIndex }.iterator.map { _._2 }.mkString
		
		/**
		 * Converts all accessible statement placements to a full text mapping
		 * @param connection Implicit DB connection
		 * @return A map where keys are IDs of the text items and values are their full text content.
		 */
		def toTextMap(implicit connection: Connection) =
			a.pullWithText.groupBy { _._1.parentId }.view
				.mapValues { placements => placements.sortBy { _._1.orderIndex }.iterator.map { _._2 }.mkString }
				.toMap.withDefaultValue("")
	}
}
/**
 * Common trait for access points which yield some row-based forms of statement-placements
 * @tparam A Type of pulled items
 * @tparam Repr Type of this access point
 * @tparam One Type of the access point which targets only one item at a time
 */
trait AccessStatementPlacementRows[+A, +Repr <: TargetingManyLike[_, Repr, _], +One]
	extends AccessStatementPlacements[A, Repr, One] with AccessStatementLinkRows[A, Repr, One]