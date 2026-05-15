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

import static com.google.common.truth.Truth.assertWithMessage;

import org.junit.jupiter.api.Test;

import com.puppycrawl.tools.checkstyle.checks.metrics.BooleanExpressionComplexityCheck;
import com.puppycrawl.tools.checkstyle.checks.metrics.ClassDataAbstractionCouplingCheck;
import com.puppycrawl.tools.checkstyle.checks.metrics.ClassFanOutComplexityCheck;
import com.puppycrawl.tools.checkstyle.checks.metrics.CyclomaticComplexityCheck;
import com.puppycrawl.tools.checkstyle.checks.metrics.JavaNCSSCheck;
import com.puppycrawl.tools.checkstyle.checks.metrics.NPathComplexityCheck;
import com.puppycrawl.tools.checkstyle.checks.naming.AbbreviationAsWordInNameCheck;
import com.puppycrawl.tools.checkstyle.checks.sizes.AnonInnerLengthCheck;
import com.puppycrawl.tools.checkstyle.checks.sizes.ExecutableStatementCountCheck;
import com.puppycrawl.tools.checkstyle.checks.sizes.LambdaBodyLengthCheck;
import com.puppycrawl.tools.checkstyle.checks.sizes.MethodCountCheck;
import com.puppycrawl.tools.checkstyle.checks.sizes.MethodLengthCheck;
import com.puppycrawl.tools.checkstyle.checks.sizes.OuterTypeNumberCheck;
import com.puppycrawl.tools.checkstyle.checks.sizes.ParameterNumberCheck;
import com.puppycrawl.tools.checkstyle.checks.sizes.RecordComponentNumberCheck;

public class CheckPipelineManagedClassesTest {

    @Test
    public void testAllMetricsAndSizesAstChecksArePipelineManaged() {
        assertWithMessage("BooleanExpressionComplexityCheck should be pipeline-managed")
                .that(CheckPipeline.isPipelineManaged(new BooleanExpressionComplexityCheck()))
                .isTrue();
        assertWithMessage("ClassDataAbstractionCouplingCheck should be pipeline-managed")
                .that(CheckPipeline.isPipelineManaged(new ClassDataAbstractionCouplingCheck()))
                .isTrue();
        assertWithMessage("ClassFanOutComplexityCheck should be pipeline-managed")
                .that(CheckPipeline.isPipelineManaged(new ClassFanOutComplexityCheck()))
                .isTrue();
        assertWithMessage("CyclomaticComplexityCheck should be pipeline-managed")
                .that(CheckPipeline.isPipelineManaged(new CyclomaticComplexityCheck()))
                .isTrue();
        assertWithMessage("JavaNCSSCheck should be pipeline-managed")
                .that(CheckPipeline.isPipelineManaged(new JavaNCSSCheck()))
                .isTrue();
        assertWithMessage("NPathComplexityCheck should be pipeline-managed")
                .that(CheckPipeline.isPipelineManaged(new NPathComplexityCheck()))
                .isTrue();

        assertWithMessage("AnonInnerLengthCheck should be pipeline-managed")
                .that(CheckPipeline.isPipelineManaged(new AnonInnerLengthCheck()))
                .isTrue();
        assertWithMessage("ExecutableStatementCountCheck should be pipeline-managed")
                .that(CheckPipeline.isPipelineManaged(new ExecutableStatementCountCheck()))
                .isTrue();
        assertWithMessage("LambdaBodyLengthCheck should be pipeline-managed")
                .that(CheckPipeline.isPipelineManaged(new LambdaBodyLengthCheck()))
                .isTrue();
        assertWithMessage("MethodCountCheck should be pipeline-managed")
                .that(CheckPipeline.isPipelineManaged(new MethodCountCheck()))
                .isTrue();
        assertWithMessage("MethodLengthCheck should be pipeline-managed")
                .that(CheckPipeline.isPipelineManaged(new MethodLengthCheck()))
                .isTrue();
        assertWithMessage("OuterTypeNumberCheck should be pipeline-managed")
                .that(CheckPipeline.isPipelineManaged(new OuterTypeNumberCheck()))
                .isTrue();
        assertWithMessage("ParameterNumberCheck should be pipeline-managed")
                .that(CheckPipeline.isPipelineManaged(new ParameterNumberCheck()))
                .isTrue();
        assertWithMessage("RecordComponentNumberCheck should be pipeline-managed")
                .that(CheckPipeline.isPipelineManaged(new RecordComponentNumberCheck()))
                .isTrue();
    }

    @Test
    public void testNonSliceCheckIsNotPipelineManaged() {
        assertWithMessage("Non-slice checks must stay outside the pipeline")
                .that(CheckPipeline.isPipelineManaged(new AbbreviationAsWordInNameCheck()))
                .isFalse();
    }
}
