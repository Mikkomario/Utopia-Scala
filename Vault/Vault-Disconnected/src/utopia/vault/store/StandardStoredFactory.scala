package utopia.vault.store

import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties

import scala.util.Try

/**
  * A standard implementation of [[StoredFromModelFactory]], which assumes that the model contains an
  * Int type "id" property
  * @tparam Data Type of the wrapped data object
  * @tparam A Type of the generated stored instances
  * @author Mikko Hilpinen
  * @since 03.08.2025, v2.0
  */
trait StandardStoredFactory[Data, A] extends StoredFromModelFactory[Data, A]
{
	// ABSTRACT --------------------------
	
	/**
	  * @param id ID of the stored instance
	  * @param data Data portion of the stored instance
	  * @return The stored instance
	  */
	def apply(id: Int, data: Data): A
	
	
	// IMPLEMENTED  ----------------------
	
	override protected def complete(model: HasProperties, data: Data): Try[A] = model("id").tryInt.map { apply(_, data) }
}
