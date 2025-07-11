package utopia.logos.database.storable.text

import utopia.logos.model.factory.text.StatementPlacementFactory
import utopia.vault.model.immutable.Storable

/**
  * Common trait for factories used for constructing statement placement database models
  * @tparam DbModel Type of database interaction models constructed
  * @tparam A Type of read instances
  * @tparam Data Supported data-part type
  * @author Mikko Hilpinen
  * @since 30.12.2024, v0.4
  */
trait StatementPlacementDbModelFactoryLike[+DbModel <: Storable, +A, -Data] 
	extends TextPlacementDbModelFactoryLike[DbModel, A, Data] with StatementPlacementFactory[DbModel]

