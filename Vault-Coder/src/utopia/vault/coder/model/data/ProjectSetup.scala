package utopia.vault.coder.model.data

import utopia.vault.coder.model.scala.Package

import java.nio.file.Path

/**
  * Represents project specific settings used when writing documents
  * @author Mikko Hilpinen
  * @since 1.9.2021, v0.1
  * @param dbModuleName Name of this project (the database portion)
  * @param modelPackage Package for the model and enum classes
  * @param databasePackage Package for the database interaction classes
  * @param sourceRoot Path to the export source directory
  * @param modelCanReferToDB Whether model classes are allowed to refer to database classes
  */
case class ProjectSetup(dbModuleName: String, modelPackage: Package, databasePackage: Package, sourceRoot: Path,
                        modelCanReferToDB: Boolean)
{
	/**
	  * @return Package that contains database access points
	  */
	lazy val accessPackage = databasePackage/"access"
	
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
