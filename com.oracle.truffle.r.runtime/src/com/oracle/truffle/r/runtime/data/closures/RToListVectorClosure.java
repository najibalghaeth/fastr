/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data.closures;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleSequence;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntSequence;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RStringSequence;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

abstract class RToListVectorClosure extends RToVectorClosure implements RAbstractListVector {

    protected RToListVectorClosure(boolean keepAttributes) {
        super(keepAttributes);
    }

    @Override
    public Object getDataAtAsObject(int index) {
        return getDataAt(index);
    }

    @Override
    public RList materialize() {
        int length = getLength();
        Object[] result = new Object[length];
        for (int i = 0; i < length; i++) {
            Object data = getDataAt(i);
            result[i] = data;
        }
        RList materialized = RDataFactory.createList(result);
        copyAttributes(materialized);
        return materialized;
    }

    @TruffleBoundary
    private void copyAttributes(RList materialized) {
        if (keepAttributes) {
            materialized.copyAttributesFrom(getVector());
        }
    }

    @Override
    public RAbstractVector copyWithNewDimensions(int[] newDimensions) {
        if (!keepAttributes) {
            return materialize().copyWithNewDimensions(newDimensions);
        }
        return this;
    }

    @Override
    public RVector<?> createEmptySameType(int newLength, boolean newIsComplete) {
        return RDataFactory.createList(new Object[newLength]);
    }
}

final class RLogicalToListVectorClosure extends RToListVectorClosure {

    private final RLogicalVector vector;

    RLogicalToListVectorClosure(RLogicalVector vector, boolean keepAttributes) {
        super(keepAttributes);
        this.vector = vector;
    }

    @Override
    public RLogicalVector getVector() {
        return vector;
    }

    @Override
    public Byte getDataAt(int index) {
        return vector.getDataAt(index);
    }
}

final class RIntToListVectorClosure extends RToListVectorClosure {

    private final RIntVector vector;

    RIntToListVectorClosure(RIntVector vector, boolean keepAttributes) {
        super(keepAttributes);
        this.vector = vector;
    }

    @Override
    public RIntVector getVector() {
        return vector;
    }

    @Override
    public Integer getDataAt(int index) {
        return vector.getDataAt(index);
    }
}

final class RIntSequenceToListVectorClosure extends RToListVectorClosure {

    private final RIntSequence vector;

    RIntSequenceToListVectorClosure(RIntSequence vector, boolean keepAttributes) {
        super(keepAttributes);
        this.vector = vector;
    }

    @Override
    public RIntSequence getVector() {
        return vector;
    }

    @Override
    public Integer getDataAt(int index) {
        return vector.getDataAt(index);
    }
}

final class RDoubleToListVectorClosure extends RToListVectorClosure {

    private final RDoubleVector vector;

    RDoubleToListVectorClosure(RDoubleVector vector, boolean keepAttributes) {
        super(keepAttributes);
        this.vector = vector;
    }

    @Override
    public RDoubleVector getVector() {
        return vector;
    }

    @Override
    public Double getDataAt(int index) {
        return vector.getDataAt(index);
    }
}

final class RDoubleSequenceToListVectorClosure extends RToListVectorClosure {

    private final RDoubleSequence vector;

    RDoubleSequenceToListVectorClosure(RDoubleSequence vector, boolean keepAttributes) {
        super(keepAttributes);
        this.vector = vector;
    }

    @Override
    public RDoubleSequence getVector() {
        return vector;
    }

    @Override
    public Double getDataAt(int index) {
        return vector.getDataAt(index);
    }
}

final class RComplexToListVectorClosure extends RToListVectorClosure {

    private final RComplexVector vector;

    RComplexToListVectorClosure(RComplexVector vector, boolean keepAttributes) {
        super(keepAttributes);
        this.vector = vector;
    }

    @Override
    public RComplexVector getVector() {
        return vector;
    }

    @Override
    public RComplex getDataAt(int index) {
        return vector.getDataAt(index);
    }
}

final class RStringToListVectorClosure extends RToListVectorClosure {

    private final RStringVector vector;

    RStringToListVectorClosure(RStringVector vector, boolean keepAttributes) {
        super(keepAttributes);
        this.vector = vector;
    }

    @Override
    public RStringVector getVector() {
        return vector;
    }

    @Override
    public String getDataAt(int index) {
        return vector.getDataAt(index);
    }
}

final class RStringSequenceToListVectorClosure extends RToListVectorClosure {

    private final RStringSequence vector;

    RStringSequenceToListVectorClosure(RStringSequence vector, boolean keepAttributes) {
        super(keepAttributes);
        this.vector = vector;
    }

    @Override
    public RStringSequence getVector() {
        return vector;
    }

    @Override
    public String getDataAt(int index) {
        return vector.getDataAt(index);
    }
}

final class RRawToListVectorClosure extends RToListVectorClosure {

    private final RRawVector vector;

    RRawToListVectorClosure(RRawVector vector, boolean keepAttributes) {
        super(keepAttributes);
        this.vector = vector;
    }

    @Override
    public RRawVector getVector() {
        return vector;
    }

    @Override
    public Object getDataAt(int index) {
        return RRaw.valueOf(vector.getRawDataAt(index));
    }
}
