package org.redhat.rhpam.tests.extension;

import org.jbpm.services.api.AdvanceRuntimeDataService;
import org.jbpm.services.api.DefinitionService;
import org.jbpm.services.api.ProcessService;
import org.jbpm.services.api.RuntimeDataService;
import org.jbpm.services.api.UserTaskService;
import org.jbpm.services.api.admin.ProcessInstanceAdminService;
import org.jbpm.services.api.admin.ProcessInstanceMigrationService;
import org.jbpm.services.api.admin.UserTaskAdminService;
import org.jbpm.services.api.query.QueryService;
import org.kie.api.executor.ExecutorService;
import org.kie.server.remote.rest.jbpm.DefinitionResource;
import org.kie.server.remote.rest.jbpm.DocumentResource;
import org.kie.server.remote.rest.jbpm.ExecutorResource;
import org.kie.server.remote.rest.jbpm.ProcessResource;
import org.kie.server.remote.rest.jbpm.QueryDataResource;
import org.kie.server.remote.rest.jbpm.RuntimeDataResource;
import org.kie.server.remote.rest.jbpm.UserTaskResource;
import org.kie.server.remote.rest.jbpm.admin.ProcessAdminResource;
import org.kie.server.remote.rest.jbpm.admin.UserTaskAdminResource;
import org.kie.server.services.api.KieServerApplicationComponentsService;
import org.kie.server.services.api.KieServerRegistry;
import org.kie.server.services.api.SupportedTransports;
import org.kie.server.services.jbpm.DefinitionServiceBase;
import org.kie.server.services.jbpm.DocumentServiceBase;
import org.kie.server.services.jbpm.ExecutorServiceBase;
import org.kie.server.services.jbpm.ProcessServiceBase;
import org.kie.server.services.jbpm.QueryDataServiceBase;
import org.kie.server.services.jbpm.RuntimeDataServiceBase;
import org.kie.server.services.jbpm.UserTaskServiceBase;
import org.kie.server.services.jbpm.admin.ProcessAdminServiceBase;
import org.kie.server.services.jbpm.admin.UserTaskAdminServiceBase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CustomJbpmKieServerAppComponentService implements KieServerApplicationComponentsService {

	private static final String OWNER_EXTENSION = "jBPM";

	@Override
	public Collection<Object> getAppComponents( String extension, SupportedTransports type, Object... services ) {
		// skip calls from other than owning extension
		if ( !OWNER_EXTENSION.equals(extension) ) {
			return Collections.emptyList();
		}

		ProcessService processService = null;
		RuntimeDataService runtimeDataService = null;
		DefinitionService definitionService = null;
		UserTaskService userTaskService = null;
		ExecutorService executorService = null;
		QueryService queryService = null;
		ProcessInstanceMigrationService processInstanceMigrationService = null;
		ProcessInstanceAdminService processInstanceAdminService = null;
		UserTaskAdminService userTaskAdminService = null;
		KieServerRegistry context = null;
		AdvanceRuntimeDataService advanceRuntimeDataService = null;

		for( Object object : services ) {
			// in case given service is null (meaning was not configured) continue with next one
			if (object == null) {
				continue;
			}
			if( ProcessService.class.isAssignableFrom(object.getClass()) ) {
				processService = (ProcessService) object;
				continue;
			} else if( RuntimeDataService.class.isAssignableFrom(object.getClass()) ) {
				runtimeDataService = (RuntimeDataService) object;
				continue;
			} else if( DefinitionService.class.isAssignableFrom(object.getClass()) ) {
				definitionService = (DefinitionService) object;
				continue;
			} else if( UserTaskService.class.isAssignableFrom(object.getClass()) ) {
				userTaskService = (UserTaskService) object;
				continue;
			} else if( ExecutorService.class.isAssignableFrom(object.getClass()) ) {
				executorService = (ExecutorService) object;
				continue;
			} else if( QueryService.class.isAssignableFrom(object.getClass()) ) {
				queryService = (QueryService) object;
				continue;
			} else if( ProcessInstanceMigrationService.class.isAssignableFrom(object.getClass()) ) {
				processInstanceMigrationService = (ProcessInstanceMigrationService) object;
				continue;
			} else if( ProcessInstanceAdminService.class.isAssignableFrom(object.getClass()) ) {
				processInstanceAdminService = (ProcessInstanceAdminService) object;
				continue;
			} else if( UserTaskAdminService.class.isAssignableFrom(object.getClass()) ) {
				userTaskAdminService = (UserTaskAdminService) object;
				continue;
			} else if( KieServerRegistry.class.isAssignableFrom(object.getClass()) ) {
				context = (KieServerRegistry) object;
				continue;
			} else if (AdvanceRuntimeDataService.class.isAssignableFrom(object.getClass())) {
				advanceRuntimeDataService = (AdvanceRuntimeDataService) object;
				continue;
			}
		}

		List<Object> components = new ArrayList<>();
		DefinitionServiceBase definitionServiceBase = new DefinitionServiceBase(definitionService, context);
		ProcessServiceBase processServiceBase = new ProcessServiceBase(processService, definitionService, runtimeDataService, context);
		UserTaskServiceBase userTaskServiceBase = new UserTaskServiceBase(userTaskService, context);
		RuntimeDataServiceBase runtimeDataServiceBase = new RuntimeDataServiceBase(runtimeDataService, advanceRuntimeDataService, context);
		ExecutorServiceBase executorServiceBase = new ExecutorServiceBase(executorService, context);
		QueryDataServiceBase queryDataServiceBase = new QueryDataServiceBase(queryService, context);
		DocumentServiceBase documentServiceBase = new DocumentServiceBase(context);
		ProcessAdminServiceBase processAdminServiceBase = new ProcessAdminServiceBase(processInstanceMigrationService, processInstanceAdminService, context);
		UserTaskAdminServiceBase userTaskAdminServiceBase = new UserTaskAdminServiceBase(userTaskAdminService, context);

		components.add(new ProcessResource(processServiceBase, runtimeDataServiceBase, context));
		components.add(new RuntimeDataResource(runtimeDataServiceBase, context));
		components.add(new DefinitionResource(definitionServiceBase, context));
		components.add(new UserTaskResource(userTaskServiceBase, runtimeDataServiceBase, context));
		components.add(new ExecutorResource(executorServiceBase, context));
		components.add(new QueryDataResource(queryDataServiceBase, context));
		components.add(new DocumentResource(documentServiceBase, context));
		components.add(new ProcessAdminResource(processAdminServiceBase, context));
		components.add(new UserTaskAdminResource(userTaskAdminServiceBase, context));

		components.add(new CustomResource(runtimeDataServiceBase, context));

		return components;
	}

}
