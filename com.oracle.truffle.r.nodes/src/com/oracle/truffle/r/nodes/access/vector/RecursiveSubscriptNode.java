/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.access.vector;

import com.oracle.truffle.r.nodes.control.RLengthNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

abstract class RecursiveSubscriptNode extends RBaseNode {

    protected final Class<?> vectorClass;
    protected final Class<?> positionClass;

    @Child protected RLengthNode positionLengthNode = RLengthNode.create();

    RecursiveSubscriptNode(RAbstractListVector vector, Object position) {
        this.vectorClass = vector.getClass();
        this.positionClass = position.getClass();
    }

    public final boolean isSupported(Object vector, Object[] positions) {
        if (vector.getClass() == vectorClass && positions.length == 1 && positions[0].getClass() == positionClass) {
            return true;
        }
        return false;
    }

    protected final RError indexingFailed(int i) {
        throw error(RError.Message.RECURSIVE_INDEXING_FAILED, i);
    }

    protected final RError noSuchIndex(int i) {
        throw error(RError.Message.NO_SUCH_INDEX, i);
    }
}
