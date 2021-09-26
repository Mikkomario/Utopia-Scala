package utopia.vault.coder.model.scala

import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.scala.template.ScalaConvertible
import utopia.vault.coder.model.data.ProjectSetup

object Reference
{
	import Package._
	
	lazy val instant = apply(javaTime, "Instant")
	lazy val localDate = apply(javaTime, "LocalDate")
	lazy val localTime = apply(javaTime, "LocalTime")
	
	lazy val noSuchElementException = apply("java.util", "NoSuchElementException")
	
	/**
	  * Imports implicit value conversions (Flow)
	  */
	lazy val valueConversions = extensions(flowGenerics, "ValueConversions")
	/**
	  * Imports implicit value unwraps (Flow)
	  */
	lazy val valueUnwraps = extensions(flowGenerics, "ValueUnwraps")
	/**
	  * Imports implicit collection extensions (Flow)
	  */
	lazy val collectionExtensions = extensions(flowUtils, "CollectionExtensions")
	/**
	  * Imports implicit sql features (Vault)
	  */
	lazy val sqlExtensions = extensions(sql, "SqlExtensions")
	
	/**
	  * Imports the (generic) Value type from Flow
	  */
	lazy val value = apply(immutableStruct, "Value")
	/**
	  * Imports the abstract Property trait (Flow)
	  */
	lazy val property = apply(struct/"template", "Property")
	/**
	  * Imports the constant type from Flow
	  */
	lazy val constant = apply(immutableStruct, "Constant")
	/**
	  * Imports the template model type (Flow)
	  */
	lazy val templateModel = apply(struct, "template.Model")
	/**
	  * Imports the immutable model type (Flow)
	  */
	lazy val model = apply(immutableStruct, "Model")
	/**
	  * Imports the ModelConvertible trait (Flow)
	  */
	lazy val modelConvertible = apply(flowGenerics, "ModelConvertible")
	
	/**
	  * Imports a database connection (Vault)
	  */
	lazy val connection = apply(database, "Connection")
	/**
	  * Imports a database table (Vault)
	  */
	lazy val table = apply(vaultModels/"immutable", "Table")
	/**
	  * Imports the Stored trait (Vault)
	  */
	lazy val stored = apply(vaultModels/"template", "Stored")
	/**
	  * Imports the Indexed trait (Vault)
	  */
	lazy val indexed = apply(noSql/"template", "Indexed")
	/**
	  * Imports the FromRowModelFactory trait (Vault)
	  */
	lazy val fromRowModelFactory = apply(fromRowFactories/"model", "FromRowModelFactory")
	/**
	  * Imports the FromValidatedRowModelFactory trait (Vault)
	  */
	lazy val fromValidatedRowModelFactory = apply(fromRowFactories/"model", "FromValidatedRowModelFactory")
	/**
	  * Imports the FromRowFactoryWithTimestamps trait (Vault)
	  */
	lazy val fromRowFactoryWithTimestamps = apply(fromRowFactories, "FromRowFactoryWithTimestamps")
	/**
	  * Imports the Storable (with factory) trait (Vault)
	  */
	lazy val storableWithFactory = apply(vaultModels/"immutable", "StorableWithFactory")
	/**
	  * Imports the DataInserter trait (Vault)
	  */
	lazy val dataInserter = apply(noSql/"storable", "DataInserter")
	/**
	  * Imports the NullDeprecatable trait from Vault
	  */
	lazy val nullDeprecatable = apply(deprecation, "NullDeprecatable")
	/**
	  * Imports the DeprecatableAfter trait from Vault
	  */
	lazy val deprecatableAfter = apply(deprecation, "DeprecatableAfter")
	/**
	  * Imports the Expiring trait from Vault
	  */
	lazy val expiring = apply(deprecation, "Expiring")
	/**
	  * Imports the SingleRowModelAccess trait (Vault)
	  */
	lazy val singleRowModelAccess = apply(singleModelAccess, "SingleRowModelAccess")
	/**
	  * Imports the ManyRowModelAccess trait (Vault)
	  */
	lazy val manyRowModelAccess = apply(manyModelAccess, "ManyRowModelAccess")
	/**
	  * Imports the DistinctModelAccess trait (Vault)
	  */
	lazy val distinctModelAccess = apply(access/"template.model", "DistinctModelAccess")
	/**
	  * Imports the UniqueModelAccess trait (Vault)
	  */
	lazy val uniqueModelAccess = apply(singleModelAccess/"distinct", "UniqueModelAccess")
	/**
	  * Imports the SingleIdModel class from Vault
	  */
	lazy val singleIdModelAccess = apply(singleModelAccess/"distinct", "SingleIdModelAccess")
	/**
	  * Imports the UnconditionalView trait (Vault)
	  */
	lazy val unconditionalView = apply(noSql/"view", "UnconditionalView")
	/**
	  * Imports the combination of Stored & ModelConvertible (Vault)
	  */
	lazy val storedModelConvertible = apply(vaultModels/"template", "StoredModelConvertible")
	
	
	// OTHER    -------------------------------
	
	/**
	  * Creates a new standard reference
	  * @param packagePath The package leading to the target
	  * @param target Referenced item
	  * @return A new reference
	  */
	def apply(packagePath: Package, target: String): Reference = apply(packagePath, None, target)
	
	/**
	  * Creates a reference to implicit extensions
	  * @param packagePath Package leading to the target
	  * @param target File / object that contains the implicits
	  * @return A reference to the implicits in that file / object
	  */
	def extensions(packagePath: Package, target: String) =
		apply(packagePath, Some(target), "_")
}

/**
  * Represents an imported external class or object etc.
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  * @param packagePath Path leading to the imported item. E.g. "utopia.vault.coder.model.scala"
  * @param parentClass Class / file that hosts this item / items (optional)
  * @param target Name of the imported item. E.g. "Reference"
  */
case class Reference(packagePath: Package, parentClass: Option[String], target: String) extends ScalaConvertible
{
	// COMPUTED --------------------------------
	
	/**
	  * @return Whether it is possible to group this reference with other references using { ... } syntax
	  */
	def canBeGrouped = parentClass.isEmpty && !target.contains('.')
	
	/**
	  * @param setup Implicit project setup
	  * @return Path to the referenced file
	  */
	def path(implicit setup: ProjectSetup) = packagePath.pathTo(parentClass.getOrElse(target.untilFirst(".")))
	
	
	// OTHER    --------------------------------
	
	/**
	  * @param typeParam1 First generic type parameter
	  * @param moreTypeParams Additional generic type parameters
	  * @return A generic type based on this reference
	  */
	def apply(typeParam1: ScalaType, moreTypeParams: ScalaType*) =
		ScalaType.generic(this, typeParam1, moreTypeParams: _*)
	
	/**
	  * @param newTarget Another target under this reference
	  * @return Reference to that sub-item
	  */
	def /(newTarget: String) = parentClass match
	{
		case Some(_) => copy(target = s"$target.$newTarget")
		case None => copy(parentClass = Some(target), target = newTarget)
	}
	
	
	// IMPLEMENTED  ----------------------------
	
	override def toScala = parentClass match
	{
		case Some(parent) => s"$packagePath.$parent.$target"
		case None => s"$packagePath.$target"
	}
}
