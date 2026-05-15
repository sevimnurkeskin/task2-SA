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

import java.io.File;
import java.util.SortedSet;
import java.util.regex.Pattern;

import com.puppycrawl.tools.checkstyle.FileStatefulCheck;
import com.puppycrawl.tools.checkstyle.api.AbstractFileSetCheck;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.FileText;
import com.puppycrawl.tools.checkstyle.api.Violation;
import com.puppycrawl.tools.checkstyle.checks.sizes.FileLengthCheck;
import com.puppycrawl.tools.checkstyle.checks.sizes.LineLengthCheck;
import com.puppycrawl.tools.checkstyle.pipeline.sizes.FileLengthFilter;
import com.puppycrawl.tools.checkstyle.pipeline.sizes.LineLengthFilter;

/**
 * An {@link AbstractFileSetCheck} that routes {@link FileLengthCheck} and
 * {@link LineLengthCheck} exclusively through a {@link CheckPipeline} of
 * file-level filters.
 *
 * <p>Register this check with {@code Checker} in place of the two individual
 * file-level checks to activate the exclusive file-level pipeline.  Configuration
 * for the underlying checks is exposed via typed setter methods on this class.</p>
 *
 * <p>Example Checkstyle XML configuration:</p>
 * <pre>{@code
 * <module name="PipelineFileSetCheck">
 *   <property name="fileMax" value="1500"/>
 *   <property name="lineMax" value="120"/>
 * </module>
 * }</pre>
 */
@FileStatefulCheck
public final class PipelineFileSetCheck extends AbstractFileSetCheck {

    private final CheckPipeline pipeline = new CheckPipeline();
    private final FileLengthCheck fileLengthCheck = new FileLengthCheck();
    private final LineLengthCheck lineLengthCheck = new LineLengthCheck();

    /**
     * Creates a new {@code PipelineFileSetCheck} and wires up the default
     * file-level filter set.  Both delegate checks are configured to process
     * {@code .java} files, mirroring the extension that {@code TreeWalker} uses.
     */
    public PipelineFileSetCheck() {
        this(true);
    }

    /**
     * Creates a new {@code PipelineFileSetCheck}.
     *
     * @param withDefaultDelegates if {@code true}, creates and registers default
     *      {@link FileLengthCheck} and {@link LineLengthCheck} delegates
     */
    public PipelineFileSetCheck(boolean withDefaultDelegates) {
        if (withDefaultDelegates) {
            fileLengthCheck.setFileExtensions("java");
            lineLengthCheck.setFileExtensions("java");
            pipeline.addFileFilter(new FileLengthFilter(fileLengthCheck));
            pipeline.addFileFilter(new LineLengthFilter(lineLengthCheck));
        }
    }

    /**
     * Registers an already-configured file-level size check in this pipeline.
     *
     * @param check configured check instance to route through this pipeline
     * @throws IllegalArgumentException when the check type is unsupported
     */
    public void addConfiguredSizeCheck(AbstractFileSetCheck check) {
        if (check instanceof FileLengthCheck cast) {
            pipeline.addFileFilter(new FileLengthFilter(cast));
        }
        else if (check instanceof LineLengthCheck cast) {
            pipeline.addFileFilter(new LineLengthFilter(cast));
        }
        else {
            throw new IllegalArgumentException(
                "Unsupported file-level size check: " + check.getClass().getName());
        }
    }

    @Override
    protected void processFiltered(File file, FileText fileText) throws CheckstyleException {
        final SortedSet<Violation> found =
            pipeline.processFile(file, fileText, getFileContents());
        addViolations(found);
    }

    @Override
    public void destroy() {
        pipeline.destroy();
        super.destroy();
    }

    // ── Delegate configuration setters ─────────────────────────────────────

    /**
     * Sets the maximum number of lines allowed in a file.
     * Delegated to the internal {@link FileLengthCheck}.
     *
     * @param max the maximum file length
     */
    public void setFileMax(int max) {
        fileLengthCheck.setMax(max);
    }

    /**
     * Sets the maximum line length allowed.
     * Delegated to the internal {@link LineLengthCheck}.
     *
     * @param max the maximum line length
     */
    public void setLineMax(int max) {
        lineLengthCheck.setMax(max);
    }

    /**
     * Sets the pattern for lines to ignore during line-length checking.
     * Delegated to the internal {@link LineLengthCheck}.
     *
     * @param pattern the ignore pattern
     */
    public void setLineIgnorePattern(Pattern pattern) {
        lineLengthCheck.setIgnorePattern(pattern);
    }

}
