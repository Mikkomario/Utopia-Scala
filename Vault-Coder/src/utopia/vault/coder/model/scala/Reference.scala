package utopia.vault.coder.model.scala

import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.scala.template.ScalaConvertible
import utopia.vault.coder.model.data.ProjectSetup
import utopia.vault.coder.model.scala.code.CodePiece

object Reference
{
	import Package._
	
	// Java time
	
	lazy val instant = apply(javaTime, "Instant")
	lazy val localDate = apply(javaTime, "LocalDate")
	lazy val localTime = apply(javaTime, "LocalTime")
	lazy val timeUnit = apply(java/"util.concurrent", "TimeUnit")
	
	// Scala time
	
	lazy val duration = apply(scalaDuration, "Duration")
	lazy val finiteDuration = apply(scalaDuration, "FiniteDuration")
	
	// Other Java
	
	lazy val noSuchElementException = apply("java.util", "NoSuchElementException")
	
	// Extensions
	
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
	  * Imports time extensions (flow)
	  */
	lazy val timeExtensions = extensions(flowTime, "TimeExtensions")
	/**
	  * Imports implicit sql features (Vault)
	  */
	lazy val sqlExtensions = extensions(sql, "SqlExtensions")
	
	// Flow
	
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
	lazy val templateModel = apply(struct, "template", "Model")
	/**
	  * Imports the immutable model type (Flow)
	  */
	lazy val model = apply(immutableStruct, "Model")
	/**
	  * Imports the ModelConvertible trait (Flow)
	  */
	lazy val modelConvertible = apply(flowGenerics, "ModelConvertible")
	/**
	  * Imports the Now -object (Flow)
	  */
	lazy val now = apply(flowTime, "Now")
	/**
	  * Imports the Days class (Flow)
	  */
	lazy val days = apply(flowTime, "Days")
	
	// Vault
	
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
	  * Imports the Deprecatable trait from Vault
	  */
	lazy val deprecatable = apply(noSql/"template", "Deprecatable")
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
	lazy val unconditionalView = apply(viewAccess, "UnconditionalView")
	/**
	  * Imports the NonDeprecatedView trait (Vault)
	  */
	lazy val nonDeprecatedView = apply(viewAccess, "NonDeprecatedView")
	/**
	  * Imports the combination of Stored & ModelConvertible (Vault)
	  */
	lazy val storedModelConvertible = apply(vaultModels/"template", "StoredModelConvertible")
	
	// Metropolis
	
	lazy val descriptionRole = apply(description, "DescriptionRole")
	lazy val descriptionLink = apply(description, "DescriptionLink")
	lazy val describedWrapper = apply(combinedDescription, "DescribedWrapper")
	lazy val simplyDescribed = apply(combinedDescription, "SimplyDescribed")
	lazy val describedFromModelFactory = apply(combinedDescription, "DescribedFromModelFactory")
	
	// Citadel
	
	lazy val citadelTables = apply(citadelDatabase, "Tables")
	lazy val descriptionLinkModelFactory = apply(citadelDatabase/"model.description", "DescriptionLinkModelFactory")
	lazy val descriptionLinkFactory = apply(citadelDatabase/"factory.description", "DescriptionLinkFactory")
	lazy val descriptionOfSingle = apply(citadelAccess/"single.description.DbDescription", "DescriptionOfSingle")
	private lazy val dbDescriptionsOrigin = citadelAccess/"many.description.DbDescriptions"
	lazy val descriptionsOfAll = apply(dbDescriptionsOrigin, "DescriptionsOfAll")
	lazy val descriptionsOfMany = apply(dbDescriptionsOrigin, "DescriptionsOfMany")
	lazy val descriptionsOfSingle = apply(dbDescriptionsOrigin, "DescriptionsOfSingle")
	
	
	// OTHER    -------------------------------
	
	/**
	  * Creates a reference to implicit extensions
	  * @param packagePath Package leading to the target
	  * @param target File / object that contains the implicits
	  * @return A reference to the implicits in that file / object
	  */
	def extensions(packagePath: Package, target: String) = apply(packagePath, s"$target._")
}

/**
  * Represents an imported external class or object etc.
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  * @param packagePath Path leading to the imported item. E.g. "utopia.vault.coder.model.scala"
  * @param importTarget The imported item which is present when referring to the referenced item,
  *                     as well as in the import
  * @param subReference Item referred to under the imported item. E.g. a specific property
  */
case class Reference(packagePath: Package, importTarget: String, subReference: String = "") extends ScalaConvertible
{
	// COMPUTED --------------------------------
	
	/**
	  * @return The target of this reference when referred from code (doesn't contain the package path,
	  *         expects an import)
	  */
	def target = if (subReference.isEmpty) importTarget else s"$importTarget.$subReference"
	/**
	  * @return A code referring to the target of this reference using its name
	  */
	def targetCode = CodePiece(target, Set(this))
	
	/**
	  * @return Whether it is possible to group this reference with other references using { ... } syntax
	  */
	def canBeGrouped = subReference.isEmpty && !importTarget.contains('.')
	
	/**
	  * @param setup Implicit project setup
	  * @return Path to the referenced file
	  */
	def path(implicit setup: ProjectSetup) =
	{
		val (packagePart, classPart) = importTarget.splitAtLast(".")
		// Case: There are no two parts in the target => Uses the only part as the file name
		if (classPart.isEmpty)
			packagePath.pathTo(packagePart)
		// Case: Target consists of multiple parts => appends the package part to the package path (directory path)
		else
			(packagePath/packagePart).pathTo(classPart)
	}
	
	
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
	def /(newTarget: String) = subReference.notEmpty match
	{
		case Some(oldSubRef) => copy(subReference = s"$oldSubRef.$newTarget")
		case None => copy(subReference = newTarget)
	}
	
	/**
	  * @param packagePath A package from which this reference is viewed from
	  * @return A copy of this reference, relative to that package
	  */
	def from(packagePath: Package) = copy(packagePath = this.packagePath.fromPerspectiveOf(packagePath))
	
	
	// IMPLEMENTED  ----------------------------
	
	override def toScala =
	{
		if (importTarget.isEmpty)
			packagePath.toScala
		else if (packagePath.isEmpty)
			importTarget
		else
			s"${packagePath.toScala}.$importTarget"
	}
}
