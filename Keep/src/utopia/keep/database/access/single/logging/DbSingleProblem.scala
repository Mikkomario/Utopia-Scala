package utopia.keep.database.access.single.logging

import utopia.keep.model.stored.logging.Problem
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual Problems, based on their id
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
case class DbSingleProblem(id: Int) extends UniqueProblemAccess with SingleIntIdModelAccess[Problem]

