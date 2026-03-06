package utopia.echo.model.vastai.instance

import utopia.echo.model.unit.ByteCount
import utopia.echo.model.unit.ByteCountExtensions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration}
import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.model.mutable.DataType.{DoubleType, IntType, StringType}

object Cpu extends FromModelFactoryWithSchema[Cpu]
{
	// ATTRIBUTES   ----------------------
	
	override val schema: ModelDeclaration = ModelDeclaration(
		"cpu_arch" -> StringType, "cpu_cores_effective" -> DoubleType, "cpu_ram" -> IntType)
	
	
	// IMPLEMENTED  ----------------------
	
	override protected def fromValidatedModel(model: Model): Cpu = apply(model("cpu_arch"), model("cpu_name"),
		model("cpu_ram").getInt.mb, model("cpu_cores_effective"), model("cpu_cores"))
}

/**
 * Contains information about a machine's CPU
 * @param architecture Name of this CPU's manufacturer / architecture (e.g. "amd64")
 * @param name Name of this CPU model. May be empty.
 * @param ram Amount of available RAM
 * @param effectiveCoreCount Number of CPU cores offered / available for utilization
 * @param coreCount Total number of CPU cores, if known
 * @author Mikko Hilpinen
 * @since 25.02.2026, v1.5
 */
case class Cpu(architecture: String, name: String, ram: ByteCount, effectiveCoreCount: Double,
               coreCount: Option[Int] = None)
