package fr.inria.diverse.logo.xtext.server

import com.google.inject.AbstractModule
import java.util.concurrent.ExecutorService
import org.eclipse.lsp4j.services.LanguageServer
import org.eclipse.xtext.ide.ExecutorServiceProvider
import org.eclipse.xtext.resource.IResourceServiceProvider
import org.eclipse.xtext.resource.ResourceServiceProviderServiceLoader
import org.eclipse.xtext.ide.server.IWorkspaceConfigFactory
import org.eclipse.xtext.ide.server.ProjectWorkspaceConfigFactory
import org.eclipse.xtext.ide.server.DefaultProjectDescriptionFactory
import org.eclipse.xtext.ide.server.IProjectDescriptionFactory
import org.eclipse.xtext.ide.server.LanguageServerImpl


// This will probably be useless when the server becomes standalone
class ServerModule extends AbstractModule {
    
    override protected configure() {
		binder.bind(ExecutorService).toProvider(ExecutorServiceProvider)
		
    	bind(LanguageServer).to(LanguageServerImpl)
        bind(IResourceServiceProvider.Registry).toProvider(ResourceServiceProviderServiceLoader)
        bind(IWorkspaceConfigFactory).to(ProjectWorkspaceConfigFactory)
        bind(IProjectDescriptionFactory).to(DefaultProjectDescriptionFactory)
    }
    
}	