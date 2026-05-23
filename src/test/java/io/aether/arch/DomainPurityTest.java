package io.aether.arch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "io.aether", importOptions = ImportOption.DoNotIncludeTests.class)
class DomainPurityTest {

    @ArchTest
    static final ArchRule domain_has_no_jakarta_imports =
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("jakarta..");

    @ArchTest
    static final ArchRule domain_has_no_spring_data_imports =
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework.data..");

    @ArchTest
    static final ArchRule domain_has_no_spring_integration_imports =
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework.integration..");

    @ArchTest
    static final ArchRule domain_has_no_spring_web_imports =
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework.web..");
}
