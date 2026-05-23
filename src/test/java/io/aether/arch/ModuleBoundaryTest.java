package io.aether.arch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "io.aether", importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleBoundaryTest {

    @ArchTest
    static final ArchRule ingestion_does_not_depend_on_processing =
        noClasses().that().resideInAPackage("..ingestion..")
            .should().dependOnClassesThat().resideInAPackage("..processing..");

    @ArchTest
    static final ArchRule ingestion_does_not_depend_on_anomaly =
        noClasses().that().resideInAPackage("..ingestion..")
            .should().dependOnClassesThat().resideInAPackage("..anomaly..");

    @ArchTest
    static final ArchRule ingestion_does_not_depend_on_forecast =
        noClasses().that().resideInAPackage("..ingestion..")
            .should().dependOnClassesThat().resideInAPackage("..forecast..");

    @ArchTest
    static final ArchRule ingestion_does_not_depend_on_insight =
        noClasses().that().resideInAPackage("..ingestion..")
            .should().dependOnClassesThat().resideInAPackage("..insight..");

    @ArchTest
    static final ArchRule processing_does_not_depend_on_ingestion =
        noClasses().that().resideInAPackage("..processing..")
            .should().dependOnClassesThat().resideInAPackage("..ingestion..");

    @ArchTest
    static final ArchRule processing_does_not_depend_on_anomaly =
        noClasses().that().resideInAPackage("..processing..")
            .should().dependOnClassesThat().resideInAPackage("..anomaly..");

    @ArchTest
    static final ArchRule processing_does_not_depend_on_forecast =
        noClasses().that().resideInAPackage("..processing..")
            .should().dependOnClassesThat().resideInAPackage("..forecast..");

    @ArchTest
    static final ArchRule processing_does_not_depend_on_insight =
        noClasses().that().resideInAPackage("..processing..")
            .should().dependOnClassesThat().resideInAPackage("..insight..");

    @ArchTest
    static final ArchRule anomaly_does_not_depend_on_ingestion =
        noClasses().that().resideInAPackage("..anomaly..")
            .should().dependOnClassesThat().resideInAPackage("..ingestion..");

    @ArchTest
    static final ArchRule anomaly_does_not_depend_on_processing =
        noClasses().that().resideInAPackage("..anomaly..")
            .should().dependOnClassesThat().resideInAPackage("..processing..");

    @ArchTest
    static final ArchRule anomaly_does_not_depend_on_forecast =
        noClasses().that().resideInAPackage("..anomaly..")
            .should().dependOnClassesThat().resideInAPackage("..forecast..");

    @ArchTest
    static final ArchRule anomaly_does_not_depend_on_insight =
        noClasses().that().resideInAPackage("..anomaly..")
            .should().dependOnClassesThat().resideInAPackage("..insight..");

    @ArchTest
    static final ArchRule forecast_does_not_depend_on_ingestion =
        noClasses().that().resideInAPackage("..forecast..")
            .should().dependOnClassesThat().resideInAPackage("..ingestion..");

    @ArchTest
    static final ArchRule forecast_does_not_depend_on_processing =
        noClasses().that().resideInAPackage("..forecast..")
            .should().dependOnClassesThat().resideInAPackage("..processing..");

    @ArchTest
    static final ArchRule forecast_does_not_depend_on_anomaly =
        noClasses().that().resideInAPackage("..forecast..")
            .should().dependOnClassesThat().resideInAPackage("..anomaly..");

    @ArchTest
    static final ArchRule forecast_does_not_depend_on_insight =
        noClasses().that().resideInAPackage("..forecast..")
            .should().dependOnClassesThat().resideInAPackage("..insight..");

    @ArchTest
    static final ArchRule insight_does_not_depend_on_ingestion =
        noClasses().that().resideInAPackage("..insight..")
            .should().dependOnClassesThat().resideInAPackage("..ingestion..");

    @ArchTest
    static final ArchRule insight_does_not_depend_on_anomaly =
        noClasses().that().resideInAPackage("..insight..")
            .should().dependOnClassesThat().resideInAPackage("..anomaly..");

    @ArchTest
    static final ArchRule insight_does_not_depend_on_forecast =
        noClasses().that().resideInAPackage("..insight..")
            .should().dependOnClassesThat().resideInAPackage("..forecast..");
}
