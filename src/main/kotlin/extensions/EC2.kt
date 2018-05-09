package extensions

import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model.DescribeInstancesRequest
import com.amazonaws.services.ec2.model.Instance
import com.amazonaws.services.ec2.model.TerminateInstancesRequest

fun AmazonEC2Client.findInstancesByIds(ids: List<String>): List<Instance> = this
	.describeInstances(
		DescribeInstancesRequest()
			.withInstanceIds(ids)
	)
	.reservations
	.flatMap { it.instances }

fun Instance.terminate() {
	AWS.EC2
		.terminateInstances(
			TerminateInstancesRequest()
				.withInstanceIds(this.instanceId)
		)
}
