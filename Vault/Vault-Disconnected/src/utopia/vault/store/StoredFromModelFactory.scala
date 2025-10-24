package utopia.vault.store

import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties

import scala.util.Try

/**
 * Used for constructing stored instances from model / JSON object data
 * @tparam Data The type of the wrapped data part
 * @tparam A Type of the constructed stored instance
 * @author Mikko Hilpinen
 * @since 14.06.2024, v1.19
 */
trait StoredFromModelFactory[Data, +A] extends FromModelFactory[A]
{
	// ABSTRACT -----------------------------
	
	/**
	 * @return Factory used for constructing the data portion of the final models
	 */
	protected def dataFactory: FromModelFactory[Data]
	
	/**
	 * Attempts to complete the parsing by wrapping the specified data
	 * @param model Input model
	 * @param data Data extracted from the model
	 * @return Parse result, which may be a failure
	 */
	protected def complete(model: HasProperties, data: Data): Try[A]
	
	
	// IMPLEMENTED  -------------------------
	
	override def apply(model: HasProperties): Try[A] = dataFactory(model).flatMap { data => complete(model, data) }
}
