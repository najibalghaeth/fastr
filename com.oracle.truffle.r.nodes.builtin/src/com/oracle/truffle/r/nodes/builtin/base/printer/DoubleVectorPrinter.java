/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1997-2013,  The R Core Team
 * Copyright (c) 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base.printer;

import static com.oracle.truffle.r.nodes.builtin.base.printer.Utils.snprintf;

import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;

import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;

//Transcribed from GnuR, src/main/format.c

final class DoubleVectorPrinter extends VectorPrinter<RAbstractDoubleVector> {

    static final DoubleVectorPrinter INSTANCE = new DoubleVectorPrinter();

    private DoubleVectorPrinter() {
        // singleton
    }

    @Override
    protected VectorPrinter<RAbstractDoubleVector>.VectorPrintJob createJob(RAbstractDoubleVector vector, int indx, PrintContext printCtx) {
        return new DoubleVectorPrintJob(vector, indx, printCtx);
    }

    private final class DoubleVectorPrintJob extends VectorPrintJob {

        protected DoubleVectorPrintJob(RAbstractDoubleVector vector, int indx, PrintContext printCtx) {
            super(vector, indx, printCtx);
        }

        @Override
        protected DoubleVectorMetrics formatVector(int offs, int len) {
            return formatDoubleVector(vector, offs, len, 0, printCtx.parameters());
        }

        @Override
        protected void printElement(int i, FormatMetrics fm) throws IOException {
            DoubleVectorMetrics dfm = (DoubleVectorMetrics) fm;
            String v = encodeReal(vector.getDataAt(i), dfm.maxWidth, dfm.d, dfm.e, '.', printCtx.parameters());
            out.print(v);
        }

        @Override
        protected void printCell(int i, FormatMetrics fm) throws IOException {
            printElement(i, fm);
        }

        @Override
        protected void printEmptyVector() throws IOException {
            out.print("numeric(0)");
        }

        @Override
        protected String elementTypeName() {
            return "double";
        }
    }

    static final class DoubleVectorMetrics extends FormatMetrics {
        public final int d;
        public final int e;

        private DoubleVectorMetrics(int w, int d, int e) {
            super(w);
            this.d = d;
            this.e = e;
        }
    }

    static DoubleVectorMetrics formatDoubleVector(RAbstractDoubleVector x, int offs, int n, int nsmall, PrintParameters pp) {
        int left;
        int right;
        int sleft;
        int mnl;
        int mxl;
        int rgt;
        int mxsl;
        int mxns;
        int wF;
        int neg;
        int sgn;
        int kpower;
        int nsig;
        boolean roundingwidens;
        boolean naflag;
        boolean nanflag;
        boolean posinf;
        boolean neginf;

        // output arguments
        int w;
        int d;
        int e;

        nanflag = false;
        naflag = false;
        posinf = false;
        neginf = false;
        neg = 0;
        rgt = mxl = mxsl = mxns = RRuntime.INT_MIN_VALUE;
        mnl = RRuntime.INT_MAX_VALUE;

        for (int i = 0; i < n; i++) {
            double xi = x.getDataAt(i + offs);
            if (!RRuntime.isFinite(xi)) {
                if (RRuntime.isNA(xi)) {
                    naflag = true;
                } else if (RRuntime.isNAorNaN(xi)) {
                    nanflag = true;
                } else if (xi > 0) {
                    posinf = true;
                } else {
                    neginf = true;
                }
            } else {
                ScientificDouble sd = scientific(xi, pp);
                sgn = sd.sgn;
                nsig = sd.nsig;
                kpower = sd.kpower;
                roundingwidens = sd.roundingwidens;

                left = kpower + 1;
                if (roundingwidens) {
                    left--;
                }

                sleft = sgn + ((left <= 0) ? 1 : left); /* >= 1 */
                right = nsig - left; /* #{digits} right of '.' ( > 0 often) */
                if (sgn > 0) {
                    neg = 1; /* if any < 0, need extra space for sign */
                }

                /* Infinite precision "F" Format : */
                if (right > rgt) {
                    rgt = right; /* max digits to right of . */
                }
                if (left > mxl) {
                    mxl = left; /* max digits to left of . */
                }
                if (left < mnl) {
                    mnl = left; /* min digits to left of . */
                }
                if (sleft > mxsl) {
                    mxsl = sleft; /*
                                   * max left includingimport static
                                   * com.oracle.truffle.r.nodes.builtin.base.printer.Utils.*;
                                   * sign(s)
                                   */
                }
                if (nsig > mxns) {
                    mxns = nsig; /* max sig digits */
                }
            }
        }
        /*
         * F Format: use "F" format WHENEVER we use not more space than 'E' and still satisfy
         * 'R_print.digits' {but as if nsmall==0 !}
         *
         * E Format has the form [S]X[.XXX]E+XX[X]
         *
         * This is indicated by setting *e to non-zero (usually 1) If the additional exponent digit
         * is required *e is set to 2
         */

        /*-- These 'mxsl' & 'rgt' are used in F Format
         * AND in the ____ if(.) "F" else "E" ___ below: */
        if (pp.getDigits() == 0) {
            rgt = 0;
        }
        if (mxl < 0) {
            mxsl = 1 + neg; /* we use %#w.dg, so have leading zero */
        }

        /* use nsmall only *after* comparing "F" vs "E": */
        if (rgt < 0) {
            rgt = 0;
        }
        wF = mxsl + rgt + (rgt != 0 ? 1 : 0); /* width for F format */

        /*-- 'see' how "E" Exponential format would be like : */
        e = (mxl > 100 || mnl <= -99) ? 2 : 1; /* 3 digit exponent */
        if (mxns != RRuntime.INT_MIN_VALUE) {
            d = mxns - 1;
            w = neg + (d != 0 ? 1 : 1) + d + 4 + e; /* width for E format */
            if (wF <= w + pp.getScipen()) { /* Fixpoint if it needs less space */
                e = 0;
                if (nsmall > rgt) {
                    rgt = nsmall;
                    wF = mxsl + rgt + (rgt != 0 ? 1 : 0);
                }
                d = rgt;
                w = wF;
            } /* else : "E" Exponential format -- all done above */
        } else { /* when all x[i] are non-finite */
            w = 0; /* to be increased */
            d = 0;
            e = 0;
        }
        if (naflag && w < pp.getNaWidth()) {
            w = pp.getNaWidth();
        }
        if (nanflag && w < 3) {
            w = 3;
        }
        if (posinf && w < 3) {
            w = 3;
        }
        if (neginf && w < 4) {
            w = 4;
        }

        return new DoubleVectorMetrics(w, d, e);
    }

    private static final int DBL_DIG = 15;

    private static final double[] tbl = {
                    1e-1,
                    1e00, 1e01, 1e02, 1e03, 1e04, 1e05, 1e06, 1e07, 1e08, 1e09,
                    1e10, 1e11, 1e12, 1e13, 1e14, 1e15, 1e16, 1e17, 1e18, 1e19,
                    1e20, 1e21, 1e22
    };
    private static final int KP_MAX = 22;
    private static final int R_dec_min_exponent = -308;

    public static final int NB = 1000;

    static final class ScientificDouble {
        public final int sgn;
        public final int kpower;
        public final int nsig;
        public final boolean roundingwidens;

        ScientificDouble(int sgn, int kpower, int nsig, boolean roundingwidens) {
            super();
            this.sgn = sgn;
            this.kpower = kpower;
            this.nsig = nsig;
            this.roundingwidens = roundingwidens;
        }
    }

    public static ScientificDouble scientific(double x, PrintParameters pp) {
        /*
         * for a number x , determine sgn = 1_{x < 0} {0/1} kpower = Exponent of 10; nsig =
         * min(R_print.digits, #{significant digits of alpha}) roundingwidens = 1 if rounding causes
         * x to increase in width, 0 otherwise
         *
         * where |x| = alpha * 10^kpower and 1 <= alpha < 10
         */
        double alpha;
        double r;
        int kp;
        int j;

        // output arguments
        int sgn;
        int kpower;
        int nsig;
        boolean roundingwidens;

        if (x == 0.0) {
            kpower = 0;
            nsig = 1;
            sgn = 0;
            roundingwidens = false;
            r = 0.0;
        } else {
            if (x < 0.0) {
                sgn = 1;
                r = -x;
            } else {
                sgn = 0;
                r = x;
            }

            if (pp.getDigits() >= DBL_DIG + 1) {
                // TODO:
                // format_via_sprintf(r, pp.getDigits(), kpower, nsig);
                roundingwidens = false;
                // return;
                throw new UnsupportedOperationException();
            }

            kp = (int) Math.floor(Math.log10(r)) - pp.getDigits() + 1; // r = |x|;
                                                                       // 10^(kp + digits - 1) <= r

            double rPrec = r;
            /* use exact scaling factor in double precision, if possible */
            if (Math.abs(kp) <= 22) {
                if (kp >= 0) {
                    rPrec /= tbl[kp + 1];
                } else {
                    rPrec *= tbl[-kp + 1];
                }
            } else if (kp <= R_dec_min_exponent) {
                /*
                 * on IEEE 1e-308 is not representable except by gradual underflow. Shifting by 303
                 * allows for any potential denormalized numbers x, and makes the reasonable
                 * assumption that R_dec_min_exponent+303 is in range. Representation of 1e+303 has
                 * low error.
                 */
                rPrec = (rPrec * 1e+303) / Math.pow(10, kp + 303);
            } else {
                rPrec /= Math.pow(10, kp);
            }
            if (rPrec < tbl[pp.getDigits()]) {
                rPrec *= 10.0;
                kp--;
            }
            /* round alpha to integer, 10^(digits-1) <= alpha <= 10^digits */
            /*
             * accuracy limited by double rounding problem, alpha already rounded to 53 bits
             */
            alpha = Math.round(rPrec);

            nsig = pp.getDigits();
            for (j = 1; j <= pp.getDigits(); j++) {
                alpha /= 10.0;
                if (alpha == Math.floor(alpha)) {
                    nsig--;
                } else {
                    break;
                }
            }
            if (nsig == 0 && pp.getDigits() > 0) {
                nsig = 1;
                kp += 1;
            }
            kpower = kp + pp.getDigits() - 1;

            /*
             * Scientific format may do more rounding than fixed format, e.g. 9996 with 3 digits is
             * 1e+04 in scientific, but 9996 in fixed. This happens when the true value r is less
             * than 10^(kpower+1) and would not round up to it in fixed format. Here rgt is the
             * decimal place that will be cut off by rounding
             */

            int rgt = pp.getDigits() - kpower;
            /* bound rgt by 0 and KP_MAX */
            rgt = rgt < 0 ? 0 : rgt > KP_MAX ? KP_MAX : rgt;
            double fuzz = 0.5 / tbl[1 + rgt];
            // kpower can be bigger than the table.
            roundingwidens = kpower > 0 && kpower <= KP_MAX && r < tbl[kpower + 1] - fuzz;

        }

        return new ScientificDouble(sgn, kpower, nsig, roundingwidens);
    }

    static String encodeReal(double x, DoubleVectorMetrics dm, PrintParameters pp) {
        return encodeReal(x, dm.maxWidth, dm.d, dm.e, '.', pp);
    }

    static String encodeReal(double initialX, int w, int d, int e, char cdec, PrintParameters pp) {
        final String buff;
        String fmt;

        /* IEEE allows signed zeros (yuck!) */
        double x = initialX;
        if (x == 0.0) {
            x = 0.0;
        }
        if (!RRuntime.isFinite(x)) {
            int numBlanks = Math.min(w, (NB - 1));
            String naFmt = "%" + Utils.asBlankArg(numBlanks) + "s";
            if (RRuntime.isNA(x)) {
                buff = snprintf(NB, naFmt, pp.getNaString());
            } else if (RRuntime.isNAorNaN(x)) {
                buff = snprintf(NB, naFmt, "NaN");
            } else if (x > 0) {
                buff = snprintf(NB, naFmt, "Inf");
            } else {
                buff = snprintf(NB, naFmt, "-Inf");
            }
        } else if (e != 0) {
            if (d != 0) {
                fmt = String.format("%%#%d.%de", Math.min(w, (NB - 1)), d);
                buff = snprintf(NB, fmt, x);
            } else {
                fmt = String.format("%%%d.%de", Math.min(w, (NB - 1)), d);
                buff = snprintf(NB, fmt, x);
            }
        } else { /* e = 0 */
            StringBuilder sb = new StringBuilder("#.#");
            DecimalFormat df = new DecimalFormat(sb.toString());
            df.setRoundingMode(RoundingMode.HALF_EVEN);
            df.setDecimalSeparatorAlwaysShown(false);
            df.setMinimumFractionDigits(d);
            df.setMaximumFractionDigits(d);
            String ds = df.format(x);
            int blanks = w - ds.length();
            fmt = "%" + Utils.asBlankArg(blanks) + "s%s";
            buff = String.format(fmt, "", ds);
        }

        if (cdec != '.') {
            buff.replace('.', cdec);
        }

        return buff;
    }
}
