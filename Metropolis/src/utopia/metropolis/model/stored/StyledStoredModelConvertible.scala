package utopia.metropolis.model.stored

import utopia.flow.collection.value.typeless.Constant
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.StyledModelConvertible

/**
  * A common trait for models that are registered to database and which can be converted to
  * models using different styling options
  * @author Mikko Hilpinen
  * @since 29.6.2021, v1.0.1
  */
trait StyledStoredModelConvertible[+Data <: StyledModelConvertible]
	extends StoredModelConvertible[Data] with StyledModelConvertible
{
	// ABSTRACT -------------------------------
	
	/**
	  * @return Whether id should be included in the simple model
	  */
	protected def includeIdInSimpleModel: Boolean
	
	
	// IMPLEMENTED  ---------------------------
	
	override def toSimpleModel =
		if (includeIdInSimpleModel) Constant("id", id) +: data.toSimpleModel else data.toSimpleModel
}
