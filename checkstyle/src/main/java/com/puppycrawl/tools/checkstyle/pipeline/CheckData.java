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
import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FileContents;
import com.puppycrawl.tools.checkstyle.api.FileText;
import com.puppycrawl.tools.checkstyle.api.Violation;

/**
 * Carries event data through the check pipeline.
 *
 * <p>A {@code CheckData} instance is created for each pipeline event
 * ({@link EventType}) and passed through every {@link CheckFilter} in order.
 * Violations produced by filters accumulate in the mutable {@code violations}
 * set; all other fields are immutable after construction.</p>
 *
 * <p>Use the static factory methods to create instances for each event type.</p>
 */
public final class CheckData {

    /**
     * The type of pipeline event carried by a {@link CheckData} instance.
     */
    public enum EventType {
        /** Signals the start of AST processing for a new file. */
        BEGIN_TREE,
        /** Signals entry into an AST node during the depth-first walk. */
        VISIT_TOKEN,
        /** Signals departure from an AST node during the depth-first walk. */
        LEAVE_TOKEN,
        /**
         * Signals the end of AST processing for a file.
         * Filters drain their accumulated violations into this event's violation set.
         */
        FINISH_TREE,
        /** Signals file-level processing used by non-AST checks. */
        PROCESS_FILE,
    }

    private final EventType eventType;
    /** Non-null for AST events (BEGIN_TREE, VISIT_TOKEN, LEAVE_TOKEN, FINISH_TREE). */
    private final DetailAST ast;
    /** Always non-null; provides line content for column-number reporting. */
    private final FileContents fileContents;
    /** Non-null only for PROCESS_FILE events. */
    private final File file;
    /** Non-null only for PROCESS_FILE events. */
    private final FileText fileText;
    /** Mutable accumulator: violations added by filters across the pipeline. */
    private final SortedSet<Violation> violations = new TreeSet<>();

    private CheckData(EventType eventType, DetailAST ast, FileContents fileContents,
                      File file, FileText fileText) {
        this.eventType = eventType;
        this.ast = ast;
        this.fileContents = fileContents;
        this.file = file;
        this.fileText = fileText;
    }

    /**
     * Creates a BEGIN_TREE event.
     *
     * @param rootAst      the root of the parsed AST
     * @param fileContents the file contents for line reporting
     * @return the new event data
     */
    public static CheckData forBeginTree(DetailAST rootAst, FileContents fileContents) {
        return new CheckData(EventType.BEGIN_TREE, rootAst, fileContents, null, null);
    }

    /**
     * Creates a VISIT_TOKEN event.
     *
     * @param ast          the AST node being entered
     * @param fileContents the file contents for line reporting
     * @return the new event data
     */
    public static CheckData forVisitToken(DetailAST ast, FileContents fileContents) {
        return new CheckData(EventType.VISIT_TOKEN, ast, fileContents, null, null);
    }

    /**
     * Creates a LEAVE_TOKEN event.
     *
     * @param ast          the AST node being exited
     * @param fileContents the file contents for line reporting
     * @return the new event data
     */
    public static CheckData forLeaveToken(DetailAST ast, FileContents fileContents) {
        return new CheckData(EventType.LEAVE_TOKEN, ast, fileContents, null, null);
    }

    /**
     * Creates a FINISH_TREE event.
     * Violations are collected into this object by each filter during its FINISH_TREE handling.
     *
     * @param rootAst      the root of the parsed AST
     * @param fileContents the file contents for line reporting
     * @return the new event data
     */
    public static CheckData forFinishTree(DetailAST rootAst, FileContents fileContents) {
        return new CheckData(EventType.FINISH_TREE, rootAst, fileContents, null, null);
    }

    /**
     * Creates a PROCESS_FILE event for file-level checks.
     *
     * @param file         the file being processed
     * @param fileText     the raw file text
     * @param fileContents the file contents wrapper
     * @return the new event data
     */
    public static CheckData forProcessFile(File file, FileText fileText,
                                            FileContents fileContents) {
        return new CheckData(EventType.PROCESS_FILE, null, fileContents, file, fileText);
    }

    /**
     * Returns the event type.
     *
     * @return the event type
     */
    public EventType getEventType() {
        return eventType;
    }

    /**
     * Returns the AST node associated with this event.
     * Non-null for AST events; {@code null} for PROCESS_FILE events.
     *
     * @return the AST node, or {@code null}
     */
    public DetailAST getAst() {
        return ast;
    }

    /**
     * Returns the file contents, used by checks for line/column reporting.
     *
     * @return the file contents (never {@code null})
     */
    public FileContents getFileContents() {
        return fileContents;
    }

    /**
     * Returns the file being processed.
     * Non-null only for PROCESS_FILE events.
     *
     * @return the file, or {@code null}
     */
    public File getFile() {
        return file;
    }

    /**
     * Returns the raw file text.
     * Non-null only for PROCESS_FILE events.
     *
     * @return the file text, or {@code null}
     */
    public FileText getFileText() {
        return fileText;
    }

    /**
     * Returns an unmodifiable view of the violations accumulated so far.
     *
     * @return unmodifiable sorted set of violations
     */
    public SortedSet<Violation> getViolations() {
        return Collections.unmodifiableSortedSet(violations);
    }

    /**
     * Appends violations produced by a filter into this event's violation set.
     *
     * @param vs the violations to add
     */
    public void addViolations(Collection<Violation> vs) {
        violations.addAll(vs);
    }

}
