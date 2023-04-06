package utopia.vault.coder.model.scala.datatype

import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.ProjectSetup
import utopia.vault.coder.model.scala.Package
import utopia.vault.coder.model.scala.code.CodePiece
import utopia.vault.coder.model.scala.template.ScalaConvertible

import java.nio.file.Path
import scala.collection.StringOps

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
	
	lazy val valueConversions = extensions(typeCasting, "ValueConversions")
	lazy val valueUnwraps = extensions(typeCasting, "ValueUnwraps")
	lazy val collectionExtensions = extensions(flow/"collection", "CollectionExtensions")
	lazy val timeExtensions = extensions(flowTime, "TimeExtensions")
	@deprecated("Deprecated in Vault", "v1.8.1")
	lazy val sqlExtensions = extensions(sql, "SqlExtensions")
	
	// Flow
	
	lazy val value = apply(immutableGenericModels, "Value")
	lazy val property = apply(genericModelTemplates, "Property")
	lazy val constant = apply(immutableGenericModels, "Constant")
	lazy val templateModel = apply(genericModelTemplates, "ModelLike")
	lazy val model = apply(immutableGenericModels, "Model")
	lazy val valueConvertible = apply(genericModelTemplates, "ValueConvertible")
	lazy val modelConvertible = apply(genericModelTemplates, "ModelConvertible")
	lazy val modelDeclaration = apply(immutableGenericModels, "ModelDeclaration")
	lazy val propertyDeclaration = apply(immutableGenericModels, "PropertyDeclaration")
	lazy val fromModelFactory = apply(flowGenerics/"factory", "FromModelFactory")
	lazy val fromModelFactoryWithSchema = apply(flowGenerics/"factory", "FromModelFactoryWithSchema")
	
	lazy val flowDataType = apply(genericModels/"mutable", "DataType")
	lazy val stringType = flowDataType/"StringType"
	lazy val instantType = flowDataType/"InstantType"
	
	lazy val now = apply(flowTime, "Now")
	lazy val today = apply(flowTime, "Today")
	lazy val days = apply(flowTime, "Days")
	
	lazy val extender = apply(flow/"view"/"template", "Extender")
	
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
	
	lazy val view = apply(viewAccess, "View")
	lazy val subView = apply(viewAccess, "SubView")
	lazy val unconditionalView = apply(viewAccess, "UnconditionalView")
	lazy val nonDeprecatedView = apply(viewAccess, "NonDeprecatedView")
	lazy val filterableView = apply(viewAccess, "FilterableView")
	lazy val chronoRowFactoryView = apply(viewAccess, "ChronoRowFactoryView")
	lazy val timeDeprecatableView = apply(viewAccess, "TimeDeprecatableView")
	lazy val nullDeprecatableView = apply(viewAccess, "NullDeprecatableView")
	
	lazy val singleModelAccess = apply(Package.singleModelAccess, "SingleModelAccess")
	lazy val singleRowModelAccess = apply(Package.singleModelAccess, "SingleRowModelAccess")
	lazy val singleChronoRowModelAccess = apply(Package.singleModelAccess, "SingleChronoRowModelAccess")
	lazy val manyModelAccess = apply(Package.manyModelAccess, "ManyModelAccess")
	lazy val manyRowModelAccess = apply(Package.manyModelAccess, "ManyRowModelAccess")
	lazy val distinctModelAccess = apply(access/"template.model", "DistinctModelAccess")
	lazy val uniqueModelAccess = apply(Package.singleModelAccess/"distinct", "UniqueModelAccess")
	lazy val singleIdModelAccess = apply(Package.singleModelAccess/"distinct", "SingleIdModelAccess")
	lazy val singleIntIdModelAccess = apply(Package.singleModelAccess/"distinct", "SingleIntIdModelAccess")
	
	// Metropolis
	
	lazy val metropolisStoredModelConvertible = apply(metropolisModel/"stored", "StoredModelConvertible")
	lazy val storedFromModelFactory = apply(metropolisModel/"stored", "StoredFromModelFactory")
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
	
	def apply(pck: Package, target: String): Reference = apply(pck, Vector(target))
	
	/**
	  * Converts a string into a reference
	  * @param ref A string representing a reference
	  * @return A reference parsed from that string
	  */
	def apply(ref: String): Reference = {
		// Case: Empty reference
		if (ref.isEmpty)
			apply(empty, "")
		else {
			val parts = ref.split(separatorRegex).toVector.filter { s => (s: StringOps).nonEmpty }
			// Case: Extensions reference or import all -reference
			if (parts.last == "_" && parts.size > 1)
				extensions(Package(parts.dropRight(2)), parts(parts.size - 2))
			// Case: Single property reference or a sub-reference
			else if (parts.last.head.isLower && parts.size > 1)
				apply(Package(parts.dropRight(2)), Vector(parts(parts.size - 2)), parts.last)
			// Case: Standard reference
			else
				apply(Package(parts.dropRight(1)), parts.last)
		}
	}
	
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
case class Reference private(packagePath: Package, importTarget: Vector[String], subReference: String = "")
	extends ScalaConvertible
{
	// COMPUTED --------------------------------
	
	/**
	  * @return The target of this reference when referred from code (doesn't contain the package path,
	  *         expects an import)
	  */
	def target = {
		val base = importTarget.last
		if (subReference.isEmpty) base else s"$base.$subReference"
	}
	/**
	  * @return A code referring to the target of this reference using its name
	  */
	def targetCode = CodePiece(target, Set(this))
	
	/**
	  * @return Whether it is possible to group this reference with other references using { ... } syntax
	  */
	def canBeGrouped = subReference.isEmpty && importTarget.size == 1 && importTarget.forall { !_.contains('.') }
	
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
	def /(newTarget: String) = subReference.notEmpty match {
		case Some(oldSubRef) => copy(subReference = s"$oldSubRef.$newTarget")
		case None => copy(importTarget = importTarget :+ newTarget)
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
	def pathIn(sourceRoot: Path) = {
		val (packagePart, classPart) = importTarget.mkString(".").splitAtLast(".")
		// Case: There are no two parts in the target => Uses the only part as the file name
		if (classPart.isEmpty)
			packagePath.pathTo(packagePart, sourceRoot)
		// Case: Target consists of multiple parts => appends the package part to the package path (directory path)
		else
			(packagePath/packagePart).pathTo(classPart, sourceRoot)
	}
	
	
	// IMPLEMENTED  ----------------------------
	
	override def toScala = {
		if (importTarget.isEmpty)
			packagePath.toScala
		else if (packagePath.isEmpty)
			importTarget.mkString(".")
		else
			s"${packagePath.toScala}.${ importTarget.mkString(".") }"
	}
}
