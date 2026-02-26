package utopia.echo.model.vastai.instance

import utopia.echo.model.unit.ByteCount
import utopia.echo.model.vastai.instance.offer.Offer
import utopia.echo.model.unit.ByteCountExtensions._
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.flow.collection.immutable.range.NumericSpan
import utopia.flow.collection.immutable.range.NumericSpan.IntSpan
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.ModelDeclaration
import utopia.flow.generic.model.mutable.DataType.DoubleType
import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties
import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.parse.string.Regex
import utopia.flow.time.TimeExtensions._
import utopia.flow.time.{Duration, Now}
import utopia.flow.util.StringExtensions._
import utopia.flow.view.template.Extender

import java.time.Instant
import scala.util.Try

object VastAiInstance extends FromModelFactory[VastAiInstance]
{
	// ATTRIBUTES   ----------------------
	
	private val schema = ModelDeclaration("client_run_time" -> DoubleType, "host_run_time" -> DoubleType)
	
	
	// IMPLEMENTED  ----------------------
	
	override def apply(model: HasProperties): Try[VastAiInstance] = schema.validate(model).flatMap { model =>
		Offer(model).flatMap { offer =>
			InstanceStatus(model).map { status =>
				val template = model("template_id").int.map { templateId =>
					TemplateIdentifier(templateId, model("template_hash_id"), model("template_name"))
				}
				val directPortRange = model("direct_port_start").int.filter { _ > 0 }.flatMap { start =>
					model("direct_port_end").int.filter { _ >= start }.map { NumericSpan(start, _) }
				}
				val ssh = model("ssh_host").string.flatMap { host =>
					model("ssh_port").int.map { port =>
						SshConnection(host, port, model("machine_dir_ssh_port").intOr(-1), model("ssh_idx"))
					}
				}
				
				apply(model("id"), offer, status, model("client_run_time").getDouble.hours,
					model("host_run_time").getDouble.hours, model("credit_balance"),
					model("uptime_mins").double.map { _.minutes }, model("label"), template, model("cpu_util"),
					model("mem_limit").int.map { _.mb }, model("mem_usage").int.map { _.mb }, model("gpu_util"),
					model("vmem_usage").int.map { _.mb }, model("gpu_temp"), model("disk_usage").double.map { _ / 100 },
					model("local_ipaddrs").getString.splitIterator(Regex.comma).map { _.stripControlCharacters.trim }
						.filter { _.nonEmpty }.toOptimizedSeq,
					model("public_ipaddr"), directPortRange, model("ports").getVector.map { _.getInt }, ssh,
					model("jupyter_token"))
			}
		}
	}
}

/**
 * Represents a rented machine / accepted contract
 * @param id ID of this instance
 * @param offer Information about this instance, which is also present in its offer
 * @param status Information about this instance's current status
 * @param clientRunTime How long this client has been on
 * @param hostRunTime How long this host has been active
 * @param creditBalance User's credit balance in $, if available
 * @param uptime Up-time for this instance
 * @param label Custom label/name given to this instance
 * @param template Information about the template used to create this instance
 * @param cpuUtilization Ratio of CPU resources currently utilized (0,1)
 * @param ramLimit RAM usage limit, if known & applicable
 * @param ramUsage Currently used RAM, if known
 * @param gpuUtilization Ratio of available GPU resources currently utilized (0,1), if known
 * @param vramUsage VRAM currently used, if known
 * @param gpuTempCelsius Current temperature of the GPU, if known
 * @param diskUsageRatio Ratio of the available disk space used (0,1), if known
 * @param localIpAddresses Local IP addresses for this instance
 * @param publicIpAddress Public IP address of this instance
 * @param directPortRange Range of direct port numbers available. None if no ports are available or known.
 * @param otherPorts Other ports that are available
 * @param ssh Information for forming an SSH connection, if applicable
 * @param jupyterToken Token for Jupyter, if applicable
 * @param timestamp Timestamp of when this instance state was acquired. Default = now.
 * @author Mikko Hilpinen
 * @since 24.02.2026, v1.5
 */
case class VastAiInstance(id: Int, offer: Offer, status: InstanceStatus,
                          clientRunTime: Duration, hostRunTime: Duration, creditBalance: Option[Double] = None,
                          uptime: Option[Duration] = None, label: String = "",
                          template: Option[TemplateIdentifier] = None,
                          cpuUtilization: Double = 0.0, ramLimit: Option[ByteCount] = None,
                          ramUsage: Option[ByteCount] = None, gpuUtilization: Option[Double] = None,
                          vramUsage: Option[ByteCount] = None, gpuTempCelsius: Option[Double] = None,
                          diskUsageRatio: Option[Double] = None,
                          localIpAddresses: Seq[String] = Empty, publicIpAddress: String = "",
                          directPortRange: Option[IntSpan] = None, otherPorts: Seq[Int] = Empty,
                          ssh: Option[SshConnection] = None, jupyterToken: String = "", timestamp: Instant = Now)
	extends Extender[Offer]
{
	override def wrapped: Offer = offer
}
