package utopia.echo.model.vastai.instance

import utopia.echo.model.unit.ByteCount
import utopia.echo.model.unit.ByteCountExtensions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration}
import utopia.flow.generic.model.mutable.DataType.IntType

object Disk extends FromModelFactoryWithSchema[Disk]
{
	// ATTRIBUTES   ---------------------
	
	override val schema: ModelDeclaration = ModelDeclaration("disk_bw" -> IntType, "disk_space" -> IntType)
	
	
	// IMPLEMENTED  ---------------------
	
	override protected def fromValidatedModel(model: Model): Disk =
		apply(model("disk_bw").getInt.mb, model("disk_space").getInt.gb, model("disk_name").getString)
}

/**
 * Contains information about the disk used
 * @param bandwidthPerSecond Disk read bandwidth /s
 * @param space Total disk space available
 * @param name Disk model name, if known
 * @author Mikko Hilpinen
 * @since 02.03.2026, v1.5
 */
case class Disk(bandwidthPerSecond: ByteCount, space: ByteCount, name: String = "")
