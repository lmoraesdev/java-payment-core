package com.lmoraesdev.payment.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DomainPurity")
class DomainPurityArchTest {

    @Test
    @DisplayName("domain/** não pode depender de Spring nem de jakarta.persistence")
    void domainMustNotDependOnSpringOrJpa() {
        JavaClasses importedClasses =
                new ClassFileImporter().importPackages("com.lmoraesdev.payment");

        ArchRule rule =
                noClasses()
                        .that()
                        .resideInAPackage("..domain..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage("org.springframework..", "jakarta.persistence..");

        rule.check(importedClasses);
    }
}
