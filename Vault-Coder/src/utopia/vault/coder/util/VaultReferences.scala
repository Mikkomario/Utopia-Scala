package utopia.vault.coder.util

import utopia.coder.model.scala.datatype.Reference

/**
  * Contains references used in this project
  * @author Mikko Hilpinen
  * @since 19.5.2023, v1.9.1
  */
object VaultReferences
{
	// COMPUTED ---------------------------
	
	def vault = Vault
	def bunnyMunch = BunnyMunch
	def metropolis = Metropolis
	def citadel = Citadel
	def paradigm = Paradigm
	def terra = Terra
	
	
	// NESTED   ---------------------------
	
	object Vault
	{
		import VaultPackages.Vault._
		
		private def pck = VaultPackages.vault
		
		lazy val connection = Reference(database, "Connection")
		lazy val table = Reference(models / "immutable", "Table")
		lazy val condition = Reference(sql, "Condition")
		
		lazy val stored = Reference(models / "template", "Stored")
		lazy val storedModelConvertible = Reference(models / "template", "StoredModelConvertible")
		lazy val fromIdFactory = Reference(models/"template", "FromIdFactory")
		
		lazy val indexed = Reference(noSql / "template", "Indexed")
		lazy val deprecatable = Reference(noSql / "template", "Deprecatable")
		lazy val nullDeprecatable = Reference(deprecation, "NullDeprecatable")
		lazy val deprecatableAfter = Reference(deprecation, "DeprecatableAfter")
		lazy val expiring = Reference(deprecation, "Expiring")
		
		lazy val fromRowModelFactory = Reference(fromRowFactories / "model", "FromRowModelFactory")
		lazy val fromValidatedRowModelFactory = Reference(fromRowFactories / "model", "FromValidatedRowModelFactory")
		lazy val fromRowFactoryWithTimestamps = Reference(fromRowFactories, "FromRowFactoryWithTimestamps")
		lazy val combiningFactory = Reference(singleLinkedFactories, "CombiningFactory")
		lazy val possiblyCombiningFactory = Reference(singleLinkedFactories, "PossiblyCombiningFactory")
		lazy val multiCombiningFactory = Reference(factories / "multi", "MultiCombiningFactory")
		
		lazy val storableWithFactory = Reference(models / "immutable", "StorableWithFactory")
		lazy val storableFactory = Reference(noSql/"storable", "StorableFactory")
		
		lazy val view = Reference(viewAccess, "View")
		lazy val subView = Reference(viewAccess, "SubView")
		lazy val unconditionalView = Reference(viewAccess, "UnconditionalView")
		lazy val nonDeprecatedView = Reference(viewAccess, "NonDeprecatedView")
		lazy val filterableView = Reference(viewAccess, "FilterableView")
		lazy val chronoRowFactoryView = Reference(viewAccess, "ChronoRowFactoryView")
		lazy val timeDeprecatableView = Reference(viewAccess, "TimeDeprecatableView")
		lazy val nullDeprecatableView = Reference(viewAccess, "NullDeprecatableView")
		
		lazy val singleModelAccess = Reference(pck.singleModelAccess, "SingleModelAccess")
		lazy val singleRowModelAccess = Reference(pck.singleModelAccess, "SingleRowModelAccess")
		lazy val singleChronoRowModelAccess = Reference(pck.singleModelAccess, "SingleChronoRowModelAccess")
		lazy val manyModelAccess = Reference(pck.manyModelAccess, "ManyModelAccess")
		lazy val manyRowModelAccess = Reference(pck.manyModelAccess, "ManyRowModelAccess")
		lazy val distinctModelAccess = Reference(modelTemplateAccess, "DistinctModelAccess")
		lazy val uniqueModelAccess = Reference(pck.singleModelAccess / "distinct", "UniqueModelAccess")
		lazy val singleIdModelAccess = Reference(pck.singleModelAccess / "distinct", "SingleIdModelAccess")
		lazy val singleIntIdModelAccess = Reference(pck.singleModelAccess / "distinct", "SingleIntIdModelAccess")
	}
	
	object BunnyMunch
	{
		import VaultPackages.BunnyMunch._
		
		lazy val jsonBunny = Reference(jawn, "JsonBunny")
	}
	
	object Metropolis
	{
		import VaultPackages.Metropolis._
		
		lazy val storedModelConvertible = Reference(models / "stored", "StoredModelConvertible")
		lazy val storedFromModelFactory = Reference(models / "stored", "StoredFromModelFactory")
		lazy val descriptionRole = Reference(description, "DescriptionRole")
		lazy val linkedDescription = Reference(combinedDescription, "LinkedDescription")
		lazy val describedWrapper = Reference(combinedDescription, "DescribedWrapper")
		lazy val simplyDescribed = Reference(combinedDescription, "SimplyDescribed")
		lazy val describedFactory = Reference(combinedDescription, "DescribedFactory")
	}
	
	object Citadel
	{
		import VaultPackages.Citadel._
		
		lazy val descriptionLinkTable = Reference(cachedModel, "DescriptionLinkTable")
		lazy val tables = Reference(database, "Tables")
		lazy val descriptionLinkModelFactory = Reference(descriptionModels, "DescriptionLinkModelFactory")
		lazy val descriptionLinkFactory = Reference(descriptionFactories, "DescriptionLinkFactory")
		lazy val linkedDescriptionFactory = Reference(descriptionFactories, "LinkedDescriptionFactory")
		lazy val linkedDescriptionAccess = Reference(descriptionAccess, "LinkedDescriptionAccess")
		lazy val linkedDescriptionsAccess = Reference(descriptionsAccess, "LinkedDescriptionsAccess")
		lazy val singleIdDescribedAccess = Reference(descriptionAccess, "SingleIdDescribedAccess")
		lazy val manyDescribedAccess = Reference(descriptionsAccess, "ManyDescribedAccess")
		lazy val manyDescribedAccessByIds = Reference(descriptionsAccess, "ManyDescribedAccessByIds")
	}
	
	object Paradigm
	{
		import VaultPackages.Paradigm._
		
		lazy val paradigmValue = Reference.extensions(generic, "ParadigmValue")
		
		lazy val dataType = Reference(generic, "ParadigmDataType")
		lazy val distance = Reference(measurement, "Distance")
		lazy val distanceUnit = Reference(measurement, "DistanceUnit")
		lazy val vector2D = Reference(VaultPackages.paradigm.vector2D, "Vector2D")
		lazy val angle = Reference(angular, "Angle")
	}
	
	object Terra
	{
		import VaultPackages.Terra._
		
		lazy val latLong = Reference(angularModels, "LatLong")
	}
}
