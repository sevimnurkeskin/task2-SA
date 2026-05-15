///////////////////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code and other text files for adherence to a set of rules.
// Copyright (C) 2001-2026 the original author or authors.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
///////////////////////////////////////////////////////////////////////////////////////////////

package com.puppycrawl.tools.checkstyle.pipeline;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

/**
 * ArchUnit tests that enforce the pipeline architectural constraints for the
 * Metrics and Size Violations slice of Checkstyle.
 *
 * <p>These tests verify three key invariants:</p>
 * <ol>
 *   <li>TreeWalker is fully decoupled from metrics/sizes check classes — it may
 *       only interact with them through {@link CheckPipeline}.</li>
 *   <li>Pipeline classes have no dependency on check packages outside the
 *       slice (metrics and sizes).</li>
 *   <li>Every concrete filter class in the pipeline subpackages implements
 *       {@link CheckFilter}.</li>
 * </ol>
 */
public class ArchitectureTest {

    private static final String CHECKSTYLE_ROOT =
            "com.puppycrawl.tools.checkstyle";

    /** Test 1 ─ Checks that TreeWalker is decoupled from the slice check classes. */
    @Test
    public void treeWalkerShouldNotDirectlyDependOnMetricsOrSizeChecks() {
        final JavaClasses allClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(CHECKSTYLE_ROOT);

        /*
         * In the refactored architecture TreeWalker must NOT import any concrete
         * class from checks.metrics or checks.sizes.  It discovers whether a check
         * belongs to the pipeline exclusively through
         * CheckPipeline.isPipelineManaged(), keeping it decoupled from the slice.
         */
        final ArchRule rule = noClasses()
                .that().haveSimpleName("TreeWalker")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        CHECKSTYLE_ROOT + ".checks.metrics..",
                        CHECKSTYLE_ROOT + ".checks.sizes.."
                );

        rule.check(allClasses);
    }

    /** Test 2 ─ Checks that pipeline classes have no dependency on non-slice check packages. */
    @Test
    public void pipelineClassesShouldNotDependOnNonSliceCheckPackages() {
        final JavaClasses allClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(CHECKSTYLE_ROOT);

        /*
         * Pipeline classes (orchestrator, filters, data carriers) must only be
         * aware of checks.metrics and checks.sizes — the architectural slice.
         * Any dependency on other check packages (annotation, blocks, coding, …)
         * would mean the pipeline has leaked outside its boundary.
         */
        final ArchRule rule = noClasses()
                .that().resideInAPackage(CHECKSTYLE_ROOT + ".pipeline..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        CHECKSTYLE_ROOT + ".checks.annotation..",
                        CHECKSTYLE_ROOT + ".checks.blocks..",
                        CHECKSTYLE_ROOT + ".checks.coding..",
                        CHECKSTYLE_ROOT + ".checks.design..",
                        CHECKSTYLE_ROOT + ".checks.header..",
                        CHECKSTYLE_ROOT + ".checks.imports..",
                        CHECKSTYLE_ROOT + ".checks.indentation..",
                        CHECKSTYLE_ROOT + ".checks.javadoc..",
                        CHECKSTYLE_ROOT + ".checks.modifier..",
                        CHECKSTYLE_ROOT + ".checks.naming..",
                        CHECKSTYLE_ROOT + ".checks.regexp..",
                        CHECKSTYLE_ROOT + ".checks.whitespace.."
                );
        rule.check(allClasses);
    }

    /** Test 3 ─ Checks that every concrete filter in the pipeline subpackages implements CheckFilter. */
    @Test
    public void allConcreteFiltersShouldImplementCheckFilter() {
        final JavaClasses pipelineClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(CHECKSTYLE_ROOT + ".pipeline");

        /*
         * Every concrete (non-abstract, non-interface) class that lives in the
         * metrics or sizes subpackage is a filter stage and must satisfy the
         * CheckFilter contract so the pipeline can invoke it uniformly.
         */
        final ArchRule rule = classes()
                .that().resideInAnyPackage(
                        CHECKSTYLE_ROOT + ".pipeline.metrics",
                        CHECKSTYLE_ROOT + ".pipeline.sizes"
                )
                .and().areNotInterfaces()
                .and().doNotHaveModifier(JavaModifier.ABSTRACT)
                .should().implement(CheckFilter.class);

        rule.check(pipelineClasses);
    }

}
