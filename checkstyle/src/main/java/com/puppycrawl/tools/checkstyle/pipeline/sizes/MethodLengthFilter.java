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

package com.puppycrawl.tools.checkstyle.pipeline.sizes;

import com.puppycrawl.tools.checkstyle.checks.sizes.MethodLengthCheck;
import com.puppycrawl.tools.checkstyle.pipeline.AbstractAstCheckFilter;

/**
 * Pipeline filter that routes {@link MethodLengthCheck} exclusively through
 * the check pipeline.  All check logic remains in the original class; this
 * wrapper only handles lifecycle delegation.
 */
public final class MethodLengthFilter extends AbstractAstCheckFilter {

    /**
     * Creates a filter wrapping the given, already-configured check.
     *
     * @param check the configured check to wrap
     */
    public MethodLengthFilter(MethodLengthCheck check) {
        super(check);
    }

    @Override
    public String getName() {
        return "MethodLength";
    }

}
