package utopia.annex.model

import utopia.flow.generic.model.template.ModelConvertible

/**
  * @author Mikko Hilpinen
  * @since 21.12.2023, v1.6.1
  */
package object request
{
	@deprecated("This type will be rewritten in a future release. Please use PostSpiritRequest instead", "v1.7")
	type PostRequest[+S <: Spirit with ModelConvertible] = PostSpiritRequest[S]
}
