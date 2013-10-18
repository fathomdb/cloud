package io.fathom.cloud.storage.api.aws;

//
//import javax.inject.Inject;
//import javax.servlet.http.HttpServletRequest;
//import javax.ws.rs.GET;
//import javax.ws.rs.Path;
//import javax.ws.rs.WebApplicationException;
//import javax.ws.rs.core.Response.Status;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import io.fathom.cloud.CloudException;
//import io.fathom.cloud.api.aws.s3.model.BucketInfo;
//import io.fathom.cloud.api.aws.s3.model.ListAllMyBucketsResult;
//import io.fathom.cloud.api.aws.s3.model.Owner;
//import io.fathom.cloud.protobuf.files.FileModel.BucketData;
//import io.fathom.cloud.protobuf.IdentityModel.CredentialData;
//import io.fathom.cloud.protobuf.IdentityModel.UserData;
//import io.fathom.cloud.state.AuthStore;
//import io.fathom.cloud.state.FileStore;
//import io.fathom.cloud.storage.Clock;
//import com.google.common.collect.Lists;
//
//@Path("/aws/s3")
//public class S3Resource extends AwsResourceBase {
//	private static final Logger log = LoggerFactory.getLogger(S3Resource.class);
//
//	@Inject
//	AuthStore authStore;
//
//	@Inject
//	FileStore fileStore;
//
//	@GET
//	public ListAllMyBucketsResult doListBuckets() throws CloudException {
//		// String uri = req.getRequestURI();
//
//		CredentialData credential = findCredential(httpRequest);
//		UserData user = findUser(credential);
//		if (user == null) {
//			throw new WebApplicationException(Status.UNAUTHORIZED);
//		}
//
//		if (!credential.hasProjectId()) {
//			throw new IllegalStateException();
//		}
//
//		long projectId = credential.getProjectId();
//
//		// // TODO: Fix this... how do we pick the correct project??
//		//
//		// if (user.hasDefaultProjectId()) {
//		// projectId = user.getDefaultProjectId();
//		// } else if (user.getProjectRolesCount() > 0) {
//		// projectId = user.getProjectRolesList().get(0).getProject();
//		// } else {
//		// throw new IllegalArgumentException("No projects available");
//		// }
//
//		ListAllMyBucketsResult result = new ListAllMyBucketsResult();
//		result.owner = new Owner();
//		result.owner.id = user.getId() + "";
//		result.owner.displayName = user.getName();
//
//		result.buckets = Lists.newArrayList();
//
//		// DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
//
//		for (BucketData bucketData : fileStore.getBuckets(projectId).list()) {
//			BucketInfo bucket = new BucketInfo();
//			result.buckets.add(bucket);
//
//			bucket.name = bucketData.getKey();
//			bucket.creationDate = Clock.toDate(bucketData.getCreatedAt());
//
//			// xml.writeCharacters(formatter.print(bucket.getCreatedAt()));
//		}
//
//		return result;
//
//		// {'status': 200, 'headers': {'x-amz-id-2':
//		// 'DjsJbMi/QMZDBcQflXAyLoFsim9n55dUNzcOv3M7G5bsnLj62mw9Be58a0HmZhTz',
//		// 'server': 'AmazonS3', 'transfer-encoding': 'chunked',
//		// 'x-amz-request-id': '6AD64A5E7DE78EFB', 'date': 'Mon, 08 Jul 2013
//		// 00:39:15 GMT', 'content-type': 'application/xml'}, 'reason': 'OK',
//		// 'data': '<?xml version="1.0"
//		// encoding="UTF-8"?>\n<ListAllMyBucketsResult
//		// xmlns="http://s3.amazonaws.com/doc/2006-03-01/"><Owner><ID>6fcad0d37dd301d1de3ee48a704dbb0d543cc5014e2859e3c145d7551e246aad</ID><DisplayName>jsantab</DisplayName></Owner><Buckets><Bucket><Name>apt-fathomdb</Name><CreationDate>2013-07-02T16:10:20.000Z</CreationDate></Bucket><Bucket><Name>apt-fathomdb-backup</Name><CreationDate>2013-07-02T16:29:55.000Z</CreationDate></Bucket><Bucket><Name>fathomdb-amis</Name><CreationDate>2008-10-01T07:00:53.000Z</CreationDate></Bucket></Buckets></ListAllMyBucketsResult>'}
//
//		// super.doGet(req, resp);
//	}
//
//	private CredentialData findCredential(HttpServletRequest req)
//			throws CloudException {
//		String auth = req.getHeader("Authorization");
//		if (auth == null) {
//			return null;
//		}
//
//		if (auth.startsWith("AWS ")) {
//			auth = auth.substring(4);
//
//			int colonIndex = auth.indexOf(':');
//			if (colonIndex == -1) {
//				return null;
//			}
//
//			String signed = auth.substring(colonIndex + 1);
//			String awsId = auth.substring(0, colonIndex);
//
//			CredentialData credential = authStore.getEc2Credentials().find(
//					awsId);
//			if (credential == null) {
//				return null;
//			}
//
//			log.warn("TODO: Must implement AWS credential validation");
//
//			return credential;
//		} else {
//			return null;
//		}
//	}
//
//	private UserData findUser(CredentialData credential) throws CloudException {
//		if (credential == null) {
//			return null;
//		}
//
//		long userId = credential.getUserId();
//		UserData user = authStore.getUsers().find(userId);
//
//		return user;
//	}
//
// }
