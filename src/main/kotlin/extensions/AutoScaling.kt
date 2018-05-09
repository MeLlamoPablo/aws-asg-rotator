package extensions

import LAUNCHING_INSTANCE_REGEX
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient
import com.amazonaws.services.autoscaling.model.*
import com.amazonaws.services.ec2.model.Instance

fun AmazonAutoScalingClient.findAutoScalingGroupByName(name: String) = this
	.describeAutoScalingGroups(
		DescribeAutoScalingGroupsRequest()
			.withAutoScalingGroupNames(name)
	)
	.autoScalingGroups
	.firstOrNull()

fun AutoScalingGroup.updateTag(key: String, value: String) = AWS.AS.
	createOrUpdateTags(
		CreateOrUpdateTagsRequest()
			.withTags(
				Tag()
					.withKey(key)
					.withValue(value)
					.withResourceId(this.autoScalingGroupName)
					.withResourceType("auto-scaling-group")
					.withPropagateAtLaunch(false)
			)
	)

fun AutoScalingGroup.deleteTag(key: String) = AWS.AS.
	deleteTags(
		DeleteTagsRequest()
			.withTags(
				Tag()
					.withKey(key)
					.withResourceId(this.autoScalingGroupName)
					.withResourceType("auto-scaling-group")
			)
	)

fun AutoScalingGroup.detachInstance(instance: Instance) = AWS.AS
	.detachInstances(
		DetachInstancesRequest()
			.withAutoScalingGroupName(this.autoScalingGroupName)
			.withInstanceIds(instance.instanceId)
			.withShouldDecrementDesiredCapacity(false)
	)
	.activities
	.firstOrNull() ?: throw Exception("Detached instance " +
	"${instance.instanceId}, but AWS didn't respond with an activity!")

fun AutoScalingGroup.waitForNewInstanceActivity(): Activity = AWS.AS
	.describeScalingActivities(
		DescribeScalingActivitiesRequest()
			.withAutoScalingGroupName(this.autoScalingGroupName)
			.withMaxRecords(1)
	)
	.activities
	.firstOrNull()
	?.let {
		if (it.description.matches(LAUNCHING_INSTANCE_REGEX)) {
			it
		} else {
			null
		}
	} ?: {
		Thread.sleep(2000)
		this.waitForNewInstanceActivity()
	}()

fun Activity.waitForCompletion() {
	val currentActivity = AWS.AS
		.describeScalingActivities(
			DescribeScalingActivitiesRequest()
				.withActivityIds(this.activityId)
		)
		.activities
		.firstOrNull() ?: throw Exception("Queried activity " +
		"${this.activityId}, but AWS didn't respond with any information!")

	if (currentActivity.statusCode != "Successful") {
		Thread.sleep(2000)
		this.waitForCompletion()
	}
}
