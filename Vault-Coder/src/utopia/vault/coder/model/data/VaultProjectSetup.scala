package utopia.vault.coder.model.data

import utopia.coder.model.data.{Name, ProjectSetup}
import utopia.coder.model.scala.Package
import utopia.flow.util.Version

import java.nio.file.Path

/**
  * Represents project specific settings used when writing documents
  * @author Mikko Hilpinen
  * @since 1.9.2021, v0.1
  * @param dbModuleName Name of this project (the database portion)
  * @param modelPackage Package for the model and enum classes
  * @param databasePackage Package for the database interaction classes
  * @param sourceRoot Path to the export source directory
  * @param mergeSourceRoots Paths to the source roots where existing versions are read from and merged (optional)
  * @param version Current project version
  * @param modelCanReferToDB Whether model classes are allowed to refer to database classes
  * @param prefixSqlProperties Whether a prefix should be added to sql properties, making them unique
  */
case class VaultProjectSetup(dbModuleName: Name, modelPackage: Package, databasePackage: Package, sourceRoot: Path,
                             mergeSourceRoots: Vector[Path], mergeConflictsFilePath: Path,
                             version: Option[Version], modelCanReferToDB: Boolean, prefixSqlProperties: Boolean)
	extends ProjectSetup
{
	// ATTRIBUTES   ------------------------
	
	/**
	  * @return Package that contains database access points
	  */
	lazy val accessPackage = databasePackage/"access"
	
	
	// COMPUTED ---------------------------
	
	/**
	  * @return Package that contains combined models
	  */
	def combinedModelPackage = modelPackage/"combined"
	
	/**
	  * @return Package that contains database access points that retrieve individual items
	  */
	def singleAccessPackage = accessPackage/"single"
	/**
	  * @return Package that contains database access points that retrieve multiple items at once
	  */
	def manyAccessPackage = accessPackage/"many"
	
	/**
	  * @return Package that contains from database read factories
	  */
	def factoryPackage = databasePackage/"factory"
	/**
	  * @return Package that contains database interaction models
	  */
	def dbModelPackage = databasePackage/"model"
}
