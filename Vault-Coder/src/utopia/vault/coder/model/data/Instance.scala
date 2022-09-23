package utopia.vault.coder.model.data

import utopia.flow.collection.value.typeless.Value

/**
  * Represents an actual class instance
  * @author Mikko Hilpinen
  * @since 18.2.2022, v1.5
  * @param parentClass Class this is an instance of
  * @param valueAssignments Property value assignments specific to this instance
  * @param id Id (primary index) of this instance. May be empty.
  */
case class Instance(parentClass: Class, valueAssignments: Map[Property, Value] = Map(), id: Value = Value.empty)
