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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.utils.TokenUtil;

/**
 * Base class for pipeline filters that wrap an {@link AbstractCheck}.
 *
 * <p>Handles the full AST event lifecycle by delegating to the wrapped check:</p>
 * <ul>
 *   <li>BEGIN_TREE  → {@code setFileContents}, {@code clearViolations}, {@code beginTree}</li>
 *   <li>VISIT_TOKEN → {@code visitToken} (only for tokens the check registered for)</li>
 *   <li>LEAVE_TOKEN → {@code leaveToken} (only for tokens the check registered for)</li>
 *   <li>FINISH_TREE → {@code finishTree}, then drains violations into the event data</li>
 * </ul>
 *
 * <p>The set of handled tokens replicates the logic used by {@code TreeWalker}:
 * if the check has no explicit token configuration, {@code getDefaultTokens()} is used;
 * otherwise {@code getRequiredTokens()} plus the configured token names are used.</p>
 *
 * <p>PROCESS_FILE events are silently ignored by this base class.</p>
 */
public abstract class AbstractAstCheckFilter implements CheckFilter {

    private final AbstractCheck delegate;
    private final Set<Integer> handledTokens;

    /**
     * Creates a new filter wrapping the given check.
     * The check must already be fully configured (tokens set via {@code configure()})
     * before this constructor is called.
     *
     * @param delegate the check to wrap; must not be {@code null}
     */
    protected AbstractAstCheckFilter(AbstractCheck delegate) {
        this.delegate = delegate;
        this.handledTokens = computeHandledTokens(delegate);
    }

    /**
     * Computes the set of token IDs this filter will forward to the delegate,
     * mirroring the registration logic of {@code TreeWalker.registerCheck}.
     *
     * @param check the configured check
     * @return unmodifiable set of integer token IDs
     */
    private static Set<Integer> computeHandledTokens(AbstractCheck check) {
        final Set<Integer> result = new HashSet<>();
        final Set<String> configuredTokens = check.getTokenNames();
        if (configuredTokens.isEmpty()) {
            for (int tokenId : check.getDefaultTokens()) {
                result.add(tokenId);
            }
        }
        else {
            // Required tokens are always registered, plus any explicitly configured ones.
            for (int tokenId : check.getRequiredTokens()) {
                result.add(tokenId);
            }
            for (String tokenName : configuredTokens) {
                result.add(TokenUtil.getTokenId(tokenName));
            }
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * Returns the set of token type IDs that this filter forwards to its delegate.
     * Used by {@link CheckPipeline} to build the token-dispatch map.
     *
     * @return unmodifiable set of token IDs
     */
    public Set<Integer> getHandledTokens() {
        return handledTokens;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates each AST lifecycle event to the wrapped check.
     * VISIT_TOKEN and LEAVE_TOKEN events are forwarded only when the node's
     * token type is in {@link #getHandledTokens()}.</p>
     */
    @Override
    public CheckData process(CheckData data) {
        switch (data.getEventType()) {
            case BEGIN_TREE -> {
                delegate.setFileContents(data.getFileContents());
                delegate.clearViolations();
                delegate.beginTree(data.getAst());
            }
            case VISIT_TOKEN -> {
                if (handledTokens.contains(data.getAst().getType())) {
                    delegate.visitToken(data.getAst());
                }
            }
            case LEAVE_TOKEN -> {
                if (handledTokens.contains(data.getAst().getType())) {
                    delegate.leaveToken(data.getAst());
                }
            }
            case FINISH_TREE -> {
                delegate.finishTree(data.getAst());
                data.addViolations(delegate.getViolations());
            }
            default -> {
                // PROCESS_FILE: not applicable to AST-based filters
            }
        }
        return data;
    }

    /**
     * {@inheritDoc}
     * Calls {@code destroy()} on the wrapped check to release its ThreadLocal context.
     */
    @Override
    public void destroy() {
        delegate.destroy();
    }

    /**
     * Returns the wrapped check instance.
     *
     * @return the delegate check (never {@code null})
     */
    protected AbstractCheck getDelegate() {
        return delegate;
    }

}
