package com.campusconnect;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Arquitetura Hexagonal (Ports & Adapters) do carona-service.
 *
 * <p>domain nao depende de application nem de infrastructure. Controllers nao acessam
 * repositories.
 */
@AnalyzeClasses(packages = "com.campusconnect", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainNaoDependeDeApplication =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..application..")
                    .because("dominio nao depende de casos de uso");

    @ArchTest
    static final ArchRule domainNaoDependeDeInfrastructure =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..infrastructure..")
                    .because("dominio nao depende de adapters");

    @ArchTest
    static final ArchRule controllersNaoDependemDeRepositories =
            noClasses()
                    .that()
                    .haveSimpleNameEndingWith("Controller")
                    .should()
                    .dependOnClassesThat()
                    .haveSimpleNameEndingWith("Repository")
                    .because("controllers devem delegar a application services");

    @ArchTest
    static final ArchRule servicesNaoDependemDeControllers =
            noClasses()
                    .that()
                    .haveSimpleNameEndingWith("Service")
                    .should()
                    .dependOnClassesThat()
                    .haveSimpleNameEndingWith("Controller")
                    .because("services nao devem depender da camada web");

    @ArchTest
    static final ArchRule repositoriesNaoDependemDeServices =
            noClasses()
                    .that()
                    .haveSimpleNameEndingWith("Repository")
                    .should()
                    .dependOnClassesThat()
                    .haveSimpleNameEndingWith("Service")
                    .because("repositories nao devem depender de services");

    @ArchTest
    static final ArchRule controllersDevemTerSufixoController =
            classes()
                    .that()
                    .areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                    .should()
                    .haveSimpleNameEndingWith("Controller");
}
