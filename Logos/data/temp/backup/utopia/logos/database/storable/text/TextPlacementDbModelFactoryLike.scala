package utopia.logos.database.storable.text

import utopia.logos.model.factory.text.TextPlacementFactory
import utopia.vault.model.immutable.Storable
import utopia.vault.model.template.{FromIdFactory, HasIdProperty}
import utopia.vault.nosql.storable.StorableFactory

/**
  * Common trait for factories used for constructing text placement database models
  * @tparam DbModel Type of database interaction models constructed
  * @tparam A Type of read instances
  * @tparam Data Supported data-part type
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait TextPlacementDbModelFactoryLike[+DbModel <: Storable, +A, -Data] 
	extends StorableFactory[DbModel, A, Data] with FromIdFactory[Int, DbModel] with HasIdProperty 
		with TextPlacementFactory[DbModel]

