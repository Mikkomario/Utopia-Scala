package utopia.citadel.coder.model.scala

object Reference
{
	/**
	  * Imports java.time.Instant
	  */
	val instant = apply("java.time", "Instant")
	
	/**
	  * Imports implicit value conversions (Flow)
	  */
	val valueConversions = apply("utopia.flow.generic.ValueConversions", "_")
	/**
	  * Imports implicit value unwraps (Flow)
	  */
	val valueUnwraps = apply("utopia.flow.generic.ValueUnwraps", "_")
	
	/**
	  * Imports the (generic) Value type from Flow
	  */
	val value = apply("utopia.flow.datastructure.immutable", "Value")
	/**
	  * Imports the constant type from Flow
	  */
	val constant = apply("utopia.flow.datastructure.immutable", "Constant")
	/**
	  * Imports the immutable model type (Flow)
	  */
	val model = apply("utopia.flow.datastructure.immutable", "Model")
	/**
	  * Imports the ModelConvertible trait (Flow)
	  */
	val modelConvertible = apply("utopia.flow.generic", "ModelConvertible")
	
	/**
	  * Imports the Stored trait (Vault)
	  */
	val stored = apply("utopia.vault.model.template", "Stored")
	/**
	  * Imports the FromValidatedRowModelFactory trait (Vault)
	  */
	val fromValidatedRowModelFactory = apply("utopia.vault.nosql.factory.row.model", "FromValidatedRowModelFactory")
	/**
	  * Imports the Storable (with factory) trait (Vault)
	  */
	val storableWithFactory = apply("utopia.vault.model.immutable", "StorableWithFactory")
	/**
	  * Imports the DataInserter trait (Vault)
	  */
	val dataInserter = apply("utopia.vault.model.template", "DataInserter")
	
	/**
	  * Imports the combination of Stored & ModelConvertible (Metropolis)
	  */
	val storedModelConvertible = apply("utopia.metropolis.model.stored", "StoredModelConvertible")
	
	/**
	  * Imports the main Tables instance from Utopia Citadel
	  */
	val citadelTables = apply("utopia.citadel.database", "Tables")
}

/**
  * Represents an imported external class or object etc.
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  * @param parentPath Path leading to the imported item. E.g. "utopia.citadel.coder.model.scala"
  * @param target Name of the imported item. E.g. "Reference"
  */
case class Reference(parentPath: String, target: String)
{
	/**
	  * @param typeParam1 First generic type parameter
	  * @param moreTypeParams Additional generic type parameters
	  * @return A generic type based on this reference
	  */
	def apply(typeParam1: ScalaType, moreTypeParams: ScalaType*) =
		ScalaType.generic(this, typeParam1, moreTypeParams: _*)
}
