/* This program is part of the ORIS Tool.
 * Copyright (C) 2011-2018 The ORIS Authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.oristool.analyzer.stop;

import org.oristool.analyzer.AnalyzerObserverAdapter;
import org.oristool.analyzer.Succession;
import org.oristool.analyzer.log.AnalysisMonitor;

/**
 * A stop criterion controlled by an external {@code AnalysisMonitor}.
 */
public class MonitorStopCriterion extends AnalyzerObserverAdapter implements
        StopCriterion {

    private final AnalysisMonitor monitor;
    private boolean interruptedExecution;
    private int enumeratedClasses;

    public MonitorStopCriterion(AnalysisMonitor monitor) {
        this.monitor = monitor;
        this.interruptedExecution = false;
    }

    @Override
    public boolean stop() {
        if (monitor.interruptRequested()) {
            this.interruptedExecution = true;
            return true;

        } else {
            return false;
        }
    }

    @Override
    public void notifyNodeAdded(Succession succession) {
        enumeratedClasses++;
        monitor.notifyMessage("Analysis: " + enumeratedClasses
                + " classes enumerated...");
    }

    public boolean interruptedExecution() {
        return this.interruptedExecution;
    }

    public int enumeratedClasses() {
        return enumeratedClasses;
    }
}