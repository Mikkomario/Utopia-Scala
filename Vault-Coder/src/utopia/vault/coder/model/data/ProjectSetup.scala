package utopia.vault.coder.model.data

import utopia.vault.coder.model.scala.Package

import java.nio.file.Path

/**
  * Represents project specific settings used when writing documents
  * @author Mikko Hilpinen
  * @since 1.9.2021, v0.1
  * @param projectPackage Package that is common to all files in the target project
  * @param sourceRoot Path to the export source directory
  */
case class ProjectSetup(projectPackage: Package, sourceRoot: Path)
{
	/**
	  * Package that contains all standard project models
	  */
	lazy val modelPackage = projectPackage/"model"
	/**
	  * Package that contains all project database interactions
	  */
	lazy val databasePackage = projectPackage/"database"
	
	/**
	  * @return Package that contains database access points
	  */
	def accessPackage = databasePackage/"access"
	/**
	  * @return Package that contains from database read factories
	  */
	def factoryPackage = databasePackage/"factory"
	/**
	  * @return Package that contains database interaction models
	  */
	def dbModelPackage = databasePackage/"model"
}
