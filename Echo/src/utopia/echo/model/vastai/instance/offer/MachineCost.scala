package utopia.echo.model.vastai.instance.offer

import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration}
import utopia.flow.generic.model.mutable.DataType.DoubleType
import utopia.flow.generic.casting.ValueUnwraps._

object MachineCost extends FromModelFactoryWithSchema[MachineCost]
{
	// ATTRIBUTES   --------------------
	
	override val schema: ModelDeclaration = ModelDeclaration("dph_base" -> DoubleType, "dph_total" -> DoubleType,
		"storage_cost" -> DoubleType, "vram_costperhour" -> DoubleType)
	
	
	// IMPLEMENTED  -------------------
	
	override protected def fromValidatedModel(model: Model): MachineCost =
		apply(model("dph_total"), model("dph_base"), model("storage_cost"), model("vram_costperhour"),
			model("min_bid"), model("is_bid"))
}

/**
 * Contains information about a machine's rental cost, in $/h (unless otherwise stated)
 * @param total Total cost in $/h. Already includes:
 *                  - Base cost
 *                  - Storage cost
 *                  - VRAM cost
 * @param base Base cost in $/h
 * @param storagePerGbPerMonth Storage cost in $/GB/month
 * @param vramPerMega VRAM cost in $/MB/h
 * @param minBid Minimum bid ($/h) that is required to take this device
 * @param isBid Whether this device is open for bidding
 * @author Mikko Hilpinen
 * @since 24.02.2026, v1.4.1
 */
case class MachineCost(total: Double, base: Double, storagePerGbPerMonth: Double, vramPerMega: Double,
                       minBid: Double, isBid: Boolean)
