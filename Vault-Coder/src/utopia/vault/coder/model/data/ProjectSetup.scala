package utopia.vault.coder.model.data

import utopia.flow.util.CollectionExtensions._
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.util.StringExtensions._
import utopia.flow.util.Version
import utopia.vault.coder.model.merging.MergeConflict
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
  * @param mergeSourceRoots Paths to the source roots where existing versions are read from and merged (optional)
  * @param version Current project version
  * @param modelCanReferToDB Whether model classes are allowed to refer to database classes
  * @param prefixSqlProperties Whether a prefix should be added to sql properties, making them unique
  */
case class ProjectSetup(dbModuleName: Name, modelPackage: Package, databasePackage: Package, sourceRoot: Path,
                        mergeSourceRoots: Vector[Path], mergeConflictsFilePath: Path,
                        version: Option[Version], modelCanReferToDB: Boolean, prefixSqlProperties: Boolean)
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
	
	
	// OTHER    -------------------------
	
	/**
	  * Records conflicts to a local file
	  * @param conflicts Merge conflicts to record
	  * @param header Header to assign for these conflicts (call-by-name)
	  */
	def recordConflicts(conflicts: Vector[MergeConflict], header: => String) =
	{
		if (conflicts.nonEmpty)
		{
			mergeConflictsFilePath.createParentDirectories().flatMap { path =>
				path.appendLines(Vector("", "", s"// $header ${"-" * 10}") ++ conflicts.flatMap { conflict =>
					if (conflict.readVersion.nonEmpty || conflict.generatedVersion.nonEmpty)
						("" +: conflict.description.notEmpty.map { "// " + _ }.toVector) ++
							("// Old Version" +: conflict.readVersion.map { _.toString }) ++
							("// New Version" +: conflict.generatedVersion.map { _.toString })
					else
						Vector(s"// ${conflict.description}")
				})
			}.failure.foreach { error =>
				println(s"Failed to write conflicts document due to an error: ${error.getMessage}")
			}
		}
	}
}
