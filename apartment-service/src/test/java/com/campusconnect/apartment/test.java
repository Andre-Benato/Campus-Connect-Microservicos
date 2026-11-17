package com.campusconnect.apartment;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.GeneralCodingRules;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
@AnalyzeClasses(packages = "com.campusconnect.apartment", importOptions = ImportOption.DoNotIncludeTests.class)
public class test { // ignora arquivos de teste
    @ArchTest // 1. Regra Pronta: Proíbe System.out.println (força uso de SLF4J, Logback ou Log4j)
    static final ArchRule nao_usar_system_out =
            GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
    @ArchTest // 2. Regra Pronta: Proíbe injeção por campo (@Autowired em atributos)
    static final ArchRule nao_usar_injecao_por_campo =
            GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
    @ArchTest // 3. Regra Pronta: Proíbe lançar exceções genéricas (ex: throw new Exception /Throwable)
    static final ArchRule nao_lancar_excecao_generica =
            GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;
    @ArchTest // 4. Regra Customizada Simples: Controllers não chamam Repositories diretamente
    static final ArchRule controllers_nao_acessam_repositorios =
            noClasses().that().resideInAPackage("..controller..")
                    .should().dependOnClassesThat().resideInAPackage("..repository..");
}