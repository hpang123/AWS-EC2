package com.amazonaws.samples;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairResult;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.KeyPair;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Tag;

public class EC2Sample {

	private Logger log = Logger.getLogger("EC2Sample.class");

	private AWSCredentials credentials;
	private String endPoint;
	private Region region;
	private AmazonEC2Client ec2client;

	private String groupName = "my-ec2-securitygroup";
	private String groupDescription = "This group added by java";

	// private String sshIpRange = " IP/32";
	private String sshIpRange = "111.111.111.111/32";
	private String sshprotocol = "tcp";
	private int sshFromPort = 22;
	private int sshToPort = 22;

	private String httpIpRange = "0.0.0.0/0";
	private String httpProtocol = "tcp";
	private int httpFromPort = 80;
	private int httpToPort = 80;

	private String httpsIpRange = "0.0.0.0/0";
	private String httpsProtocol = "tcp";
	private int httpsFromPort = 443;
	private int httpsToProtocol = 443;

	private String keyName = "test-generated-keypair";
	private String pemFilePath = "c:\temp";
	private String pemFileName = "test_keypair.pem";

	//Microsoft Windows Server 2016 Base - ami-05446e60
	private String imageId = "ami-05446e60";
	private String instanceType = "t2.micro"; //Free tier
	private String instanceName = "my-ondemand-instance";

	public static void main(String[] args) {
		EC2Sample ec2sample = new EC2Sample();

		ec2sample.init();
		/*
		 * Delete the test security group first Otherwise, it throws already exist
		 * exception
		 */
		
		ec2sample.createEC2SecurityGroup(); 
		 
		ec2sample.createKeyPair();
		 

		ec2sample.createEC2OnDemandInstance();
	}

	private void init() {
		// credentials = new BasicAWSCredentials(accessKey, secretKey);

		try {
			credentials = new ProfileCredentialsProvider("default").getCredentials();
		} catch (Exception e) {
			throw new AmazonClientException("Cannot load the credentials from the credential profiles file. "
					+ "Please make sure that your credentials file is at the correct "
					+ "location (C:\\Users\\Ray\\.aws\\credentials), and is in valid format.", e);
		}

		// endPoint = "https://rds.ap-southeast-1.amazonaws.com";
		endPoint = "https://ec2.us-east-2.amazonaws.com";

		// region = Region.getRegion(Regions.AP_SOUTHEAST_1);
		region = Region.getRegion(Regions.US_EAST_2);
		ec2client = new AmazonEC2Client(credentials);
		ec2client.setEndpoint(endPoint);
	}

	private void createEC2SecurityGroup() {
		try {
			log.info("Create request for security group");
			CreateSecurityGroupRequest createSecurityGroupRequest = new CreateSecurityGroupRequest();
			createSecurityGroupRequest.withGroupName(groupName).withDescription(groupDescription);
			createSecurityGroupRequest.setRequestCredentials(credentials);
			CreateSecurityGroupResult csgr = ec2client.createSecurityGroup(createSecurityGroupRequest);

			String groupid = csgr.getGroupId();
			log.info("New Security Group Id : " + groupid);

			log.info("Security Group Permission");
			Collection<IpPermission> ips = new ArrayList<IpPermission>();

			IpPermission ipssh = new IpPermission();
			ipssh.withIpRanges(sshIpRange).withIpProtocol(sshprotocol).withFromPort(sshFromPort).withToPort(sshToPort);
			ips.add(ipssh);

			IpPermission iphttp = new IpPermission();
			iphttp.withIpRanges(httpIpRange).withIpProtocol(httpProtocol).withFromPort(httpFromPort)
					.withToPort(httpToPort);
			ips.add(iphttp);

			IpPermission iphttps = new IpPermission();
			iphttps.withIpRanges(httpsIpRange).withIpProtocol(httpsProtocol).withFromPort(httpsFromPort)
					.withToPort(httpsToProtocol);
			ips.add(iphttps);

			AuthorizeSecurityGroupIngressRequest authorizeSecurityGroupIngressRequest = new AuthorizeSecurityGroupIngressRequest();
			authorizeSecurityGroupIngressRequest.withGroupName(groupName).withIpPermissions(ips);
			ec2client.authorizeSecurityGroupIngress(authorizeSecurityGroupIngressRequest);

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	private void createKeyPair() {
		try {
			CreateKeyPairRequest ckpr = new CreateKeyPairRequest();
			ckpr.withKeyName(keyName);

			CreateKeyPairResult ckpresult = ec2client.createKeyPair(ckpr);
			KeyPair keypair = ckpresult.getKeyPair();
			String privateKey = keypair.getKeyMaterial();
			log.info("KeyPair will be :" + privateKey);
			// writePemFile(privateKey, pemFilePath, pemFileName);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	private void createEC2OnDemandInstance() {
		try {
			RunInstancesRequest uv = new RunInstancesRequest();
			uv.withImageId(imageId);
			uv.withInstanceType(instanceType);
			uv.withMinCount(1);
			uv.withMaxCount(1);
			uv.withKeyName(keyName);
			uv.withMonitoring(true);
			uv.withSecurityGroups(groupName);

			RunInstancesResult riresult = ec2client.runInstances(uv);

			log.info(riresult.getReservation().getReservationId());

			String instanceId = null;
			DescribeInstancesResult result = ec2client.describeInstances();
			Iterator<Reservation> i = result.getReservations().iterator();
			while (i.hasNext()) {
				Reservation r = i.next();
				List<Instance> instances = r.getInstances();
				for (Instance ii : instances) {
					log.info(ii.getImageId() + "\t" + ii.getInstanceId() + "\t" + ii.getState().getName() + "\t"
							+ ii.getPrivateDnsName());
					if (ii.getState().getName().equals("pending") || ii.getState().getName().equals("running") ) {
						instanceId = ii.getInstanceId();
					}
				}
			}

			log.info("New Instance ID will be:" + instanceId);

			boolean isWaiting = true;
			while (isWaiting) {
				log.info("we are Waiting");
				Thread.sleep(1010);
				DescribeInstancesResult r = ec2client.describeInstances();
				Iterator<Reservation> ir = r.getReservations().iterator();
				while (ir.hasNext()) {
					Reservation rr = ir.next();
					List<Instance> instances = rr.getInstances();
					for (Instance ii : instances) {
						log.info(ii.getImageId() + "\t" + ii.getInstanceId() + "\t" + ii.getState().getName() + "\t"
								+ ii.getPrivateDnsName());
						if (ii.getState().getName().equals("running") && ii.getInstanceId().equals(instanceId)) {
							log.info(ii.getPublicDnsName());
							isWaiting = false;
						}
					}
				}
			}

			CreateTagsRequest crt = new CreateTagsRequest();
			ArrayList<Tag> arrTag = new ArrayList<Tag>();
			arrTag.add(new Tag().withKey("Name").withValue(instanceName));
			crt.setTags(arrTag);

			ArrayList<String> arrInstances = new ArrayList<String>();
			arrInstances.add(instanceId);
			crt.setResources(arrInstances);
			ec2client.createTags(crt);

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

}
