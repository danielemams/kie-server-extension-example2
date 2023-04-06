package org.redhat.rhpam.tests.extension;

import org.jbpm.runtime.manager.impl.identity.UserDataServiceProvider;
import org.jbpm.runtime.manager.impl.jpa.EntityManagerFactoryManager;
import org.jbpm.shared.services.impl.TransactionalCommandService;
import org.jbpm.shared.services.impl.commands.QueryStringCommand;
import org.kie.api.runtime.query.QueryContext;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.identity.IdentityProvider;
import org.kie.internal.query.QueryFilter;
import org.kie.server.api.KieServerConstants;
import org.kie.server.api.model.instance.TaskSummaryList;
import org.kie.server.remote.rest.common.Header;
import org.kie.server.services.api.KieServerRegistry;
import org.kie.server.services.jbpm.RuntimeDataServiceBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManagerFactory;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.jbpm.kie.services.impl.CommonUtils.getCallbackUserRoles;
import static org.kie.server.remote.rest.common.util.RestUtils.buildConversationIdHeader;
import static org.kie.server.remote.rest.common.util.RestUtils.createCorrectVariant;
import static org.kie.server.remote.rest.common.util.RestUtils.errorMessage;
import static org.kie.server.remote.rest.common.util.RestUtils.getVariant;
import static org.kie.server.remote.rest.common.util.RestUtils.internalServerError;
import static org.kie.server.services.jbpm.ConvertUtils.buildTaskByNameQueryFilter;
import static org.kie.server.services.jbpm.ConvertUtils.buildTaskStatuses;
import static org.kie.server.services.jbpm.ConvertUtils.convertToTaskSummaryList;

@Path("/server/containers/myRestApi")
public class CustomResource {

	public static final Logger logger = LoggerFactory.getLogger(CustomResource.class);

	private RuntimeDataServiceBase runtimeDataService;
	private KieServerRegistry context;
	private IdentityProvider identityProvider;
	private boolean bypassAuthUser;
	private TransactionalCommandService commandService;
	private EntityManagerFactory emf;

	public CustomResource(RuntimeDataServiceBase runtimeDataService, KieServerRegistry context) {
		this.runtimeDataService = runtimeDataService;
		this.context = context;
		this.identityProvider = this.context.getIdentityProvider();
		this.bypassAuthUser = Boolean.parseBoolean(this.context.getConfig().getConfigItemValue(
				KieServerConstants.CFG_BYPASS_AUTH_USER, "false"));
		this.emf = EntityManagerFactoryManager.get().getOrCreate("org.jbpm.domain");
		this.commandService = new TransactionalCommandService(emf);
	}

	@GET
	@Path("/restapi")
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
	public Response getTasksAssignedAsPotentialOwner(
			@Context HttpHeaders headers,
			@QueryParam("user") String userId,
			@QueryParam("status") List<String> status,
			@QueryParam("pid") List<String> pid,
			@QueryParam("page") @DefaultValue("0") Integer page,
			@QueryParam("pageSize") @DefaultValue("10") Integer pageSize) {
		Variant v = getVariant(headers);
		Header conversationIdHeader = buildConversationIdHeader("", context, headers);
		try {
			TaskSummaryList result = this.getTasksAssignedAsPotentialOwner(userId, status, pid, page, pageSize);
			return createCorrectVariant(result, headers, Response.Status.OK, conversationIdHeader);
		} catch (Exception e) {
			logger.error("Unexpected error during processing {}", e.getMessage(), e);
			return internalServerError(errorMessage(e), v, conversationIdHeader);
		}
	}

	private TaskSummaryList getTasksAssignedAsPotentialOwner(String userId, List<String> status, List<String> pid, Integer page, Integer pageSize){
		List<Status> taskStatuses = buildTaskStatuses(status);
		List<Long> notInProcessInstanceIds = pid.stream().map(Long::valueOf).collect(Collectors.toList());
		userId = getUser(userId);
		logger.debug("About to search for task assigned as potential owner for user '{}'", userId);
		Map<String, Object> params = new HashMap<>();
		params.put("userId", userId);
		params.put("groupIds", getCallbackUserRoles(UserDataServiceProvider.getUserGroupCallback(), userId));
		params.put("status", taskStatuses);
		params.put("notInProcessInstanceIds", notInProcessInstanceIds);
		QueryFilter queryFilter = buildTaskByNameQueryFilter(page, pageSize, null, true, null);
		applyQueryContext(params, queryFilter);
		final String query = "select distinct " +
				"                new org.jbpm.services.task.query.TaskSummaryImpl( " +
				"                    t.id, " +
				"                    t.name, " +
				"                    t.subject, " +
				"                    t.description, " +
				"                    t.taskData.status, " +
				"                    t.priority, " +
				"                    t.taskData.actualOwner.id, " +
				"                    t.taskData.createdBy.id, " +
				"                    t.taskData.createdOn, " +
				"                    t.taskData.activationTime, " +
				"                    t.taskData.expirationTime, " +
				"                    t.taskData.processId, " +
				"                    t.taskData.processInstanceId, " +
				"                    t.taskData.parentId, " +
				"                    t.taskData.deploymentId, " +
				"                    t.taskData.skipable ) " +
				"            from " +
				"                TaskImpl t " +
				"                join t.peopleAssignments.potentialOwners potentialOwners " +
				"            where " +
				"                t.archived = 0 and " +
				"                (t.taskData.actualOwner.id = :userId or t.taskData.actualOwner is null) and " +
				"                t.taskData.status in (:status) and " +
				"                (potentialOwners.id  = :userId or potentialOwners.id in (:groupIds)) and " +
				"                (:userId not member of t.peopleAssignments.excludedOwners) and " +
				"                (t.taskData.processInstanceId NOT IN (:notInProcessInstanceIds)) " +
				"            order by t.id DESC";

		List<TaskSummary> tasks = commandService.execute(new QueryStringCommand<>(query, params));
		logger.debug("Found {} tasks for user '{}' assigned as potential owner", tasks.size(), userId);
		TaskSummaryList result = convertToTaskSummaryList(tasks);
		return result;
	}

	private String getUser(String queryParamUser) {
		if (bypassAuthUser) {
			return queryParamUser;
		}
		return identityProvider.getName();
	}

	private void applyQueryContext(Map<String, Object> params, QueryContext queryContext) {
		if (queryContext != null) {
			params.put("firstResult", queryContext.getOffset());
			params.put("maxResults", queryContext.getCount());
		}
	}
}
