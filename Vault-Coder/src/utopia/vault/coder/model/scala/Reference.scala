package utopia.vault.coder.model.scala

object Reference
{
	/**
	  * Imports java.time.Instant
	  */
	val instant = apply("java.time", "Instant")
	val localDate = apply("java.time", "LocalDate")
	val localTime = apply("java.time", "LocalTime")
	
	val noSuchElementException = apply("java.util", "NoSuchElementException")
	
	/**
	  * Imports implicit value conversions (Flow)
	  */
	val valueConversions = apply("utopia.flow.generic.ValueConversions", "_")
	/**
	  * Imports implicit value unwraps (Flow)
	  */
	val valueUnwraps = apply("utopia.flow.generic.ValueUnwraps", "_")
	/**
	  * Imports implicit collection extensions (Flow)
	  */
	val collectionExtensions = apply("utopia.flow.util.CollectionExtensions", "_")
	/**
	  * Imports implicit sql features (Vault)
	  */
	val sqlExtensions = apply("utopia.vault.sql.SqlExtensions", "_")
	
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
	  * Imports a database connection (Vault)
	  */
	val connection = apply("utopia.vault.database", "Connection")
	/**
	  * Imports a database table (Vault)
	  */
	val table = apply("utopia.vault.model.immutable", "Table")
	/**
	  * Imports the Stored trait (Vault)
	  */
	val stored = apply("utopia.vault.model.template", "Stored")
	/**
	  * Imports the Indexed trait (Vault)
	  */
	val indexed = apply("utopia.vault.nosql.template", "Indexed")
	/**
	  * Imports the FromValidatedRowModelFactory trait (Vault)
	  */
	val fromValidatedRowModelFactory = apply("utopia.vault.nosql.factory.row.model", "FromValidatedRowModelFactory")
	/**
	  * Imports the FromRowFactoryWithTimestamps trait (Vault)
	  */
	val fromRowFactoryWithTimestamps = apply("utopia.vault.nosql.factory.row", "FromRowFactoryWithTimestamps")
	/**
	  * Imports the Storable (with factory) trait (Vault)
	  */
	val storableWithFactory = apply("utopia.vault.model.immutable", "StorableWithFactory")
	/**
	  * Imports the DataInserter trait (Vault)
	  */
	val dataInserter = apply("utopia.vault.model.template", "DataInserter")
	/**
	  * Imports the SingleRowModelAccess trait (Vault)
	  */
	val singleRowModelAccess = apply("utopia.vault.nosql.access.single.model", "SingleRowModelAccess")
	/**
	  * Imports the ManyRowModelAccess trait (Vault)
	  */
	val manyRowModelAccess = apply("utopia.vault.nosql.access.many.model", "ManyRowModelAccess")
	/**
	  * Imports the DistinctModelAccess trait (Vault)
	  */
	val distinctModelAccess = apply("utopia.vault.nosql.access.template.model", "DistinctModelAccess")
	/**
	  * Imports the UniqueModelAccess trait (Vault)
	  */
	val uniqueModelAccess = apply("utopia.vault.nosql.access.single.model.distinct", "UniqueModelAccess")
	/**
	  * Imports the SingleIdModel class from Vault
	  */
	val singleIdModelAccess = apply("utopia.vault.nosql.access.single.model.distinct", "SingleIdModelAccess")
	/**
	  * Imports the UnconditionalView trait (Vault)
	  */
	val unconditionalView = apply("utopia.vault.nosql.view", "UnconditionalView")
	/**
	  * Imports the combination of Stored & ModelConvertible (Vault)
	  */
	val storedModelConvertible = apply("utopia.vault.model.template", "StoredModelConvertible")
}

/**
  * Represents an imported external class or object etc.
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  * @param parentPath Path leading to the imported item. E.g. "utopia.vault.coder.model.scala"
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
