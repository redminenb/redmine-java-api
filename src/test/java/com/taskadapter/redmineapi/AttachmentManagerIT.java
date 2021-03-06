package com.taskadapter.redmineapi;

import com.taskadapter.redmineapi.bean.Attachment;
import com.taskadapter.redmineapi.bean.Issue;
import com.taskadapter.redmineapi.bean.IssueFactory;
import com.taskadapter.redmineapi.bean.Project;
import org.apache.http.entity.ContentType;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class AttachmentManagerIT {

    private static RedmineManager mgr;
    private static int projectId;
    private static String projectKey;
    private static IssueManager issueManager;
    private static AttachmentManager attachmentManager;

    @BeforeClass
    public static void oneTimeSetup() {
        mgr = IntegrationTestHelper.createRedmineManager();

        issueManager = mgr.getIssueManager();
        attachmentManager = mgr.getAttachmentManager();
        Project project = IntegrationTestHelper.createProject(mgr);
        projectId = project.getId();
        projectKey = project.getIdentifier();
    }

    @AfterClass
    public static void oneTimeTearDown() {
        IntegrationTestHelper.deleteProject(mgr, projectKey);
    }

    @Test
    public void uploadAttachment() throws RedmineException, IOException {
        final byte[] content = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        final Attachment attach1 = attachmentManager.uploadAttachment("test.bin",
                "application/ternary", content);
        final Issue testIssue = IssueFactory.create(projectId, "This is upload ticket!");
        testIssue.addAttachment(attach1);
        final Issue createdIssue = issueManager.createIssue(testIssue);
        try {
            final Collection<Attachment> attachments = createdIssue.getAttachments();
            assertThat(attachments.size()).isEqualTo(1);
            final Attachment added = attachments.iterator().next();
            assertThat(added.getFileName()).isEqualTo("test.bin");
            assertThat(added.getContentType()).isEqualTo("application/ternary");
            final byte[] receivedContent = attachmentManager.downloadAttachmentContent(added);
            assertArrayEquals(content, receivedContent);

            Issue issueById = issueManager.getIssueById(createdIssue.getId(), Include.attachments);
            assertThat(issueById.getAttachments().size()).isEqualTo(1);
        } finally {
            issueManager.deleteIssue(createdIssue.getId());
        }
    }

    /**
     * Regression test for https://github.com/taskadapter/redmine-java-api/issues/194
     */
    @Test
    public void severalAttachmentsAreAddedToIssue() throws Exception {
        final byte[] content = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        final Attachment attach1 = attachmentManager.uploadAttachment("test1.bin", "application/ternary", content);
        final Attachment attach2 = attachmentManager.uploadAttachment("test2.bin", "application/ternary", content);
        final Issue issue = IssueFactory.create(projectId, "This is upload ticket!");
        issue.addAttachment(attach1);
        issue.addAttachment(attach2);
        final Issue createdIssue = issueManager.createIssue(issue);
        try {
            final Collection<Attachment> attachments = createdIssue.getAttachments();
            assertThat(attachments.size()).isEqualTo(2);
        } finally {
            issueManager.deleteIssue(createdIssue.getId());
        }
    }

    @Test
    public void addAttachment() throws RedmineException, IOException {
        String attachmentContent = "some text";
        File tempFile = createTempFile(attachmentContent);

        final Issue issue = IssueFactory.create(projectId, "task with attachment");
        final Issue createdIssue = issueManager.createIssue(issue);
        attachmentManager.addAttachmentToIssue(createdIssue.getId(), tempFile, ContentType.TEXT_PLAIN.getMimeType());
        try {
            Issue loadedIssue = issueManager.getIssueById(createdIssue.getId(), Include.attachments);
            final Collection<Attachment> attachments = loadedIssue.getAttachments();
            Attachment next = attachments.iterator().next();
            assertThat(next.getFileName()).isEqualTo(tempFile.getName());
            final byte[] receivedContent = attachmentManager.downloadAttachmentContent(next);
            String contentAsString = new String(receivedContent);
            assertThat(contentAsString).isEqualTo(attachmentContent);
        } finally {
            issueManager.deleteIssue(createdIssue.getId());
        }
    }

    @Test(expected = IOException.class)
    public void testUploadException() throws RedmineException, IOException {
        final InputStream content = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("Unsupported read!");
            }
        };
        attachmentManager.uploadAttachment("test.bin", "application/ternary", content);
    }

    /**
     * Tests the retrieval of an {@link com.taskadapter.redmineapi.bean.Attachment} by ID.
     *
     * @throws RedmineException               thrown in case something went wrong in Redmine
     * @throws IOException                    thrown in case something went wrong while performing I/O
     *                                        operations
     * @throws RedmineAuthenticationException thrown in case something went wrong while trying to login
     * @throws NotFoundException              thrown in case the objects requested for could not be found
     */
    // TODO reactivate once Redmine REST API allows for creating attachments
    @Ignore
    @Test
    public void testGetAttachmentById() throws RedmineException {
        // TODO where do we get a valid attachment number from? We can't create
        // an attachment by our own for the test as the Redmine REST API does
        // not support that.
        int attachmentID = 1;
        Attachment attachment = attachmentManager.getAttachmentById(attachmentID);
        assertNotNull("Attachment retrieved by ID " + attachmentID
                + " should not be null", attachment);
        assertNotNull("Content URL of attachment retrieved by ID "
                + attachmentID + " should not be null",
                attachment.getContentURL());
        // TODO more asserts on the attachment once this delivers an attachment
    }

    /**
     * Tests the download of the content of an
     * {@link com.taskadapter.redmineapi.bean.Attachment}.
     *
     * @throws RedmineException               thrown in case something went wrong in Redmine
     * @throws IOException                    thrown in case something went wrong while performing I/O
     *                                        operations
     * @throws RedmineAuthenticationException thrown in case something went wrong while trying to login
     * @throws NotFoundException              thrown in case the objects requested for could not be found
     */
    // TODO reactivate once Redmine REST API allows for creating attachments
    @Ignore
    @Test
    public void testDownloadAttachmentContent() throws RedmineException {
        // TODO where do we get a valid attachment number from? We can't create
        // an attachment by our own for the test as the Redmine REST API does
        // not support that.
        int attachmentID = 1;
        // retrieve issue attachment
        Attachment attachment = attachmentManager.getAttachmentById(attachmentID);
        // download attachment content
        byte[] attachmentContent = attachmentManager.downloadAttachmentContent(attachment);
        assertNotNull("Download of content of attachment with content URL " + attachment.getContentURL()
                + " should not be null", attachmentContent);
    }

    /**
     * Tests the download of the content of an
     * {@link com.taskadapter.redmineapi.bean.Attachment Attachment} using API key authentication.
     *
     * @throws RedmineException               thrown in case something went wrong in Redmine
     * @throws IOException                    thrown in case something went wrong while performing I/O
     *                                        operations
     * @throws RedmineAuthenticationException thrown in case something went wrong while trying to login
     * @throws NotFoundException              thrown in case the objects requested for could not be found
     */
    // TODO reactivate once Redmine REST API allows for creating attachments
    @Ignore
    @Test
    public void testDownloadAttachmentContentWithAPIKey() throws RedmineException {
        // TODO where do we get a valid attachment number from? We can't create
        // an attachment by our own for the test as the Redmine REST API does
        // not support that.
        int attachmentID = 1;
        // Create managers using API key authentication
        RedmineManager mgrWithApiKey = IntegrationTestHelper.createRedmineManagerWithAPIKey();
        AttachmentManager attachmentMgr = mgrWithApiKey.getAttachmentManager();
        // retrieve issue attachment
        Attachment attachment = attachmentMgr.getAttachmentById(attachmentID);
        // download attachment content
        byte[] attachmentContent = attachmentMgr.downloadAttachmentContent(attachment);
        assertNotNull("Download of content of attachment with content URL " + attachment.getContentURL()
                + " should not be null", attachmentContent);
        // TODO perform downloaded content validation (when we'll be able to create
        // an attachment using Redmine REST API)
    }

    /**
     * Tests the retrieval of an {@link Issue}, inlcuding the
     * {@link com.taskadapter.redmineapi.bean.Attachment}s.
     *
     * @throws RedmineException               thrown in case something went wrong in Redmine
     * @throws IOException                    thrown in case something went wrong while performing I/O
     *                                        operations
     * @throws RedmineAuthenticationException thrown in case something went wrong while trying to login
     * @throws NotFoundException              thrown in case the objects requested for could not be found
     */
    @Test
    public void testGetIssueWithAttachments() throws RedmineException {
        Issue newIssue = null;
        try {
            // create at least 1 issue
            Issue issueToCreate = IssueFactory.create(projectId, "testGetIssueAttachment_" + UUID.randomUUID());
            newIssue = issueManager.createIssue(issueToCreate);
            // TODO create test attachments for the issue once the Redmine REST
            // API allows for it
            // retrieve issue attachments
            Issue retrievedIssue = issueManager.getIssueById(newIssue.getId(),
                    Include.attachments);
            assertNotNull("List of attachments retrieved for issue "
                    + newIssue.getId()
                    + " delivered by Redmine Java API should not be null",
                    retrievedIssue.getAttachments());
            // TODO assert attachments once we actually receive ones for our
            // test issue
        } finally {
            // scrub test issue
            if (newIssue != null) {
                issueManager.deleteIssue(newIssue.getId());
            }
        }
    }

    /**
     * Requires Redmine 3.3.0+ because "delete attachment" feature is only added in 3.3.0.
     */
    @Test
    public void attachmentIsDeleted() throws Exception {
        final Issue issue = IssueFactory.create(projectId, "task with attachment");
        final byte[] content = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        final Attachment attachment = attachmentManager.uploadAttachment("test.bin", "application/ternary", content);
        issue.addAttachment(attachment);
        final Issue createdIssue = issueManager.createIssue(issue);
        Collection<Attachment> attachments = createdIssue.getAttachments();
        Attachment attachment1 = attachments.iterator().next();
        int attachmentId = attachment1.getId();
        attachmentManager.delete(attachmentId);
        try {
            attachmentManager.getAttachmentById(attachmentId);
            fail("must have failed with NotFoundException");
        } catch (NotFoundException e) {
            System.out.println("got expected exception for deleted attachment");
        }
    }

    private static File createTempFile(String content) throws IOException {
        File tempFile = File.createTempFile("redmine_test_", ".tmp");
        FileWriter fileWriter = new FileWriter(tempFile.getAbsolutePath());
        fileWriter.write(content);
        fileWriter.close();
        return tempFile;
    }
}