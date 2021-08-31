package utopia.citadel.coder.model.scala

import scala.collection.immutable.VectorBuilder

/**
  * Represents a scala file
  * @author Mikko Hilpinen
  * @since 31.8.2021, v0.1
  */
case class File(packagePath: String, classes: Vector[ClassDeclaration] = Vector(),
                objects: Vector[ObjectDeclaration] = Vector())
	extends CodeConvertible
{
	/**
	  * @return All (top level) declarations in this file
	  */
	def declarations = objects ++ classes
	
	override def toCodeLines =
	{
		val builder = new VectorBuilder[String]()
		
		// Writes the package declaration
		builder += s"package $packagePath"
		builder += ""
		
		// Writes the imports
		val imports = declarations.flatMap { _.references }.toSet.groupMap[String, String] { _.parentPath } { _.target }
		imports.keys.toVector.sorted.foreach { path =>
			val targets = imports(path)
			val finalPart =
			{
				if (targets.contains("_"))
					"_"
				else if (targets.size == 1)
					targets.head
				else
					builder += s"{ ${targets.toVector.sorted.mkString(", ") } }"
			}
			builder += s"import $path.$finalPart"
		}
		if (imports.nonEmpty)
			builder += ""
		
		// Writes the objects, then classes
		declarations.foreach { declaration =>
			builder ++= declaration.toCodeLines
			builder += ""
		}
		
		builder.result()
	}
}
