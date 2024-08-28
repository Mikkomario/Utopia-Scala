package utopia.logos.model.stored.word

import utopia.logos.model.partial.word.TextStatementLinkDataLike
import utopia.logos.model.template.StoredPlaced
import utopia.vault.model.template.FromIdFactory

/**
 * Common trait for stored text statement links (i.e. links between texts (custom class) and statements (logos class))
 *
 * @author Mikko Hilpinen
 * @since 15/03/2024, v0.2
 */
@deprecated("Replaced with StoredTextPlacementLike", "v0.3")
trait TextStatementLinkLike[+Data <: TextStatementLinkDataLike[Data], +Repr]
	extends TextStatementLinkDataLike[Repr] with StoredPlaced[Data, Int] with FromIdFactory[Int, Repr]
{
	override def textId: Int = data.textId
	override def statementId: Int = data.statementId
}
