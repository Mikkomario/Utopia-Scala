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
		"cpu_arch" -> StringType, "cpu_cores" -> IntType, "cpu_cores_effective" -> DoubleType, "cpu_name" -> StringType,
		"cpu_ram" -> IntType)
	
	
	// IMPLEMENTED  ----------------------
	
	override protected def fromValidatedModel(model: Model): Cpu = apply(model("cpu_arch"), model("cpu_name"),
		model("cpu_cores"), model("cpu_cores_effective"), model("cpu_ram").getInt.mb)
}

/**
 * Contains information about a machine's CPU
 * @param architecture Name of this CPU's manufacturer / architecture (e.g. "amd64")
 * @param name Name of this CPU model
 * @param coreCount Total number of CPU cores
 * @param effectiveCoreCount Number of CPU cores offered / available for utilization
 * @param ram Amount of available RAM
 * @author Mikko Hilpinen
 * @since 25.02.2026, v1.5
 */
case class Cpu(architecture: String, name: String, coreCount: Int, effectiveCoreCount: Double, ram: ByteCount)
