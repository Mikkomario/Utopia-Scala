package utopia.vault.coder.util

import utopia.coder.model.scala.Package._

/**
  * Access point to various packages used in this project
  * @author Mikko Hilpinen
  * @since 19.5.2023, v1.9.1
  */
object VaultPackages
{
	// COMPUTED -------------------------
	
	def vault = Vault
	def bunnyMunch = BunnyMunch
	def metropolis = Metropolis
	def citadel = Citadel
	def paradigm = Paradigm
	def terra = Terra
	
	
	// NESTED   -------------------------
	
	object Vault
	{
		val base = utopia / "vault"
		
		lazy val database = base / "database"
		lazy val models = base / "model"
		lazy val sql = base / "sql"
		lazy val noSql = base / "nosql"
		lazy val deprecation = noSql / "storable.deprecation"
		
		lazy val factories = noSql / "factory"
		lazy val fromRowFactories = factories / ".row"
		lazy val singleLinkedFactories = fromRowFactories / "linked"
		
		lazy val access = noSql / "access"
		lazy val viewAccess = noSql / "view"
		lazy val singleModelAccess = access / "single.model"
		lazy val manyModelAccess = access / "many.model"
		lazy val modelTemplateAccess = access/"template.model"
	}
	object BunnyMunch
	{
		val base = utopia/"bunnymunch"
		
		lazy val jawn = base/"jawn"
	}
	object Metropolis
	{
		val base = utopia / "metropolis"
		
		lazy val models = base / "model"
		lazy val description = models / "stored.description"
		lazy val combinedDescription = models / "combined.description"
	}
	object Citadel
	{
		val base = utopia / "citadel"
		
		lazy val cachedModel = base/"model.cached"
		lazy val database = base / "database"
		lazy val descriptionFactories = database/"factory.description"
		lazy val descriptionModels = database/"model.description"
		lazy val access = database / "access"
		lazy val descriptionsAccess = access / "many.description"
		lazy val descriptionAccess = access / "single.description"
	}
	object Paradigm
	{
		val base = utopia/"paradigm"
		
		lazy val generic = base/"generic"
		lazy val measurement = base/"measurement"
		lazy val vector2D = base/"shape.shape2d.vector"
		lazy val angular = base/"angular"
	}
	object Terra
	{
		val base = utopia/"terra"
		
		lazy val angularModels = base/"model.angular"
	}
}
