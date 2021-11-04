package utopia.metropolis.model.stored.organization

import utopia.metropolis.model.partial.organization.TaskData
import utopia.metropolis.model.stored.StoredModelConvertible

/**
  * Represents a Task that has already been stored in the database
  * @param id id of this Task in the database
  * @param data Wrapped Task data
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class Task(id: Int, data: TaskData) extends StoredModelConvertible[TaskData]

