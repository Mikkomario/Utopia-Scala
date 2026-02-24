package utopia.echo.model.vastai.offer

import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration}
import utopia.flow.generic.model.mutable.DataType.{DoubleType, IntType}

object Gpu extends FromModelFactoryWithSchema[Gpu]
{
	// ATTRIBUTES   ---------------------
	
	override val schema: ModelDeclaration = ModelDeclaration(
		"compute_cap" -> IntType, "cuda_max_good" -> DoubleType, "gpu_ram" -> IntType, "gpu_total_ram" -> IntType,
		"gpu_max_power" -> IntType, "gpu_max_temp" -> IntType)
	
	
	// IMPLEMENTED  --------------------
	
	override protected def fromValidatedModel(model: Model): Gpu =
		apply(model("gpu_arch"), model("gpu_name"), model("driver_version"),
			model("compute_cap").getDouble / 100, model("cuda_max_good"), model("gpu_ram"), model("gpu_total_ram"),
			model("gpu_max_power"), model("gpu_max_temp"), model("num_gpus").intOr(1), model("gpu_frac").doubleOr(1.0),
			model("gpu_display_active"))
}

/**
 * Contains information about a rentable GPU
 * @param architecture GPU architecture (e.g. "nvidia" or "amd")
 * @param name GPU model name (e.g. "Titan Xp")
 * @param nvidiaDriverVersion NVIDIA driver version in the format "XXX.XX.XX"
 * @param computeCapability CUDA compute capability.
 * @param cudaMaxVersion Highest supported CUDA version, as a Double
 * @param ramMegas Amount of VRAM available, in megabytes
 * @param ramTotalMegas Amount of VRAM available accross all GPUs, in megabytes
 * @param maxPowerWatts GPU power limit in watts
 * @param maxTemperatureCelcius GPU temperature limit in Celsius
 * @param count Number of available / rented GPUs
 * @param availableFraction Fraction of the total GPU resources being offered
 * @param hasActiveDisplay Whether the GPU has an attached display
 * @author Mikko Hilpinen
 * @since 24.02.2026, v1.4.1
 */
case class Gpu(architecture: String, name: String, nvidiaDriverVersion: String, computeCapability: Double,
               cudaMaxVersion: Double, ramMegas: Int, ramTotalMegas: Int,
               maxPowerWatts: Int, maxTemperatureCelcius: Int, count: Int,
               availableFraction: Double, hasActiveDisplay: Boolean)
