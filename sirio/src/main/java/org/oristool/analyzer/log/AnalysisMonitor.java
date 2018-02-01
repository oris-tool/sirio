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

package org.oristool.analyzer.log;

/**
 * Generic monitor interface for the analysis engines.
 */
public interface AnalysisMonitor {

    /**
     * Notifies a message to the user.
     *  
     * @param message input message
     */
    void notifyMessage(String message);
    
    /**
     * Notifies an interruption request from the user.
     * 
     * @return true if the user requested to stop the analysis 
     */
    boolean interruptRequested();
}
