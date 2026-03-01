package utopia.echo.model.vastai.instance

import utopia.echo.model.unit.ByteCount
import utopia.echo.model.unit.ByteCountExtensions._
import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration}
import utopia.flow.generic.model.mutable.DataType.{DoubleType, IntType}

object Gpu extends FromModelFactoryWithSchema[Gpu]
{
	// ATTRIBUTES   ---------------------
	
	override val schema: ModelDeclaration = ModelDeclaration(
		"compute_cap" -> IntType, "cuda_max_good" -> DoubleType, "gpu_ram" -> IntType)
	
	
	// IMPLEMENTED  --------------------
	
	override protected def fromValidatedModel(model: Model): Gpu =
		apply(model("gpu_arch"), model("gpu_name"), model("driver_version"),
			model("compute_cap").getDouble / 100, model("cuda_max_good"), model("gpu_ram").getInt.mb,
			model("gpu_display_active"))
}

/**
 * Contains information about a rentable GPU
 * @param architecture GPU architecture (e.g. "nvidia" or "amd")
 * @param name GPU model name (e.g. "Titan Xp")
 * @param nvidiaDriverVersion NVIDIA driver version in the format "XXX.XX.XX"
 * @param computeCapability CUDA compute capability.
 * @param cudaMaxVersion Highest supported CUDA version, as a Double
 * @param ram Amount of VRAM available
 * @param hasActiveDisplay Whether the GPU has an attached display
 * @author Mikko Hilpinen
 * @since 24.02.2026, v1.5
 */
case class Gpu(architecture: String, name: String, nvidiaDriverVersion: String, computeCapability: Double,
               cudaMaxVersion: Double, ram: ByteCount, hasActiveDisplay: Boolean)
{
	override def toString = s"$name ${ ram.gigas.round } GB"
}