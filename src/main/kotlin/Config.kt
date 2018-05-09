object Config {
	const val rotationInProgressTagKey = "aws-asg-rotator-RotationInProgress"

	val awsRegion: String = getEnv("AWS_REGION")
	val autoScalingGroupName = getEnv("ASG_NAME")

	private fun getEnv(key: String) = System.getenv(key)
		?: throw Exception("Cannot find the environment variable $key!")
}