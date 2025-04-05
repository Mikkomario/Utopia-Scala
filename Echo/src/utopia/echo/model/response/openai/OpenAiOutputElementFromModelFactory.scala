package utopia.echo.model.response.openai

import utopia.flow.collection.immutable.Empty
import utopia.flow.generic.model.template.ModelLike.AnyModel
import utopia.flow.util.TryExtensions._

/**
  * Common trait for factories which convert (ordered) output models into output elements of a specific type
  * @author Mikko Hilpinen
  * @since 04.04.2025, v1.3
  */
trait OpenAiOutputElementFromModelFactory[+A] extends HasTypeIdentifiers
{
	// ABSTRACT -------------------------
	
	/**
	  * @param index Index of the targeted model
	  * @return An interface for parsing a model at that index
	  */
	def at(index: Int): OpenAiModelParser[A]
	
	
	// OTHER    --------------------------
	
	/**
	  * @param modelsByType A map where keys are model type properties and values are output models,
	  *                     coupled with their indices.
	  * @return Parsed output elements. Failure if failed to parse any element.
	  */
	def apply(modelsByType: Map[String, IterableOnce[(AnyModel, Int)]]) =
		typeIdentifiers.iterator
			.flatMap { typeIdentifier =>
				modelsByType.get(typeIdentifier) match {
					case Some(models) => models.iterator.map { case (model, index) => at(index)(model) }
					case None => Empty
				}
			}
			.toTry
}
