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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FileContents;
import com.puppycrawl.tools.checkstyle.api.FileText;
import com.puppycrawl.tools.checkstyle.api.Violation;
import com.puppycrawl.tools.checkstyle.checks.metrics.BooleanExpressionComplexityCheck;
import com.puppycrawl.tools.checkstyle.checks.metrics.ClassDataAbstractionCouplingCheck;
import com.puppycrawl.tools.checkstyle.checks.metrics.ClassFanOutComplexityCheck;
import com.puppycrawl.tools.checkstyle.checks.metrics.CyclomaticComplexityCheck;
import com.puppycrawl.tools.checkstyle.checks.metrics.JavaNCSSCheck;
import com.puppycrawl.tools.checkstyle.checks.metrics.NPathComplexityCheck;
import com.puppycrawl.tools.checkstyle.checks.sizes.AnonInnerLengthCheck;
import com.puppycrawl.tools.checkstyle.checks.sizes.ExecutableStatementCountCheck;
import com.puppycrawl.tools.checkstyle.checks.sizes.LambdaBodyLengthCheck;
import com.puppycrawl.tools.checkstyle.checks.sizes.MethodCountCheck;
import com.puppycrawl.tools.checkstyle.checks.sizes.MethodLengthCheck;
import com.puppycrawl.tools.checkstyle.checks.sizes.OuterTypeNumberCheck;
import com.puppycrawl.tools.checkstyle.checks.sizes.ParameterNumberCheck;
import com.puppycrawl.tools.checkstyle.checks.sizes.RecordComponentNumberCheck;
import com.puppycrawl.tools.checkstyle.pipeline.metrics.BooleanExpressionComplexityFilter;
import com.puppycrawl.tools.checkstyle.pipeline.metrics.ClassDataAbstractionCouplingFilter;
import com.puppycrawl.tools.checkstyle.pipeline.metrics.ClassFanOutComplexityFilter;
import com.puppycrawl.tools.checkstyle.pipeline.metrics.CyclomaticComplexityFilter;
import com.puppycrawl.tools.checkstyle.pipeline.metrics.JavaNcssFilter;
import com.puppycrawl.tools.checkstyle.pipeline.metrics.NPathComplexityFilter;
import com.puppycrawl.tools.checkstyle.pipeline.sizes.AnonInnerLengthFilter;
import com.puppycrawl.tools.checkstyle.pipeline.sizes.ExecutableStatementCountFilter;
import com.puppycrawl.tools.checkstyle.pipeline.sizes.LambdaBodyLengthFilter;
import com.puppycrawl.tools.checkstyle.pipeline.sizes.MethodCountFilter;
import com.puppycrawl.tools.checkstyle.pipeline.sizes.MethodLengthFilter;
import com.puppycrawl.tools.checkstyle.pipeline.sizes.OuterTypeNumberFilter;
import com.puppycrawl.tools.checkstyle.pipeline.sizes.ParameterNumberFilter;
import com.puppycrawl.tools.checkstyle.pipeline.sizes.RecordComponentNumberFilter;

/**
 * Orchestrates an ordered sequence of {@link CheckFilter} instances for checks
 * that must be routed exclusively through the pipeline rather than through
 * {@code TreeWalker}'s direct dispatch.
 *
 * <h3>AST pipeline</h3>
 * <p>The pipeline performs its own iterative depth-first AST walk (identical
 * to {@code TreeWalker.processIter}) and dispatches {@link CheckData} events
 * to registered {@link AbstractAstCheckFilter} instances.  A token-dispatch
 * map ensures only filters that declared interest in a token type receive
 * VISIT/LEAVE events; BEGIN_TREE and FINISH_TREE are broadcast to every AST
 * filter.</p>
 *
 * <h3>File pipeline</h3>
 * <p>A separate ordered list handles file-level filters (wrapping checks that
 * extend {@code AbstractFileSetCheck}).  A single PROCESS_FILE event is
 * dispatched to every file filter for each file.</p>
 *
 * <h3>Exclusive interception</h3>
 * <p>{@link #isPipelineManaged(AbstractCheck)} lets {@code TreeWalker} decide
 * whether a check should be added to this pipeline instead of being registered
 * via the normal token-map path.  {@link #addManagedCheck(AbstractCheck)}
 * creates the appropriate filter wrapper and builds the token-dispatch
 * entry.</p>
 */
public final class CheckPipeline {

    /**
     * The set of {@link AbstractCheck} subclasses managed exclusively by this
     * pipeline.  Any check whose runtime class is in this set will be
     * intercepted by {@code TreeWalker} and routed here instead of being
     * registered for direct dispatch.
     */
    private static final Set<Class<? extends AbstractCheck>> PIPELINE_MANAGED_CLASSES = Set.of(
        BooleanExpressionComplexityCheck.class,
        ClassDataAbstractionCouplingCheck.class,
        ClassFanOutComplexityCheck.class,
        CyclomaticComplexityCheck.class,
        JavaNCSSCheck.class,
        NPathComplexityCheck.class,
        AnonInnerLengthCheck.class,
        ExecutableStatementCountCheck.class,
        LambdaBodyLengthCheck.class,
        MethodCountCheck.class,
        MethodLengthCheck.class,
        OuterTypeNumberCheck.class,
        ParameterNumberCheck.class,
        RecordComponentNumberCheck.class
    );

    /** Ordered list of AST-based filters. */
    private final List<AbstractAstCheckFilter> astFilters = new ArrayList<>();

    /** Ordered list of file-level filters. */
    private final List<CheckFilter> fileFilters = new ArrayList<>();

    /**
     * Token-dispatch map: token type ID → filters interested in that token.
     * Built incrementally as checks are added via {@link #addManagedCheck}.
     */
    private final Map<Integer, List<AbstractAstCheckFilter>> tokenToFilters = new HashMap<>();

    /**
     * Returns {@code true} if the given check should be routed exclusively
     * through this pipeline rather than through {@code TreeWalker}'s normal
     * token-registration mechanism.
     *
     * @param check the check to test
     * @return {@code true} if the check's runtime class is pipeline-managed
     */
    public static boolean isPipelineManaged(AbstractCheck check) {
        return PIPELINE_MANAGED_CLASSES.contains(check.getClass());
    }

    /**
     * Wraps the given (already-configured) check in the appropriate
     * {@link AbstractAstCheckFilter} and registers it with the pipeline.
     * Must only be called for checks for which {@link #isPipelineManaged}
     * returns {@code true}.
     *
     * @param check the configured check to wrap
     * @throws IllegalArgumentException if no filter is registered for the check's class
     */
    public void addManagedCheck(AbstractCheck check) {
        final AbstractAstCheckFilter filter = createFilter(check);
        astFilters.add(filter);
        for (int tokenId : filter.getHandledTokens()) {
            tokenToFilters.computeIfAbsent(tokenId, k -> new ArrayList<>()).add(filter);
        }
    }

    /**
     * Adds a file-level filter to the pipeline.
     * File filters are invoked in insertion order for each PROCESS_FILE event.
     *
     * @param filter the filter to add
     */
    public void addFileFilter(CheckFilter filter) {
        fileFilters.add(filter);
    }

    /**
     * Returns {@code true} if no AST filters have been registered.
     * Used by {@code TreeWalker} to decide whether to parse the file at all.
     *
     * @return {@code true} when the AST filter list is empty
     */
    public boolean isEmpty() {
        return astFilters.isEmpty();
    }

    /**
     * Runs all registered AST filters over the given AST.
     *
     * <p>The pipeline performs its own iterative DFS walk so that registered
     * checks are invoked exclusively here — {@code TreeWalker} does not call
     * them directly.  The walk order and semantics are identical to
     * {@code TreeWalker.processIter}.</p>
     *
     * @param rootAst  the root of the parsed AST
     * @param contents the file contents (needed for line/column reporting)
     * @return all violations collected from every AST filter
     */
    public SortedSet<Violation> processAst(DetailAST rootAst, FileContents contents) {
        final CheckData beginData = CheckData.forBeginTree(rootAst, contents);
        for (AbstractAstCheckFilter filter : astFilters) {
            filter.process(beginData);
        }

        walkAst(rootAst, contents);

        final CheckData finishData = CheckData.forFinishTree(rootAst, contents);
        for (AbstractAstCheckFilter filter : astFilters) {
            filter.process(finishData);
        }

        return new TreeSet<>(finishData.getViolations());
    }

    /**
     * Runs all registered file-level filters for a single file.
     *
     * @param file     the file being checked
     * @param fileText the raw file text
     * @param contents the file contents wrapper
     * @return all violations collected from every file filter
     */
    public SortedSet<Violation> processFile(File file, FileText fileText,
                                             FileContents contents) {
        final CheckData data = CheckData.forProcessFile(file, fileText, contents);
        for (CheckFilter filter : fileFilters) {
            filter.process(data);
        }
        return new TreeSet<>(data.getViolations());
    }

    /**
     * Destroys all registered AST and file filters, releasing their
     * {@code ThreadLocal} contexts.
     */
    public void destroy() {
        astFilters.forEach(AbstractAstCheckFilter::destroy);
        fileFilters.forEach(CheckFilter::destroy);
    }

    /**
     * Iterative depth-first AST walk, mirroring {@code TreeWalker.processIter}.
     * Dispatches VISIT and LEAVE events only to filters registered for each token type.
     *
     * @param root     the root AST node
     * @param contents the file contents
     */
    private void walkAst(DetailAST root, FileContents contents) {
        DetailAST curNode = root;
        while (curNode != null) {
            notifyVisit(curNode, contents);
            DetailAST toVisit = curNode.getFirstChild();
            while (curNode != null && toVisit == null) {
                notifyLeave(curNode, contents);
                toVisit = curNode.getNextSibling();
                curNode = curNode.getParent();
            }
            curNode = toVisit;
        }
    }

    private void notifyVisit(DetailAST ast, FileContents contents) {
        final List<AbstractAstCheckFilter> filters = tokenToFilters.get(ast.getType());
        if (filters != null) {
            final CheckData data = CheckData.forVisitToken(ast, contents);
            for (AbstractAstCheckFilter filter : filters) {
                filter.process(data);
            }
        }
    }

    private void notifyLeave(DetailAST ast, FileContents contents) {
        final List<AbstractAstCheckFilter> filters = tokenToFilters.get(ast.getType());
        if (filters != null) {
            final CheckData data = CheckData.forLeaveToken(ast, contents);
            for (AbstractAstCheckFilter filter : filters) {
                filter.process(data);
            }
        }
    }

    /**
     * Factory method: maps a configured check to its concrete filter wrapper.
     *
     * @param check the check to wrap
     * @return the corresponding filter
     * @throws IllegalArgumentException if the check class has no registered filter
     * @noinspection ChainOfInstanceofChecks
     * @noinspectionreason ChainOfInstanceofChecks - required to dispatch to concrete filter types
     */
    private static AbstractAstCheckFilter createFilter(AbstractCheck check) {
        if (check instanceof BooleanExpressionComplexityCheck cast) {
            return new BooleanExpressionComplexityFilter(cast);
        }
        if (check instanceof ClassDataAbstractionCouplingCheck cast) {
            return new ClassDataAbstractionCouplingFilter(cast);
        }
        if (check instanceof ClassFanOutComplexityCheck cast) {
            return new ClassFanOutComplexityFilter(cast);
        }
        if (check instanceof CyclomaticComplexityCheck cast) {
            return new CyclomaticComplexityFilter(cast);
        }
        if (check instanceof JavaNCSSCheck cast) {
            return new JavaNcssFilter(cast);
        }
        if (check instanceof NPathComplexityCheck cast) {
            return new NPathComplexityFilter(cast);
        }
        if (check instanceof AnonInnerLengthCheck cast) {
            return new AnonInnerLengthFilter(cast);
        }
        if (check instanceof ExecutableStatementCountCheck cast) {
            return new ExecutableStatementCountFilter(cast);
        }
        if (check instanceof LambdaBodyLengthCheck cast) {
            return new LambdaBodyLengthFilter(cast);
        }
        if (check instanceof MethodCountCheck cast) {
            return new MethodCountFilter(cast);
        }
        if (check instanceof MethodLengthCheck cast) {
            return new MethodLengthFilter(cast);
        }
        if (check instanceof OuterTypeNumberCheck cast) {
            return new OuterTypeNumberFilter(cast);
        }
        if (check instanceof ParameterNumberCheck cast) {
            return new ParameterNumberFilter(cast);
        }
        if (check instanceof RecordComponentNumberCheck cast) {
            return new RecordComponentNumberFilter(cast);
        }
        throw new IllegalArgumentException(
            "No pipeline filter registered for: " + check.getClass().getName());
    }

}
