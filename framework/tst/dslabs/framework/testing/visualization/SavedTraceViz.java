/*
 * Copyright (c) 2022 Ellis Michael (emichael@cs.washington.edu)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package dslabs.framework.testing.visualization;

import dslabs.framework.testing.StatePredicate;
import dslabs.framework.testing.search.SearchSettings;
import dslabs.framework.testing.search.SearchState;
import dslabs.framework.testing.search.SerializableTrace;
import java.io.IOException;

public class SavedTraceViz {
    public static void main(String[] args) throws IOException {
        assert args.length > 0;

        SerializableTrace trace = SerializableTrace.loadTrace(args[0]);
        if (trace == null) {
            System.err.println("Could not start visual debugger.");
            System.exit(1);
        }

        SearchState endState = trace.endState();

        if (endState == null) {
            System.err.println(
                    "Trace no longer fully replays. Could not start visual debugger.");
            System.exit(1);
        }

        SearchSettings settings = new SearchSettings();
        for (StatePredicate invariant : trace.invariants()) {
            settings.addInvariant(invariant);
        }

        new DebuggerWindow(endState, settings);
    }
}