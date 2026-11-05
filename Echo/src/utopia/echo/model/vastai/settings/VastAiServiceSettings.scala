package utopia.echo.model.vastai.settings

import utopia.echo.controller.vastai.InstanceReuseLogic
import utopia.echo.controller.vastai.InstanceReuseLogic.NeverReuse
import utopia.echo.model.vastai.instance.VastAiInstance
import utopia.echo.model.vastai.settings.VastAiServiceSettings.VastAiServiceSettingsFactory
import utopia.flow.collection.immutable.Pair
import utopia.flow.parse.file.KeptOpenWriter
import utopia.flow.time.Duration
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.Mutate
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy

import java.nio.file.Path

object VastAiServiceSettings
{
	// ATTRIBUTES   ---------------------
	
	/**
	 * Settings applied by default
	 */
	lazy val default = apply()
	
	
	// NESTED   -------------------------
	
	/**
	 * Common trait for factory-like interfaces that are used in Vast AI service construction
	 * @tparam Repr Type of the constructed copies
	 */
	trait VastAiServiceSettingsFactory[+Repr]
	{
		// ABSTRACT ---------------------
		
		/**
		 * @return A copy of this instance with port forwarding disabled
		 */
		def withoutPortForwarding: Repr
		
		/**
		 * @param reuseLogic Logic that may allow reusing already rented Vast AI instances.
		 * @return A copy of this instance applying the specified instance-reuse logic.
		 */
		def reusingInstancesWith(reuseLogic: InstanceReuseLogic): Repr
		/**
		 * @param shouldDestroy A function that determines whether a live instance should be destroyed.
		 *                      Should yield false in situations where the instance should only be stopped instead.
		 *                      Notice that stopped instances still incur costs.
		 *
		 *                      Receives three parameters:
		 *                      1. Last state of the Vast AI instance in question
		 *                      1. How long it's been since the instance started
		 *                      1. Whether the service was successfully hosted. False if the service was not hosted yet.
		 *
		 *                      Never called in situations where the instance is unstable or disconnected,
		 *                      or where the service became unresponsive (indicating a low-quality instance).
		 *
		 * @return A copy of this instance applying the specified instance-destruction logic
		 */
		def withDestructionLogic(shouldDestroy: (VastAiInstance, Duration, Boolean) => Boolean): Repr
		
		/**
		 * @param scriptPath Path to a script file that installs the service when run on the rented device.
		 *                   Note: Installation may be skipped, depending on how the instance / offer is selected.
		 * @return A copy of this instance using the specified installation script when appropriate.
		 */
		def runningInstallScript(scriptPath: Path): Repr
		/**
		 * Enables port forwarding
		 * @param portsView A view to the ports to use, where the first value represents the local port
		 *                  (where the service will be accessed/used)
		 *                  and the second value matches the remote (internal) port,
		 *                  where it is hosted on the remote server.
		 * @return A copy of this instance applying port forwarding over the specified ports.
		 */
		def withPortForwarding(portsView: View[Pair[Int]]): Repr
		
		/**
		 * @param timeout Timeout for the setup process, i.e. the process after the instance has been loaded,
		 *                during which the service should become usable.
		 *
		 *                If the service doesn't become usable before this timeout, the instance is destroyed.
		 * @return A copy of this instance applying the specified setup timeout
		 */
		def withSetupTimeout(timeout: Duration): Repr
		/**
		 * @param timeout Timeout for recovering from SSH and/or service failures.
		 *                During this time period, the service and port forwarding (if applicable) are restarted and
		 *                reconnected to using the normal setup procedure.
		 * @return A copy of this instance applying the specified recovery timeout.
		 */
		def withRecoveryTimeout(timeout: Duration): Repr
		
		/**
		 * @param interval Interval between instance status updates.
		 * @return A copy of this instance applying the specified status-check interval.
		 */
		def updatingStatusEvery(interval: Duration): Repr
		
		/**
		 * @param label Custom label to give to the rented Vast AI instance(s)
		 * @return A copy of this instance using the specified label.
		 */
		def withLabel(label: String): Repr
		
		/**
		 * Enables debug logging
		 * @param writer Interface for making debug log entries. None if debug logging should be disabled.
		 * @return Copy of this instance using the specified debug logger.
		 */
		def debugLoggingWith(writer: Option[KeptOpenWriter]): Repr
		
		
		// OTHER -------------------------
		
		/**
		 * Enables port forwarding
		 * @param localPort Local port to which the remotely hosted service will be forwarded.
		 *                  Call-by-name: Only called once service hosting / port forwarding is started.
		 * @param remotePort (Internal) port at which the hosted service is accessible on the remote instance.
		 *                   Call-by-name: Only called once service hosting / port forwarding is started.
		 * @return A copy of this instance applying port forwarding over the specified ports.
		 */
		def withPortForwarding(localPort: => Int, remotePort: => Int): Repr =
			withPortForwarding(Lazy { Pair(localPort, remotePort) })
		
		/**
		 * Enables debug logging
		 * @param writer Interface for making debug log entries.
		 * @return Copy of this instance using the specified debug logger.
		 */
		def debugLoggingWith(writer: KeptOpenWriter): Repr = debugLoggingWith(Some(writer))
	}
	
	trait VastAiServiceSettingsWrapper[+Repr] extends VastAiServiceSettingsFactory[Repr]
	{
		// ABSTRACT -------------------------
		
		/**
		 * @return Applied Vast AI service settings
		 */
		def serviceSettings: VastAiServiceSettings
		/**
		 * @param settings New settings to apply
		 * @return A copy of this instance applying the specified settings
		 */
		def withSettings(settings: VastAiServiceSettings): Repr
		
		
		// IMPLEMENTED  ---------------------
		
		override def withoutPortForwarding: Repr = mapServiceSettings { _.withoutPortForwarding }
		override def reusingInstancesWith(reuseLogic: InstanceReuseLogic): Repr =
			mapServiceSettings { _.reusingInstancesWith(reuseLogic) }
		override def withDestructionLogic(shouldDestroy: (VastAiInstance, Duration, Boolean) => Boolean): Repr =
			mapServiceSettings { _.withDestructionLogic(shouldDestroy) }
		override def runningInstallScript(scriptPath: Path): Repr =
			mapServiceSettings { _.runningInstallScript(scriptPath) }
		override def withPortForwarding(portsView: View[Pair[Int]]): Repr =
			mapServiceSettings { _.withPortForwarding(portsView) }
		override def withSetupTimeout(timeout: Duration): Repr = mapServiceSettings { _.withSetupTimeout(timeout) }
		override def withRecoveryTimeout(timeout: Duration): Repr =
			mapServiceSettings { _.withRecoveryTimeout(timeout) }
		override def updatingStatusEvery(interval: Duration): Repr =
			mapServiceSettings { _.updatingStatusEvery(interval) }
		override def withLabel(label: String): Repr = mapServiceSettings { _.withLabel(label) }
		override def debugLoggingWith(writer: Option[KeptOpenWriter]): Repr =
			mapServiceSettings { _.debugLoggingWith(writer) }
		
		
		// OTHER    -------------------------
		
		def mapServiceSettings(f: Mutate[VastAiServiceSettings]) = withSettings(f(serviceSettings))
	}
}

/**
 Wraps settings for Vast AI service creation
 * @param instanceReuseLogic Logic applied for reusing already rented Vast AI instances.
 *                           Default = never reuse instances.
 * @param installScriptPath Path to a script for installing vLLM on the rented device.
 *                          Used (and required), only if 'chooseImage' indicates that vLLM should be installed.
 *                          Default = None.
 * @param forwardedPorts Applied port forwarding represented using a Pair of port numbers.
 *                       The first number matches the local port and the second value the remote port.
 *                       A [[View]] should be passed in order to reserve the ports lazily.
 *                       None if no port forwarding should be applied (default).
 * @param setupTimeout Timeout for the setup process.
 *                     If the service doesn't become usable before this timeout, the instance is destroyed.
 *                     Default = infinite (not recommended).
 * @param recoveryTimeout Timeout for recovering from SSH and/or service failures. Default = 60 seconds.
 * @param statusCheckInterval Interval between instance pointer / instance status-check updates. Default = 30 seconds.
 * @param instanceLabel Custom label given to the rented Vast AI instance. Default = empty.
 * @param debugLogger Interface for making debug log entries (optional)
 * @param shouldDestroy A function that determines whether a live instance should be destroyed.
 *                      Should yield false in situations where the instance should only be stopped instead.
 *                      Notice that stopped instances still incur costs.
 *
 *                      Receives two parameters:
 *                      1. Last state of the Vast AI instance in question
 *                      1. Whether the service was successfully hosted. False if the service was not hosted yet.
 *
 *                      Never called in situations where the instance is unstable or disconnected,
 *                      or where the service became unresponsive (indicating a low-quality instance).
 *
 *                      Default = always yield true = always destroy.
 * @author Mikko Hilpinen
 * @since 05.11.2026, v1.6
 */
case class VastAiServiceSettings(instanceReuseLogic: InstanceReuseLogic = NeverReuse,
                                 installScriptPath: Option[Path] = None,
                                 forwardedPorts: Option[View[Pair[Int]]] = None,
                                 setupTimeout: Duration = 15.minutes, recoveryTimeout: Duration = 60.seconds,
                                 statusCheckInterval: Duration = 30.seconds, instanceLabel: String = "",
                                 debugLogger: Option[KeptOpenWriter] = None,
                                 shouldDestroy: (VastAiInstance, Duration, Boolean) => Boolean = { (_, _, _) => true })
	extends VastAiServiceSettingsFactory[VastAiServiceSettings]
{
	override def withoutPortForwarding: VastAiServiceSettings = copy(forwardedPorts = None)
	override def reusingInstancesWith(reuseLogic: InstanceReuseLogic): VastAiServiceSettings =
		copy(instanceReuseLogic = reuseLogic)
	override def withDestructionLogic(shouldDestroy: (VastAiInstance, Duration, Boolean) => Boolean): VastAiServiceSettings =
		copy(shouldDestroy = shouldDestroy)
	override def runningInstallScript(scriptPath: Path): VastAiServiceSettings =
		copy(installScriptPath = Some(scriptPath))
	override def withPortForwarding(portsView: View[Pair[Int]]): VastAiServiceSettings =
		copy(forwardedPorts = Some(portsView))
	override def withSetupTimeout(timeout: Duration): VastAiServiceSettings = copy(setupTimeout = timeout)
	override def withRecoveryTimeout(timeout: Duration): VastAiServiceSettings = copy(recoveryTimeout = timeout)
	override def updatingStatusEvery(interval: Duration): VastAiServiceSettings =
		copy(statusCheckInterval = interval)
	override def withLabel(label: String): VastAiServiceSettings = copy(instanceLabel = label)
	override def debugLoggingWith(writer: Option[KeptOpenWriter]): VastAiServiceSettings =
		copy(debugLogger = writer)
}