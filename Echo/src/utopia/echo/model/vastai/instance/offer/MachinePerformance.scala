package utopia.echo.model.vastai.instance.offer

import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration}
import utopia.flow.generic.model.mutable.DataType.DoubleType
import utopia.flow.generic.casting.ValueUnwraps._

object MachinePerformance extends FromModelFactoryWithSchema[MachinePerformance]
{
	override val schema: ModelDeclaration =
		ModelDeclaration("dlperf" -> DoubleType, "dlperf_per_dphtotal" -> DoubleType,
			"flops_per_dphtotal" -> DoubleType, "total_flops" -> DoubleType)
	
	override protected def fromValidatedModel(model: Model): MachinePerformance =
		apply(model("dlperf"), model("dlperf_per_dphtotal"), model("total_flops"), model("flops_per_dphtotal"))
}

/**
 * Contains information about a machine's performance
 * @param deepLearning Deep Learning performance score
 * @param deepLearningPerDph DLPerf per dollar per hour
 * @param totalFlops Total theoretical GPU compute performance (TFLOPs) across all GPUs.
 * @param flopsPerDph TFLOPs per $/hour
 * @author Mikko Hilpinen
 * @since 24.02.2026, v1.5
 */
case class MachinePerformance(deepLearning: Double, deepLearningPerDph: Double,
                              totalFlops: Double, flopsPerDph: Double)
