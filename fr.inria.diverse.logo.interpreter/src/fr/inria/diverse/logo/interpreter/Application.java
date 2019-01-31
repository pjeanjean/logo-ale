package fr.inria.diverse.logo.interpreter;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.EMFCompare;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.compare.diff.DefaultDiffEngine;
import org.eclipse.emf.compare.diff.DiffBuilder;
import org.eclipse.emf.compare.diff.IDiffEngine;
import org.eclipse.emf.compare.diff.IDiffProcessor;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.DifferenceSource;
import org.eclipse.emf.compare.merge.BatchMerger;
import org.eclipse.emf.compare.merge.IBatchMerger;
import org.eclipse.emf.compare.merge.IMerger;
import org.eclipse.emf.compare.scope.DefaultComparisonScope;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecoretools.ale.ALEInterpreter;
import org.eclipse.emf.ecoretools.ale.core.parser.Dsl;
import org.eclipse.emf.ecoretools.ale.core.parser.DslBuilder;
import org.eclipse.emf.ecoretools.ale.core.parser.visitor.ParseResult;
import org.eclipse.emf.ecoretools.ale.implementation.ExtendedClass;
import org.eclipse.emf.ecoretools.ale.implementation.Method;
import org.eclipse.emf.ecoretools.ale.implementation.ModelUnit;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.xtext.resource.XtextResourceSet;
import com.google.inject.Injector;
import fr.inria.diverse.logo.LogoPackage;
import fr.inria.diverse.logo.xtext.LogoStandaloneSetup;

public class Application implements IApplication {

	@Override
	public Object start(IApplicationContext context) throws Exception {
		ALEInterpreter aleInterpreter = new ALEInterpreter();
		Dsl environment = new Dsl(Arrays.asList(URI.createFileURI("/home/rhiobet/logo-ale/fr.inria.diverse.logo.model/model/logo.ecore").toString()),
				Arrays.asList("/home/rhiobet/logo-ale/fr.inria.diverse.logo.model/model/logo.ale"));
		
		Injector injector = new LogoStandaloneSetup().createInjectorAndDoEMFRegistration();
		XtextResourceSet resourceSet = injector.getInstance(XtextResourceSet.class);
		resourceSet.getPackageRegistry().put("http://www.example.org/logo", LogoPackage.eINSTANCE);
		
		Resource resource = resourceSet.createResource(URI.createURI("dummy:/read.logo"));
		resource.load(new ByteArrayInputStream(new byte[] {}), resourceSet.getLoadOptions());
		EObject caller = resource.getContents().get(0);
		
		List<ParseResult<ModelUnit>> parsedSemantics = new DslBuilder(aleInterpreter.getQueryEnvironment(),
				resourceSet).parse(environment);
		
		List<ExtendedClass> classes = parsedSemantics.stream().map(p -> p.getRoot()).filter(e -> e != null)
				.flatMap(unit -> unit.getClassExtensions().stream())
				.filter(ext -> ext.getBaseClass().getName().equals(caller.eClass().getName()))
				.collect(Collectors.toList());
		
		Optional<Method> init = classes.stream().flatMap(cls -> cls.getMethods().stream())
				.filter(mtd -> mtd.getTags().contains("init")).findFirst();

		List<ExtendedClass> classExtends = classes;
		while (!init.isPresent() && classExtends.size() > 0) {
			classExtends = classExtends.stream().flatMap(cls -> cls.getExtends().stream())
					.collect(Collectors.toList());
			if (!init.isPresent()) {
				init = classExtends.stream().flatMap(cls -> cls.getMethods().stream())
						.filter(mtd -> mtd.getTags().contains("init")).findFirst();
			}
		}
		
		aleInterpreter.eval(caller, init.get(), Arrays.asList(), parsedSemantics);
				
		
		Scanner scanner = new Scanner(System.in);
		String read = "";
		while (true) {
			System.out.print("~ ");
			System.out.flush();
				
			if ((read = scanner.nextLine()).equals("exit")) {
				break;
			}
		
			XtextResourceSet newResourceSet = injector.getInstance(XtextResourceSet.class);
			newResourceSet.setPackageRegistry(resourceSet.getPackageRegistry());	
			
			Resource newResource = newResourceSet.createResource(URI.createURI("dummy:/read.logo"));
			newResource.load(new ByteArrayInputStream(("~ " + read).getBytes()), newResourceSet.getLoadOptions());
			
			if (newResource.getErrors().size() > 0) {
				System.err.println(newResource.getErrors());
				continue;
			}
			
			EObject newCaller = newResource.getContents().get(0);
			
			// Needed to keep the objects referenced by other changed objects
			// TODO : Check that the referencing object is indeed changed
			IDiffProcessor customDiffProcessor = new DiffBuilder() {
				@Override
				public void referenceChange(Match match, EReference reference, EObject value, DifferenceKind kind,
						DifferenceSource source) {
					if (kind != DifferenceKind.ADD) {
						super.referenceChange(match, reference, value, kind, source);
					}
				}
			};
			IDiffEngine diffEngine = new DefaultDiffEngine(customDiffProcessor);
			
			IComparisonScope scope = new DefaultComparisonScope(resourceSet, newResourceSet, null);
			Comparison comparison = EMFCompare.builder().setDiffEngine(diffEngine).build().compare(scope);
			
			List<Diff> differences = comparison.getDifferences();
			
			IMerger.Registry mergerRegistry = IMerger.RegistryImpl.createStandaloneInstance();
			IBatchMerger merger = new BatchMerger(mergerRegistry);
			
			merger.copyAllRightToLeft(differences, new BasicMonitor());
		
			
			classes = parsedSemantics.stream().map(p -> p.getRoot()).filter(e -> e != null)
					.flatMap(unit -> unit.getClassExtensions().stream())
					.filter(ext -> ext.getBaseClass().getName().equals(caller.eClass().getName()))
					.collect(Collectors.toList());
			Optional<Method> main = classes.stream().flatMap(cls -> cls.getMethods().stream())
					.filter(mtd -> mtd.getTags().contains("main")).findFirst();
			
			classExtends = classes;
			while (!main.isPresent() && classExtends.size() > 0) {
				classExtends = classExtends.stream().flatMap(cls -> cls.getExtends().stream())
						.collect(Collectors.toList());
				if (!main.isPresent()) {
				main = classExtends.stream().flatMap(cls -> cls.getMethods().stream())
						.filter(mtd -> mtd.getTags().contains("main")).findFirst();
				}
			}
		
			aleInterpreter.getCurrentEngine().eval(caller, main.get(), Arrays.asList());
		}
		
		return null;
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

}
