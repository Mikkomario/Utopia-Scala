package utopia.echo.model.vastai.instance

import utopia.echo.model.enumeration.NetworkTrafficDirection
import utopia.echo.model.enumeration.NetworkTrafficDirection.{Download, Upload}
import utopia.echo.model.vastai.instance.offer.{MachineCost, MachinePerformance, NetworkStats}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.ModelDeclaration
import utopia.flow.generic.model.mutable.DataType.{IntType, LongType, StringType}
import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._

import java.time.Instant
import scala.util.Try

object BasicInstanceInfo extends FromModelFactory[BasicInstanceInfo]
{
	// ATTRIBUTES   ----------------------
	
	private val schema = ModelDeclaration("machine_id" -> IntType, "host_id" -> IntType,
		"geolocation" -> StringType, "end_date" -> LongType)
	
	
	// IMPLEMENTED  ----------------------
	
	override def apply(model: HasProperties): Try[BasicInstanceInfo] = schema.validate(model).flatMap { model =>
		MachineCost(model).flatMap { cost =>
			Cpu(model).flatMap { cpu =>
				Gpu(model).flatMap { gpu =>
					Disk(model).flatMap { disk =>
						MachinePerformance(model).flatMap { performance =>
							NetworkTrafficDirection.values
								.tryMapAll { dir => NetworkStats(dir)(model).map { dir -> _ } }
								.flatMap { network =>
									model.tryGet("end_date") { v => Try { Instant.ofEpochSecond(v.getLong) } }
										.map { ends =>
											val started = model("start_date").long.flatMap { start =>
												Try { Instant.ofEpochSecond(start) }.toOption
											}
											apply(model("machine_id"), model("host_id"), cost, cpu, gpu, disk,
												network.toMap, performance, model("geolocation"),
												ends, started, model("static_ip"))
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
 * Contains basic information about a rentable machine
 * @param machineId ID of this machine
 * @param hostId ID of the machine's host
 * @param cost Information about this offer's costs
 * @param cpu Information about the CPU
 * @param gpu Information about the GPU(s)
 * @param disk Information about the disk
 * @param network Information about network usage
 * @param performance Information about this machine's performance
 * @param location This machine's location, as a String
 * @param ends Time when this offer expires / ends
 * @param started Time when this offer started or was published, if known
 * @param hasStaticIp Whether this machine has a static IP address
 * @author Mikko Hilpinen
 * @since 24.02.2026, v1.5
 */
case class BasicInstanceInfo(machineId: Int, hostId: Int, cost: MachineCost, cpu: Cpu, gpu: Gpu, disk: Disk,
                             network: Map[NetworkTrafficDirection, NetworkStats],
                             performance: MachinePerformance, location: String,
                             ends: Instant, started: Option[Instant] = None,
                             hasStaticIp: Boolean = false)
{
	def download = network(Download)
	def upload = network(Upload)
	
	/**
	 * @return How long this instance or offer will remain available
	 */
	def duration = ends - Now
}