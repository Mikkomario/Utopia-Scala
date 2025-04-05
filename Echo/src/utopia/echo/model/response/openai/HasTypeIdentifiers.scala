package utopia.echo.model.response.openai

/**
  * An interface for objects which define 1-n type identifiers.
  * Usually used when parsing Open AI model output, which can have variable (model) typing.
  * @author Mikko Hilpinen
  * @since 04.04.2025, v1.3
  */
trait HasTypeIdentifiers
{
	/**
	  * @return Accepted model "type" property values.
	  */
	def typeIdentifiers: Set[String]
}
