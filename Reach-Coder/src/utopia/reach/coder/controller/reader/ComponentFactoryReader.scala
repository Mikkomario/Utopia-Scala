package utopia.reach.coder.controller.reader

import utopia.bunnymunch.jawn.JsonBunny
import utopia.coder.model.data.ProjectSetup
import utopia.coder.model.scala.Package
import utopia.flow.collection.mutable.builder.{CompoundingMapBuilder, CompoundingVectorBuilder}
import utopia.flow.generic.model.immutable.Model
import utopia.flow.util.Version

import java.nio.file.Path

/**
  * Used for reading component factory data from json
  * @author Mikko Hilpinen
  * @since 19.5.2023, v1.0
  */
object ComponentFactoryReader
{
	def apply(path: Path) = JsonBunny(path).map { rootV =>
		val root = rootV.getModel
		// Parses standard project information
		val projectName = root("project").stringOr("Project")
		val version = root("version").string.flatMap { Version.findFrom }
		val author = root("author").getString
		val basePackage = Package(root("package").getString)
		// Parses package aliases
		
		???
	}
	
	private def packageAliasesFrom(aliases: Model) = {
		val builder = new CompoundingMapBuilder[String, Package]()
		/*
		aliases.properties.foreach { prop =>
			val pck = prop.value.getString
			if (pck.contains('/'))
			
		}
		 */
	}
}
