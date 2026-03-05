package utopia.echo.model.vastai.instance.offer

import utopia.echo.model.vastai.instance.BasicInstanceInfo
import utopia.echo.model.vastai.instance.offer.VerificationStatus.Unverified
import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.ModelDeclaration
import utopia.flow.generic.model.mutable.DataType.{DoubleType, LongType, StringType}
import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties
import utopia.flow.view.template.Extender

import scala.util.Try

object Offer extends FromModelFactory[Offer]
{
	// ATTRIBUTES   ----------------------
	
	private val schema = ModelDeclaration("ask_contract_id" -> LongType, "reliability" -> DoubleType,
		"score" -> DoubleType, "verification" -> StringType)
	
	
	// IMPLEMENTED  ----------------------
	
	override def apply(model: HasProperties): Try[Offer] = schema.validate(model).flatMap { model =>
		BasicInstanceInfo(model).flatMap { info =>
			GpuLimits(model).flatMap { gpuLimits =>
				model.tryGet("verification") { v => VerificationStatus.forKey(v.getString) }.map { verification =>
					apply(model("ask_contract_id"), info, gpuLimits, model("reliability"), model("score"), verification,
						model("bundle_id"), model("rentable").booleanOr(true), model("rented"))
				}
			}
		}
	}
}

/**
 * Represents an offer for renting a machine
 * @param id ID of this offer
 * @param reliability A reliability score (0,1)
 * @param score Score given to this offer by Vast AI
 * @param verification This offer's verification status
 * @param rentable Whether the machine is currently rentable
 * @param rented Whether you've already rented this machine
 * @author Mikko Hilpinen
 * @since 24.02.2026, v1.5
 */
case class Offer(id: Long, details: BasicInstanceInfo, gpuLimits: GpuLimits, reliability: Double, score: Double,
                 verification: VerificationStatus = Unverified, bundleId: Option[Int] = None, rentable: Boolean = true,
                 rented: Boolean = false)
	extends Extender[BasicInstanceInfo]
{
	override def wrapped: BasicInstanceInfo = details
}