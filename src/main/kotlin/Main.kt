import extensions.*
import kotlin.system.exitProcess

fun main(args: Array<String>) {
	val group = AWS.AS.findAutoScalingGroupByName(Config.autoScalingGroupName)
		?: throw Exception("Couldn't find the auto scaling " +
		"group ${Config.autoScalingGroupName} in region ${Config.awsRegion}")

	// Instead of checking if group.instances is empty, we check the group
	// capacity and size, because AWS will return all instances in the region
	// if there are no instances in the group.
	if (group.desiredCapacity == 0 &&
		group.minSize == 0 &&
		group.maxSize == 0) {
		println("The group ${group.autoScalingGroupName} has no instances. " +
			"No rotation will be performed.")
		exitProcess(0)
	}

	val rotationInProgress = group.tags
		.find { it.key == Config.rotationInProgressTagKey }

	if (rotationInProgress?.value == "true") {
		throw Exception("Refusing to rotate group " +
			"${Config.autoScalingGroupName} because there is already a " +
			"rotation in progress!")
	}

	group.updateTag(Config.rotationInProgressTagKey, "true")

	print("Performing a rotation on the auto scaling group " +
		"'${group.autoScalingGroupName}'")

	// The instance information from the autoScalingGroup is not complete;
	// request it from EC2.
	val instances = AWS.EC2.findInstancesByIds(
		group.instances.map { it.instanceId }
	)
		.sortedBy { it.launchTime }

	println(", with instances:")

	val colLength = 19 // Length of instance IDs
	val colName = "Instance ID".padEnd(19)
	println("\t- $colName | Launched at")

	instances.forEach {
		val instanceId = it.instanceId.padEnd(colLength)
		println("\t- $instanceId | ${it.launchTime}")
	}

	println()

	instances.forEach {
		println("Rotating instance ${it.instanceId}...")
		group
			.detachInstance(it)
			.waitForCompletion()

		println("Terminating instance ${it.instanceId}...")
		it.terminate()

		print("Waiting for the new instance to be ready...")
		val newInstanceActivity = group.waitForNewInstanceActivity()

		LAUNCHING_INSTANCE_REGEX
			.matchEntire(newInstanceActivity.description)
			?.groupValues?.get(1)
			?.let {
				println("\rWaiting for the instance $it to be ready...")
			} ?: println()

		newInstanceActivity.waitForCompletion()

		println()
	}

	// TODO delete the tag in case of error
	group.deleteTag(Config.rotationInProgressTagKey)

	println("The rotation was successful!")
}
