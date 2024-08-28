package utopia.logos.database.storable.word

import utopia.logos.model.template.StatementLinkFactory
import utopia.vault.model.immutable.Storable
import utopia.vault.model.template.FromIdFactory
import utopia.vault.nosql.storable.DataInserter

/**
 * Common trait for factories used for constructing DB models for text-statement links
 * and for inserting new links to the DB.
 * @tparam DbModel Type of the constructed database-model
 * @tparam Complete Type of the complete stored item
 * @tparam Data Type of the data-portion of the constructed links
 * @author Mikko Hilpinen
 * @since 14/03/2024, v1.0
 */
@deprecated("Replaced with TextPlacementDbModelFactoryLike", "v0.3")
trait TextStatementLinkModelFactoryLike[+DbModel <: Storable, +Complete, -Data]
	extends DataInserter[DbModel, Complete, Data] with StatementLinkFactory[DbModel] with FromIdFactory[Int, DbModel]