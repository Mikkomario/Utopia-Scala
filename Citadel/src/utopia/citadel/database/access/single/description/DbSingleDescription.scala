package utopia.citadel.database.access.single.description

import utopia.metropolis.model.stored.description.Description
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual Descriptions, based on their id
  * @since 2021-10-23
  */
case class DbSingleDescription(id: Int) 
	extends UniqueDescriptionAccess with SingleIntIdModelAccess[Description]

