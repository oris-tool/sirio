/* This program is part of the ORIS Tool.
 * Copyright (C) 2011-2021 The ORIS Authors.
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

package org.oristool.lello.parse;

import java.util.List;

import org.oristool.lello.exception.ParsingException;

/**
 * An list-based implementation of TerminalStream.
 */
public class ListTerminalStream implements TerminalStream {

    /** Index of the current symbol in the list. */
    private int index;

    /** Ordered list of symbols. */
    private List<Terminal> terminals;

    /**
     * Initializes a stream of symbols based on a list; the symbols will be
     * received from the stream in the same order they appear in the list.
     *
     * @param terminals
     *            The list of symbols.
     */
    public ListTerminalStream(List<Terminal> terminals) {
        if (terminals == null)
            throw new NullPointerException(
                    "Argument terminals can not be null.");

        index = 0;
        this.terminals = terminals;
    }

    @Override
    public Terminal getCurrent() {
        if (index >= terminals.size())
            throw new ParsingException(
                    "No more terminal symbols in the stream.");

        return terminals.get(index);
    }

    @Override
    public void moveNext() {
        if (index >= terminals.size())
            throw new ParsingException(
                    "No more terminal symbols in the stream.");

        ++index;
    }

    @Override
    public boolean hasNext() {
        return index < terminals.size();
    }
}
