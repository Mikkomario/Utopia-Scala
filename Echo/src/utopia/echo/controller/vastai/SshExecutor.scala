package utopia.echo.controller.vastai

import utopia.echo.model.vastai.instance.SshConnection
import utopia.flow.collection.immutable.Empty

import java.nio.file.Path
import scala.sys.process.Process

/**
 * An interface which provides functions for executing commands over SSH
 * @param config Configuration for connecting to the remote device via SSH
 * @param privateKeyPath Path to the private SSH key file to use
 * @author Mikko Hilpinen
 * @since 02.03.2026, v1.5
 */
case class SshExecutor(config: SshConnection, privateKeyPath: Path)
{
	// ATTRIBUTES   ------------------------
	
	private val address = s"root@${ config.host }"
	
	private val prefixArgs = s"-i $privateKeyPath -p ${ config.port }"
	private val suffixArgs =
		s"-o BatchMode=yes -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null"
	
	private val sshBase = s"ssh $prefixArgs $address $suffixArgs"
	
	
	// OTHER    ---------------------------
	
	/**
	 * Prepares a command to be executed over SSH
	 * @param command Command to execute
	 * @return Process representing that command
	 */
	def apply(command: String) = Process(s"$sshBase $command")
	
	/**
	 * Prepares a command for transferring a file to a remote device using SCP
	 * @param file File to transfer
	 * @param toRemotePath Path to the file on the remote device
	 * @return Process for executing the transfer
	 */
	def transfer(file: Path, toRemotePath: String) =
		Process(s"scp $prefixArgs $file $address:$toRemotePath $suffixArgs")
	/**
	 * Prepares a command for executing a file over SSH
	 * @param remoteScriptPath Path to the script / file on the remote instance, which should be executed
	 * @param args Arguments to pass to the execution
	 * @return A process for executing that script / file
	 */
	def executeScript(remoteScriptPath: String, args: Seq[String] = Empty) =
		apply(s"chmod +x $remoteScriptPath && $remoteScriptPath${ args.iterator.map { arg => s" '$arg'" }.mkString }")
	
	/**
	 * Prepares a command for initiating port-forwarding
	 * @param localPort Local port that will be forwarded to a remote port
	 * @param remotePort Remote port the local port will match
	 * @param remoteHost Targeted host on the remote device. Default = 127.0.0.1 (i.e. localhost).
	 * @return A process for starting / running the port-forwarding (will run as long as forwarding is active)
	 */
	def portForwarding(localPort: Int, remotePort: Int, remoteHost: String = "127.0.0.1") =
		Process(s"ssh -N $prefixArgs -L $localPort:$remoteHost:$remotePort $address $suffixArgs -o ServerAliveInterval=60 -o ServerAliveCountMax=5")
}
