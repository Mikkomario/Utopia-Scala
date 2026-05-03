package utopia.vigil.database.storable.scope

import utopia.vault.model.immutable.Storable
import utopia.vault.model.template.HasIdProperty
import utopia.vault.nosql.storable.StorableFactory
import utopia.vault.store.FromIdFactory
import utopia.vigil.model.factory.scope.ScopeRightFactory

/**
  * Common trait for factories used for constructing scope right database models
  * @tparam DbModel Type of database interaction models constructed
  * @tparam A Type of read instances
  * @tparam Data Supported data-part type
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
trait ScopeRightDbModelFactoryLike[+DbModel <: Storable, +A, -Data] 
	extends StorableFactory[DbModel, A, Data] with FromIdFactory[Int, DbModel] with HasIdProperty 
		with ScopeRightFactory[DbModel]

