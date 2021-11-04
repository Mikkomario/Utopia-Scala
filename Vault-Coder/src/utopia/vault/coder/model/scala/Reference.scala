package utopia.vault.coder.model.scala

import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.scala.template.ScalaConvertible
import utopia.vault.coder.model.data.ProjectSetup
import utopia.vault.coder.model.scala.code.CodePiece

import java.nio.file.Path

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
	
	lazy val valueConversions = extensions(flowGenerics, "ValueConversions")
	lazy val valueUnwraps = extensions(flowGenerics, "ValueUnwraps")
	lazy val collectionExtensions = extensions(flowUtils, "CollectionExtensions")
	lazy val timeExtensions = extensions(flowTime, "TimeExtensions")
	lazy val sqlExtensions = extensions(sql, "SqlExtensions")
	
	// Flow
	
	lazy val value = apply(immutableStruct, "Value")
	lazy val property = apply(struct/"template", "Property")
	lazy val constant = apply(immutableStruct, "Constant")
	lazy val templateModel = apply(struct, "template", "Model")
	lazy val model = apply(immutableStruct, "Model")
	lazy val modelConvertible = apply(flowGenerics, "ModelConvertible")
	
	lazy val now = apply(flowTime, "Now")
	lazy val today = apply(flowTime, "Today")
	lazy val days = apply(flowTime, "Days")
	
	lazy val extender = apply(flowUtils, "Extender")
	
	// Vault
	
	lazy val connection = apply(database, "Connection")
	lazy val table = apply(vaultModels/"immutable", "Table")
	lazy val condition = apply(vault/"sql", "Condition")
	
	lazy val stored = apply(vaultModels/"template", "Stored")
	lazy val storedModelConvertible = apply(vaultModels/"template", "StoredModelConvertible")
	
	lazy val indexed = apply(noSql/"template", "Indexed")
	lazy val deprecatable = apply(noSql/"template", "Deprecatable")
	lazy val nullDeprecatable = apply(deprecation, "NullDeprecatable")
	lazy val deprecatableAfter = apply(deprecation, "DeprecatableAfter")
	lazy val expiring = apply(deprecation, "Expiring")
	
	lazy val fromRowModelFactory = apply(fromRowFactories/"model", "FromRowModelFactory")
	lazy val fromValidatedRowModelFactory = apply(fromRowFactories/"model", "FromValidatedRowModelFactory")
	lazy val fromRowFactoryWithTimestamps = apply(fromRowFactories, "FromRowFactoryWithTimestamps")
	lazy val combiningFactory = apply(singleLinkedFactories, "CombiningFactory")
	lazy val possiblyCombiningFactory = apply(singleLinkedFactories, "PossiblyCombiningFactory")
	lazy val multiCombiningFactory = apply(factories/"multi", "MultiCombiningFactory")
	
	lazy val storableWithFactory = apply(vaultModels/"immutable", "StorableWithFactory")
	lazy val dataInserter = apply(noSql/"storable", "DataInserter")
	
	lazy val subView = apply(viewAccess, "SubView")
	lazy val unconditionalView = apply(viewAccess, "UnconditionalView")
	lazy val nonDeprecatedView = apply(viewAccess, "NonDeprecatedView")
	
	lazy val singleRowModelAccess = apply(singleModelAccess, "SingleRowModelAccess")
	lazy val manyRowModelAccess = apply(manyModelAccess, "ManyRowModelAccess")
	lazy val distinctModelAccess = apply(access/"template.model", "DistinctModelAccess")
	lazy val uniqueModelAccess = apply(singleModelAccess/"distinct", "UniqueModelAccess")
	lazy val singleIdModelAccess = apply(singleModelAccess/"distinct", "SingleIdModelAccess")
	lazy val singleIntIdModelAccess = apply(singleModelAccess/"distinct", "SingleIntIdModelAccess")
	
	
	// Metropolis
	
	lazy val metropolisStoredModelConvertible = apply(metropolisModel/"stored", "StoredModelConvertible")
	lazy val descriptionRole = apply(description, "DescriptionRole")
	lazy val linkedDescription = apply(combinedDescription, "LinkedDescription")
	lazy val describedWrapper = apply(combinedDescription, "DescribedWrapper")
	lazy val simplyDescribed = apply(combinedDescription, "SimplyDescribed")
	lazy val describedFactory = apply(combinedDescription, "DescribedFactory")
	
	// Citadel
	
	lazy val descriptionLinkTable = apply(citadel/"model.cached", "DescriptionLinkTable")
	lazy val citadelTables = apply(citadelDatabase, "Tables")
	lazy val descriptionLinkModelFactory = apply(citadelDatabase/"model.description", "DescriptionLinkModelFactory")
	lazy val descriptionLinkFactory = apply(citadelDatabase/"factory.description", "DescriptionLinkFactory")
	lazy val linkedDescriptionFactory = apply(citadelDatabase/"factory.description", "LinkedDescriptionFactory")
	lazy val linkedDescriptionAccess = apply(descriptionAccess, "LinkedDescriptionAccess")
	lazy val linkedDescriptionsAccess = apply(descriptionsAccess, "LinkedDescriptionsAccess")
	@deprecated("Replaced with linkedDescriptionAccess", "v2.0")
	lazy val descriptionLinkAccess = apply(descriptionAccess, "DescriptionLinkAccess")
	@deprecated("Replaced with linkedDescriptionsAccess", "v2.0")
	lazy val descriptionLinksAccess = apply(descriptionsAccess, "DescriptionLinksAccess")
	lazy val singleIdDescribedAccess = apply(descriptionAccess, "SingleIdDescribedAccess")
	lazy val manyDescribedAccess = apply(descriptionsAccess, "ManyDescribedAccess")
	lazy val manyDescribedAccessByIds = apply(descriptionsAccess, "ManyDescribedAccessByIds")
	
	
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
	def path(implicit setup: ProjectSetup) = pathIn(setup.sourceRoot)
	
	
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
	
	/**
	  * @param sourceRoot Root directory
	  * @return Path to the referenced file
	  */
	def pathIn(sourceRoot: Path) =
	{
		val (packagePart, classPart) = importTarget.splitAtLast(".")
		// Case: There are no two parts in the target => Uses the only part as the file name
		if (classPart.isEmpty)
			packagePath.pathTo(packagePart, sourceRoot)
		// Case: Target consists of multiple parts => appends the package part to the package path (directory path)
		else
			(packagePath/packagePart).pathTo(classPart, sourceRoot)
	}
	
	
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
