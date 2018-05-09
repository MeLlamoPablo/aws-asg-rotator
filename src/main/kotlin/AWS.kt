import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient
import com.amazonaws.services.ec2.AmazonEC2Client

object AWS {
	val EC2 = AmazonEC2Client()
	val AS = AmazonAutoScalingClient()

	init {
		listOf(EC2, AS).forEach {
			it.setRegion(Region.getRegion(Regions.fromName(Config.awsRegion)))
		}
	}
}