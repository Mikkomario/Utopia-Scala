package utopia.echo.model.vastai.instance.offer

import utopia.echo.model.enumeration.NetworkTrafficDirection
import utopia.echo.model.vastai.instance.{Cpu, Gpu}
import VerificationStatus.Unverified
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.immutable.range.Span
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.ModelDeclaration
import utopia.flow.generic.model.mutable.DataType.{DoubleType, LongType, StringType}
import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties
import utopia.flow.time.Duration
import utopia.flow.time.TimeExtensions._
import utopia.flow.generic.casting.ValueUnwraps._

import java.time.Instant
import scala.util.Try

object Offer extends FromModelFactory[Offer]
{
	// ATTRIBUTES   ----------------------
	
	private val schema = ModelDeclaration("ask_contract_id" -> LongType, "machine_id" -> LongType, "host_id" -> LongType,
		"reliability" -> DoubleType, "score" -> DoubleType, "geolocation" -> StringType,
		"duration" -> DoubleType, "end_date" -> LongType, "start_date" -> LongType, "verification" -> StringType)
	
	
	// IMPLEMENTED  ----------------------
	
	override def apply(model: HasProperties): Try[Offer] = schema.validate(model).flatMap { model =>
		MachineCost(model).flatMap { cost =>
			Cpu(model).flatMap { cpu =>
				Gpu(model).flatMap { gpu =>
					MachinePerformance(model).flatMap { performance =>
						NetworkTrafficDirection.values.tryMapAll { dir => NetworkStats(dir)(model).map { dir -> _ } }
							.flatMap { network =>
								model.tryGet("verification") { v => VerificationStatus.forKey(v.getString) }
									.flatMap { verification =>
										Try { Pair("start_date", "end_date").map { key => Instant.ofEpochSecond(model(key)) } }
											.map { timespan =>
												apply(model("ask_contract_id"), model("machine_id"), model("host_id"),
													cost, cpu, gpu, network.toMap, performance, model("reliability"),
													model("score"), model("geolocation"),
													model("duration").getDouble.seconds, Span(timespan), verification,
													model("bundle_id"), model("rentable").booleanOr(true),
													model("rented"), model("static_ip"))
											}
									}
							}
					}
				}
			}
		}
	}
}

/**
 * Represents an offer for renting a machine
 * @param id ID of this offer
 * @param machineId ID of this machine
 * @param hostId ID of the machine's host
 * @param cost Information about this offer's costs
 * @param cpu Information about the CPU
 * @param gpu Information about the GPU(s)
 * @param network Information about network usage
 * @param performance Information about this machine's performance
 * @param reliability A reliability score (0,1)
 * @param score Score given to this offer by Vast AI
 * @param location This machine's location, as a String
 * @param duration Duration how long this offer is in effect
 * @param timespan Timespan of this offer
 * @param verification This offer's verification status
 * @param rentable Whether the machine is currently rentable
 * @param rented Whether you've already rented this machine
 * @param hasStaticIp Whether this machine has a static IP address
 * @author Mikko Hilpinen
 * @since 24.02.2026, v1.5
 */
case class Offer(id: Long, machineId: Int, hostId: Long, cost: MachineCost, cpu: Cpu, gpu: Gpu,
                 network: Map[NetworkTrafficDirection, NetworkStats],
                 performance: MachinePerformance, reliability: Double, score: Double, location: String,
                 duration: Duration, timespan: Span[Instant],
                 verification: VerificationStatus = Unverified, bundleId: Option[Int] = None, rentable: Boolean = true,
                 rented: Boolean = false, hasStaticIp: Boolean = false)
