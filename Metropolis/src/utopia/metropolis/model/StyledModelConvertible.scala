package utopia.metropolis.model

import utopia.flow.collection.value.typeless.Model
import utopia.flow.generic.ModelConvertible
import utopia.metropolis.model.enumeration.ModelStyle
import utopia.metropolis.model.enumeration.ModelStyle.{Full, Simple}

/**
  * A common trait for items that can be converted to models and which support different styling options
  * @author Mikko Hilpinen
  * @since 29.6.2021, v1.0.1
  */
trait StyledModelConvertible extends ModelConvertible
{
	// ABSTRACT -----------------------------
	
	/**
	  * @return This instance as a simpler model
	  */
	def toSimpleModel: Model
	
	
	// OTHER    ----------------------------
	
	/**
	  * Converts this instance to a model
	  * @param style Styling that should be used in the conversion
	  * @return A model based on this instance
	  */
	def toModelWith(style: ModelStyle) = style match
	{
		case Full => toModel
		case Simple => toSimpleModel
	}
}
