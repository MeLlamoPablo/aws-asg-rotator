# aws-asg-rotator 

**WARNING: this tool is not mature enough to be considered production ready.
Only use it in production if you're willing to contribute to it and make changes
to the code yourself.**

`aws-asg-rotator` is a simple tool that can rotate the instances of an
[Amazon Web Services](https://aws.amazon.com)
[Auto Scaling](https://aws.amazon.com/es/autoscaling/) Group. This has two major
use cases:

* **Deploying a new release of your application with a rolling update**: first
update the launch configuration of the auto scaling group, and then perform an
instance rotation. The new instances will have the new launch configuration.
* **Keeping the instances fresh**: run this tool periodically to terminate old
instances and ensure that your auto scaling group is always running young
instances.

## How to use

You can use this tool in a machine with [Docker](https://www.docker.com/)
installed. You can run the tool with the following command:

```bash
docker run -e AWS_ACCESS_KEY_ID='your_access_key_id' \
    -e AWS_SECRET_ACCESS_KEY='your_secret_access_key' \
    -e AWS_REGION='your_aws_region' \
    -e ASG_NAME='your_auto_scaling_group_name' \
    mellamopablo/aws-asg-rotator:1.0.1 \
    rotate-asg
```

All four environment variables are required.

## Using with Concourse

`aws-asg-rotator` integrates nicely with [Concourse](https://concourse-ci.org).
The following example contains a task definition that rotates an ASG:

```yaml
platform: linux

image_resource:
  type: docker-image
  source:
    repository: mellamopablo/aws-asg-rotator
    tag: 1.0.1

params:
  - AWS_ACCESS_KEY_ID: ((aws_access_key_id))
  - AWS_SECRET_ACCESS_KEY: ((aws_secret_access_key))
  - AWS_REGION: eu-west-1
  - ASG_NAME: my-auto-scaling-group

run:
  path: rotate-asg
```

## IAM Policy

The following IAM policy contains the permissions required by `aws-asg-rotator`
to work.

```javascript
{
	"Version": "2012-10-17",
	"Statement": [
		{
			"Effect": "Allow",
			"Action": [
				"autoscaling:DescribeScalingActivities",
				"autoscaling:DescribeAutoScalingGroups"
			],
			"Resource": "*"
		},
		{
			"Effect": "Allow",
			"Action": [
				"autoscaling:DeleteTags",
				"autoscaling:CreateOrUpdateTags",
				"autoscaling:DetachInstances"
			],
			/*
			 * It is recommended to limit the policy to only the necessary auto
			 * scaling groups. However, if you don't want to bother, you may
			 * replace this with:
			 *
			 * "Resource": "*"
			 */
			"Resource": [
				"arn:aws:autoscaling:<region>:<account-id>:autoScalingGroup:<group-id>:autoScalingGroupName/<group-name>"
				/* Add more ARNs if needed */
			]
		},
		{
			"Effect": "Allow",
			"Action": [
				"ec2:DescribeInstances"
			],
			"Resource": "*"
		},
		{
			"Effect": "Allow",
			"Action": [
				"ec2:TerminateInstances"
			],
			/*
			 * We cannont limit by instances because we we don't know the
			 * instance IDs that will be assigned to the new instances. However,
			 * we can limit by region and account, so the policy doesn't have
			 * permissions to terminate instances outside the region it runs in.
			 */
			"Resource": "arn:aws:ec2:<region>:<account-id>:instance/*"
		}
	]
}
```

Make sure to remove the JSON comments before creating the policy!

## Building the Docker image for production

The `Dockerfile` does not optimize its intermediate images. Instead, it tries to
create as many intermediate images as possible, and then has one final step 
where it cleans up all the generated bloat. This is good for development,
because it makes subsequent builds faster. However, it is bad for production,
because the last step removes the bloat from the final image, but the bloat
stays in the intermediate images.

The solution to this is to build the docker image with the `--squash` flag:

```bash
docker build --squash -t aws-asg-rotator .
```

This will squash all the intermediate images into a single one, effectively
removing the bloat from the final image (and decreasing its size from ~470 MB
to ~70 MB).

Note that at the time of writing, this is an
[experimental feature](https://github.com/docker/docker-ce/blob/master/components/cli/experimental/README.md).
If you would like to avoid using it, you can also edit the `Dockerfile` to
perform all build commands in a single `RUN` instruction.
