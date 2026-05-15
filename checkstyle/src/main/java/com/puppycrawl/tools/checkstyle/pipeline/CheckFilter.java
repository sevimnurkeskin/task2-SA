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

/**
 * Represents a single processing stage in the check pipeline.
 *
 * <p>Each filter receives a {@link CheckData} event, optionally acts on it
 * (e.g. by delegating to an underlying check), and returns the same data
 * object so the next filter in the chain can run.  Returning {@code null}
 * short-circuits the pipeline for that event.</p>
 */
public interface CheckFilter {

    /**
     * Processes one pipeline event.
     *
     * @param data the event data carrying AST node, file context, and violations
     * @return the data (possibly with violations added), or {@code null} to halt processing
     */
    CheckData process(CheckData data);

    /**
     * Returns a human-readable name for this filter used in diagnostics.
     *
     * @return the filter name
     */
    String getName();

    /**
     * Releases any resources held by this filter.
     * Called once when the containing pipeline is destroyed.
     * The default implementation is a no-op.
     */
    default void destroy() {
        // no-op by default
    }

}
