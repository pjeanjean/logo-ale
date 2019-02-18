package fr.inria.diverse.logo.xtext.server

import org.eclipse.xtext.ide.server.LanguageServerImpl
import org.eclipse.lsp4j.InitializeParams
import fr.inria.diverse.logo.LogoPackage
import fr.inria.diverse.logo.xtext.ide.LogoIdeSetup
import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.InitializedParams
import org.eclipse.xtext.resource.IResourceServiceProvider
import java.net.ServerSocket
import com.google.inject.Injector
import java.net.Socket

class LogoLspServer {

	static LogoPackage pkg = null
	static Injector injector = null
	
	LanguageServerImpl server = null
	Socket clientSocket
	
	def static void main(String[] args) {
		System.out.println("Starting server on " + args.get(0) + "...")
		new LogoLspServer().runServer(Integer.parseInt(args.get(0)))
	}
	
	def runServer(int port) {
		if (pkg === null) {
			pkg = LogoPackage.eINSTANCE
		}
		if (injector === null) {
			injector = new LogoIdeSetup().createInjectorAndDoEMFRegistration()
			// Keeping `ecl` leads to a NullPointerException when getting
			//   the resourceServiceProvider for each language
			injector.getInstance(IResourceServiceProvider.Registry).extensionToFactoryMap.remove("ecl")
		}
		this.server = injector.getInstance(LanguageServerImpl)
		
		val serverSocket = new ServerSocket(port)
		this.clientSocket = serverSocket.accept
		val launcher = LSPLauncher.createServerLauncher(server, this.clientSocket.inputStream,
			this.clientSocket.outputStream)
		this.server.connect(launcher.remoteProxy)
		launcher.startListening()
		
		this.server.initialize(new InitializeParams)
		this.server.initialized(new InitializedParams)
	}
	
	def stopServer() {
		this.server.shutdown
		this.server = null
	}
}