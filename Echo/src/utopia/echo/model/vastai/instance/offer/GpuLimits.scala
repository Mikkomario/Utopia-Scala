package utopia.echo.model.vastai.instance.offer

import utopia.echo.model.unit.ByteCount
import utopia.echo.model.unit.ByteCountExtensions._
import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration}
import utopia.flow.generic.model.mutable.DataType.IntType

object GpuLimits extends FromModelFactoryWithSchema[GpuLimits]
{
	// ATTRIBUTES   ----------------------
	
	override val schema: ModelDeclaration = ModelDeclaration(
		"gpu_total_ram" -> IntType, "gpu_max_power" -> IntType, "gpu_max_temp" -> IntType)
	
	
	// IMPLEMENTED  ----------------------
	
	override protected def fromValidatedModel(model: Model): GpuLimits =
		apply(model("gpu_total_ram").getInt.mb, model("gpu_max_power"), model("gpu_max_temp"), model("num_gpus").intOr(1),
			model("gpu_frac").doubleOr(1.0))
}

/**
 * Contains information about GPU limits that's only present in the offer
 * @param ramTotal Amount of VRAM available across all GPUs
 * @param maxPowerWatts GPU power limit in watts
 * @param maxTemperatureCelsius GPU temperature limit in Celsius
 * @param count Number of available / rented GPUs
 * @param availableFraction Fraction of the total GPU resources being offered
 * @author Mikko Hilpinen
 * @since 01.03.2026, v1.5
 */
case class GpuLimits(ramTotal: ByteCount, maxPowerWatts: Int, maxTemperatureCelsius: Int, count: Int,
                     availableFraction: Double)
