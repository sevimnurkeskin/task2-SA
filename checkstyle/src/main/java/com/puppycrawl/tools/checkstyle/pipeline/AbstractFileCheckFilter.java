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

import com.puppycrawl.tools.checkstyle.api.AbstractFileSetCheck;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;

/**
 * Base class for pipeline filters that wrap an {@link AbstractFileSetCheck}.
 *
 * <p>Responds only to {@link CheckData.EventType#PROCESS_FILE} events by
 * delegating to {@link AbstractFileSetCheck#process(java.io.File,
 * com.puppycrawl.tools.checkstyle.api.FileText)} and appending the returned
 * violations into the event data. All other event types are passed through
 * unchanged.</p>
 */
public abstract class AbstractFileCheckFilter implements CheckFilter {

    private final AbstractFileSetCheck delegate;

    /**
     * Creates a new filter wrapping the given file-set check.
     *
     * @param delegate the check to wrap; must not be {@code null}
     */
    protected AbstractFileCheckFilter(AbstractFileSetCheck delegate) {
        this.delegate = delegate;
    }

    /**
     * {@inheritDoc}
     *
     * <p>On PROCESS_FILE events, calls the delegate's {@code process(File, FileText)}
     * and accumulates the returned violations into {@code data}.
     * Any other event type is returned immediately without processing.</p>
     *
     * @throws IllegalStateException if the delegate throws a {@link CheckstyleException}
     */
    @Override
    public CheckData process(CheckData data) {
        if (data.getEventType() == CheckData.EventType.PROCESS_FILE) {
            try {
                data.addViolations(delegate.process(data.getFile(), data.getFileText()));
            }
            catch (CheckstyleException ex) {
                throw new IllegalStateException(
                    "Pipeline filter '" + getName() + "' failed: " + ex.getMessage(), ex);
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
     * Returns the wrapped file-set check instance.
     *
     * @return the delegate check (never {@code null})
     */
    protected AbstractFileSetCheck getDelegate() {
        return delegate;
    }

}
